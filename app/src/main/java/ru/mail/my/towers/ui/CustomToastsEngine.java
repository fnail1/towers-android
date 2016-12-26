package ru.mail.my.towers.ui;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.LinkedList;

import ru.mail.my.towers.R;

public class CustomToastsEngine {
    private final int MAX_ITEMS = 4;
    private final RelativeLayout surface;
    private final Handler handler;
    private final LinkedList<Animation> queue = new LinkedList<>();

    private Item top;
    private boolean started;
    int count;

    public CustomToastsEngine(RelativeLayout surface) {
        this.surface = surface;
        handler = new Handler();
    }

    public void showMessage(String text, int duration, int textColor, int backgroundColor) {
        if (count > MAX_ITEMS)
            return;

        Context context = surface.getContext();

        Item item = new Item();
        TextView textView = (TextView) LayoutInflater.from(context).inflate(R.layout.toast_custom_layout, surface, false);
        textView.setTag(CustomToastsEngine.class);
        textView.setText(text);
        textView.setTextColor(textColor);

        item.view = textView;
        surface.addView(item.view);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) item.view.getLayoutParams();
        lp.leftMargin = (surface.getWidth() - lp.width) / 2;
        item.view.setLayoutParams(lp);

        item.view.measure(View.MeasureSpec.makeMeasureSpec(lp.width, View.MeasureSpec.EXACTLY),
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        item.height = item.view.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

        Item last = top;
        if (last != null) {
            top.previous = item;
            item.next = top;
            item.view.setAlpha(0);

            Animation animation = new MoveAllDownAnimation(item.height, top);
            animation.setDuration(500);
            duration += 500;
            queue.add(animation);
        }
        item.setCurrentY(80);
        item.targetY = 80;
        top = item;
        count++;

        SetAlphaAnimation alphaAnimation = new SetAlphaAnimation(top.view, 1);
        alphaAnimation.setDuration(500);
        duration += 500;

        queue.add(alphaAnimation);

        handler.postDelayed(() -> removeMessage(item), duration);

        startNextAnimation();
    }

    public void removeMessage(Item item) {
        RemoveAnimation removeAnimation = new RemoveAnimation(item);
        removeAnimation.setDuration(500);
        queue.add(removeAnimation);

        if (item.next != null) {
            MoveAllDownAnimation moveAllDownAnimation = new MoveAllDownAnimation(-item.height, item.next);
            moveAllDownAnimation.setDuration(300);
            queue.add(moveAllDownAnimation);
        }

        startNextAnimation();
    }

    public void startNextAnimation() {
        if (started)
            return;

        Animation next = queue.poll();
        if (next != null) {
            started = true;
            surface.startAnimation(next);
        }
    }

    public void removeAll() {
        queue.clear();
        for (int i = surface.getChildCount() - 1; i >= 0; i--) {
            if (surface.getChildAt(i).getTag() == CustomToastsEngine.class)
                surface.removeViewAt(i);
        }
    }

    private class BaseAnimation extends Animation implements Animation.AnimationListener {

        public BaseAnimation() {
            setAnimationListener(this);
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            started = false;
            startNextAnimation();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    private class SetAlphaAnimation extends BaseAnimation {
        private final View view;
        private final float targetValue;
        private float startValue;

        public SetAlphaAnimation(View view, float targetValue) {
            this.view = view;
            this.targetValue = targetValue;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            view.setAlpha(startValue + (targetValue - startValue) * interpolatedTime);
        }

        @Override
        public void onAnimationStart(Animation animation) {
            super.onAnimationStart(animation);
            startValue = view.getAlpha();
        }
    }

    private class MoveAllDownAnimation extends BaseAnimation {
        private final int offset;
        private final Item top;
        private boolean startCalled;

        public MoveAllDownAnimation(int offset, Item top) {
            super();
            this.offset = offset;
            this.top = top;
        }

        @Override
        protected void applyTransformation(float alpha, Transformation t) {
            if (!startCalled) {
                onAnimationStart(this);
            }

            super.applyTransformation(alpha, t);

            Item item = top;
            while (item != null) {
                float y = item.startY + (item.targetY - item.startY) * alpha;
                item.setCurrentY(y);
                item = item.next;
            }
        }

        @Override
        public void onAnimationStart(Animation animation) {
            if (startCalled)
                return;
            startCalled = true;
            Item item = top;
            while (item != null) {
                item.targetY += offset;
                item.startY = item.getCurrentY();
                item = item.next;
            }
        }
    }

    private class RemoveAnimation extends BaseAnimation {
        private float startValue;
        private final Item item;
        private boolean startCalled;

        public RemoveAnimation(Item item) {
            this.item = item;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (!startCalled)
                onAnimationStart(this);

            super.applyTransformation(interpolatedTime, t);
            float alpha = startValue * (1 - interpolatedTime);
            item.view.setAlpha(alpha);
            float translationX = 100 * interpolatedTime;
            item.view.setTranslationX(translationX);
        }

        @Override
        public void onAnimationStart(Animation animation) {
            super.onAnimationStart(animation);
            startCalled = true;
            startValue = item.view.getAlpha();
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            Item previous = item.previous;
            Item next = item.next;

            if (previous != null)
                previous.next = next;
            else
                top = null;

            if (next != null)
                next.previous = previous;

            surface.removeView(item.view);
            count--;
            super.onAnimationEnd(animation);

        }
    }

    private static class Item {
        public Item next;
        public Item previous;
        public View view;
        public float currentY;
        public float targetX;
        public float targetY;
        public float targetAlpha;
        public float startY;
        public int height;

        public void setCurrentY(float y) {
            view.setTranslationY(currentY = y);
            if (currentY == 0) {
                Log.d("CustomToastsEngine", "UPS!!");
            }
        }

        public float getCurrentY() {
            if (currentY == 0) {
                Log.d("CustomToastsEngine", "UPS!!");
            }
            return currentY;
        }
    }
}
