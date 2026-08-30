package qikahome.jsonmore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;

import static qikahome.jsonmore.JsonMore.LOGGER;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class Utils {
    /**
     * 构造用于解析 SlotDisplay 的 ContextMap（包含 REGISTRIES）。
     * 1.21.2 中 TagSlotDisplay 需要 registry 上下文才能解析 tag，
     * 传空 ContextMap 会导致 tag 类 ingredient 在配方预览中显示为空。
     * 服务器/客户端 level 都不可用（如启动早期 JEI 构建预览）时，
     * 退回用静态的 BuiltInRegistries 兜底，保证原版 tag 也能解析。
     */
    public static ContextMap displayContext() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return new ContextMap.Builder()
                    .withParameter(SlotDisplayContext.REGISTRIES, server.registryAccess())
                    .create(SlotDisplayContext.CONTEXT);
        }
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level != null) {
            return new ContextMap.Builder()
                    .withParameter(SlotDisplayContext.REGISTRIES, minecraft.level.registryAccess())
                    .create(SlotDisplayContext.CONTEXT);
        }
        return new ContextMap.Builder()
                .withParameter(SlotDisplayContext.REGISTRIES,
                        net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(
                                net.minecraft.core.registries.BuiltInRegistries.REGISTRY))
                .create(SlotDisplayContext.CONTEXT);
    }

    /**
     * 执行Supplier类型的Lambda，捕获异常并返回默认值
     * 
     * @param supplier     要执行的Lambda（无参，返回T类型）
     * @param defaultValue 异常时返回的默认值
     * @param logLevel     日志级别（null时不打印日志）
     * @param errorMessage 异常时的日志消息（null时使用默认消息）
     * 
     * @return Lambda执行结果（正常）/ 默认值（异常）
     * @param <T> 返回值泛型类型
     */
    public static <T> T getOrElse(Supplier<T> supplier, T defaultValue, @Nullable org.slf4j.event.Level logLevel,
            @Nullable String errorMessage) {
        try {
            return supplier.get();
        } catch (Exception e) {
            if (logLevel != null)
                switch (logLevel) {
                    case TRACE:
                        LOGGER.trace(errorMessage != null ? errorMessage : "Error while getting value from supplier",
                                e);
                        break;
                    case DEBUG:
                        LOGGER.debug(errorMessage != null ? errorMessage : "Error while getting value from supplier",
                                e);
                        break;
                    case INFO:
                        LOGGER.info(errorMessage != null ? errorMessage : "Error while getting value from supplier", e);
                        break;
                    case WARN:
                        LOGGER.warn(errorMessage != null ? errorMessage : "Error while getting value from supplier", e);
                        break;
                    case ERROR:
                        LOGGER.error(errorMessage != null ? errorMessage : "Error while getting value from supplier",
                                e);
                        break;
                    default:
                        LOGGER.error(errorMessage != null ? errorMessage : "Error while getting value from supplier",
                                e);
                        break;
                }
            return defaultValue;
        }
    }

    /**
     * 创建枚举的 Codec，区分大小写。
     */
    public static <E extends Enum<E>> Codec<E> enumCodec(Class<E> enumClass) {
        return Codec.STRING.xmap(
                str -> Enum.valueOf(enumClass, str),
                Enum::name);
    }

    /**
     * 创建枚举的 Codec，不区分大小写。
     */
    public static <E extends Enum<E>> Codec<E> enumCodecIgnoreCase(Class<E> enumClass) {
        return Codec.STRING.xmap(
                str -> {
                    for (var c : enumClass.getEnumConstants()) {
                        if (c.name().equalsIgnoreCase(str))
                            return c;
                    }
                    throw new IllegalArgumentException("No enum constant " + enumClass.getName() + "." + str);
                },
                e -> e.name().toLowerCase(java.util.Locale.ROOT));
    }

    public static final class IntRange {
        private static final Pattern RANGE_PATTERN = Pattern.compile(
                "^\\s*" + // 开头的空白
                        "(\\[|\\()" + // 左括号类型 (group 1)
                        "\\s*" +
                        "([-+]?\\d*)" + // 最小值 (group 2)，可以为空或带正负号
                        "\\s*,\\s*" +
                        "([-+]?\\d*)" + // 最大值 (group 3)，可以为空或带正负号
                        "\\s*" +
                        "(\\]|\\))" + // 右括号类型 (group 4)
                        "\\s*$" // 结尾的空白
        );

        public static final PrimitiveCodec<IntRange> CODEC = new PrimitiveCodec<IntRange>() {
            @Override
            public <T> DataResult<IntRange> read(final DynamicOps<T> ops, final T input) {
                DataResult<String> asString = ops.getStringValue(input);
                if (asString.result().isPresent()) {
                    return asString.map(IntRange::parse);
                }
                return ops.getNumberValue(input).flatMap(num -> {
                    int v = num.intValue();
                    return DataResult.success(new IntRange(v, v, true, true));
                });
            }

            @Override
            public <T> T write(final DynamicOps<T> ops, final IntRange value) {
                return ops.createString(value.toString());
            }

            @Override
            public String toString() {
                return "JsonMore.Utils.IntRange";
            }
        };

        private final int min;
        private final int max;
        private final boolean minInclusive;
        private final boolean maxInclusive;

        private IntRange(int min, int max, boolean minInclusive, boolean maxInclusive) {
            this.min = min;
            this.max = max;
            this.minInclusive = minInclusive;
            this.maxInclusive = maxInclusive;
        }

        public static IntRange parse(String rangeStr) {
            Matcher matcher = RANGE_PATTERN.matcher(rangeStr);
            if (!matcher.matches()) {
                // 特殊处理通配符 "*"
                if (rangeStr.trim().equals("*")) {
                    return new IntRange(Integer.MIN_VALUE, Integer.MAX_VALUE, true, true);
                }
                throw new IllegalArgumentException("Invalid range format: " + rangeStr);
            }

            boolean minInclusive = matcher.group(1).equals("[");
            boolean maxInclusive = matcher.group(4).equals("]");

            String minStr = matcher.group(2);
            String maxStr = matcher.group(3);

            // 解析最小值
            int min;
            if (minStr.isEmpty()) {
                min = Integer.MIN_VALUE;
            } else {
                try {
                    min = Integer.parseInt(minStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid min value: " + minStr);
                }
            }

            // 解析最大值
            int max;
            if (maxStr.isEmpty()) {
                max = Integer.MAX_VALUE;
            } else {
                try {
                    max = Integer.parseInt(maxStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid max value: " + maxStr);
                }
            }

            // 有效性检查
            if (min > max) {
                throw new IllegalArgumentException("min > max");
            }
            if (min == max && (!minInclusive || !maxInclusive)) {
                throw new IllegalArgumentException("Empty range: single point but not closed on both ends");
            }

            return new IntRange(min, max, minInclusive, maxInclusive);
        }

        public boolean contains(int value) {
            boolean minOk = minInclusive ? (value >= min) : (value > min);
            boolean maxOk = maxInclusive ? (value <= max) : (value < max);
            return minOk && maxOk;
        }

        // getters 便于调试
        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public boolean isMinInclusive() {
            return minInclusive;
        }

        public boolean isMaxInclusive() {
            return maxInclusive;
        }

        @Override
        public String toString() {
            return (minInclusive ? "[" : "(") +
                    (min == Integer.MIN_VALUE ? "" : min) + "," +
                    (max == Integer.MAX_VALUE ? "" : max) +
                    (maxInclusive ? "]" : ")");
        }
    }

    @FunctionalInterface
    public static interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    public static class PackedValue<T> {
        public PackedValue() {
        }

        public PackedValue(T v) {
            value = v;
        }

        private T value;

        public T getValue() {
            return value;
        }

        public void setValue(T v) {
            value = v;
        }
    }
}
