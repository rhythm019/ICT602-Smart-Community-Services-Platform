package com.greenfield.scsp.domain;

import java.time.LocalDateTime;

/**
 * Resident feedback on a completed request (FR-15, BR-08).
 *
 * @param rating      score from 1 (poor) to 5 (excellent)
 * @param comment     optional free-text comment
 * @param submittedAt when the feedback was given
 */
public record Feedback(int rating, String comment, LocalDateTime submittedAt) {

    /** Lowest allowed rating. */
    public static final int MIN_RATING = 1;
    /** Highest allowed rating. */
    public static final int MAX_RATING = 5;
}
