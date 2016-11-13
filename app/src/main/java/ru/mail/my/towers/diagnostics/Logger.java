package ru.mail.my.towers.diagnostics;

import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

import okhttp3.logging.HttpLoggingInterceptor;
import ru.mail.my.towers.BuildConfig;

public class Logger {
    public static final boolean LOG_ERRORS = true;
    public static final boolean LOG_VERBOSE = BuildConfig.DEBUG;
    public static final boolean LOG_DEBUG = BuildConfig.DEBUG;

    private static final boolean LOG_LOCATION = LOG_DEBUG;
    public static final boolean LOG_API = LOG_DEBUG;

    private static final String TAG_LOCATION = "location";
    public static final String TAG_API = "game_api";


    public static void traceUi(Object caller) {
        if (!LOG_VERBOSE)
            return;
        StackTraceElement traceElement = new Throwable().getStackTrace()[1];

        int pt = traceElement.getClassName().lastIndexOf('.');
        String className = traceElement.getClassName();
        if (pt > 0 && pt < className.length() - 1) {
            className = className.substring(pt + 1);
        }
        Logger.logV("TRACE", "%s (%s).%s ", className, caller.getClass().getCanonicalName(), traceElement.getMethodName());
    }

    public static void logE(String tag, String s, Object... args) {
        if (LOG_ERRORS) {
            if (args.length > 0)
                s = String.format(s, (Object[]) args);
            Log.e(tag, s);
        }
    }

    public static void logD(String tag, String s, Object... args) {
        if (LOG_DEBUG) {
            if (args.length > 0)
                s = String.format(s, (Object[]) args);
            Log.d(tag, s);
        }
    }

    public static void logV(String tag, String s, Object... args) {
        if (LOG_VERBOSE) {
            if (args.length > 0)
                s = String.format(s, (Object[]) args);
            Log.v(tag, s);
        }

    }


    public static void trace() {
        if (!Logger.LOG_VERBOSE)
            return;
        StackTraceElement traceElement = new Throwable().getStackTrace()[1];
        String className = traceElement.getClassName();
        if (className.startsWith(Logger.class.getPackage().getName()))
            className = className.substring(Logger.class.getPackage().getName().length());
        Logger.logV("TRACE", "%s.%s ", className, traceElement.getMethodName());
    }

    public static void trace(String line) {
        if (!Logger.LOG_VERBOSE)
            return;
        StackTraceElement traceElement = new Throwable().getStackTrace()[1];
        Logger.logV("TRACE", "%s.%s (%s)", traceElement.getClassName(), traceElement.getMethodName(), line);
    }


    public static void logLocation(String s) {
        if (LOG_LOCATION) {
            Log.d(TAG_LOCATION, s);
        }
    }

    public static void logLocation(Location location) {
        if (location == null) {
            logLocation("undefined");
            return;
        }
        String provider = location.getProvider();
        float accuracy = location.getAccuracy();
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        double alt = location.getAltitude();
        long time = System.currentTimeMillis() - location.getTime();
        StringBuilder sb = new StringBuilder();
        long s = time / 1000;
        long m = s / 60;
        long h = m / 60;
        if (h > 0)
            sb.append(h).append(":");
        sb.append(m % 60).append(":").append(s % 60);
        logLocation(provider + " (" + accuracy + ", " + sb.toString() + ") -> lat: " + lat + ", lon: " + lng + ", alt: " + alt);
    }

    public static HttpLoggingInterceptor.Logger createApiLogger() {
        if (LOG_API) {
            return message -> Log.v(TAG_API, message);
        } else {
            return message -> {
            };
        }
    }
}
