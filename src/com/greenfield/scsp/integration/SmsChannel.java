package com.greenfield.scsp.integration;

import com.greenfield.scsp.domain.Notification;
import com.greenfield.scsp.domain.Resident;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simulated SMS gateway. Only residents with a phone number receive SMS;
 * staff are notified by email and in-app messages only.
 */
public class SmsChannel implements NotificationChannel {

    /** Standard single-SMS length. */
    private static final int MAX_SMS_LENGTH = 160;

    private final List<String> outbox = new ArrayList<>();

    @Override
    public String name() {
        return "SMS";
    }

    @Override
    public boolean send(Notification n) {
        if (!(n.recipient() instanceof Resident resident)
                || resident.getPhone() == null || resident.getPhone().isBlank()) {
            return false;
        }
        String text = n.subject() + ": " + n.message();
        if (text.length() > MAX_SMS_LENGTH) {
            text = text.substring(0, MAX_SMS_LENGTH - 3) + "...";
        }
        outbox.add("To: " + resident.getPhone() + " | " + text);
        return true;
    }

    public List<String> getOutbox() { return Collections.unmodifiableList(outbox); }
}
