package ru.mail.my.towers.gis;

import android.graphics.Paint;
import android.support.v4.util.LongSparseArray;
import android.text.TextPaint;

import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;

public interface IMapEngine {
    Paint getPaint(int color);

    TextPaint getPrimaryTextPaint();

    LongSparseArray<Tower> getSelectedTowers();

    LongSparseArray<TowerNetwork> getSelectedNetworks();

    TextPaint getSelectionTextPaint();
}
