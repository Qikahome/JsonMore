package qikahome.jsonmore;

import com.google.common.base.Supplier;



import static qikahome.jsonmore.JsonMore.LOGGER;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class Utils {
    public static <T> T getOrDebug(Supplier<T> supplier, T defaultValue) {
        return getOrDebug(supplier, defaultValue, null);
    }

    public static <T> T getOrDebug(Supplier<T> supplier, T defaultValue, @Nullable String errorMessage) {
        return getOrElse(supplier, defaultValue, org.slf4j.event.Level.DEBUG, errorMessage);
    }

    public static <T> T getOrInfo(Supplier<T> supplier, T defaultValue) {
        return getOrInfo(supplier, defaultValue, null);
    }

    public static <T> T getOrInfo(Supplier<T> supplier, T defaultValue, @Nullable String errorMessage) {
        return getOrElse(supplier, defaultValue, org.slf4j.event.Level.INFO, errorMessage);
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
        // 捕获所有Exception（如需捕获Error，可改为catch (Throwable e)）
        try {
            // 执行Lambda并返回结果
            return supplier.get();
        } catch (Throwable e) {
            // 可选：打印异常日志（便于排查问题）
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
            // 异常时返回默认值
            return defaultValue;
        }
    }

    public static final class IntRange implements Predicate<Integer> {
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
            // 正则表达式：支持 [a,b] [a,b) (a,b] (a,b) [a,) (a,) (,b] (,b) *
            Pattern pattern = Pattern.compile(
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

            Matcher matcher = pattern.matcher(rangeStr);
            if (!matcher.matches()) {
                // Minecraft 风格范围：a..b, ..b, a.., ..
                String trimmed = rangeStr.trim();
                if (trimmed.contains("..")) {
                    String[] parts = trimmed.split("\\.\\.", -1);
                    String minStr = parts[0].trim();
                    String maxStr = parts.length > 1 ? parts[1].trim() : "";
                    int min = minStr.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(minStr);
                    int max = maxStr.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(maxStr);
                    if (min > max) throw new IllegalArgumentException("min > max");
                    return new IntRange(min, max, true, true);
                }
                // 特殊处理通配符 "*"
                if (trimmed.equals("*")) {
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

        @Override
        public boolean test(Integer t) {
            return contains(t);
        }
    }

    @FunctionalInterface
    public static interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }
}
