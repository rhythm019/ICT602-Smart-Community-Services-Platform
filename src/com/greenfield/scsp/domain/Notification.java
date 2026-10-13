package com.greenfield.scsp.domain;

import java.time.LocalDateTime;

/**
 * A message sent to a user about a request, booking or event (FR-14).
 *
 * @param recipient user receiving the message
 * @param subject   short subject line
 * @param message   body text
 * @param createdAt when it was generated
 */
public record Notification(User recipient, String subject, String message,
                           LocalDateTime createdAt) {
}
