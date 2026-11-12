package com.resqmeal.app.viewmodel;

import com.resqmeal.app.utils.Constants;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FoodPostViewModelFilterTest {

    @Test
    public void foodTypeFilterIncludesMixedPostsForEitherDiet() {
        assertTrue(FoodPostViewModel.matchesFoodType(Constants.FOOD_TYPE_VEG, Constants.FOOD_TYPE_VEG));
        assertFalse(FoodPostViewModel.matchesFoodType(Constants.FOOD_TYPE_NON_VEG, Constants.FOOD_TYPE_VEG));
        assertTrue(FoodPostViewModel.matchesFoodType(Constants.FOOD_TYPE_BOTH, Constants.FOOD_TYPE_VEG));
        assertTrue(FoodPostViewModel.matchesFoodType(Constants.FOOD_TYPE_BOTH, Constants.FOOD_TYPE_NON_VEG));
        assertTrue(FoodPostViewModel.matchesFoodType(Constants.FOOD_TYPE_BOTH, Constants.FOOD_TYPE_BOTH));
        assertFalse(FoodPostViewModel.matchesFoodType(Constants.FOOD_TYPE_VEG, Constants.FOOD_TYPE_BOTH));
        assertTrue(FoodPostViewModel.matchesFoodType(Constants.FOOD_TYPE_NON_VEG, null));
    }

    @Test
    public void categoryFilterMatchesExactCategoryOrAll() {
        assertTrue(FoodPostViewModel.matchesCategory(Constants.CATEGORY_BAKERY, Constants.CATEGORY_BAKERY));
        assertFalse(FoodPostViewModel.matchesCategory(Constants.CATEGORY_OTHER, Constants.CATEGORY_BAKERY));
        assertTrue(FoodPostViewModel.matchesCategory(Constants.CATEGORY_OTHER, null));
    }
}
