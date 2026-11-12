package com.resqmeal.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.resqmeal.app.databinding.ItemNotificationBinding;
import com.resqmeal.app.model.AppNotification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    public interface OnMarkReadListener {
        void onMarkRead(AppNotification notification);
    }

    private final OnMarkReadListener listener;
    private final List<AppNotification> notifications = new ArrayList<>();

    public NotificationAdapter(OnMarkReadListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<AppNotification> notifications) {
        this.notifications.clear();
        if (notifications != null) {
            this.notifications.addAll(notifications);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(notifications.get(position));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;
        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());

        ViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AppNotification notification) {
            binding.tvNotificationTitle.setText(notification.getTitle());
            binding.tvNotificationBody.setText(notification.getBody());
            binding.tvNotificationTime.setText(notification.getCreatedAt() == null
                    ? "" : dateFormat.format(notification.getCreatedAt().toDate()));
            binding.btnMarkRead.setOnClickListener(v -> listener.onMarkRead(notification));
        }
    }
}
