package com.resqmeal.app.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.resqmeal.app.R;
import com.resqmeal.app.adapter.NotificationAdapter;
import com.resqmeal.app.databinding.FragmentNotificationsBinding;
import com.resqmeal.app.utils.Resource;
import com.resqmeal.app.viewmodel.NotificationViewModel;

public class NotificationsFragment extends Fragment {
    private FragmentNotificationsBinding binding;
    private NotificationViewModel viewModel;
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        adapter = new NotificationAdapter(notification ->
                viewModel.markAsRead(notification.getNotificationId()));
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifications.setAdapter(adapter);
        binding.emptyState.hideAction();

        viewModel.getNotifications().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null || binding == null) {
                return;
            }
            switch (resource.status) {
                case LOADING:
                    binding.progressLoading.setVisibility(View.VISIBLE);
                    binding.rvNotifications.setVisibility(View.GONE);
                    binding.emptyState.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    binding.progressLoading.setVisibility(View.GONE);
                    if (resource.data == null || resource.data.isEmpty()) {
                        binding.rvNotifications.setVisibility(View.GONE);
                        binding.emptyState.setContent(
                                getString(R.string.notifications_empty_title),
                                getString(R.string.notifications_empty_message));
                        binding.emptyState.hideAction();
                        binding.emptyState.setVisibility(View.VISIBLE);
                    } else {
                        binding.emptyState.setVisibility(View.GONE);
                        binding.rvNotifications.setVisibility(View.VISIBLE);
                        adapter.setNotifications(resource.data);
                    }
                    break;
                case ERROR:
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.rvNotifications.setVisibility(View.GONE);
                    binding.emptyState.setContent(
                            getString(R.string.notifications_error_title), resource.message);
                    binding.emptyState.setAction(
                            getString(R.string.retry),
                            action -> viewModel.startListeningForUnreadNotifications());
                    binding.emptyState.setVisibility(View.VISIBLE);
                    break;
            }
        });

        viewModel.getActionStatus().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null || resource == null) {
                return;
            }
            if (resource.status == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (viewModel != null) {
            viewModel.startListeningForUnreadNotifications();
        }
    }

    @Override
    public void onStop() {
        if (viewModel != null) {
            viewModel.stopListeningForUnreadNotifications();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }
}
