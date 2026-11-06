package com.resqmeal.app.utils;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.resqmeal.app.R;
import com.resqmeal.app.model.User;
import com.resqmeal.app.ui.auth.AuthActivity;
import com.resqmeal.app.ui.shell.AdminActivity;
import com.resqmeal.app.ui.shell.DonorActivity;
import com.resqmeal.app.ui.shell.ReceiverActivity;

public final class RoleRouter {

    private RoleRouter() {
    }

    public static void route(@NonNull Activity activity, @NonNull User user) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (!user.isActive()) {
            FirebaseAuth.getInstance().signOut();
            routeToLogin(activity);
            Toast.makeText(activity, R.string.account_deactivated, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent;
        String role = user.getRole();
        if (Constants.ROLE_DONOR.equals(role)) {
            intent = new Intent(activity, DonorActivity.class);
        } else if (Constants.ROLE_RECEIVER.equals(role)) {
            intent = new Intent(activity, ReceiverActivity.class);
            intent.putExtra(ReceiverActivity.EXTRA_APPROVAL_STATUS, user.getApprovalStatus());
            intent.putExtra(ReceiverActivity.EXTRA_REJECTION_REASON, user.getRejectionReason());
        } else if (Constants.ROLE_ADMIN.equals(role)) {
            intent = new Intent(activity, AdminActivity.class);
        } else {
            FirebaseAuth.getInstance().signOut();
            routeToLogin(activity);
            Toast.makeText(activity, R.string.account_role_unrecognized, Toast.LENGTH_LONG).show();
            return;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void routeToLogin(@NonNull Activity activity) {
        Intent intent = new Intent(activity, AuthActivity.class);
        intent.putExtra(AuthActivity.EXTRA_START_DESTINATION, AuthActivity.DESTINATION_LOGIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
