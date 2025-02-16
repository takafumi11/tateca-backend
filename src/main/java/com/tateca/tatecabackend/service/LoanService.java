package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.accessor.GroupAccessor;
import com.tateca.tatecabackend.accessor.ObligationAccessor;
import com.tateca.tatecabackend.accessor.TransactionAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.LoanCreationRequest;
import com.tateca.tatecabackend.dto.response.LoanCreationResponse;
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
import java.util.stream.Collectors;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToLocalDateInUtc;

@Service
@RequiredArgsConstructor
public class LoanService {
    private final TransactionAccessor accessor;
    private final UserAccessor userAccessor;
    private final GroupAccessor groupAccessor;
    private final ObligationAccessor obligationAccessor;
    private final ExchangeRateAccessor exchangeRateAccessor;
    private final CurrencyNameAccessor currencyNameAccessor;

    @Transactional
    public LoanCreationResponse getLoan(UUID loanId) {
        TransactionEntity loan = accessor.findById(loanId);
        List<ObligationEntity> obligations = obligationAccessor.findByTransactionId(loan.getUuid());

        return LoanCreationResponse.buildResponse(loan, obligations);
    }

    @Transactional
    public LoanCreationResponse createLoan(LoanCreationRequest request, UUID groupId) {
        UUID userId = UUID.fromString(request.getLoanRequestDTO().getPayerId());
        UserEntity user = userAccessor.findById(userId);
        GroupEntity group = groupAccessor.findById(groupId);
        ExchangeRateEntity exchangeRate = null;
        LocalDate date = convertToLocalDateInUtc(request.getLoanRequestDTO().getDate());
        try {
            exchangeRate = exchangeRateAccessor.findByCurrencyCodeAndDate(request.getLoanRequestDTO().getCurrencyCode(), date);
        } catch (ResponseStatusException e) {
            CurrencyNameEntity currencyName = currencyNameAccessor.findById("JPY");

            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                ExchangeRateEntity exchangeRateEntity = ExchangeRateEntity.getJPYEntity(date, currencyName);
                exchangeRate = exchangeRateAccessor.save(exchangeRateEntity);
            }
        }
      
        TransactionEntity savedLoan = accessor.save(TransactionEntity.from(request, user, group, exchangeRate));

        List<ObligationEntity> obligationEntityList = request.getObligationRequestDTOs().stream()
                .map(obligation -> {
                    UUID obligationUserId = UUID.fromString(obligation.getUserUuid());
                    UserEntity obligationUser = userAccessor.findById(obligationUserId);

                    return ObligationEntity.builder()
                            .uuid(UUID.randomUUID())
                            .transaction(savedLoan)
                            .user(obligationUser)
                            .amount(obligation.getAmount())
                            .build();
                })
                .collect(Collectors.toList());

        List<ObligationEntity> savedObligations = obligationAccessor.saveAll(obligationEntityList);
      
        return LoanCreationResponse.buildResponse(savedLoan, savedObligations);
    }
}
