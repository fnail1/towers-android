package ru.mail.my.towers.model.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import ru.mail.my.towers.data.CursorWrapper;
import ru.mail.my.towers.data.DbUtils;
import ru.mail.my.towers.ui.mytowers.MyTowersListItem;

import static ru.mail.my.towers.model.db.ColumnNames.*;

public class MyTowersCommands {
    private final SQLiteDatabase db;

    public MyTowersCommands(SQLiteDatabase db) {
        this.db = db;
    }

    public int count() {
        return DbUtils.count(db, "select count(*) from " + AppData.TABLE_TOWER_NETWORKS + " where my=1", (String[]) null) +
                DbUtils.count(db, "select count(*) from " + AppData.TABLE_TOWERS + " where my=1", (String[]) null);
    }

    public CursorWrapper<MyTowersListItem> select(int skip, int limit) {

        String sql = "select * \n" +
                "from (\n" +
                "\tselect _id as " + NETWORK + ", 0 as " + TOWER + ", " + COUNT + ", " + AREA + ", " + GOLD_GAIN + ", " + LEVEL + ", " + HEALTH + ", " + MAX_HEALTH + ", 0 as " + RADIUS + ", 0 as " + REPAIR_COST + ", null as " + TITLE + ", 0 as " + UPDATE_COST + "\n" +
                "\tfrom TowerNetworks\n" +
                "\twhere my=1\n" +
                "\n" +
                "\tunion \n" +
                "\n" +
                "\tselect network as " + NETWORK + ", _id as " + TOWER + ", 0 as " + COUNT + ", 0 as " + AREA + ", " + GOLD_GAIN + ", " + LEVEL + ", " + HEALTH + ", " + MAX_HEALTH + ", " + RADIUS + ", " + REPAIR_COST + ", " + TITLE + ", " + UPDATE_COST + "\n" +
                "\tfrom Towers\n" +
                "\twhere my=1\n" +
                ")\n" +
                "order by " + NETWORK + ", " + COUNT + ", " + TOWER + "\n " +
                "limit " + limit + " offset " + skip;

        return new MyTowersCursor(db.rawQuery(sql, null));
    }

    private static class MyTowersCursor extends CursorWrapper<MyTowersListItem> {
        int networkColumnIndex;
        int towerColumnIndex;
        int countColumnIndex;
        int goldGainColumnIndex;
        int levelColumnIndex;
        int healthColumnIndex;
        int maxHealthColumnIndex;
        int radiusColumnIndex;
        int repairCostColumnIndex;
        int titleColumnIndex;
        int updateCostColumnIndex;


        public MyTowersCursor(Cursor cursor) {
            super(cursor);
            networkColumnIndex = cursor.getColumnIndex(NETWORK);
            towerColumnIndex = cursor.getColumnIndex(TOWER);
            countColumnIndex = cursor.getColumnIndex(COUNT);
            goldGainColumnIndex = cursor.getColumnIndex(GOLD_GAIN);
            levelColumnIndex = cursor.getColumnIndex(LEVEL);
            healthColumnIndex = cursor.getColumnIndex(HEALTH);
            maxHealthColumnIndex = cursor.getColumnIndex(MAX_HEALTH);
            radiusColumnIndex = cursor.getColumnIndex(RADIUS);
            repairCostColumnIndex = cursor.getColumnIndex(REPAIR_COST);
            titleColumnIndex = cursor.getColumnIndex(TITLE);
            updateCostColumnIndex = cursor.getColumnIndex(UPDATE_COST);
        }

        @Override
        protected MyTowersListItem get(Cursor cursor) {
            MyTowersListItem item = new MyTowersListItem();
            item.network = cursor.getLong(networkColumnIndex);
            item.tower = cursor.getLong(towerColumnIndex);
            item.count = cursor.getInt(countColumnIndex);
            item.goldGain = cursor.getInt(goldGainColumnIndex);
            item.level = cursor.getFloat(levelColumnIndex);
            item.health = cursor.getInt(healthColumnIndex);
            item.maxHealth = cursor.getInt(maxHealthColumnIndex);
            item.radius = cursor.getDouble(radiusColumnIndex);
            item.repairCost = cursor.getInt(repairCostColumnIndex);
            item.title = cursor.getString(titleColumnIndex);
            item.updateCost = cursor.getInt(updateCostColumnIndex);


            return item;
        }
    }
}
