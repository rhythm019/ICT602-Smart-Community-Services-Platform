package com.greenfield.scsp.integration;

import com.greenfield.scsp.domain.Notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simulated email gateway. Messages are kept in an outbox so the console
 * demo and tests can show what would have been sent.
 */
public class EmailChannel implements NotificationChannel {

    private final List<String> outbox = new ArrayList<>();

    @Override
    public String name() {
        return "EMAIL";
    }

    @Override
    public boolean send(Notification n) {
        outbox.add("To: " + n.recipient().getEmail() + " | " + n.subject() + " | " + n.message());
        return true;
    }

    public List<String> getOutbox() { return Collections.unmodifiableList(outbox); }
}
