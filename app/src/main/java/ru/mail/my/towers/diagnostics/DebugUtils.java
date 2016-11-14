package ru.mail.my.towers.diagnostics;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;


import ru.mail.my.towers.R;
import ru.mail.my.towers.Utils;

import static ru.mail.my.towers.TowersApp.app;
import static ru.mail.my.towers.TowersApp.data;

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

    public static void importFile(Context context, File outputDir) {
        File dst = new File(outputDir, app().getString(R.string.app_name) + "_data.sqlite");
        Utils.copyFile(new File(data().getDbPath()), dst);


        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(dst));
        sendIntent.setType("application/octet-stream");
        context.startActivity(sendIntent);
    }

}
