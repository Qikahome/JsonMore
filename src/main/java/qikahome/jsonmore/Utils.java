package qikahome.jsonmore;

import com.google.common.base.Supplier;
import static qikahome.jsonmore.JsonMore.LOGGER;

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
}
