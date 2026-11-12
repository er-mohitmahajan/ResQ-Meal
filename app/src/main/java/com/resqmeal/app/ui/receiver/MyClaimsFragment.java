package com.resqmeal.app.ui.receiver;

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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.resqmeal.app.R;
import com.resqmeal.app.adapter.ClaimAdapter;
import com.resqmeal.app.databinding.FragmentMyClaimsBinding;
import com.resqmeal.app.model.Claim;
import com.resqmeal.app.utils.Resource;
import com.resqmeal.app.viewmodel.ClaimViewModel;

import java.util.ArrayList;
import java.util.List;

public class MyClaimsFragment extends Fragment {
    private FragmentMyClaimsBinding binding;
    private ClaimViewModel viewModel;
    private ClaimAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentMyClaimsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ClaimViewModel.class);
        adapter = new ClaimAdapter(this::openClaim);
        binding.rvClaims.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvClaims.setAdapter(adapter);
        binding.emptyState.setAction(getString(R.string.feed_browse), v ->
                ((BottomNavigationView) requireActivity().findViewById(R.id.bottom_navigation))
                        .setSelectedItemId(R.id.nav_feed));
        binding.emptyState.hideAction();

        viewModel.getReceiverClaims().observe(getViewLifecycleOwner(), this::renderClaims);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.fetchReceiverClaims();
        }
    }

    private void renderClaims(Resource<List<Claim>> resource) {
        if (binding == null) {
            return;
        }
        switch (resource.status) {
            case LOADING:
                binding.progressLoading.setVisibility(View.VISIBLE);
                binding.rvClaims.setVisibility(View.GONE);
                binding.emptyState.setVisibility(View.GONE);
                break;
            case SUCCESS:
                binding.progressLoading.setVisibility(View.GONE);
                List<Claim> claims = resource.data == null ? new ArrayList<>() : resource.data;
                adapter.setClaims(claims);
                if (claims.isEmpty()) {
                    binding.rvClaims.setVisibility(View.GONE);
                    binding.emptyState.setVisibility(View.VISIBLE);
                    binding.emptyState.setContent(
                            getString(R.string.claims_empty_title),
                            getString(R.string.claims_empty_message));
                    binding.emptyState.setAction(getString(R.string.feed_browse), v ->
                            ((BottomNavigationView) requireActivity().findViewById(R.id.bottom_navigation))
                                    .setSelectedItemId(R.id.nav_feed));
                } else {
                    binding.rvClaims.setVisibility(View.VISIBLE);
                    binding.emptyState.setVisibility(View.GONE);
                    binding.emptyState.hideAction();
                }
                break;
            case ERROR:
                binding.progressLoading.setVisibility(View.GONE);
                binding.rvClaims.setVisibility(View.GONE);
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.emptyState.setContent(
                        getString(R.string.claims_load_error_title),
                        resource.message == null ? getString(R.string.claims_load_error_message) : resource.message);
                binding.emptyState.setAction(getString(R.string.retry), v -> viewModel.fetchReceiverClaims());
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void openClaim(Claim claim) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.role_fragment_container, ClaimDetailFragment.newInstance(claim.getClaimId()))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
