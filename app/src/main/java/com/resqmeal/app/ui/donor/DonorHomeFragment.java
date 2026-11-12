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

import com.resqmeal.app.R;
import com.resqmeal.app.adapter.FoodPostAdapter;
import com.resqmeal.app.databinding.FragmentDonorHomeBinding;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.model.User;
import com.resqmeal.app.viewmodel.FoodPostViewModel;
import com.resqmeal.app.viewmodel.ProfileViewModel;

import java.util.List;

public class DonorHomeFragment extends Fragment {

    private FragmentDonorHomeBinding binding;
    private ProfileViewModel profileViewModel;
    private FoodPostViewModel foodPostViewModel;
    private FoodPostAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDonorHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        foodPostViewModel = new ViewModelProvider(this).get(FoodPostViewModel.class);

        setupRecyclerView();
        setupListeners();
        setupObservers();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (profileViewModel != null && foodPostViewModel != null) {
            profileViewModel.fetchCurrentUser();
            foodPostViewModel.fetchDonorPosts("All");
        }
    }

    private void setupRecyclerView() {
        adapter = new FoodPostAdapter(post -> {
            PostDetailFragment fragment = PostDetailFragment.newInstance(post);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.role_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvRecentPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentPosts.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.fabAddFood.setOnClickListener(v -> openAddFood());
    }

    private void openAddFood() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.role_fragment_container, new AddFoodFragment())
                .addToBackStack(null)
                .commit();
    }

    private void setupObservers() {
        profileViewModel.getUserData().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == com.resqmeal.app.utils.Resource.Status.SUCCESS && resource.data != null) {
                updateImpactStats(resource.data);
            }
        });

        foodPostViewModel.getDonorPosts().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.shimmerLoading.getRoot().setVisibility(View.VISIBLE);
                    binding.shimmerLoading.shimmerLayout.startShimmer();
                    binding.emptyState.setVisibility(View.GONE);
                    binding.rvRecentPosts.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    binding.shimmerLoading.shimmerLayout.stopShimmer();
                    binding.shimmerLoading.getRoot().setVisibility(View.GONE);
                    binding.rvRecentPosts.setVisibility(View.VISIBLE);
                    List<FoodPost> posts = resource.data;
                    if (posts == null || posts.isEmpty()) {
                        binding.emptyState.setVisibility(View.VISIBLE);
                        binding.emptyState.setContent("No Posts Found", "You haven't posted any food yet.");
                        binding.emptyState.setAction("Add Food", v -> openAddFood());
                        adapter.setPosts(new java.util.ArrayList<>());
                    } else {
                        binding.emptyState.setVisibility(View.GONE);
                        List<FoodPost> recent = posts.size() > 5 ? posts.subList(0, 5) : posts;
                        adapter.setPosts(recent);
                    }
                    break;
                case ERROR:
                    binding.shimmerLoading.shimmerLayout.stopShimmer();
                    binding.shimmerLoading.getRoot().setVisibility(View.GONE);
                    binding.rvRecentPosts.setVisibility(View.GONE);
                    binding.emptyState.setVisibility(View.VISIBLE);
                    binding.emptyState.setContent("Could not load posts", resource.message);
                    binding.emptyState.hideAction();
                    break;
            }
        });
    }

    private void updateImpactStats(User user) {
        if (binding != null) {
            binding.statMealsDonated.setStat(String.valueOf(user.getTotalMealsDonated()), "Meals Donated", R.drawable.ic_nav_home);
            binding.statTotalPosts.setStat(String.valueOf(user.getTotalPosted()), "Food Posts", R.drawable.ic_nav_posts);
        }
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
