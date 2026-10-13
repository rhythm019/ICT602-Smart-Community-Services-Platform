package com.greenfield.scsp.exception;

/**
 * Thrown when an operation would break a business rule from the SRS.
 * The rule id (e.g. "BR-03") is kept so tests and logs can trace the cause.
 */
public class BusinessRuleException extends RuntimeException {

    private final String ruleId;

    /**
     * @param ruleId  SRS rule identifier
     * @param message explanation shown to the user
     */
    public BusinessRuleException(String ruleId, String message) {
        super(message);
        this.ruleId = ruleId;
    }

    public String getRuleId() { return ruleId; }
}
