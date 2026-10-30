package com.resqmeal.app;

import com.resqmeal.app.model.User;
import com.resqmeal.app.ui.onboarding.OnboardingItem;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.Resource;
import com.resqmeal.app.utils.Validators;

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

    @Test
    public void validators_name_rules() {
        assertFalse(Validators.isValidName(null));
        assertFalse(Validators.isValidName(""));
        assertFalse(Validators.isValidName("Ab")); // < 3 chars
        assertTrue(Validators.isValidName("Alice"));
        assertTrue(Validators.isValidName("City Shelter NGO"));
    }

    @Test
    public void validators_phone_rules() {
        assertFalse(Validators.isValidPhone(null));
        assertFalse(Validators.isValidPhone("123"));
        assertFalse(Validators.isValidPhone("abcdefghij"));
        assertFalse(Validators.isValidPhone("12345678901")); // 11 digits
        assertTrue(Validators.isValidPhone("9876543210")); // exactly 10 digits
    }

    @Test
    public void validators_password_rules() {
        assertFalse(Validators.isValidPassword(null));
        assertFalse(Validators.isValidPassword("short1")); // < 8 chars
        assertFalse(Validators.isValidPassword("onlyletters")); // no numbers
        assertFalse(Validators.isValidPassword("12345678")); // no letters
        assertTrue(Validators.isValidPassword("Password123")); // >= 8 chars, letter + number
    }

    @Test
    public void resource_status_helpers() {
        Resource<String> loading = Resource.loading(null);
        assertTrue(loading.isLoading());
        assertFalse(loading.isSuccess());

        Resource<String> success = Resource.success("data");
        assertTrue(success.isSuccess());
        assertEquals("data", success.getData());

        Resource<String> error = Resource.error("Failed", null);
        assertTrue(error.isError());
        assertEquals("Failed", error.getMessage());
    }
}
