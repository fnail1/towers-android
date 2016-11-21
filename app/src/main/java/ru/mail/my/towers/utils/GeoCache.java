package ru.mail.my.towers.utils;

import java.util.ArrayList;
import java.util.Arrays;

public class GeoCache<T> {
    public static final int INITIAL_CAPACITY = 100;
    private double[] topValues = new double[INITIAL_CAPACITY];
    private int[] topIndices = new int[INITIAL_CAPACITY];
    private double[] bottomValues = new double[INITIAL_CAPACITY];
    private int[] bottomIndices = new int[INITIAL_CAPACITY];
    private double[] leftValues = new double[INITIAL_CAPACITY];
    private int[] leftIndices = new int[INITIAL_CAPACITY];
    private double[] rightValues = new double[INITIAL_CAPACITY];
    private int[] rightIndices = new int[INITIAL_CAPACITY];

    private Object[] values = new Object[INITIAL_CAPACITY];

    private int capacity = INITIAL_CAPACITY;
    private int size = 0;

    public void put(T obj, double left, double right, double top, double bottom) {
        int index = size++;
        if (size == capacity) {
            capacity <<= 1;
            topValues = Arrays.copyOf(topValues, capacity);
            topIndices = Arrays.copyOf(topIndices, capacity);
            bottomValues = Arrays.copyOf(bottomValues, capacity);
            bottomIndices = Arrays.copyOf(bottomIndices, capacity);
            leftValues = Arrays.copyOf(leftValues, capacity);
            leftIndices = Arrays.copyOf(leftIndices, capacity);
            rightValues = Arrays.copyOf(rightValues, capacity);
            rightIndices = Arrays.copyOf(rightIndices, capacity);
            values = Arrays.copyOf(values, capacity);
        }

        values[index] = obj;

        insertIndex(topValues, topIndices, top, index);
        insertIndex(bottomValues, bottomIndices, bottom, index);
        insertIndex(leftValues, leftIndices, left, index);
        insertIndex(rightValues, rightIndices, right, index);
    }

    private void insertIndex(double[] keys, int[] indices, double key, int index) {
        int i = findIndex(keys, key, index);
        System.arraycopy(keys, i, keys, i + 1, index - i);
        keys[i] = key;
        System.arraycopy(indices, i, indices, i + 1, index - i);
        indices[i] = index;
    }

    public ArrayList<T> select(double left, double right, double top, double bottom) {
        ArrayList<T> list = new ArrayList<>();
        int leftIdx = findIndex(leftValues, right, size);
        int rightIdx = findIndex(rightValues, left, size);
        int topIdx = findIndex(topValues, bottom, size);
        int bottomIdx = findIndex(bottomValues, top, size);

        int[] orderedByX = new int[rightIdx - leftIdx];
        int idx = 0;
        for (int x = rightIdx; x <= leftIdx; x++)
            orderedByX[idx++] = leftIndices[x];

        return list;
    }

    private int findIndex(double[] keys, double key, int size) {
        int i = Arrays.binarySearch(keys, 0, size, key);
        if (i < 0)
            i = ~i;
        return i;
    }
}
