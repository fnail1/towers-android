package ru.mail.my.towers.toolkit.collections;

/**
 *
 */
public interface JoinOperator<Item, SecondItem, Result> {
    boolean predicate(Item left, SecondItem right);
    Result select(Item left, SecondItem right);
}
