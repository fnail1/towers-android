package ru.mail.my.towers.ui.widgets;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;

public class HighlightTargetDrawable extends Drawable {
    final private Paint paintBackground;
    final private Paint paintLineWhite;
    final private Paint paintLineBlack;
    private Path path;
    private float centerX;
    private float centerY;
    private float radius;

    public HighlightTargetDrawable() {
        paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBackground.setStyle(Paint.Style.STROKE);
        paintBackground.setColor(0x66000000);
        paintBackground.setStrokeWidth(2);

        paintLineWhite = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLineWhite.setColor(0xffffffff);
        paintLineWhite.setStyle(Paint.Style.STROKE);
        paintLineWhite.setStrokeWidth(1);

        paintLineBlack = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLineBlack.setColor(0xff000000);
        paintLineBlack.setStyle(Paint.Style.STROKE);
        paintLineBlack.setStrokeWidth(1);
        paintLineBlack.setStrokeCap(Paint.Cap.SQUARE);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        if (radius > Float.MIN_NORMAL) {
            canvas.drawCircle(centerX, centerY, radius, paintBackground);

            canvas.clipPath(path, Region.Op.DIFFERENCE);
        }

        canvas.drawColor(0xCC000000);

        canvas.restore();

        canvas.drawLine(centerX - 2 * radius, centerY, centerX - radius, centerY, paintLineWhite);
        canvas.drawLine(centerX - radius, centerY, centerX - radius * 2 / 3, centerY, paintLineBlack);

        canvas.drawLine(centerX + 2 * radius, centerY, centerX + radius, centerY, paintLineWhite);
        canvas.drawLine(centerX + radius, centerY, centerX + radius * 2 / 3, centerY, paintLineBlack);

        canvas.drawLine(centerX, centerY - 2 * radius, centerX, centerY - radius, paintLineWhite);
        canvas.drawLine(centerX, centerY - radius, centerX, centerY - radius * 2 / 3, paintLineBlack);

        canvas.drawLine(centerX, centerY + 2 * radius, centerX, centerY + radius, paintLineWhite);
        canvas.drawLine(centerX, centerY + radius, centerX, centerY + radius * 2 / 3, paintLineBlack);

        canvas.drawLine(centerX - radius / 5, centerY, centerX + radius / 5, centerY, paintLineBlack);
        canvas.drawLine(centerX, centerY - radius / 5, centerX, centerY + radius / 5, paintLineBlack);

        canvas.drawCircle(centerX, centerY, radius / 3, paintLineBlack);
    }

    @Override
    public void setAlpha(int i) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    public void setWindow(float centerX, float centerY, float radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        path = new Path();
        this.radius = radius;
        path.addCircle(this.centerX, this.centerY, this.radius, Path.Direction.CW);
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getRadius() {
        return radius;
    }
}
