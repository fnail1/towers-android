package ru.mail.my.towers.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class SqlBuilder {

    public static class b1 {
        Class<?> from;
        Map<Class<?>, String> tables = new LinkedHashMap<>();
        ArrayList<Join> joins = new ArrayList<>();

        public void from(Class<?> tableType) {
            if (tables.size() != 0)
                throw new IllegalStateException();
            from = tableType;
            tables.put(from, "t0");
        }

        public void innerJoin(Class<?> tableType, String fk, Class<?> tablePk, String pk) {
            if (!tables.containsKey(tablePk))
                throw new IllegalArgumentException();
            Join join = new Join();
            join.joiningTable = tableType;
            join.fieldFk = fk;
            join.tablePk = tablePk;
            join.Pk = pk;
            joins.add(join);

            tables.put(tableType, "t" + tables.size());
        }

        public void select(Class<?> table) {

        }

        public void where(Class<?> table, String field, String conditional) {

        }

        public String build() {
            StringBuilder sb = new StringBuilder();
            return sb.toString();
        }
    }

    public static class b2{
        private final StringBuilder sb = new StringBuilder();
        public b2 _(String s){
            sb.append(s);
            return this;
        }

        public b2 _(int i) {
            sb.append(i);
            return this;
        }
    }

    public static class Join {
        Class<?> joiningTable;
        String fieldFk;

        Class<?> tablePk;
        String Pk;
    }

    public static void test() {
        new SqlBuilder().select("a,b,c").a(',').select("d,e,f")
                .from("table").as("t");

    }

    private SqlBuilder a(char c) {
        sb.append(c);
        return this;
    }

    public SqlBuilder _(String text) {
        sb.append(text);
        return this;
    }

    public SqlBuilder select(CharSequence fields) {
        sb.append(fields);
        return this;
    }

    private final StringBuilder sb = new StringBuilder("select ");

    public SqlBuilder nl() {
        sb.append('\n');
        return this;
    }

    public SqlBuilder from(String table) {
        sb.append(table).append('\n');
        return this;
    }

    public SqlBuilder as(String alias) {
        sb.append(alias).append(' ');
        return this;
    }

    public SqlBuilder join(String table) {
        sb.append("join ").append(table).append(' ');
        return this;
    }


    public SqlBuilder leftJoin(String table) {
        sb.append("left join ").append(table).append(' ');
        return this;
    }

    public SqlBuilder on() {
        sb.append("on ");
        return this;
    }

    public StringBuilder sb() {
        return sb;
    }
}
