package ru.mail.my.towers.gis.geometry;

import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;

import ru.mail.my.towers.model.TowerNetwork;

public class NetworkPoint extends Point {
    public Paint paint;
    public Rect symbolRect;
    public Rect textRect;
    public String text;
    public int textLeft;
    public int textBottom;
    public TextPaint textPaint;
    public TowerNetwork network;
}
