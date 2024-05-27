package com.moneyme.moneymebackend.accessor;

import com.moneyme.moneymebackend.repository.LoanRepository;
import com.moneyme.moneymebackend.repository.RepaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionAccessor {
    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;


}
