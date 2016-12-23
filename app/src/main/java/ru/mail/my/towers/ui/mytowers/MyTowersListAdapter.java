package ru.mail.my.towers.ui.mytowers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.mail.my.towers.R;
import ru.mail.my.towers.utils.Utils;

public class MyTowersListAdapter extends RecyclerView.Adapter {

    private Context context;
    private MyTowersDataSource dataSource;

    public MyTowersListAdapter() {

    }

    @Override
    public int getItemViewType(int position) {
        MyTowersListItem item = dataSource.get(position);
        return item.tower == 0
                ? R.layout.item_my_network
                : R.layout.item_my_tower;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        context = recyclerView.getContext();
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        context = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case R.layout.item_my_network:
                return new NetworkViewHolder(LayoutInflater.from(context).inflate(R.layout.item_my_network, parent, false));
            case R.layout.item_my_tower:
                return new TowerViewHolder(LayoutInflater.from(context).inflate(R.layout.item_my_tower, parent, false));
        }
        throw new IllegalArgumentException(String.valueOf(viewType));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MyTowersListItem item = dataSource.get(position);
        switch (holder.getItemViewType()) {
            case R.layout.item_my_network:
                ((NetworkViewHolder) holder).bind(item);
                break;
            case R.layout.item_my_tower:
                ((TowerViewHolder) holder).bind(item);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return dataSource == null ? 0 : dataSource.count();
    }

    public void init() {
        dataSource = new MyTowersDataSource();
    }

    public static class TowerViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.caption)
        TextView caption;

        public TowerViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(MyTowersListItem item) {
            caption.setText(item.title + " LV: " + item.level + " (" + item.health + "/" + item.maxHealth + ")");
        }
    }

    public class NetworkViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.caption)
        TextView caption;
        @BindView(R.id.level)
        TextView level;
        @BindView(R.id.area)
        TextView area;
        @BindView(R.id.gold)
        TextView gold;
        @BindView(R.id.health)
        TextView health;

        public NetworkViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @SuppressLint("SetTextI18n")
        public void bind(MyTowersListItem item) {
            caption.setText("Сеть " + item.network + " (" + item.count + ")");
            level.setText("LV: " + Utils.formatNetworkLevel(item.level));
            area.setText("AR: " + item.area);
            gold.setText("GD: " + item.goldGain);
            health.setText("HP: " + item.health + "/" + item.maxHealth);
        }

    }

}
