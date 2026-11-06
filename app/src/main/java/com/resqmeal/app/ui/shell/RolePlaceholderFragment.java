package com.resqmeal.app.ui.shell;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentRolePlaceholderBinding;

public class RolePlaceholderFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";

    public static RolePlaceholderFragment newInstance(String title, String message) {
        RolePlaceholderFragment fragment = new RolePlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
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
        FragmentRolePlaceholderBinding binding =
                FragmentRolePlaceholderBinding.inflate(inflater, container, false);
        Bundle args = getArguments();
        binding.tvPlaceholderTitle.setText(args == null
                ? getString(R.string.placeholder_title_default)
                : args.getString(ARG_TITLE, getString(R.string.placeholder_title_default)));
        binding.tvPlaceholderMessage.setText(args == null
                ? getString(R.string.placeholder_message_default)
                : args.getString(ARG_MESSAGE, getString(R.string.placeholder_message_default)));
        return binding.getRoot();
    }
}
