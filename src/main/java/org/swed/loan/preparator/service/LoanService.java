package org.swed.loan.preparator.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.swed.loan.preparator.entity.ApprovedLoan;
import org.swed.loan.preparator.entity.Loan;
import org.swed.loan.preparator.entity.Statistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@Getter
public class LoanService {

    private final Map<String, Loan> pendingLoans = new HashMap<>();
    private final Map<ApprovedLoan, LocalTime> approvedLoans = new HashMap<>();
    @Value("${statistics.time.seconds:60}")
    private long statisticsPeriod;

    public synchronized void requestLoanApproval(
            String id,
            BigDecimal loanAmount,
            String approver1,
            String approver2,
            String approver3
    ) {
        validateLoanApprovalRequest(id);
        pendingLoans.put(id, createLoan(id, loanAmount, approver1, approver2, approver3));
    }

    private Loan createLoan(
            String id,
            BigDecimal loanAmount,
            String approver1,
            String approver2,
            String approver3
    ) {
        Map<String, Boolean> approves = new HashMap<>();

        approves.put(approver1, false);
        if (approver2 != null) {
            approves.put(approver2, false);
        }
        if (approver3 != null) {
            approves.put(approver3, false);
        }
        return new Loan(id, loanAmount, approves);
    }

    private void validateLoanApprovalRequest(String id) {
        if (!id.matches("..-....-...")) {
            log.warn("Customer's id " + id + " does not meet the pattern requirements");
            throw new IllegalArgumentException("Bad customer's id!");
        }
        if (pendingLoans.containsKey(id)) {
            log.warn("Customer with id " + id + " has just requested for one too many loans.");
            throw new IllegalArgumentException("There can be only one pending loan request for one customer!");
        }
    }

    public synchronized Loan approveLoan(String id, String username) {
        validateLoanApproval(id, username);

        Loan pendingLoan = pendingLoans.get(id);
        pendingLoan.getApproves().put(username, true);

        if (!pendingLoan.getApproves().containsValue(false)) {
            approvedLoans.put(
                    new ApprovedLoan(pendingLoan.getAmount(), approvedLoans, statisticsPeriod),
                    LocalTime.now()
            );
            pendingLoans.remove(id);
        }
        return pendingLoan;
    }

    private void validateLoanApproval(String id, String username) {
        Loan loan = pendingLoans.get(id);

        if (loan == null) {
            log.warn("An attempt to approve non existent loan with " + id + " id has been made");
            throw new IllegalArgumentException("Loan with such id does not exist!");
        }
        if (loan.getApproves().get(username) == null) {
            log.warn("User with id " + username + " just tried to approve someone else's loan with id " + id);
            throw new IllegalArgumentException("It is not your loan to approve!");
        }
        if (loan.getApproves().get(username)) {
            log.warn("User with id " + username + " just tried to approve a loan with id "
                    + id + " for the second time");
            throw new IllegalArgumentException("The loan has already been approved by you!");
        }
    }

    public Statistics getStatistics() {
        Set<ApprovedLoan> loans = approvedLoans.keySet();
        if (!loans.isEmpty()) {
            BigDecimal sum = getSum(loans);
            int size = loans.size();

            return new Statistics(
                    statisticsPeriod,
                    size,
                    sum,
                    getAvg(sum, size),
                    getMax(loans),
                    getMin(loans)
            );
        } else {
            return null;
        }
    }

    private BigDecimal getSum(Set<ApprovedLoan> loans) {
        return loans.stream()
                .map(ApprovedLoan::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getAvg(BigDecimal sum, int size) {
        return sum.divide(new BigDecimal(size), 2, RoundingMode.DOWN);
    }

    private BigDecimal getMax(Set<ApprovedLoan> loans) {
        return loans
                .stream()
                .map(ApprovedLoan::getAmount)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getMin(Set<ApprovedLoan> loans) {
        return loans
                .stream()
                .map(ApprovedLoan::getAmount)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }
}
