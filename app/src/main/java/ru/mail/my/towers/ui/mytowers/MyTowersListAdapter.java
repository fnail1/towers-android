package ru.mail.my.towers.ui.mytowers;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.mail.my.towers.R;

public class MyTowersListAdapter extends RecyclerView.Adapter {

    private Context context;

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
        return new TowerViewHolder(LayoutInflater.from(context).inflate(R.layout.item_my_tower_info, parent));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
//        ((TowerViewHolder) holder).bind();
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class TowerViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.caption)
        TextView caption;
        @BindView(R.id.content)
        TextView content;

        public TowerViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
