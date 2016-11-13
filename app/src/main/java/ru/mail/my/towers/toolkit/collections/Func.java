package ru.mail.my.towers.toolkit.collections;

public interface Func<Param, Result>{
    Result invoke(Param p);
}

