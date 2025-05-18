package com.tateca.tatecabackend.util;

import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor
public class LogFactory {
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static void logQueryTime(Logger logger, String apiName, Object... queryTimes) {
        StringBuilder logMessage = new StringBuilder(apiName);
        
        for (int i = 0; i < queryTimes.length; i += 2) {
            if (i > 0) {
                logMessage.append(", ");
            }
            logMessage.append(queryTimes[i]).append(": ").append(queryTimes[i + 1]).append("ms");
        }
        
        logger.info(logMessage.toString());
    }
} 