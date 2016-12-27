package ru.mail.my.towers.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import static ru.mail.my.towers.TowersApp.app;

public final class Utils {
    private static SimpleDateFormat dateLongFormatWithYear = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());


    private Utils() {
    }

    public static void hideKeyboard(View view) {
        if (view == null)
            return;
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void hideKeyboard(Activity a) {
        InputMethodManager inputManager = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus:
        View v = a.getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, 0);
    }

    public static boolean isPortrait() {
        return app().getResources().getConfiguration().orientation == Configuration
                .ORIENTATION_PORTRAIT;
    }


    public static int getDisplayWidth() {
        return app().getResources().getDisplayMetrics().widthPixels;
    }

    public static boolean equalsHash(String s1, String s2) {
        if (s1 == null)
            return s2 == null;
        if (s2 == null)
            return false;
        return s1.hashCode() == s2.hashCode();
    }

    public static void copyFile(File src, File dst) {
        try {
            FileInputStream fis = new FileInputStream(src);
            try {
                FileOutputStream fos = new FileOutputStream(dst);
                try {
                    byte[] buf = new byte[64 * 1024];
                    int c;
                    while ((c = fis.read(buf)) >= 0)
                        fos.write(buf, 0, c);
                } finally {
                    fos.close();
                }
            } finally {
                fis.close();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }


    public static String getNormalizedPhoneNumber(String number) {
        if (number == null)
            return null;

        StringBuilder sb = new StringBuilder(number.length());

        for (int i = 0; i < number.length(); i++) {
            char ch = number.charAt(i);
            if (Character.isDigit(ch)) {
                sb.append(ch);
            } else if (ch == '+') {
                if (sb.length() > 0)
                    return null;
                else
                    sb.append("+");
            } else if (ch == '*' || ch == '#')
                return null;
        }

        return sb.length() >= 5 ? sb.toString() : null;
    }


    public static String bundleToString(Bundle bundle) {
        if (bundle == null)
            return "null";
        StringBuilder sb = new StringBuilder();
        for (String key : bundle.keySet()) {
            if (key != null && bundle.get(key) != null) {
                sb.append(key).append("='").append(bundle.get(key)).append("', ");
            }
        }
        return sb.toString();
    }


    /**
     * see http://stackoverflow.com/a/17625641
     *
     * @return unique device id
     */
    public static String getDeviceId() {
        String android_id = Settings.Secure.getString(app().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        if (TextUtils.isEmpty(android_id))
            android_id = getUniquePsuedoID();

        return android_id;
    }

    /**
     * Return pseudo unique ID
     *
     * @return ID
     */
    public static String getUniquePsuedoID() {
        // If ALL else fails, if the user does have lower than API 9 (lower
        // than Gingerbread), has reset their device or 'Secure.ANDROID_ID'
        // returns 'null', then simply the ID returned will be solely based
        // off their Android device information. This is where the collisions
        // can happen.
        // Thanks http://www.pocketmagic.net/?p=1662!
        // Try not to use DISPLAY, HOST or ID - these items could change.
        // If there are collisions, there will be overlapping data
        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);

        // Thanks to @Roman SL!
        // http://stackoverflow.com/a/4789483/950427
        // Only devices with API >= 9 have android.os.Build.SERIAL
        // http://developer.android.com/reference/android/os/Build.html#SERIAL
        // If a user upgrades software or roots their device, there will be a duplicate entry
        String serial = null;
        try {
            serial = Build.class.getField("SERIAL").get(null).toString();
        } catch (Exception exception) {
            // String needs to be initialized
            serial = "2f8ab2c67788e3d7"; // some value
        }

        // Thanks @Joe!
        // http://stackoverflow.com/a/2853253/950427
        // Finally, combine the values we have found by using the UUID class to create a unique identifier
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

    public static int getColor(Context context, @ColorRes int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getColor(colorResId);
        } else {
            return context.getResources().getColor(colorResId);
        }
    }

    public static int mulColors(int c1, int c2, float alpha) {
        int a1 = Color.alpha(c1);
        int r1 = Color.red(c1);
        int g1 = Color.green(c1);
        int b1 = Color.blue(c1);

        int a2 = Color.alpha(c2);
        int r2 = Color.red(c2);
        int g2 = Color.green(c2);
        int b2 = Color.blue(c2);

        int c = Color.argb(
                (int) (alpha * a2 + (1 - alpha) * a1),
                (int) (alpha * r2 + (1 - alpha) * r1),
                (int) (alpha * g2 + (1 - alpha) * g1),
                (int) (alpha * b2 + (1 - alpha) * b1));
        return c;
    }

    /**
     * Converts DIP units into pixels
     *
     * @param context The reference to a context to take display metrics from
     * @param dp      Size in DIP units
     * @return Size in pixels
     */

    public static float dpToPx(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    /**
     * Converts SP units into pixels
     *
     * @param context The reference to a context to take display metrics from
     * @param sp      Size in SP units
     * @return Size in pixels
     */
    public static float spToPx(Context context, float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static float normalizeAlpha(float alpha) {
        if (alpha < 0F)
            alpha = 0F;
        else if (alpha > 1F)
            alpha = 1F;
        return alpha;
    }

    public static String formatLocation(LatLng location) {
        return formatLocation(location.latitude, location.longitude);

    }

    public static String formatLocation(Location location) {
        return formatLocation(location.getLatitude(), location.getLongitude());
    }

    @NonNull
    public static String formatLocation(double latitude, double longitude) {
        StringBuilder sb = new StringBuilder();
        formatCoord(latitude, sb);
        sb.append("; ");
        formatCoord(longitude, sb);
        return sb.toString();
    }

    public static String formatCoord(double coord) {
        StringBuilder sb = new StringBuilder();
        formatCoord(coord, sb);
        return sb.toString();
    }

    private static void formatCoord(double coord, StringBuilder sb) {
        int degree = (int) coord;
        coord -= degree;
        coord *= 60;
        int minutes = (int) coord;
        coord -= minutes;
        coord *= 60;
        int seconds = (int) coord;

        sb.append(degree).append("° ")
                .append(minutes).append("' ")
                .append(seconds).append("\" ");
    }

    public static boolean isMockProvider(Location args) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && args.isFromMockProvider();
    }

    public static int highlightColor(int baseColor, int highlighting, int alpha) {
        return Color.argb(alpha,
                Math.max(255, Color.red(baseColor) + Color.red(highlighting)),
                Math.max(255, Color.green(baseColor) + Color.green(highlighting)),
                Math.max(255, Color.blue(baseColor) + Color.blue(highlighting)));
    }

    public static String formatNetworkLevel(float level) {
        return Float.toString((float) Math.round((level + 1) * 10) / 10);
    }

    public static String formatDate(Calendar calendar) {
        return dateLongFormatWithYear.format(calendar.getTime());
    }

    public static double distance(double lat1, double lng1, double lat2, double lng2) {
//      Удельное расстояние по меридиану(УРМ).Одно не изменно для любой точки земного шара.
//      УРМ = Окружность земли / 360 градусов.
//      УРМ = Радиус земли * 2 * Пи / 360 градусов.
//      УРМ = 6371 км * 2 * Пи / 360 градусов = 111, 194926645 км / градус.
//        double M = 6378137.0 * 2 * Math.PI / 360;
//        double M = 111194.926645;
        double M = 111319.49079327358;

//      Удельное расстояние по паралели(УРП). Оно будет иметь разное значение для для разных широт.
//
//      УРП = Окружность по паралели / 360 градусов.
//      Для вычисления окружности по паралели нам необходимо знать ее радиус, у для радиуса — широту данной паралели.
//      Радиус паралели = Радиус земли * cos( Широты ).
//      Отсюда,
//      УРП = Радиус земли * cos( Широты )  * 2 * Пи / 360 градусов.
//      Можно упростить. «cos( Широты ) * ( Радиус земли * 2 * Пи / 360 градусов)«. Вторая  часть нам уже известна. Получается:
//      УРП = cos( Широты ) * УРМ.
//        double P0 = 6356752.3142 * 2 * Math.PI / 360;
        double P0 = 110946.25761655909;
        double P = Math.cos(lat1) * P0;

//      Далее по теореме пифагора
        double dlat = (lat1 - lat2) * M;
        double dlng = (lng1 - lng2) * P;

        return Math.sqrt(dlat * dlat + dlng * dlng);
    }

    public static double offsetLatitude(double lat, double offset) {
        double M = 111319.49079327358;
        return lat + offset / M;
    }

    public static double offsetLongitude(double lat, double lng, double offset) {
        double P0 = 110946.25761655909;
        double P = Math.cos(lat) * P0;
        return lng + offset / P;
    }

    public static double distance2(double lat1, double lng1, double lat2, double lng2) {
        Location p1 = new Location("");
        p1.setLatitude(lat1);
        p1.setLongitude(lng1);
        Location p2 = new Location("");
        p2.setLatitude(lat2);
        p2.setLongitude(lng2);

        return p1.distanceTo(p2);
    }
}
