package com.resqmeal.app.ui.donor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.resqmeal.app.R;
import com.resqmeal.app.adapter.FoodPostAdapter;
import com.resqmeal.app.databinding.FragmentMyPostsBinding;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.viewmodel.FoodPostViewModel;

import java.util.List;

public class MyPostsFragment extends Fragment {

    private FragmentMyPostsBinding binding;
    private FoodPostViewModel viewModel;
    private FoodPostAdapter adapter;
    private String currentFilter = "All";

    private final String[] statuses = {"All", Constants.STATUS_AVAILABLE, Constants.STATUS_CLAIMED, 
            Constants.STATUS_COMPLETED, Constants.STATUS_EXPIRED, Constants.STATUS_CANCELLED};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyPostsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FoodPostViewModel.class);

        setupTabLayout();
        setupRecyclerView();
        setupObservers();
        setupSwipeRefresh();

    }

    private void setupTabLayout() {
        for (String status : statuses) {
            binding.tabLayoutStatus.addTab(binding.tabLayoutStatus.newTab().setText(capitalize(status)));
        }
        int selectedPosition = 0;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equals(currentFilter)) {
                selectedPosition = i;
                break;
            }
        }
        binding.tabLayoutStatus.selectTab(binding.tabLayoutStatus.getTabAt(selectedPosition));

        binding.tabLayoutStatus.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = statuses[tab.getPosition()];
                fetchPosts();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            fetchPosts();
        }
    }

    private void setupRecyclerView() {
        adapter = new FoodPostAdapter(post -> {
            // Navigate to Post Detail
            PostDetailFragment fragment = PostDetailFragment.newInstance(post);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.role_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvMyPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMyPosts.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getDonorPosts().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    if (!binding.swipeRefresh.isRefreshing()) {
                        binding.shimmerLoading.getRoot().setVisibility(View.VISIBLE);
                        binding.shimmerLoading.shimmerLayout.startShimmer();
                        binding.rvMyPosts.setVisibility(View.GONE);
                    }
                    binding.emptyState.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    binding.shimmerLoading.shimmerLayout.stopShimmer();
                    binding.shimmerLoading.getRoot().setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    binding.rvMyPosts.setVisibility(View.VISIBLE);
                    List<FoodPost> posts = resource.data;
                    if (posts == null || posts.isEmpty()) {
                        binding.emptyState.setVisibility(View.VISIBLE);
                        binding.emptyState.setContent("Nothing here", "No posts found for this status.");
                        adapter.setPosts(new java.util.ArrayList<>());
                    } else {
                        binding.emptyState.setVisibility(View.GONE);
                        adapter.setPosts(posts);
                    }
                    break;
                case ERROR:
                    binding.shimmerLoading.shimmerLayout.stopShimmer();
                    binding.shimmerLoading.getRoot().setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    binding.rvMyPosts.setVisibility(View.GONE);
                    binding.emptyState.setVisibility(View.VISIBLE);
                    binding.emptyState.setContent("Could not load posts", resource.message);
                    adapter.setPosts(new java.util.ArrayList<>());
                    break;
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::fetchPosts);
    }

    private void fetchPosts() {
        viewModel.fetchDonorPosts(currentFilter);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        if (str.contains("_")) {
            String[] parts = str.split("_");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1)).append(" ");
            }
            return sb.toString().trim();
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.shimmerLoading.shimmerLayout.stopShimmer();
        }
        super.onDestroyView();
        binding = null;
    }
}
