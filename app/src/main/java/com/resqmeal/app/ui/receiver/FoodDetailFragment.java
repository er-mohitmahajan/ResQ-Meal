package com.resqmeal.app.ui.receiver;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentFoodDetailReceiverBinding;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.viewmodel.ClaimViewModel;
import com.resqmeal.app.viewmodel.FoodPostViewModel;
import com.resqmeal.app.views.ConfirmDialog;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class FoodDetailFragment extends Fragment {

    private static final String ARG_POST = "arg_post";

    private FragmentFoodDetailReceiverBinding binding;
    private ClaimViewModel claimViewModel;
    private FoodPost post;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;

    public static FoodDetailFragment newInstance(FoodPost post) {
        FoodDetailFragment fragment = new FoodDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_POST, post);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            post = (FoodPost) getArguments().getSerializable(ARG_POST);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFoodDetailReceiverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        claimViewModel = new ViewModelProvider(this).get(ClaimViewModel.class);

        updateUI();
        setupListeners();
        setupObservers();
        startExpiryCountdown();
    }

    private void updateUI() {
        if (binding == null || post == null) return;

        binding.tvTitle.setText(valueOrEmpty(post.getTitle()));
        binding.tvQuantityServings.setText(String.format(Locale.getDefault(), "%s %s • feeds ~%d",
                post.getQuantity(), valueOrEmpty(post.getUnit()), post.getServings()));
        binding.tvDescription.setText(post.getDescription() == null || post.getDescription().trim().isEmpty()
                ? "No description provided." : post.getDescription());
        binding.tvPreparedAt.setText(post.getPreparedAt() == null
                ? "Not provided" : dateTimeFormat.format(post.getPreparedAt().toDate()));
        binding.tvPickupAddress.setText(valueOrEmpty(post.getPickupAddress()));
        binding.tvPickupInstructions.setText(post.getPickupInstructions() == null
                || post.getPickupInstructions().trim().isEmpty()
                ? "No specific pickup instructions provided." : post.getPickupInstructions());
        binding.tvDonorName.setText(valueOrEmpty(post.getDonorName()));
        binding.tvDonorType.setText(capitalize(post.getDonorType()));
        binding.tvFoodType.setText(
                capitalize(post.getFoodType()) + " • " + categoryLabel(post.getCategory()));
        
        binding.chipStatus.setStatus(post.getStatus());

        Glide.with(this)
                .load(post.getPhotoUrl())
                .centerCrop()
                .placeholder(R.color.shimmer_base)
                .into(binding.ivFoodPhoto);

        Glide.with(this)
                .load(R.drawable.ic_nav_profile)
                .into(binding.ivDonorPhoto);

        boolean available = Constants.STATUS_AVAILABLE.equals(post.getStatus())
                && post.getExpiryTime() != null
                && post.getExpiryTime().toDate().getTime() > System.currentTimeMillis();
        binding.btnClaim.setEnabled(available);
        binding.btnClaim.setAlpha(available ? 1f : 0.5f);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String categoryLabel(String category) {
        if (Constants.CATEGORY_COOKED_MEAL.equals(category)) return "Cooked meal";
        if (Constants.CATEGORY_BAKERY.equals(category)) return "Bakery";
        if (Constants.CATEGORY_FRUITS_VEG.equals(category)) return "Fruits & vegetables";
        if (Constants.CATEGORY_PACKAGED.equals(category)) return "Packaged";
        if (Constants.CATEGORY_OTHER.equals(category)) return "Other";
        return "Category unavailable";
    }

    private void setupListeners() {
        binding.btnClaim.setOnClickListener(v -> ConfirmDialog.show(
                requireContext(),
                "Claim Surplus Food?",
                "Are you sure you want to claim this food? You must pick it up before it expires.",
                "Yes, Claim",
                "Cancel",
                () -> claimViewModel.claimFood(post),
                null
        ));
    }

    private void setupObservers() {
        claimViewModel.getClaimStatus().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.btnClaim.setLoading(true);
                    break;
                case SUCCESS:
                    binding.btnClaim.setLoading(false);
                    if (resource.data != null) {
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.role_fragment_container,
                                        ClaimDetailFragment.newInstance(resource.data))
                                .addToBackStack(null)
                                .commit();
                    }
                    break;
                case ERROR:
                    binding.btnClaim.setLoading(false);
                    if (resource.message != null && resource.message.contains("Someone just claimed")) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.claim_already_taken_title)
                                .setMessage(R.string.claim_already_taken_message)
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                    new ViewModelProvider(requireActivity())
                                            .get(FoodPostViewModel.class)
                                            .refreshNearbyPosts();
                                    getParentFragmentManager().popBackStack();
                                })
                                .show();
                    } else {
                        Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        });
    }

    private void startExpiryCountdown() {
        if (post == null || post.getExpiryTime() == null) {
            binding.tvExpiryCountdown.setText("Expiry unavailable");
            binding.btnClaim.setEnabled(false);
            return;
        }
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long diff = post.getExpiryTime().toDate().getTime() - System.currentTimeMillis();
                if (diff <= 0) {
                    binding.tvExpiryCountdown.setText("Expired");
                    binding.tvExpiryCountdown.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.error));
                    binding.btnClaim.setEnabled(false);
                    binding.btnClaim.setAlpha(0.5f);
                    return;
                }

                long totalMinutes = (diff + 59_999L) / 60_000L;
                long hours = totalMinutes / 60;
                long minutes = totalMinutes % 60;
                String countdown = hours > 0
                        ? String.format(Locale.getDefault(), "Expires in %dh %dm", hours, minutes)
                        : String.format(Locale.getDefault(), "Expires in %dm", minutes);
                binding.tvExpiryCountdown.setText(countdown);
                binding.tvExpiryCountdown.setTextColor(ContextCompat.getColor(
                        requireContext(), diff < 60 * 60_000L ? R.color.accent : R.color.warning));
                
                countdownHandler.postDelayed(this, 60000);
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        String normalized = str.replace("_", " ");
        return normalized.substring(0, 1).toUpperCase(Locale.getDefault())
                + normalized.substring(1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        countdownHandler.removeCallbacks(countdownRunnable);
        binding = null;
    }
}
