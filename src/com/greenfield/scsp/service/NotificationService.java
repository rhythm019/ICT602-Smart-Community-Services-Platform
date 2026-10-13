package com.greenfield.scsp.service;

import com.greenfield.scsp.domain.Notification;
import com.greenfield.scsp.domain.User;
import com.greenfield.scsp.integration.NotificationChannel;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sends notifications through every configured channel (FR-14).
 * <p>
 * The service only knows the {@link NotificationChannel} interface, so the
 * set of channels is decided by the composition root (Main), not here.
 */
public class NotificationService {

    private final List<NotificationChannel> channels;
    private final Clock clock;

    /**
     * @param channels delivery channels, e.g. email, SMS, in-app
     * @param clock    time source
     */
    public NotificationService(List<NotificationChannel> channels, Clock clock) {
        this.channels = List.copyOf(channels);
        this.clock = clock;
    }

    /**
     * Notifies a user on all channels. A failure on one channel does not stop
     * the others, so a down SMS gateway never blocks the main workflow.
     *
     * @param recipient user to notify
     * @param subject   subject line
     * @param message   body
     * @return number of channels that delivered the message
     */
    public int notify(User recipient, String subject, String message) {
        Notification n = new Notification(recipient, subject, message, LocalDateTime.now(clock));
        int delivered = 0;
        for (NotificationChannel channel : channels) {
            try {
                if (channel.send(n)) {
                    delivered++;
                }
            } catch (RuntimeException ex) {
                // Delivery problems are tolerated; the request itself is already saved.
            }
        }
        return delivered;
    }
}
