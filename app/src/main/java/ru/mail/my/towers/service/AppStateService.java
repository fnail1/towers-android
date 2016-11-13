package ru.mail.my.towers.service;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import java.util.HashSet;

import ru.mail.my.towers.toolkit.events.ObservableEvent;

import static ru.mail.my.towers.TowersApp.app;
import static ru.mail.my.towers.TowersApp.prefs;


public class AppStateService {
    private static final long BACKGROUND_THRESHOLD = 1 * 60 * 1000;
    public final ObservableEvent<DateChangedEventHandler, AppStateService, Void> dateChangedEvent = new ObservableEvent<DateChangedEventHandler, AppStateService, Void>(this) {
        @Override
        protected void notifyHandler(DateChangedEventHandler handler, AppStateService sender, Void args) {
            handler.onDateTimeChanged();
        }
    };

    public final ObservableEvent<AppStateEventHandler, AppStateService, Void> stateEvent = new ObservableEvent<AppStateEventHandler, AppStateService, Void>(this) {
        @Override
        protected void notifyHandler(AppStateEventHandler handler, AppStateService sender, Void args) {
            handler.onAppStateChanged();
        }
    };

    public final ObservableEvent<LowMemoryEventHandler, AppStateService, Void> lowMemoryEvent = new ObservableEvent<LowMemoryEventHandler, AppStateService, Void>(this) {
        @Override
        protected void notifyHandler(LowMemoryEventHandler handler, AppStateService sender, Void args) {
            handler.onLowMemory();
        }
    };

    public final ObservableEvent<ServerTimeOffsetChanged, AppStateService, Void> serverTimeOffsetChangedEvent = new ObservableEvent<ServerTimeOffsetChanged, AppStateService, Void>(this) {
        @Override
        protected void notifyHandler(ServerTimeOffsetChanged handler, AppStateService sender, Void args) {
            handler.onServerTimeOffsetChanged();
        }
    };

    private Activity mTopActivity;
    private long serverTimeOffset;
    private final HashSet<String> requiredPermissions = new HashSet<>();
    public boolean initialized;
    private boolean displayCurrentLocation;
    private long lastForegroundTS;


    public AppStateService(Context context, Preferences preferences) {

        serverTimeOffset = preferences.getServerTimeOffset();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                dateChangedEvent.fire(null);
            }
        }, filter);
        initialized = true;
    }

    public Activity getTopActivity() {
        return mTopActivity;
    }

    public void setTopActivity(@NonNull Activity topActivity) {
        if (mTopActivity != topActivity) {
            if (mTopActivity == null && SystemClock.elapsedRealtime() - lastForegroundTS > BACKGROUND_THRESHOLD)
                displayCurrentLocation = true;
            mTopActivity = topActivity;
            onStateChanged();
        }
    }

    public void resetTopActivity(@NonNull Context activity) {
        if (mTopActivity == activity) {
            mTopActivity = null;
            lastForegroundTS = SystemClock.elapsedRealtime();
            onStateChanged();
        }
    }

    public boolean isForeground() {
        return mTopActivity != null;
    }


    protected void onStateChanged() {
        stateEvent.fire(null);
    }

    public void onLowMemory() {
        lowMemoryEvent.fire(null);
    }

    protected void onServerTimeOffsetChanged() {
        serverTimeOffsetChangedEvent.fire(null);
    }


    public long getServerTime() {
        return getServerTime(System.currentTimeMillis());
    }

    public long getServerTime(long currentTimeMillis) {
        return currentTimeMillis + serverTimeOffset;
    }

    public long getLocalTime(long serverTimeMillis) {
        return serverTimeMillis - serverTimeOffset;
    }

    public void adjustServerTimeOffset(long time) {
        long offset = time - System.currentTimeMillis();
        if (Math.abs(offset - serverTimeOffset) > 3000) {
            serverTimeOffset = offset;
            prefs().setServerTimeOffset(serverTimeOffset);
            onServerTimeOffsetChanged();
        }
    }

    public void addPermissionRequested(String permission) {
        requiredPermissions.add(permission);
    }

    public void onPermissionGranted(String permission) {
        requiredPermissions.remove(permission);
    }

    public boolean isPermissionRequested(String permission) {
        return requiredPermissions.contains(permission);
    }

    public boolean displayCurrentLocation() {
        boolean b = this.displayCurrentLocation;
        displayCurrentLocation = false;
        return b;
    }


    public interface AppStateEventHandler {
        void onAppStateChanged();
    }

    public interface LowMemoryEventHandler {
        void onLowMemory();
    }

    public interface ServerTimeOffsetChanged {
        void onServerTimeOffsetChanged();
    }

    public interface DateChangedEventHandler {
        void onDateTimeChanged();
    }
}
