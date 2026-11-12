package com.resqmeal.app.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resqmeal.app.R;
import com.resqmeal.app.adapter.AdminUserAdapter;
import com.resqmeal.app.databinding.FragmentAdminListBinding;
import com.resqmeal.app.model.User;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.Resource;
import com.resqmeal.app.viewmodel.AdminViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminUsersFragment extends Fragment {
    private static final String ARG_APPROVALS_MODE = "approvals_mode";

    private FragmentAdminListBinding binding;
    private AdminViewModel viewModel;
    private AdminUserAdapter adapter;
    private List<User> loadedUsers = new ArrayList<>();
    private boolean approvalsMode;

    public static AdminUsersFragment newApprovals() {
        AdminUsersFragment fragment = new AdminUsersFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(ARG_APPROVALS_MODE, true);
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
        binding = FragmentAdminListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        approvalsMode = getArguments() != null
                && getArguments().getBoolean(ARG_APPROVALS_MODE, false);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
        binding.etSearch.setVisibility(approvalsMode ? View.GONE : View.VISIBLE);
        binding.rvItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminUserAdapter(approvalsMode, new AdminUserAdapter.Listener() {
            @Override
            public void onApprove(User user) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.admin_approve_title)
                        .setMessage(getString(R.string.admin_approve_message, user.getName()))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.admin_approve, (dialog, which) ->
                                viewModel.setReceiverApproval(user.getUid(), true, null))
                        .show();
            }

            @Override
            public void onReject(User user) {
                showRejectDialog(user);
            }

            @Override
            public void onToggleActive(User user) {
                boolean activate = !user.isActive();
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(activate ? R.string.admin_activate_title : R.string.admin_deactivate_title)
                        .setMessage(getString(
                                activate ? R.string.admin_activate_message : R.string.admin_deactivate_message,
                                user.getName()))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(activate ? R.string.admin_activate : R.string.admin_deactivate,
                                (dialog, which) -> viewModel.setUserActive(user.getUid(), activate))
                        .show();
            }
        });
        binding.rvItems.setAdapter(adapter);
        binding.emptyState.hideAction();
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        observeData();
        observeActions();
        loadData();
    }

    private void observeData() {
        viewModel.getUsers().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null || resource == null) {
                return;
            }
            if (resource.status == Resource.Status.LOADING) {
                binding.progressLoading.setVisibility(View.VISIBLE);
                binding.rvItems.setVisibility(View.GONE);
                binding.emptyState.setVisibility(View.GONE);
            } else if (resource.status == Resource.Status.SUCCESS) {
                binding.progressLoading.setVisibility(View.GONE);
                loadedUsers = resource.data == null ? new ArrayList<>() : resource.data;
                filterUsers(binding.etSearch.getText() == null
                        ? "" : binding.etSearch.getText().toString());
            } else {
                binding.progressLoading.setVisibility(View.GONE);
                binding.rvItems.setVisibility(View.GONE);
                binding.emptyState.setContent(
                        getString(R.string.admin_load_error_title), resource.message);
                binding.emptyState.setAction(
                        getString(R.string.retry), action -> loadData());
                binding.emptyState.setVisibility(View.VISIBLE);
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
                Toast.makeText(requireContext(), R.string.admin_action_success, Toast.LENGTH_SHORT).show();
                loadData();
            }
        });
    }

    private void filterUsers(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<User> filtered = new ArrayList<>();
        for (User user : loadedUsers) {
            if (normalized.isEmpty()
                    || contains(user.getName(), normalized)
                    || contains(user.getEmail(), normalized)
                    || contains(user.getPhone(), normalized)
                    || contains(user.getRole(), normalized)) {
                filtered.add(user);
            }
        }
        if (filtered.isEmpty()) {
            binding.rvItems.setVisibility(View.GONE);
            binding.emptyState.setContent(
                    approvalsMode ? getString(R.string.admin_no_approvals_title)
                            : getString(R.string.admin_no_users_title),
                    approvalsMode ? getString(R.string.admin_no_approvals_message)
                            : getString(R.string.admin_no_users_message));
            binding.emptyState.hideAction();
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.rvItems.setVisibility(View.VISIBLE);
            adapter.setUsers(filtered);
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void loadData() {
        if (approvalsMode) {
            viewModel.loadApprovals();
        } else {
            viewModel.loadUsers();
        }
    }

    private void showRejectDialog(User user) {
        EditText reasonInput = new EditText(requireContext());
        reasonInput.setHint(R.string.admin_rejection_reason_hint);
        reasonInput.setMinLines(2);
        reasonInput.setGravity(android.view.Gravity.TOP);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_reject_title)
                .setMessage(R.string.admin_reject_message)
                .setView(reasonInput)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.admin_reject, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String reason = reasonInput.getText() == null
                            ? "" : reasonInput.getText().toString().trim();
                    if (reason.isEmpty()) {
                        reasonInput.setError(getString(R.string.admin_rejection_reason_required));
                        return;
                    }
                    viewModel.setReceiverApproval(user.getUid(), false, reason);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            loadData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
        loadedUsers.clear();
    }
}
