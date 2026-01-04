package com.tateca.tatecabackend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that transaction details (loan or repayment) match the transaction type.
 * - LOAN transactions must have loan details with non-empty obligations
 * - REPAYMENT transactions must have repayment details with recipient ID
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TransactionDetailsValidator.class)
@Documented
public @interface ValidTransactionDetails {
    String message() default "Transaction details must match transaction type";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
