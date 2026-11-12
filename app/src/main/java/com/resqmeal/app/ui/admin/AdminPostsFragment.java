package com.resqmeal.app.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resqmeal.app.R;
import com.resqmeal.app.adapter.AdminPostAdapter;
import com.resqmeal.app.databinding.FragmentAdminListBinding;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.utils.Resource;
import com.resqmeal.app.viewmodel.AdminViewModel;

public class AdminPostsFragment extends Fragment {
    private FragmentAdminListBinding binding;
    private AdminViewModel viewModel;
    private AdminPostAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAdminListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
        binding.etSearch.setVisibility(View.GONE);
        binding.rvItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminPostAdapter(this::confirmRemovePost);
        binding.rvItems.setAdapter(adapter);
        binding.emptyState.hideAction();
        observeData();
        observeActions();
        viewModel.loadPosts();
    }

    private void observeData() {
        viewModel.getPosts().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null || resource == null) {
                return;
            }
            switch (resource.status) {
                case LOADING:
                    binding.progressLoading.setVisibility(View.VISIBLE);
                    binding.rvItems.setVisibility(View.GONE);
                    binding.emptyState.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    binding.progressLoading.setVisibility(View.GONE);
                    if (resource.data == null || resource.data.isEmpty()) {
                        binding.rvItems.setVisibility(View.GONE);
                        binding.emptyState.setContent(
                                getString(R.string.admin_no_posts_title),
                                getString(R.string.admin_no_posts_message));
                        binding.emptyState.hideAction();
                        binding.emptyState.setVisibility(View.VISIBLE);
                    } else {
                        binding.emptyState.setVisibility(View.GONE);
                        binding.rvItems.setVisibility(View.VISIBLE);
                        adapter.setPosts(resource.data);
                    }
                    break;
                case ERROR:
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.rvItems.setVisibility(View.GONE);
                    binding.emptyState.setContent(
                            getString(R.string.admin_load_error_title), resource.message);
                    Log.d("Create Index", "Index :" + resource.message);
                    binding.emptyState.setAction(
                            getString(R.string.retry), action -> viewModel.loadPosts());
                    binding.emptyState.setVisibility(View.VISIBLE);
                    break;
            }
        });
    }

    private void observeActions() {
        viewModel.getActionStatus().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null || resource == null || resource.status == Resource.Status.LOADING) {
                return;
            }
            if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), R.string.admin_post_removed, Toast.LENGTH_SHORT).show();
                viewModel.loadPosts();
            }
        });
    }

    private void confirmRemovePost(FoodPost post) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_remove_post_title)
                .setMessage(getString(R.string.admin_remove_post_message, post.getTitle()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.admin_remove_post, (dialog, which) ->
                        viewModel.removePost(post.getPostId()))
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadPosts();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }
}
