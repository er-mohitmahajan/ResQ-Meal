package com.resqmeal.app.ui.onboarding;

import androidx.annotation.RawRes;

public class OnboardingItem {

    @RawRes
    private final int lottieRes;
    private final String title;
    private final String description;

    public OnboardingItem(@RawRes int lottieRes, String title, String description) {
        this.lottieRes = lottieRes;
        this.title = title;
        this.description = description;
    }

    public int getLottieRes() {
        return lottieRes;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
