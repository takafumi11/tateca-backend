package com.tateca.tatecabackend.accessor;

import com.tateca.tatecabackend.repository.LoanRepository;
import com.tateca.tatecabackend.repository.RepaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionAccessor {
    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;


}
