package ru.mail.my.towers.gis.geometry;

import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;

import ru.mail.my.towers.model.Tower;

public class TowerPoint extends Point {
    public Paint paint;
    public Rect iconRect;
    public Rect levelRect;
    public String levelText;
    public int levelLeft;
    public int levelBottom;
    public TextPaint levelTextPaint;
    public Tower tower;
    public Rect hpTotalRect;
    public Paint hpTotalPaint;
    public Rect hpCurrentRect;
    public Paint hpCurrentPaint;
}
