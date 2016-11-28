package ru.mail.my.towers.gis;

import java.util.ArrayList;

import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;

public class ScreenDataObjects {
    public final int generation;
    public ArrayList<TowerNetwork> networks;
    public ArrayList<Tower> towers;


    public ScreenDataObjects(int generation) {
        this.generation = generation;
    }
}
