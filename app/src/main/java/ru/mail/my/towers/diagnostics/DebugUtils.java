package ru.mail.my.towers.diagnostics;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import java.io.File;


import ru.mail.my.towers.R;
import ru.mail.my.towers.utils.Utils;

import static ru.mail.my.towers.TowersApp.app;
import static ru.mail.my.towers.TowersApp.data;

public class DebugUtils {
    public static void safeThrow(Throwable e) {
        e.printStackTrace();
//        throw new RuntimeException(e);
    }

    public static void importFile(Context context, File outputDir) {
        File dst = new File(outputDir, app().getString(R.string.app_name) + "_data.sqlite");
        Utils.copyFile(new File(data().getDbPath()), dst);


        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", dst);

        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setType("application/octet-stream");
        context.startActivity(sendIntent);
    }

}
