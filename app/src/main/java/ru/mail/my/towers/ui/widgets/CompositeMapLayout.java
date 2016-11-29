package ru.mail.my.towers.ui.widgets;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class CompositeMapLayout extends FrameLayout {
    public CompositeMapLayout(Context context) {
        super(context);
    }

    public CompositeMapLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CompositeMapLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CompositeMapLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int count = getChildCount();
        for (int i = count - 1; i >= 0; i--)
            getChildAt(i).dispatchTouchEvent(ev);
        return true;
    }
}
