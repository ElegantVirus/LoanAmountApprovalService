package org.swed.loan.preparator.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.swed.loan.preparator.entity.ApprovedLoan;
import org.swed.loan.preparator.entity.Statistics;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * I was trying to test the concurrency really hard, however :
 * a) Whenever I tried creating threads manually,
 * they wouldn't call the synchronised methods at
 * the *exact* same time so they therefore weren't valid
 * b) I tried @RunWith(ConcurrentTestRunner.class) and it worked
 * perfectly, too perfectly, because I would get an exception
 * when calling the methods concurrently (as expected)
 * and exception handling when it's thrown not in the main thread
 * is an awful, awful thing.
 * <p>
 * So I gave up. :(
 */
public class LoanServiceTest {
    final LoanService loanService = new LoanService();

    @Test
    public void requestLoanTwoTimes_fails() {
        requestSimpleLoan("XX-XXXX-XXX");
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> requestSimpleLoan("XX-XXXX-XXX")
        );
        assertEquals(
                "There can be only one pending loan request for one customer!",
                exception.getMessage()
        );
    }

    @Test
    public void requestLoanBadId_fails() {
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> requestSimpleLoan("XXodcmlkem")
        );
        assertEquals(
                "Bad customer's id!",
                exception.getMessage()
        );
    }

    @Test
    public void approveLoanBadId_fails() {
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.approveLoan("XXodcmlkem", "username")
        );
        assertEquals(
                "Loan with such id does not exist!",
                exception.getMessage()
        );
    }

    @Test
    public void approveLoanBadUsername_fails() {
        requestSimpleLoan("xx-xxxx-xxx");
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.approveLoan("xx-xxxx-xxx", "username")
        );
        assertEquals(
                "It is not your loan to approve!",
                exception.getMessage()
        );
    }

    @Test
    public void approveLoanSecondTime_fails() {
        loanService.requestLoanApproval(
                "xx-xxxx-xxx",
                BigDecimal.valueOf(117),
                "approver1",
                "approver2",
                null
        );
        loanService.approveLoan("xx-xxxx-xxx", "approver1");
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.approveLoan("xx-xxxx-xxx", "approver1")
        );
        assertEquals(
                "The loan has already been approved by you!",
                exception.getMessage()
        );
    }

    @Test
    public void getInformation_empty() {
        Statistics statistics = loanService.getStatistics();
        assert statistics == null;
    }

    @Test
    public void getLoansKillingThemselves() throws InterruptedException {
        ReflectionTestUtils.setField(loanService, "approvedLoans", getApprovedLoans());
        assert loanService.getStatistics().getCount() == 2;
        synchronized (this) {
            wait(5000);
        }
        assert loanService.getStatistics() == null;
    }

    private Map<ApprovedLoan, LocalTime> getApprovedLoans() {
        Map<ApprovedLoan, LocalTime> approvedLoans = new HashMap<>();

        approvedLoans.put(
                new ApprovedLoan(BigDecimal.valueOf(45), approvedLoans, 4),
                LocalTime.now()
        );
        approvedLoans.put(
                new ApprovedLoan(BigDecimal.valueOf(578), approvedLoans, 4),
                LocalTime.now()
        );
        return approvedLoans;
    }

    @Test
    public void getInformation_afterFlow() {
        ReflectionTestUtils.setField(loanService, "statisticsPeriod", 60);

        loanService.requestLoanApproval(
                "12-Aqg9-QQw",
                BigDecimal.valueOf(117),
                "approver1",
                "approver2",
                null
        );
        loanService.requestLoanApproval(
                "55-Yjds-QQy",
                BigDecimal.valueOf(21),
                "approver3",
                "approver2",
                "approver4"
        );
        loanService.requestLoanApproval(
                "78-ghty-888",
                BigDecimal.valueOf(785),
                "approver1",
                null,
                null
        );
        loanService.approveLoan("12-Aqg9-QQw", "approver1");
        loanService.approveLoan("12-Aqg9-QQw", "approver2");
        loanService.approveLoan("78-ghty-888", "approver1");
        loanService.approveLoan("55-Yjds-QQy", "approver3");
        loanService.approveLoan("55-Yjds-QQy", "approver2");
        loanService.approveLoan("55-Yjds-QQy", "approver4");
        Statistics statistics = loanService.getStatistics();
        assert statistics.getCount() == 3;
        assert statistics.getSum().equals(BigDecimal.valueOf(923));
        assert statistics.getAvg().equals(BigDecimal.valueOf(307.66));
        assert statistics.getMax().equals(BigDecimal.valueOf(785));
        assert statistics.getMin().equals(BigDecimal.valueOf(21));
    }

    private void requestSimpleLoan(String id) {
        loanService.requestLoanApproval(
                id,
                BigDecimal.TEN,
                "approver1",
                null,
                null
        );
    }

}
