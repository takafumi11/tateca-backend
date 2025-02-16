package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.accessor.GroupAccessor;
import com.tateca.tatecabackend.accessor.ObligationAccessor;
import com.tateca.tatecabackend.accessor.TransactionAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.RepaymentCreationRequest;
import com.tateca.tatecabackend.dto.request.RepaymentRequestDTO;
import com.tateca.tatecabackend.dto.response.RepaymentCreationResponse;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.ObligationEntity;
import com.tateca.tatecabackend.entity.TransactionEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToLocalDateInUtc;

@Service
@RequiredArgsConstructor
public class RepaymentService {
    private final TransactionAccessor accessor;
    private final ObligationAccessor obligationAccessor;
    private final UserAccessor userAccessor;
    private final GroupAccessor groupAccessor;
    private final ExchangeRateAccessor exchangeRateAccessor;
    private final CurrencyNameAccessor currencyNameAccessor;

    @Transactional
    public RepaymentCreationResponse getRepayment(UUID repaymentId) {
        List<ObligationEntity> repayments = obligationAccessor.findByTransactionId(repaymentId);

        if (repayments.size() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "repayment not found:");
        }

        return RepaymentCreationResponse.from(repayments.get(0));
    }

    @Transactional
    public RepaymentCreationResponse createRepayment(RepaymentCreationRequest request, UUID groupId) {
        RepaymentRequestDTO repaymentRequestDTO = request.getRepaymentRequestDTO();
        String payerId = repaymentRequestDTO.getPayerId();
        String recipientId = repaymentRequestDTO.getRecipientId();

        UserEntity payer = userAccessor.findById(UUID.fromString(payerId));
        UserEntity recipient = userAccessor.findById(UUID.fromString(recipientId));
        GroupEntity group = groupAccessor.findById(groupId);

        ExchangeRateEntity exchangeRate = null;
        LocalDate date = convertToLocalDateInUtc(request.getRepaymentRequestDTO().getDate());
        try {
            exchangeRate = exchangeRateAccessor.findByCurrencyCodeAndDate(request.getRepaymentRequestDTO().getCurrencyCode(), date);
        } catch (ResponseStatusException e) {
            CurrencyNameEntity currencyName = currencyNameAccessor.findById("JPY");

            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                ExchangeRateEntity exchangeRateEntity = ExchangeRateEntity.getJPYForToday(date, currencyName);
                exchangeRate = exchangeRateAccessor.save(exchangeRateEntity);
            }
        }

        TransactionEntity savedTransaction = accessor.save(TransactionEntity.from(request, payer, group, exchangeRate));

        ObligationEntity savedObligation = obligationAccessor.save(ObligationEntity.from(savedTransaction, recipient));

        return RepaymentCreationResponse.from(savedObligation);
    }
}