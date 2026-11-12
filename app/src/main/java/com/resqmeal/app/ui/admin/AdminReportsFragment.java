package com.resqmeal.app.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.resqmeal.app.R;
import com.resqmeal.app.adapter.AdminReportAdapter;
import com.resqmeal.app.databinding.FragmentAdminListBinding;
import com.resqmeal.app.model.Report;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.Resource;
import com.resqmeal.app.viewmodel.AdminViewModel;

public class AdminReportsFragment extends Fragment {
    private FragmentAdminListBinding binding;
    private AdminViewModel viewModel;
    private AdminReportAdapter adapter;

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
        adapter = new AdminReportAdapter(this::showReportDetail);
        binding.rvItems.setAdapter(adapter);
        binding.emptyState.hideAction();
        observeData();
        observeActions();
        viewModel.loadReports();
    }

    private void observeData() {
        viewModel.getReports().observe(getViewLifecycleOwner(), resource -> {
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
                                getString(R.string.admin_no_reports_title),
                                getString(R.string.admin_no_reports_message));
                        binding.emptyState.hideAction();
                        binding.emptyState.setVisibility(View.VISIBLE);
                    } else {
                        binding.emptyState.setVisibility(View.GONE);
                        binding.rvItems.setVisibility(View.VISIBLE);
                        adapter.setReports(resource.data);
                    }
                    break;
                case ERROR:
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.rvItems.setVisibility(View.GONE);
                    binding.emptyState.setContent(
                            getString(R.string.admin_load_error_title), resource.message);
                    Log.d("Create Index", "Index :" + resource.message);
                    binding.emptyState.setAction(
                            getString(R.string.retry), action -> viewModel.loadReports());
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
                Toast.makeText(requireContext(), R.string.admin_action_success, Toast.LENGTH_SHORT).show();
                viewModel.loadReports();
            }
        });
    }

    private void showReportDetail(Report report) {
        EditText noteInput = new EditText(requireContext());
        noteInput.setHint(R.string.admin_report_note_hint);
        noteInput.setMinLines(2);
        noteInput.setGravity(android.view.Gravity.TOP);
        String targetType = report.getTargetType();
        boolean userTarget = Constants.REPORT_TARGET_USER.equals(targetType);
        boolean postTarget = Constants.REPORT_TARGET_POST.equals(targetType);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.admin_report_detail_title))
                .setMessage(getString(
                        R.string.admin_report_detail_message,
                        valueOrEmpty(report.getTargetType()),
                        valueOrEmpty(report.getTargetId()),
                        valueOrEmpty(report.getReason()),
                        valueOrEmpty(report.getDescription())))
                .setView(noteInput)
                .setNegativeButton(R.string.admin_dismiss, (dialog, which) ->
                        viewModel.resolveReport(report.getReportId(),
                                Constants.REPORT_STATUS_DISMISSED, text(noteInput), false, false))
                .setPositiveButton(R.string.admin_resolve, (dialog, which) ->
                        viewModel.resolveReport(report.getReportId(),
                                Constants.REPORT_STATUS_RESOLVED, text(noteInput), false, false));
        builder.setNeutralButton(R.string.admin_report_more_actions, null);
        MaterialAlertDialogBuilder detailBuilder = builder;
        androidx.appcompat.app.AlertDialog detailDialog = detailBuilder.create();
        detailDialog.setOnShowListener(ignored -> {
            detailDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(button -> {
                        detailDialog.dismiss();
                        if (userTarget) {
                            confirmDeactivateReportedUser(report, noteInput);
                        } else if (postTarget) {
                            confirmRemoveReportedPost(report, noteInput);
                        }
                    });
        });
        detailDialog.show();
    }

    private void confirmDeactivateReportedUser(Report report, EditText noteInput) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_deactivate_title)
                .setMessage(R.string.admin_deactivate_reported_user_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.admin_deactivate, (dialog, which) ->
                        viewModel.resolveReport(report.getReportId(),
                                Constants.REPORT_STATUS_RESOLVED,
                                text(noteInput), true, false))
                .show();
    }

    private void confirmRemoveReportedPost(Report report, EditText noteInput) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_remove_post_title)
                .setMessage(R.string.admin_remove_reported_post_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.admin_remove_post, (dialog, which) ->
                        viewModel.resolveReport(report.getReportId(),
                                Constants.REPORT_STATUS_RESOLVED,
                                text(noteInput), false, true))
                .show();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String text(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadReports();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }
}
