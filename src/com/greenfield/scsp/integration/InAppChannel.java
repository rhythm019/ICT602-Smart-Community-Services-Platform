package com.greenfield.scsp.integration;

import com.greenfield.scsp.domain.Notification;

import java.util.ArrayList;
import java.util.List;

/**
 * In-app inbox. Stores notifications so each user can read them on their
 * dashboard (wireframe W-02, "Notifications").
 */
public class InAppChannel implements NotificationChannel {

    private final List<Notification> inbox = new ArrayList<>();

    @Override
    public String name() {
        return "IN_APP";
    }

    @Override
    public boolean send(Notification notification) {
        inbox.add(notification);
        return true;
    }

    /**
     * Returns the notifications for one user, newest first.
     *
     * @param userId recipient id
     * @return that user's messages
     */
    public List<Notification> inboxFor(String userId) {
        List<Notification> mine = new ArrayList<>(inbox.stream()
                .filter(n -> n.recipient().getUserId().equals(userId))
                .toList());
        java.util.Collections.reverse(mine);
        return mine;
    }
}
