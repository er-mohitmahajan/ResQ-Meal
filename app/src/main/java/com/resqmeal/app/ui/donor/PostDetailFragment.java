package com.resqmeal.app.ui.donor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.DialogVerifyPickupBinding;
import com.resqmeal.app.databinding.FragmentPostDetailBinding;
import com.resqmeal.app.model.Claim;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.viewmodel.ClaimViewModel;
import com.resqmeal.app.viewmodel.FoodPostViewModel;
import com.resqmeal.app.views.ConfirmDialog;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class PostDetailFragment extends Fragment {

    private static final String ARG_POST = "arg_post";

    private FragmentPostDetailBinding binding;
    private FoodPostViewModel viewModel;
    private ClaimViewModel claimViewModel;
    private FoodPost post;
    private DialogVerifyPickupBinding pickupDialogBinding;
    private AlertDialog pickupDialog;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;
    private String loadedClaimId;

    public static PostDetailFragment newInstance(FoodPost post) {
        PostDetailFragment fragment = new PostDetailFragment();
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
        binding = FragmentPostDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FoodPostViewModel.class);
        claimViewModel = new ViewModelProvider(this).get(ClaimViewModel.class);

        setupListeners();
        setupObservers();
        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && post != null) {
            viewModel.fetchPost(post.getPostId());
        }
    }

    private void updateUI() {
        if (binding == null || post == null) return;

        binding.tvTitle.setText(valueOrEmpty(post.getTitle()));
        binding.tvQuantityServings.setText(String.format(Locale.getDefault(), "%s %s • feeds ~%d",
                post.getQuantity(), post.getUnit(), post.getServings()));
        binding.tvFoodType.setText(valueOrEmpty(post.getFoodType()).toUpperCase(Locale.ROOT));
        binding.tvCategory.setText(categoryLabel(post.getCategory()));
        binding.tvDescription.setText(post.getDescription() == null || post.getDescription().trim().isEmpty()
                ? "No description provided." : post.getDescription());
        binding.tvPreparedAt.setText(post.getPreparedAt() == null
                ? "Not provided" : dateTimeFormat.format(post.getPreparedAt().toDate()));
        binding.tvExpiryTime.setText(post.getExpiryTime() == null
                ? "Not provided" : dateTimeFormat.format(post.getExpiryTime().toDate()));
        binding.tvPickupAddress.setText(valueOrEmpty(post.getPickupAddress()));
        binding.tvPickupInstructions.setText(post.getPickupInstructions() != null && !post.getPickupInstructions().isEmpty() 
                ? post.getPickupInstructions() : "No specific instructions provided.");
        
        binding.chipStatus.setStatus(post.getStatus());

        Glide.with(this)
                .load(post.getPhotoUrl())
                .centerCrop()
                .placeholder(R.color.shimmer_base)
                .into(binding.ivFoodPhoto);

        if (Constants.STATUS_CLAIMED.equals(post.getStatus()) || Constants.STATUS_COMPLETED.equals(post.getStatus())) {
            binding.layoutClaimedBy.setVisibility(View.VISIBLE);
            binding.tvReceiverName.setText(valueOrEmpty(post.getClaimedByName()));
            binding.tvReceiverPhone.setText("Phone: Contact details loading");
            if (post.getClaimId() != null && !post.getClaimId().equals(loadedClaimId)) {
                loadedClaimId = post.getClaimId();
                viewModel.fetchClaim(loadedClaimId);
            }
        } else {
            binding.layoutClaimedBy.setVisibility(View.GONE);
            loadedClaimId = null;
        }

        boolean isAvailable = Constants.STATUS_AVAILABLE.equals(post.getStatus());
        boolean isClaimed = Constants.STATUS_CLAIMED.equals(post.getStatus());

        binding.btnEditPost.setVisibility(isAvailable ? View.VISIBLE : View.GONE);
        binding.btnCancelPost.setVisibility((isAvailable || isClaimed) ? View.VISIBLE : View.GONE);
        binding.btnVerifyPickup.setVisibility(isClaimed ? View.VISIBLE : View.GONE);
        stopExpiryCountdown();
        startExpiryCountdown();
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
        binding.btnCancelPost.setOnClickListener(v -> ConfirmDialog.show(
                requireContext(),
                "Cancel Post?",
                "Are you sure you want to cancel this food post? This action cannot be undone.",
                "Yes, Cancel",
                "No, Keep it",
                () -> viewModel.cancelPost(post.getPostId()),
                null
        ));

        binding.btnEditPost.setOnClickListener(v -> {
            AddFoodFragment fragment = AddFoodFragment.newInstance(post);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.role_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnVerifyPickup.setOnClickListener(v -> showVerifyPickupDialog());
    }

    private void setupObservers() {
        viewModel.getSelectedPost().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == com.resqmeal.app.utils.Resource.Status.SUCCESS && resource.data != null) {
                post = resource.data;
                updateUI();
            } else if (resource.status == com.resqmeal.app.utils.Resource.Status.ERROR) {
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getSelectedClaim().observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == com.resqmeal.app.utils.Resource.Status.SUCCESS && resource.data != null) {
                Claim claim = resource.data;
                String phone = claim.getReceiverPhone();
                binding.tvReceiverPhone.setText("Phone: "
                        + (phone == null || phone.trim().isEmpty() ? "Not provided" : phone));
            } else if (resource.status == com.resqmeal.app.utils.Resource.Status.ERROR) {
                binding.tvReceiverPhone.setText("Phone: unavailable");
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getActionStatus().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.btnCancelPost.setEnabled(false);
                    break;
                case SUCCESS:
                    binding.btnCancelPost.setEnabled(true);
                    Toast.makeText(requireContext(), "Action successful", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    break;
                case ERROR:
                    binding.btnCancelPost.setEnabled(true);
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });

        claimViewModel.getCompletionStatus().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null || resource == null || pickupDialog == null) {
                return;
            }
            switch (resource.status) {
                case LOADING:
                    pickupDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    pickupDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setText(R.string.pickup_verifying);
                    break;
                case SUCCESS:
                    pickupDialog.dismiss();
                    pickupDialog = null;
                    pickupDialogBinding = null;
                    binding.btnVerifyPickup.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.pickup_completed, Toast.LENGTH_SHORT).show();
                    viewModel.fetchPost(post.getPostId());
                    break;
                case ERROR:
                    pickupDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    pickupDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setText(R.string.pickup_verify);
                    pickupDialogBinding.tilPickupCode.setError(resource.message);
                    break;
            }
        });
    }

    private void showVerifyPickupDialog() {
        if (post == null || post.getClaimId() == null) {
            Toast.makeText(requireContext(), R.string.pickup_details_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        pickupDialogBinding = DialogVerifyPickupBinding.inflate(getLayoutInflater());
        EditText codeInput = pickupDialogBinding.etPickupCode;
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        codeInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});

        pickupDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pickup_verify_title)
                .setMessage(R.string.pickup_verify_message)
                .setView(pickupDialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    pickupDialog = null;
                    pickupDialogBinding = null;
                })
                .setPositiveButton(R.string.pickup_verify, null)
                .create();
        pickupDialog.setOnShowListener(dialog -> pickupDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String pickupCode = codeInput.getText() == null
                            ? "" : codeInput.getText().toString().trim();
                    if (!pickupCode.matches("[0-9]{4}")) {
                        pickupDialogBinding.tilPickupCode.setError(
                                getString(R.string.pickup_code_invalid));
                        return;
                    }
                    pickupDialogBinding.tilPickupCode.setError(null);
                    claimViewModel.completePickup(post.getClaimId(), pickupCode);
                }));
        pickupDialog.setOnDismissListener(dialog -> {
            pickupDialog = null;
            pickupDialogBinding = null;
        });
        pickupDialog.show();
    }

    private void startExpiryCountdown() {
        if (post == null || post.getExpiryTime() == null
                || (!Constants.STATUS_AVAILABLE.equals(post.getStatus())
                && !Constants.STATUS_CLAIMED.equals(post.getStatus()))) {
            return;
        }

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long diff = post.getExpiryTime().toDate().getTime() - System.currentTimeMillis();
                if (diff <= 0) {
                    binding.tvExpiryTime.setText("Expired");
                    binding.tvExpiryTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
                    viewModel.fetchPost(post.getPostId());
                    return;
                }

                long hours = diff / (60 * 60 * 1000);
                long minutes = (diff / (60 * 1000)) % 60;

                String countdown;
                if (hours > 0) {
                    countdown = String.format(Locale.getDefault(), "Expires in %dh %dm", hours, minutes);
                    binding.tvExpiryTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                } else {
                    countdown = String.format(Locale.getDefault(), "Expires in %dm", minutes);
                    binding.tvExpiryTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
                }

                binding.tvExpiryTime.setText(countdown);
                countdownHandler.postDelayed(this, 60000);
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    private void stopExpiryCountdown() {
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopExpiryCountdown();
        if (pickupDialog != null) {
            pickupDialog.dismiss();
            pickupDialog = null;
            pickupDialogBinding = null;
        }
        binding = null;
    }
}
