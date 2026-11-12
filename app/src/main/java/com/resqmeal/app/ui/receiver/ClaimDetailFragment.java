package com.resqmeal.app.ui.receiver;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentClaimDetailBinding;
import com.resqmeal.app.model.Claim;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.Resource;
import com.resqmeal.app.viewmodel.ClaimViewModel;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ClaimDetailFragment extends Fragment {
    private static final String ARG_CLAIM_ID = "claim_id";

    private FragmentClaimDetailBinding binding;
    private ClaimViewModel viewModel;
    private Claim claim;
    private FoodPost post;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());

    public static ClaimDetailFragment newInstance(String claimId) {
        ClaimDetailFragment fragment = new ClaimDetailFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_CLAIM_ID, claimId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentClaimDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ClaimViewModel.class);
        setupObservers();
        binding.btnNavigate.setOnClickListener(v -> navigateToPickup());
        binding.btnCancelClaim.setOnClickListener(v -> confirmCancel());
        viewModel.fetchClaim(getArguments() == null ? null : getArguments().getString(ARG_CLAIM_ID));
    }

    private void setupObservers() {
        viewModel.getSelectedClaim().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) {
                return;
            }
            if (resource.status == Resource.Status.LOADING) {
                binding.progressLoading.setVisibility(View.VISIBLE);
            } else if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                binding.progressLoading.setVisibility(View.GONE);
                binding.claimContent.setVisibility(View.VISIBLE);
                claim = resource.data;
                renderClaim();
            } else if (resource.status == Resource.Status.ERROR) {
                binding.progressLoading.setVisibility(View.GONE);
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                getParentFragmentManager().popBackStack();
            }
        });

        viewModel.getClaimPost().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) {
                return;
            }
            if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                post = resource.data;
                renderPost();
            } else if (resource.status == Resource.Status.ERROR) {
                binding.claimContent.setVisibility(View.VISIBLE);
                binding.tvPickupAddress.setText(R.string.claim_location_unavailable);
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getActionStatus().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) {
                return;
            }
            switch (resource.status) {
                case LOADING:
                    binding.btnCancelClaim.setEnabled(false);
                    break;
                case SUCCESS:
                    binding.btnCancelClaim.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.claim_cancelled, Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    break;
                case ERROR:
                    binding.btnCancelClaim.setEnabled(true);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void renderClaim() {
        binding.tvTitle.setText(claim.getPostTitle() == null ? "" : claim.getPostTitle());
        binding.tvDonor.setText(getString(
                R.string.claim_donor, claim.getDonorName() == null ? "" : claim.getDonorName()));
        binding.tvClaimedAt.setText(claim.getClaimedAt() == null
                ? "" : dateFormat.format(claim.getClaimedAt().toDate()));

        boolean active = Constants.CLAIM_STATUS_ACTIVE.equals(claim.getStatus());
        binding.chipStatus.setChipText(active
                ? getString(R.string.claim_status_active)
                : Constants.CLAIM_STATUS_COMPLETED.equals(claim.getStatus())
                ? getString(R.string.claim_status_completed)
                : getString(R.string.claim_status_cancelled));
        binding.tvPickupCode.setText(active && claim.getPickupCode() != null
                ? claim.getPickupCode() : getString(R.string.claim_code_unavailable));
        binding.tvPickupCode.setVisibility(active ? View.VISIBLE : View.GONE);
        binding.btnCancelClaim.setVisibility(active ? View.VISIBLE : View.GONE);
        binding.btnNavigate.setEnabled(active && post != null && post.getLat() != null && post.getLng() != null);
    }

    private void renderPost() {
        if (post == null) {
            return;
        }
        binding.tvPickupAddress.setText(post.getPickupAddress() == null
                ? getString(R.string.claim_location_unavailable) : post.getPickupAddress());
        binding.tvPickupInstructions.setText(post.getPickupInstructions() == null
                || post.getPickupInstructions().trim().isEmpty()
                ? getString(R.string.claim_no_instructions) : post.getPickupInstructions());
        boolean active = claim != null && Constants.CLAIM_STATUS_ACTIVE.equals(claim.getStatus());
        boolean notExpired = post.getExpiryTime() != null
                && post.getExpiryTime().toDate().getTime() > System.currentTimeMillis();
        binding.btnNavigate.setEnabled(active && notExpired
                && post.getLat() != null && post.getLng() != null);
    }

    private void confirmCancel() {
        if (claim == null || !Constants.CLAIM_STATUS_ACTIVE.equals(claim.getStatus())) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.claim_cancel_title)
                .setMessage(R.string.claim_cancel_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.claim_cancel, (dialog, which) ->
                        viewModel.cancelClaim(claim.getClaimId()))
                .show();
    }

    private void navigateToPickup() {
        if (post == null || post.getLat() == null || post.getLng() == null) {
            Toast.makeText(requireContext(), R.string.claim_location_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        Uri locationUri = Uri.parse(String.format(
                Locale.US,
                "geo:%f,%f?q=%f,%f(%s)",
                post.getLat(),
                post.getLng(),
                post.getLat(),
                post.getLng(),
                Uri.encode(post.getTitle() == null ? "Pickup location" : post.getTitle())
        ));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, locationUri);
        if (mapIntent.resolveActivity(requireContext().getPackageManager()) == null) {
            Toast.makeText(requireContext(), R.string.claim_maps_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(mapIntent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
