package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.GroupAccessor;
import com.tateca.tatecabackend.accessor.LoanAccessor;
import com.tateca.tatecabackend.accessor.ObligationAccessor;
import com.tateca.tatecabackend.accessor.UserAccessor;
import com.tateca.tatecabackend.dto.request.LoanRequestDTO;
import com.tateca.tatecabackend.dto.request.ObligationRequestDTO;
import com.tateca.tatecabackend.dto.request.LoanCreationRequest;
import com.tateca.tatecabackend.dto.response.LoanCreationResponse;
import com.tateca.tatecabackend.entity.GroupEntity;
import com.tateca.tatecabackend.entity.LoanEntity;
import com.tateca.tatecabackend.entity.ObligationEntity;
import com.tateca.tatecabackend.entity.UserEntity;
import com.tateca.tatecabackend.service.util.AmountHelper;
import com.tateca.tatecabackend.service.util.TimeHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tateca.tatecabackend.service.util.TimeHelper.convertToTokyoTime;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class LoanService {
    private final LoanAccessor accessor;
    private final UserAccessor userAccessor;
    private final GroupAccessor groupAccessor;
    private final ObligationAccessor obligationAccessor;

    @Transactional
    public LoanCreationResponse getLoan(UUID loanId) {
        LoanEntity loan = accessor.findById(loanId);
        List<ObligationEntity> obligations = obligationAccessor.findByLoanId(loan.getUuid());

        return LoanCreationResponse.buildResponse(loan, obligations);
    }

    @Transactional
    public LoanCreationResponse createLoan(LoanCreationRequest request, UUID groupId) {
        UUID userId = UUID.fromString(request.getLoanRequestDTO().getPayerId());
        UserEntity user = userAccessor.findById(userId);
        GroupEntity group = groupAccessor.findById(groupId);

        LoanEntity savedLoan = accessor.save(LoanEntity.from(request, user, group));

        List<ObligationEntity> obligationEntityList = request.getObligationRequestDTOs().stream()
                .map(obligation -> {
                    UUID obligationUserId = UUID.fromString(obligation.getUserUuid());
                    UserEntity obligationUser = userAccessor.findById(obligationUserId);

                    return ObligationEntity.builder()
                            .uuid(UUID.randomUUID())
                            .loan(savedLoan)
                            .user(obligationUser)
                            .amount(obligation.getAmount())
                            .build();
                })
                .collect(Collectors.toList());

        List<ObligationEntity> savedObligations = obligationAccessor.saveAll(obligationEntityList);

        Map<String, BigDecimal> balanceUpdates = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ObligationEntity obligation : savedObligations) {
            BigDecimal amount = AmountHelper.calculateAmount(obligation.getAmount(), request.getLoanRequestDTO().getCurrencyRate());
            balanceUpdates.put(obligation.getUser().getUuid().toString(), amount);
            totalAmount = totalAmount.add(amount);
        }

        // Subtract the total amount from the payer's balance
        balanceUpdates.put(user.getUuid().toString(), totalAmount.negate());

        return LoanCreationResponse.buildResponse(savedLoan, savedObligations);
    }

    @Transactional
    public void deleteLoan(UUID loanId) {
        // Delete Obligations
        List<ObligationEntity> obligationEntityList = obligationAccessor.findByLoanId(loanId);
        List<UUID> uuidList = obligationEntityList.stream().map(ObligationEntity::getUuid).toList();
        obligationAccessor.deleteAllById(uuidList);

        // Delete Loan
        accessor.deleteById(loanId);
    }

}
