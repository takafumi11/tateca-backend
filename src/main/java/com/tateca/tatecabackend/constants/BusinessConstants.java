package com.tateca.tatecabackend.constants;

/**
 * Business rule constants for the application.
 * These constants define core business logic constraints that should remain consistent across the application.
 */
public class BusinessConstants {

    /**
     * Maximum number of participants in a group (excluding the host).
     * This limit ensures optimal group management and performance.
     */
    public static final int MAX_GROUP_PARTICIPANTS = 8;

    /**
     * Maximum total members in a group including the host.
     * Calculated as: MAX_GROUP_PARTICIPANTS + 1 (host) = 9 total members
     */
    public static final int MAX_GROUP_SIZE = MAX_GROUP_PARTICIPANTS + 1; // 9 total

    /**
     * Maximum number of obligations in a loan transaction.
     * This equals MAX_GROUP_PARTICIPANTS because:
     * - A group has up to 9 members (8 participants + 1 host)
     * - The payer/lender cannot owe themselves
     * - Therefore, maximum obligations = 9 - 1 = 8
     */
    public static final int MAX_TRANSACTION_OBLIGATIONS = MAX_GROUP_PARTICIPANTS; // 8

    private BusinessConstants() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
