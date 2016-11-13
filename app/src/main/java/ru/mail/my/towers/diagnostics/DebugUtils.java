package ru.mail.my.towers.diagnostics;

public class DebugUtils {
    public static void safeThrow(Throwable e) {
        e.printStackTrace();
//        throw new RuntimeException(e);
    }

    public static void traceUi(Object caller) {
        if (!Logger.LOG_VERBOSE)
            return;
        StackTraceElement traceElement = new Throwable().getStackTrace()[1];

        int pt = traceElement.getClassName().lastIndexOf('.');
        String className = traceElement.getClassName();
        if (pt > 0 && pt < className.length() - 1) {
            className = className.substring(pt + 1);
        }
        Logger.logV("TRACE", "%s (%s).%s ", className, caller.getClass().getCanonicalName(), traceElement.getMethodName());
    }

    public static void trace() {
        if (!Logger.LOG_VERBOSE)
            return;
        StackTraceElement traceElement = new Throwable().getStackTrace()[1];
        Logger.logV("TRACE", "%s.%s ", traceElement.getClassName(), traceElement.getMethodName());
    }

    public static void trace(String line) {
        if (!Logger.LOG_VERBOSE)
            return;
        StackTraceElement traceElement = new Throwable().getStackTrace()[1];
        String className = traceElement.getClassName();
        int pos = className.lastIndexOf('.');
        if (pos >= 0) {
            className = className.substring(pos) + 1;
        }
        Logger.logV("TRACE", "%s.%s (%s)", className, traceElement.getMethodName(), line);
    }


}
