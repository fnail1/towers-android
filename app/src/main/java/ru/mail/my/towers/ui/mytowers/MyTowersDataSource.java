package ru.mail.my.towers.ui.mytowers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.mail.my.towers.model.Tower;
import ru.mail.my.towers.model.TowerNetwork;
import ru.mail.my.towers.ui.PagedDataSource;

import static ru.mail.my.towers.TowersApp.data;

public class MyTowersDataSource extends PagedDataSource<Tower> {
    private final ArrayList<TowerNetwork> networks;
    private final ArrayList<Tower> towers;
    private final int[] index;
    private final int count;

    MyTowersDataSource() {
        super();
        networks = data().networks().selectMy();
        index = new int[networks.size()];
        int c = 0;
        int idx = 0;
        for (TowerNetwork network : networks) {
            c += network.count;
            index[idx] = c + idx;
            idx++;
        }
        count = c + networks.size();
        towers = new ArrayList<>(c);
    }

    @Override
    protected List<Tower> prepareDataSync(int skip, int limit) {
        int start = Arrays.binarySearch(index, skip);
        int end = Arrays.binarySearch(index, start, index.length, skip + limit);
//        ArrayList<Tower> batch = data().towers().selectMy(skip - start, limit - (end - start));
        synchronized (towers) {
//            towers.addAll(batch);
        }

        return towers.subList(0,0);
    }

    @Override
    public int count() {
        return count;
    }
}
