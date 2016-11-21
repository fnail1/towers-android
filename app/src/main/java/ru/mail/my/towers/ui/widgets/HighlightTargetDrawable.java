package ru.mail.my.towers.ui.widgets;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.drawable.Drawable;

public class HighlightTargetDrawable extends Drawable {
    final private Paint mPaintAntiAlias;
    private Path mPath;
    private float mCenterX;
    private float mCenterY;
    private float mRadius;

    public HighlightTargetDrawable() {
        mPaintAntiAlias = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintAntiAlias.setStyle(Paint.Style.STROKE);
        mPaintAntiAlias.setColor(0xCC000000);
        mPaintAntiAlias.setStrokeWidth(2);
    }

    @Override
    public void draw(Canvas canvas) {
        if (mRadius > Float.MIN_NORMAL) {
            canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaintAntiAlias);

            canvas.clipPath(mPath, Region.Op.DIFFERENCE);
        }

        canvas.drawColor(0xCC000000);
    }

    @Override
    public void setAlpha(int i) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return 1;
    }

    public void setWindow(float centerX, float centerY, float radius) {
        mCenterX = centerX;
        mCenterY = centerY;
        mPath = new Path();
        mRadius = radius;
        mPath.addCircle(mCenterX, mCenterY, mRadius, Path.Direction.CW);
    }

    public float getCenterX() {
        return mCenterX;
    }

    public float getCenterY() {
        return mCenterY;
    }

    public float getRadius() {
        return mRadius;
    }
}
