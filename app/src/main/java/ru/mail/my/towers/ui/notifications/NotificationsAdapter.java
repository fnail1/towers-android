package ru.mail.my.towers.ui.notifications;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.mail.my.towers.R;
import ru.mail.my.towers.model.Notification;
import ru.mail.my.towers.utils.Utils;

public class NotificationsAdapter extends RecyclerView.Adapter {

    private NotificationsDataSource dataSource;
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
    public int getItemViewType(int position) {
        if (position == 0 ||
                dataSource.get(position - 1).getTime().get(Calendar.DAY_OF_MONTH) != dataSource.get(position).getTime().get(Calendar.DAY_OF_MONTH)) {
            return R.layout.item_notification_with_date;
        }
        return R.layout.item_notification;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case R.layout.item_notification_with_date:
                return new NotificationWithDateViewHolder(LayoutInflater.from(context).inflate(R.layout.item_notification_with_date, parent, false));
            case R.layout.item_notification:
                return new NotificationViewHolder(LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false));
            default:
                throw new IllegalArgumentException("" + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((NotificationViewHolder) holder).bind(dataSource.get(position));
    }

    @Override
    public int getItemCount() {
        return dataSource == null ? 0 : dataSource.count();
    }

    public void init() {
        dataSource = new NotificationsDataSource();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.time)
        TextView time;

        @BindView(R.id.message)
        TextView message;

        public NotificationViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(Notification notification) {
            time.setText(String.format(Locale.getDefault(), "%02d:%02d",
                    notification.getTime().get(Calendar.HOUR_OF_DAY),
                    notification.getTime().get(Calendar.MINUTE)));
            message.setText(notification.message);
            int foreColor = notification.type.getTextColor(context);
            time.setTextColor(foreColor);
            message.setTextColor(foreColor);
        }
    }

    class NotificationWithDateViewHolder extends NotificationViewHolder {
        @BindView(R.id.date)
        TextView date;

        public NotificationWithDateViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void bind(Notification notification) {
            super.bind(notification);
            date.setText(Utils.formatDate(notification.getTime()));
        }
    }
}
