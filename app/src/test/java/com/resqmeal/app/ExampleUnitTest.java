package com.resqmeal.app;

import com.resqmeal.app.ui.onboarding.OnboardingItem;
import com.resqmeal.app.utils.Constants;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void onboardingItem_getters_returnExpectedValues() {
        OnboardingItem item = new OnboardingItem(
                R.raw.onboarding_slide1,
                "Reduce Food Waste",
                "Connect surplus food from restaurants, messes, and events."
        );

        assertEquals(R.raw.onboarding_slide1, item.getLottieRes());
        assertEquals("Reduce Food Waste", item.getTitle());
        assertEquals("Connect surplus food from restaurants, messes, and events.", item.getDescription());
    }

    @Test
    public void constants_collections_areExact() {
        assertEquals("users", Constants.COLLECTION_USERS);
        assertEquals("food_posts", Constants.COLLECTION_FOOD_POSTS);
        assertEquals("claims", Constants.COLLECTION_CLAIMS);
        assertEquals("notifications", Constants.COLLECTION_NOTIFICATIONS);
        assertEquals("reports", Constants.COLLECTION_REPORTS);
    }
}
