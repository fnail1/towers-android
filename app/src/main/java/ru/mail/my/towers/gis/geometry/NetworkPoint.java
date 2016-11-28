package ru.mail.my.towers.gis.geometry;

import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;

import ru.mail.my.towers.model.TowerNetwork;

public class NetworkPoint extends Point {
    public Paint paint;
    public Rect iconRect;
    public Rect levelRect;
    public String levelText;
    public int levelLeft;
    public int levelBottom;
    public TextPaint levelTextPaint;
    public TowerNetwork network;
}
