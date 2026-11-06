package com.resqmeal.app.ui.shell;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.resqmeal.app.R;
import com.resqmeal.app.utils.Constants;

import java.util.Arrays;
import java.util.List;

public class ReceiverActivity extends BaseRoleActivity {

    public static final String EXTRA_APPROVAL_STATUS = "approval_status";
    public static final String EXTRA_REJECTION_REASON = "rejection_reason";

    @NonNull
    @Override
    protected List<RoleTab> getTabs() {
        return Arrays.asList(
                new RoleTab(R.id.nav_feed, R.drawable.ic_nav_feed,
                        getString(R.string.tab_feed), getString(R.string.placeholder_feed)),
                new RoleTab(R.id.nav_map, R.drawable.ic_nav_map,
                        getString(R.string.tab_map), getString(R.string.placeholder_map)),
                new RoleTab(R.id.nav_claims, R.drawable.ic_nav_claims,
                        getString(R.string.tab_my_claims), getString(R.string.placeholder_my_claims)),
                new RoleTab(R.id.nav_profile, R.drawable.ic_nav_profile,
                        getString(R.string.tab_profile), getString(R.string.placeholder_profile))
        );
    }

    @Override
    protected int getDefaultTabId() {
        return R.id.nav_feed;
    }

    @Override
    protected boolean isPendingApprovalScreen() {
        String status = getIntent().getStringExtra(EXTRA_APPROVAL_STATUS);
        return !Constants.APPROVAL_APPROVED.equals(status);
    }

    @Override
    protected Fragment createPendingApprovalFragment() {
        return PendingApprovalFragment.newInstance(
                getIntent().getStringExtra(EXTRA_APPROVAL_STATUS),
                getIntent().getStringExtra(EXTRA_REJECTION_REASON)
        );
    }

    @Override
    protected String getPendingApprovalTitle() {
        if (Constants.APPROVAL_REJECTED.equals(
                getIntent().getStringExtra(EXTRA_APPROVAL_STATUS))) {
            return getString(R.string.approval_rejected_title);
        }
        return super.getPendingApprovalTitle();
    }
}
