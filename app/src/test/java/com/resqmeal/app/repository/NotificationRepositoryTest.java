package com.resqmeal.app.repository;

import com.resqmeal.app.model.User;
import com.resqmeal.app.utils.Constants;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotificationRepositoryTest {

    @Test
    public void nearbyPostNotifications_requireApprovedActiveReceiverWithinTenKilometers() {
        User receiver = new User();
        receiver.setRole(Constants.ROLE_RECEIVER);
        receiver.setApprovalStatus(Constants.APPROVAL_APPROVED);
        receiver.setActive(true);

        assertTrue(NotificationRepository.isEligibleNearbyReceiver(receiver, 10_000));
        assertTrue(NotificationRepository.isEligibleNearbyReceiver(receiver, 1_000));
        assertFalse(NotificationRepository.isEligibleNearbyReceiver(receiver, 10_001));

        receiver.setActive(false);
        assertFalse(NotificationRepository.isEligibleNearbyReceiver(receiver, 1_000));
        receiver.setActive(true);
        receiver.setApprovalStatus(Constants.APPROVAL_PENDING);
        assertFalse(NotificationRepository.isEligibleNearbyReceiver(receiver, 1_000));
        receiver.setApprovalStatus(Constants.APPROVAL_APPROVED);
        receiver.setRole(Constants.ROLE_DONOR);
        assertFalse(NotificationRepository.isEligibleNearbyReceiver(receiver, 1_000));
        assertFalse(NotificationRepository.isEligibleNearbyReceiver(receiver, Double.NaN));
    }
}
