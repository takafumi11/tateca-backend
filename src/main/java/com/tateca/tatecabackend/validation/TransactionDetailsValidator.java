package com.tateca.tatecabackend.validation;

import com.tateca.tatecabackend.dto.request.CreateTransactionRequestDTO;
import com.tateca.tatecabackend.model.TransactionType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link ValidTransactionDetails} annotation.
 * Ensures that transaction details match the specified transaction type.
 */
public class TransactionDetailsValidator
        implements ConstraintValidator<ValidTransactionDetails, CreateTransactionRequestDTO> {

    @Override
    public boolean isValid(CreateTransactionRequestDTO dto, ConstraintValidatorContext context) {
        if (dto == null || dto.transactionType() == null) {
            return true; // @NotNull handles null checks separately
        }

        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        if (dto.transactionType() == TransactionType.LOAN) {
            isValid = validateLoanTransaction(dto, context);
        } else if (dto.transactionType() == TransactionType.REPAYMENT) {
            isValid = validateRepaymentTransaction(dto, context);
        }

        return isValid;
    }

    private boolean validateLoanTransaction(CreateTransactionRequestDTO dto, ConstraintValidatorContext context) {
        boolean isValid = true;

        if (dto.loan() == null) {
            context.buildConstraintViolationWithTemplate(
                    "{validation.transaction.details.loan.required}"
            ).addPropertyNode("loan").addConstraintViolation();
            isValid = false;
        } else if (dto.loan().obligations() == null || dto.loan().obligations().isEmpty()) {
            context.buildConstraintViolationWithTemplate(
                    "{validation.transaction.details.loan.obligations.empty}"
            ).addPropertyNode("loan").addPropertyNode("obligations").addConstraintViolation();
            isValid = false;
        }

        if (dto.repayment() != null) {
            context.buildConstraintViolationWithTemplate(
                    "{validation.transaction.details.loan.repayment.conflict}"
            ).addPropertyNode("repayment").addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }

    private boolean validateRepaymentTransaction(CreateTransactionRequestDTO dto, ConstraintValidatorContext context) {
        boolean isValid = true;

        if (dto.repayment() == null) {
            context.buildConstraintViolationWithTemplate(
                    "{validation.transaction.details.repayment.required}"
            ).addPropertyNode("repayment").addConstraintViolation();
            isValid = false;
        } else if (dto.repayment().recipientId() == null) {
            context.buildConstraintViolationWithTemplate(
                    "{validation.transaction.details.repayment.recipientId.required}"
            ).addPropertyNode("repayment").addPropertyNode("recipientId").addConstraintViolation();
            isValid = false;
        }

        if (dto.loan() != null) {
            context.buildConstraintViolationWithTemplate(
                    "{validation.transaction.details.repayment.loan.conflict}"
            ).addPropertyNode("loan").addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }
}
