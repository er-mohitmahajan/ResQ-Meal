package com.resqmeal.app.ui.shell;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.resqmeal.app.R;
import com.resqmeal.app.ui.donor.DonorHomeFragment;
import com.resqmeal.app.ui.donor.MyPostsFragment;
import com.resqmeal.app.ui.common.ProfileFragment;

import java.util.Arrays;
import java.util.List;

public class DonorActivity extends BaseRoleActivity {

    @NonNull
    @Override
    protected List<RoleTab> getTabs() {
        return Arrays.asList(
                new RoleTab(R.id.nav_home, R.drawable.ic_nav_home,
                        getString(R.string.tab_home), getString(R.string.placeholder_home)),
                new RoleTab(R.id.nav_posts, R.drawable.ic_nav_posts,
                        getString(R.string.tab_my_posts), getString(R.string.placeholder_my_posts)),
                new RoleTab(R.id.nav_notifications, R.drawable.ic_nav_notifications,
                        getString(R.string.tab_notifications), getString(R.string.placeholder_notifications)),
                new RoleTab(R.id.nav_profile, R.drawable.ic_nav_profile,
                        getString(R.string.tab_profile), getString(R.string.placeholder_profile))
        );
    }

    @Override
    protected int getDefaultTabId() {
        return R.id.nav_home;
    }

    @Override
    protected Fragment getFragmentForTab(int tabId) {
        if (tabId == R.id.nav_home) {
            return new DonorHomeFragment();
        } else if (tabId == R.id.nav_posts) {
            return new MyPostsFragment();
        } else if (tabId == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return super.getFragmentForTab(tabId);
    }
}
