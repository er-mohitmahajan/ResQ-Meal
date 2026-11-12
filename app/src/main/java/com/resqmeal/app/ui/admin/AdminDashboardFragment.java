package com.resqmeal.app.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentAdminDashboardBinding;
import com.resqmeal.app.model.AdminDashboardStats;
import com.resqmeal.app.ui.shell.BaseRoleActivity;
import com.resqmeal.app.utils.Resource;
import com.resqmeal.app.viewmodel.AdminViewModel;

public class AdminDashboardFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private AdminViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
        setupObservers();
        binding.btnManageUsers.setOnClickListener(v -> openSubscreen(
                new AdminUsersFragment(), getString(R.string.admin_users_title)));
        binding.btnManagePosts.setOnClickListener(v -> openSubscreen(
                new AdminPostsFragment(), getString(R.string.admin_posts_title)));
        binding.btnOpenApprovals.setOnClickListener(v ->
                ((BottomNavigationView) requireActivity().findViewById(R.id.bottom_navigation))
                        .setSelectedItemId(R.id.nav_approvals));
        binding.btnOpenReports.setOnClickListener(v ->
                ((BottomNavigationView) requireActivity().findViewById(R.id.bottom_navigation))
                        .setSelectedItemId(R.id.nav_reports));
        viewModel.loadDashboard();
    }

    private void setupObservers() {
        viewModel.getDashboardStats().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null || resource == null) {
                return;
            }
            switch (resource.status) {
                case LOADING:
                    binding.progressLoading.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.progressLoading.setVisibility(View.GONE);
                    if (resource.data != null) {
                        renderStats(resource.data);
                    }
                    break;
                case ERROR:
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.tvDashboardError.setText(resource.message);
                    // Log.d("Create Index", "Index : " + resource.message);
                    binding.tvDashboardError.setVisibility(View.VISIBLE);
                    break;
            }
        });
    }

    private void renderStats(AdminDashboardStats stats) {
        binding.tvDashboardError.setVisibility(View.GONE);
        binding.statUsers.setStat(String.valueOf(stats.getTotalUsers()), getString(R.string.admin_total_users), R.drawable.ic_nav_profile);
        binding.statDonors.setStat(String.valueOf(stats.getDonors()), getString(R.string.admin_donors), R.drawable.ic_nav_home);
        binding.statReceivers.setStat(String.valueOf(stats.getReceivers()), getString(R.string.admin_receivers), R.drawable.ic_nav_claims);
        binding.statActivePosts.setStat(String.valueOf(stats.getActivePosts()), getString(R.string.admin_active_posts), R.drawable.ic_nav_posts);
        binding.statPickups.setStat(String.valueOf(stats.getCompletedPickups()), getString(R.string.admin_completed_pickups), R.drawable.ic_nav_claims);
        binding.statMeals.setStat(String.valueOf(stats.getMealsSaved()), getString(R.string.admin_meals_saved), R.drawable.ic_nav_home);
        binding.statApprovals.setStat(String.valueOf(stats.getPendingApprovals()), getString(R.string.admin_pending_approvals), R.drawable.ic_nav_approvals);
        binding.statReports.setStat(String.valueOf(stats.getOpenReports()), getString(R.string.admin_open_reports), R.drawable.ic_nav_reports);
    }

    private void openSubscreen(Fragment fragment, String title) {
        if (requireActivity() instanceof BaseRoleActivity) {
            ((BaseRoleActivity) requireActivity()).openAdminSubscreen(fragment, title);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadDashboard();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
