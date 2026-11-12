package com.resqmeal.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.resqmeal.app.R;
import com.resqmeal.app.databinding.ItemAdminPostBinding;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class AdminPostAdapter extends RecyclerView.Adapter<AdminPostAdapter.ViewHolder> {
    public interface Listener {
        void onRemovePost(FoodPost post);
    }

    private final Listener listener;
    private final List<FoodPost> posts = new ArrayList<>();

    public AdminPostAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setPosts(List<FoodPost> posts) {
        this.posts.clear();
        if (posts != null) {
            this.posts.addAll(posts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemAdminPostBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(posts.get(position));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminPostBinding binding;

        ViewHolder(ItemAdminPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FoodPost post) {
            binding.tvPostTitle.setText(post.getTitle() == null ? "" : post.getTitle());
            binding.tvPostDonor.setText(post.getDonorName() == null ? "" : post.getDonorName());
            binding.tvPostStatus.setText(post.getStatus() == null ? "" : post.getStatus());
            boolean removable = !Constants.STATUS_CANCELLED.equals(post.getStatus());
            binding.btnRemovePost.setVisibility(removable ? View.VISIBLE : View.GONE);
            binding.btnRemovePost.setText(R.string.admin_remove_post);
            binding.btnRemovePost.setOnClickListener(v -> listener.onRemovePost(post));
        }
    }
}
