package ru.mail.my.towers.model;

import android.content.Context;
import android.support.annotation.ColorRes;


import ru.mail.my.towers.R;
import ru.mail.my.towers.utils.Utils;

public enum NotificationType {
    SUCCESS(R.color.colorNotificationSuccess),
    ERROR(R.color.colorNotificationError),
    INFO(R.color.colorNotificationInfo),
    GOOD_NEWS(R.color.colorNotificationGoodNews),
    BAD_NEWS(R.color.colorNotificationBadNews),
    ALARM(R.color.colorNotificationAlarm);

    @ColorRes
    private final int textColorResX;

    private int textColor = 0;

    NotificationType(int textColorResX) {
        this.textColorResX = textColorResX;
    }

    @ColorRes
    public int getTextColorResX() {
        return textColorResX;
    }

    public int getTextColor(Context context) {
        if (textColor == 0)
            textColor = Utils.getColor(context, textColorResX);
        return textColor;
    }
}
