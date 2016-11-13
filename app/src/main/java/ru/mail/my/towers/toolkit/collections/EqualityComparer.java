package ru.mail.my.towers.toolkit.collections;

public interface EqualityComparer<Item> {
    public boolean invoke(Item a, Item b);
}
