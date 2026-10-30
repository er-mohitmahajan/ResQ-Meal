package com.resqmeal.app.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.resqmeal.app.R;
import com.resqmeal.app.databinding.FragmentOnboardingBinding;
import com.resqmeal.app.utils.SharedPrefsHelper;

import java.util.ArrayList;
import java.util.List;

public class OnboardingFragment extends Fragment {

    private FragmentOnboardingBinding binding;
    private OnboardingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupViewPager();
        setupListeners();
    }

    private void setupViewPager() {
        List<OnboardingItem> items = new ArrayList<>();
        items.add(new OnboardingItem(
                R.raw.onboarding_slide1,
                "Reduce Food Waste",
                "Connect surplus food from restaurants, messes, and events with people who need it before it goes to waste."
        ));
        items.add(new OnboardingItem(
                R.raw.onboarding_slide2,
                "Quick & Easy Redistribution",
                "Donors list food in seconds. Verified NGOs and volunteers claim and collect it fresh."
        ));
        items.add(new OnboardingItem(
                R.raw.onboarding_slide3,
                "Real Impact, Verified Pickups",
                "Secure 4-digit pickup verification ensures safe delivery and tracks every meal saved."
        ));

        adapter = new OnboardingAdapter(items);
        binding.viewPagerOnboarding.setAdapter(adapter);

        binding.viewPagerOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                if (position == items.size() - 1) {
                    binding.btnOnboardingAction.setText("Get Started");
                    binding.btnSkip.setVisibility(View.INVISIBLE);
                } else {
                    binding.btnOnboardingAction.setText("Next");
                    binding.btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void updateIndicators(int position) {
        View[] dots = new View[]{binding.dot1, binding.dot2, binding.dot3};
        int activeWidth = (int) (20 * getResources().getDisplayMetrics().density);
        int inactiveWidth = (int) (6 * getResources().getDisplayMetrics().density);
        int height = (int) (6 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < dots.length; i++) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dots[i].getLayoutParams();
            if (i == position) {
                dots[i].setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.dot_active));
                lp.width = activeWidth;
            } else {
                dots[i].setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive));
                lp.width = inactiveWidth;
            }
            lp.height = height;
            dots[i].setLayoutParams(lp);
        }
    }

    private void setupListeners() {
        binding.btnSkip.setOnClickListener(v -> completeOnboarding());

        binding.btnOnboardingAction.setOnClickListener(v -> {
            int current = binding.viewPagerOnboarding.getCurrentItem();
            if (current < adapter.getItemCount() - 1) {
                binding.viewPagerOnboarding.setCurrentItem(current + 1, true);
            } else {
                completeOnboarding();
            }
        });
    }

    private void completeOnboarding() {
        SharedPrefsHelper.setOnboardingCompleted(requireContext(), true);
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_onboardingFragment_to_loginFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

