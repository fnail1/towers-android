package ru.mail.my.towers.ui;

public interface AsyncDataSource<T> {
    int count();

    void requestData();

    T get(int index);

}
