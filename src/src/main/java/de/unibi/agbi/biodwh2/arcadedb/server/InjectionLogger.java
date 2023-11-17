package de.unibi.agbi.biodwh2.arcadedb.server;

import com.arcadedb.log.LogManager;
import com.arcadedb.log.Logger;

import java.util.logging.Level;

class InjectionLogger implements Logger {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(ArcadeDBService.class);

    public static void inject() {
        LogManager.instance().setLogger(new InjectionLogger());
    }

    @Override
    public void log(Object requester, Level level, String message, Throwable exception, String context,
                    Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
                    Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13,
                    Object arg14, Object arg15, Object arg16, Object arg17) {
        final String requesterName;
        if (requester instanceof String)
            requesterName = (String) requester;
        else if (requester instanceof Class<?>)
            requesterName = ((Class<?>) requester).getName();
        else if (requester != null)
            requesterName = requester.getClass().getName();
        else
            requesterName = "com.arcadedb";
        message = '[' + requesterName + "] " + String.format(message, arg1, arg2, arg3, arg4, arg5, arg6, arg7,
                                                             arg8, arg9, arg10, arg11, arg12, arg13, arg14,
                                                             arg15, arg16, arg17);
        if (level == Level.INFO)
            LOGGER.info(message, exception);
        else if (level == Level.WARNING)
            LOGGER.warn(message, exception);
        else if (level == Level.SEVERE)
            LOGGER.error(message, exception);
        else if (level == Level.FINE)
            LOGGER.debug(message, exception);
    }

    @Override
    public void log(Object requester, Level level, String message, Throwable exception, String context,
                    Object... args) {
        final String requesterName;
        if (requester instanceof String)
            requesterName = (String) requester;
        else if (requester instanceof Class<?>)
            requesterName = ((Class<?>) requester).getName();
        else if (requester != null)
            requesterName = requester.getClass().getName();
        else
            requesterName = "com.arcadedb";
        message = '[' + requesterName + "] " + String.format(message, args);
        if (level == Level.INFO)
            LOGGER.info(message, exception);
        else if (level == Level.WARNING)
            LOGGER.warn(message, exception);
        else if (level == Level.SEVERE)
            LOGGER.error(message, exception);
        else if (level == Level.FINE)
            LOGGER.debug(message, exception);
    }

    @Override
    public void flush() {
    }
}
