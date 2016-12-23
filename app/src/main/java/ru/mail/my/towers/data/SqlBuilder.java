package ru.mail.my.towers.data;


import java.util.ArrayList;
import java.util.Collections;

public class SqlBuilder {
    private final ArrayList<String> selections = new ArrayList<>();
    private String from;
    private final StringBuilder joinedTables = new StringBuilder();

    public SqlBuilder select(String column) {
        selections.add(column);
        return this;
    }

    public SqlBuilder select(String... columns) {
        selections.add("\n");
        Collections.addAll(selections, columns);
        return this;
    }

    public SqlBuilder from(String table) {
        from = table;
        return this;
    }

    public SqlBuilder from(String table, String alias) {
        from = table + ' ' + alias;
        return this;
    }

    public SqlBuilder join(String table) {
        joinedTables.append("\njoin ").append(table);
        return this;
    }

    public SqlBuilder join(String table, String alias) {
        joinedTables.append("\njoin ").append(table).append(' ').append(alias);
        return this;
    }

    public SqlBuilder leftJoin(String table) {
        joinedTables.append("\nleft join ").append(table);
        return this;
    }

    public SqlBuilder leftJoin(String table, String alias) {
        joinedTables.append("\nleft join ").append(table).append(' ').append(alias);
        return this;
    }

    public SqlBuilder rightJoin(String table) {
        joinedTables.append("\nright join ").append(table);
        return this;
    }

    public SqlBuilder rightJoin(String table, String alias) {
        joinedTables.append("\nright join ").append(table).append(' ').append(alias);
        return this;
    }

    public SqlBuilder on(String condition) {
        joinedTables.append("on ").append(condition);
        return this;
    }

    public SqlBuilder where(String condition) {
        joinedTables.append("\nwhere ").append(condition);
        return this;
    }

    public SqlBuilder and(String condition) {
        joinedTables.append("\n\tand ").append(condition);
        return this;
    }

    public SqlBuilder or(String condition) {
        joinedTables.append("\n\tor ").append(condition);
        return this;
    }

    public SqlBuilder append(String s) {
        joinedTables.append(s);
        return this;
    }

    public SqlBuilder build() {
        StringBuilder sb = new StringBuilder();
        sb.append("select");
        return null;
    }

    private static class Table {
        String name;
        String alias;
    }

}
