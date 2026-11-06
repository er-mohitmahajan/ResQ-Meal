package com.resqmeal.app.ui.shell;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentPendingApprovalBinding;
import com.resqmeal.app.utils.Constants;

public class PendingApprovalFragment extends Fragment {

    private static final String ARG_STATUS = "approval_status";
    private static final String ARG_REJECTION_REASON = "rejection_reason";

    public static PendingApprovalFragment newInstance(String status, String rejectionReason) {
        PendingApprovalFragment fragment = new PendingApprovalFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        args.putString(ARG_REJECTION_REASON, rejectionReason);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        FragmentPendingApprovalBinding binding =
                FragmentPendingApprovalBinding.inflate(inflater, container, false);
        Bundle args = getArguments();
        String status = args == null
                ? Constants.APPROVAL_PENDING
                : args.getString(ARG_STATUS, Constants.APPROVAL_PENDING);

        if (Constants.APPROVAL_REJECTED.equals(status)) {
            binding.tvApprovalTitle.setText(R.string.approval_rejected_title);
            binding.tvApprovalMessage.setText(R.string.approval_rejected_message);
            String reason = args == null ? null : args.getString(ARG_REJECTION_REASON);
            if (reason != null && !reason.trim().isEmpty()) {
                binding.tvRejectionReason.setText(getString(R.string.approval_rejection_reason, reason));
                binding.tvRejectionReason.setVisibility(View.VISIBLE);
            }
        } else {
            binding.tvApprovalTitle.setText(R.string.pending_approval_title);
            binding.tvApprovalMessage.setText(R.string.pending_approval_message);
            binding.tvRejectionReason.setVisibility(View.GONE);
        }
        return binding.getRoot();
    }
}
