package api.autotest.common;

import org.apache.log4j.Logger;

public class LoggerUtils {
    public static void info(Logger logger, Object message ) {
        logger.info(message);
        if(logger.isInfoEnabled()) {
            System.out.println(message);
        }
    }

    public static void warn(Logger logger, Object message) {
        logger.warn(message);
        System.out.println(message);
    }

    public static void error(Logger logger, Object message) {
        logger.error(message);
        System.err.println(message);
    }
}
