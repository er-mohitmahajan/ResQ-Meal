package com.resqmeal.app.ui.shell;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.ActivityRoleShellBinding;

import java.util.List;

public abstract class BaseRoleActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_TAB = "selected_tab";

    private ActivityRoleShellBinding binding;
    private List<RoleTab> tabs;

    protected abstract List<RoleTab> getTabs();

    protected abstract int getDefaultTabId();

    protected boolean isPendingApprovalScreen() {
        return false;
    }

    protected Fragment createPendingApprovalFragment() {
        return new PendingApprovalFragment();
    }

    protected String getPendingApprovalTitle() {
        return getString(R.string.pending_approval_title);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoleShellBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (isPendingApprovalScreen()) {
            binding.toolbar.setTitle(getPendingApprovalTitle());
            binding.bottomNavigation.setVisibility(View.GONE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.role_fragment_container, createPendingApprovalFragment())
                    .commit();
            return;
        }

        tabs = getTabs();
        setupBottomNavigation(savedInstanceState);
    }

    private void setupBottomNavigation(@Nullable Bundle savedInstanceState) {
        Menu menu = binding.bottomNavigation.getMenu();
        menu.clear();
        for (RoleTab tab : tabs) {
            menu.add(Menu.NONE, tab.getId(), Menu.NONE, tab.getLabel())
                    .setIcon(tab.getIcon())
                    .setCheckable(true);
        }

        int selectedTabId = savedInstanceState == null
                ? getDefaultTabId()
                : savedInstanceState.getInt(STATE_SELECTED_TAB, getDefaultTabId());
        RoleTab selectedTab = findTab(selectedTabId);
        if (selectedTab == null) {
            selectedTab = findTab(getDefaultTabId());
        }
        if (selectedTab == null) {
            throw new IllegalStateException("The default navigation tab is not configured.");
        }

        RoleTab initialTab = selectedTab;
        binding.toolbar.setTitle(initialTab.getLabel());
        binding.bottomNavigation.setSelectedItemId(initialTab.getId());
        showTab(initialTab);
        binding.bottomNavigation.setOnItemSelectedListener(this::selectTab);
    }

    private boolean selectTab(@NonNull MenuItem item) {
        RoleTab tab = findTab(item.getItemId());
        if (tab == null) {
            return false;
        }

        binding.toolbar.setTitle(tab.getLabel());
        showTab(tab);
        return true;
    }

    private void showTab(@NonNull RoleTab tab) {
        Fragment fragment = RolePlaceholderFragment.newInstance(
                tab.getLabel(),
                tab.getPlaceholderMessage()
        );
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.role_fragment_container, fragment)
                .commit();
    }

    @Nullable
    private RoleTab findTab(int tabId) {
        if (tabs == null) {
            return null;
        }
        for (RoleTab tab : tabs) {
            if (tab.getId() == tabId) {
                return tab;
            }
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (binding != null && binding.bottomNavigation.getVisibility() == View.VISIBLE) {
            outState.putInt(STATE_SELECTED_TAB, binding.bottomNavigation.getSelectedItemId());
        }
        super.onSaveInstanceState(outState);
    }

    public static final class RoleTab {
        private final int id;
        private final int icon;
        private final String label;
        private final String placeholderMessage;

        public RoleTab(int id, int icon, String label, String placeholderMessage) {
            this.id = id;
            this.icon = icon;
            this.label = label;
            this.placeholderMessage = placeholderMessage;
        }

        public int getId() {
            return id;
        }

        public int getIcon() {
            return icon;
        }

        public String getLabel() {
            return label;
        }

        public String getPlaceholderMessage() {
            return placeholderMessage;
        }
    }
}
