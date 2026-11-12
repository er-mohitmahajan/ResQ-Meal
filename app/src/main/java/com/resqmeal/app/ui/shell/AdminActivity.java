package com.resqmeal.app.ui.shell;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.resqmeal.app.R;
import com.resqmeal.app.ui.admin.AdminDashboardFragment;
import com.resqmeal.app.ui.admin.AdminReportsFragment;
import com.resqmeal.app.ui.admin.AdminUsersFragment;

import java.util.Arrays;
import java.util.List;

public class AdminActivity extends BaseRoleActivity {

    @NonNull
    @Override
    protected List<RoleTab> getTabs() {
        return Arrays.asList(
                new RoleTab(R.id.nav_dashboard, R.drawable.ic_nav_home,
                        getString(R.string.tab_dashboard), getString(R.string.placeholder_dashboard)),
                new RoleTab(R.id.nav_approvals, R.drawable.ic_nav_approvals,
                        getString(R.string.tab_approvals), getString(R.string.placeholder_approvals)),
                new RoleTab(R.id.nav_reports, R.drawable.ic_nav_reports,
                        getString(R.string.tab_reports), getString(R.string.placeholder_reports)),
                new RoleTab(R.id.nav_profile, R.drawable.ic_nav_profile,
                        getString(R.string.tab_profile), getString(R.string.placeholder_profile))
        );
    }

    @Override
    protected int getDefaultTabId() {
        return R.id.nav_dashboard;
    }

    @Override
    protected Fragment getFragmentForTab(int tabId) {
        if (tabId == R.id.nav_dashboard) {
            return new AdminDashboardFragment();
        } else if (tabId == R.id.nav_approvals) {
            return AdminUsersFragment.newApprovals();
        } else if (tabId == R.id.nav_reports) {
            return new AdminReportsFragment();
        }
        return super.getFragmentForTab(tabId);
    }
}
