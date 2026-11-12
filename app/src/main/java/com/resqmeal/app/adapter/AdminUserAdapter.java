package com.resqmeal.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.resqmeal.app.R;
import com.resqmeal.app.databinding.ItemAdminUserBinding;
import com.resqmeal.app.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {
    public interface Listener {
        void onApprove(User user);
        void onReject(User user);
        void onToggleActive(User user);
    }

    private final Listener listener;
    private final boolean approvalsMode;
    private final List<User> users = new ArrayList<>();

    public AdminUserAdapter(boolean approvalsMode, Listener listener) {
        this.approvalsMode = approvalsMode;
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.users.clear();
        if (users != null) {
            this.users.addAll(users);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemAdminUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminUserBinding binding;

        ViewHolder(ItemAdminUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user) {
            binding.tvUserName.setText(user.getName() == null ? "" : user.getName());
            binding.tvUserEmail.setText(user.getEmail() == null ? "" : user.getEmail());
            String role = user.getRole() == null ? "User" : user.getRole();
            String phone = user.getPhone() == null || user.getPhone().trim().isEmpty()
                    ? "" : " • " + user.getPhone();
            binding.tvUserMeta.setText(String.format(
                    Locale.getDefault(), "%s%s", role, phone));
            if (approvalsMode) {
                binding.btnApprove.setVisibility(View.VISIBLE);
                binding.btnReject.setVisibility(View.VISIBLE);
                binding.btnToggleActive.setVisibility(View.GONE);
                binding.btnApprove.setOnClickListener(v -> listener.onApprove(user));
                binding.btnReject.setOnClickListener(v -> listener.onReject(user));
            } else {
                binding.btnApprove.setVisibility(View.GONE);
                binding.btnReject.setVisibility(View.GONE);
                binding.btnToggleActive.setVisibility(View.VISIBLE);
                binding.btnToggleActive.setText(user.isActive()
                        ? R.string.admin_deactivate : R.string.admin_activate);
                binding.btnToggleActive.setOnClickListener(v -> listener.onToggleActive(user));
            }
        }
    }
}
