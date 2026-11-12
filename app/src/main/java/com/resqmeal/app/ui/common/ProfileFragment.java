package com.resqmeal.app.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentProfileBinding;
import com.resqmeal.app.model.User;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.RoleRouter;
import com.resqmeal.app.viewmodel.ProfileViewModel;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupObservers();
        setupListeners();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.fetchCurrentUser();
        }
    }

    private void setupObservers() {
        viewModel.getUserData().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.shimmerProfile.shimmerLayout.startShimmer();
                    binding.shimmerProfile.getRoot().setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.shimmerProfile.shimmerLayout.stopShimmer();
                    binding.shimmerProfile.getRoot().setVisibility(View.GONE);
                    if (resource.data != null) {
                        updateUI(resource.data);
                    }
                    break;
                case ERROR:
                    binding.shimmerProfile.shimmerLayout.stopShimmer();
                    binding.shimmerProfile.getRoot().setVisibility(View.GONE);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void setupListeners() {
        binding.btnEditProfile.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.role_fragment_container, new EditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnLogout.setOnClickListener(v -> {
            viewModel.logout();
            RoleRouter.routeToLogin(requireActivity());
        });
    }

    private void updateUI(User user) {
        binding.tvName.setText(user.getName());
        binding.tvEmail.setText(user.getEmail());
        binding.tvPhone.setText(user.getPhone());
        binding.tvAddress.setText(user.getAddress() != null ? user.getAddress() : "No address set");
        
        binding.chipRole.setChipText(capitalize(user.getRole()));
        
        // Impact stats
        if (Constants.ROLE_DONOR.equals(user.getRole())) {
            binding.statMeals.setStat(String.valueOf(user.getTotalMealsDonated()), "Meals Donated", R.drawable.ic_nav_home);
            binding.statAction.setStat(String.valueOf(user.getTotalPosted()), "Food Posts", R.drawable.ic_nav_posts);
        } else {
            binding.statMeals.setStat(String.valueOf(user.getTotalMealsReceived()), "Meals Received", R.drawable.ic_nav_home);
            binding.statAction.setStat(String.valueOf(user.getTotalClaimed()), "Food Claims", R.drawable.ic_nav_claims);
        }

        Glide.with(this)
                .load(user.getProfilePhotoUrl())
                .placeholder(R.drawable.ic_nav_profile)
                .error(R.drawable.ic_nav_profile)
                .into(binding.ivProfilePhoto);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
