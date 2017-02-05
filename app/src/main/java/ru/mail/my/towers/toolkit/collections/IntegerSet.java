package ru.mail.my.towers.toolkit.collections;

public class IntegerSet {
    private final int[] data;

    public IntegerSet(int capacity) {
        data = new int[capacity];
    }

    public boolean put(int v) {
        int p = data.length / 2;
        if (data[p] < v) {
            data[p] = v;
            return false;
        }
        return false;
    }
}
