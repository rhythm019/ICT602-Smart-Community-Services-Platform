package com.greenfield.scsp.integration;

import com.greenfield.scsp.domain.Notification;

/**
 * Strategy interface for one way of delivering a notification (SI-03).
 * <p>
 * New channels (e.g. push notifications) can be added by writing a new
 * implementation, without changing NotificationService
 * (Open/Closed Principle).
 */
public interface NotificationChannel {

    /** @return short channel name used in logs, e.g. "EMAIL" */
    String name();

    /**
     * Sends the notification through this channel.
     *
     * @param notification message to deliver
     * @return true when the provider accepted the message
     */
    boolean send(Notification notification);
}
