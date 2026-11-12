package com.resqmeal.app.ui.shell;

import android.Manifest;
import android.os.Bundle;
import android.os.Build;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.badge.BadgeDrawable;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.ActivityRoleShellBinding;
import com.resqmeal.app.ui.common.NotificationsFragment;
import com.resqmeal.app.ui.common.ProfileFragment;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.RoleRouter;
import com.resqmeal.app.repository.NotificationRepository;

import java.util.List;

public abstract class BaseRoleActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_NOTIFICATIONS = "open_notifications";

    private static final String TAG = "BaseRoleActivity";
    private static final String STATE_SELECTED_TAB = "selected_tab";
    private static final String KEY_NOTIFICATION_PERMISSION_PROMPTED =
            "notification_permission_prompted";

    protected ActivityRoleShellBinding binding;
    private List<RoleTab> tabs;
    private ListenerRegistration accountListener;
    private ListenerRegistration unreadNotificationsListener;
    private boolean redirectingToLogin;
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Log.i(TAG, "Notification permission was not granted");
                }
            });

    protected abstract List<RoleTab> getTabs();

    protected abstract int getDefaultTabId();

    protected Fragment getFragmentForTab(int tabId) {
        if (tabId == R.id.nav_profile) {
            return new ProfileFragment();
        }
        if (tabId == R.id.nav_notifications) {
            return new NotificationsFragment();
        }
        return null;
    }

    protected boolean isPendingApprovalScreen() {
        return false;
    }

    protected Fragment createPendingApprovalFragment() {
        return new PendingApprovalFragment();
    }

    protected String getPendingApprovalTitle() {
        return getString(R.string.pending_approval_title);
    }

    protected void onAccountStatusLoaded(@NonNull com.google.firebase.firestore.DocumentSnapshot snapshot) {
    }

    public void openAdminSubscreen(@NonNull Fragment fragment, @NonNull String title) {
        binding.toolbar.setTitle(title);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.role_fragment_container, fragment)
                .addToBackStack(title)
                .commit();
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

    @Override
    protected void onStart() {
        super.onStart();
        observeActiveAccount();
        observeUnreadNotifications();
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onStop() {
        if (accountListener != null) {
            accountListener.remove();
            accountListener = null;
        }
        if (unreadNotificationsListener != null) {
            unreadNotificationsListener.remove();
            unreadNotificationsListener = null;
        }
        super.onStop();
    }

    private void observeUnreadNotifications() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || !firebaseUser.isEmailVerified()
                || binding.bottomNavigation.getVisibility() != View.VISIBLE
                || findTab(R.id.nav_notifications) == null) {
            return;
        }
        unreadNotificationsListener = new NotificationRepository()
                .addUserNotificationsListener(firebaseUser.getUid(), (snapshot, error) -> {
                    if (isFinishing() || isDestroyed() || binding == null) {
                        return;
                    }
                    if (error != null) {
                        Log.e(TAG, "Could not update the unread notification badge", error);
                        return;
                    }
                    if (snapshot == null) {
                        return;
                    }
                    int unreadCount = countUnread(snapshot);
                    if (unreadCount == 0) {
                        binding.bottomNavigation.removeBadge(R.id.nav_notifications);
                    } else {
                        BadgeDrawable badge = binding.bottomNavigation
                                .getOrCreateBadge(R.id.nav_notifications);
                        badge.setNumber(unreadCount);
                        badge.setVisible(true);
                    }
                });
    }

    private int countUnread(QuerySnapshot snapshot) {
        int unreadCount = 0;
        for (com.google.firebase.firestore.DocumentSnapshot document : snapshot.getDocuments()) {
            if (!Boolean.TRUE.equals(document.getBoolean("isRead"))) {
                unreadCount++;
            }
        }
        return unreadCount;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (findTab(R.id.nav_notifications) == null && !isPendingApprovalScreen()) {
            return;
        }
        if (Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_NOTIFICATION_PERMISSION_PROMPTED, false)) {
            return;
        }
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTIFICATION_PERMISSION_PROMPTED, true)
                .apply();
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void observeActiveAccount() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || !firebaseUser.isEmailVerified()) {
            redirectToLogin(R.string.account_profile_invalid);
            return;
        }

        accountListener = FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.getUid())
                .addSnapshotListener(this, (snapshot, error) -> {
                    if (isFinishing() || isDestroyed() || redirectingToLogin) {
                        return;
                    }
                    if (error != null) {
                        Toast.makeText(this,
                                getString(R.string.session_load_failed, error.getMessage()),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        redirectToLogin(R.string.account_profile_missing);
                        return;
                    }

                    Boolean active = snapshot.getBoolean("active");
                    if (!Boolean.TRUE.equals(active)) {
                        redirectToLogin(R.string.account_deactivated);
                        return;
                    }
                    onAccountStatusLoaded(snapshot);
                });
    }

    private void redirectToLogin(int messageResId) {
        if (redirectingToLogin || isFinishing() || isDestroyed()) {
            return;
        }
        redirectingToLogin = true;
        FirebaseAuth.getInstance().signOut();
        RoleRouter.routeToLogin(this);
        android.widget.Toast.makeText(this, messageResId, android.widget.Toast.LENGTH_LONG).show();
    }

    private void setupBottomNavigation(@Nullable Bundle savedInstanceState) {
        Menu menu = binding.bottomNavigation.getMenu();
        menu.clear();
        for (RoleTab tab : tabs) {
            menu.add(Menu.NONE, tab.getId(), Menu.NONE, tab.getLabel())
                    .setIcon(tab.getIcon())
                    .setCheckable(true);
        }

        int requestedTabId = getIntent().getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false)
                && findTab(R.id.nav_notifications) != null
                ? R.id.nav_notifications : getDefaultTabId();
        int restoredTabId = savedInstanceState == null
                ? requestedTabId
                : savedInstanceState.getInt(STATE_SELECTED_TAB, getDefaultTabId());
        int selectedTabId = findTab(restoredTabId) == null ? getDefaultTabId() : restoredTabId;

        binding.bottomNavigation.setSelectedItemId(selectedTabId);

        RoleTab initialTab = findTab(selectedTabId);
        if (initialTab != null) {
            binding.toolbar.setTitle(initialTab.getLabel());
            showTab(initialTab);
        }

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
        Fragment fragment = getFragmentForTab(tab.getId());

        if (fragment == null) {
            fragment = RolePlaceholderFragment.newInstance(
                    tab.getLabel(),
                    tab.getPlaceholderMessage()
            );
        }

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

    @Override
    protected void onDestroy() {
        if (accountListener != null) {
            accountListener.remove();
            accountListener = null;
        }
        if (unreadNotificationsListener != null) {
            unreadNotificationsListener.remove();
            unreadNotificationsListener = null;
        }
        super.onDestroy();
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
