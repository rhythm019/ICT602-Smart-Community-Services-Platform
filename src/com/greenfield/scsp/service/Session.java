package com.greenfield.scsp.service;

import com.greenfield.scsp.domain.User;

import java.time.LocalDateTime;

/**
 * An authenticated session returned by a successful login (FR-02).
 * Every protected service method receives the session, so the service layer
 * (not the UI) decides what the user may do.
 *
 * @param user      the signed-in user
 * @param startedAt login time
 */
public record Session(User user, LocalDateTime startedAt) {
}
