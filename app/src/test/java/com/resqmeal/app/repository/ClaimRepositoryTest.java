package com.resqmeal.app.repository;

import com.resqmeal.app.utils.Constants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClaimRepositoryTest {

    @Test
    public void pickupCode_isAlwaysFourDigits() {
        for (int i = 0; i < 1_000; i++) {
            String pickupCode = ClaimRepository.generatePickupCode();
            assertEquals(4, pickupCode.length());
            assertTrue(pickupCode.matches("[0-9]{4}"));
        }
    }

    @Test
    public void activeClaimCapacity_stopsAtThree() {
        assertTrue(ClaimRepository.hasActiveClaimCapacity(0));
        assertTrue(ClaimRepository.hasActiveClaimCapacity(Constants.MAX_ACTIVE_CLAIMS - 1));
        assertFalse(ClaimRepository.hasActiveClaimCapacity(Constants.MAX_ACTIVE_CLAIMS));
        assertFalse(ClaimRepository.hasActiveClaimCapacity(Constants.MAX_ACTIVE_CLAIMS + 1));
    }

    @Test
    public void pickupCodeFormat_requiresExactlyFourDigits() {
        assertTrue(ClaimRepository.isValidPickupCodeFormat("0123"));
        assertFalse(ClaimRepository.isValidPickupCodeFormat("123"));
        assertFalse(ClaimRepository.isValidPickupCodeFormat("12345"));
        assertFalse(ClaimRepository.isValidPickupCodeFormat("12a4"));
        assertFalse(ClaimRepository.isValidPickupCodeFormat(null));
    }
}
