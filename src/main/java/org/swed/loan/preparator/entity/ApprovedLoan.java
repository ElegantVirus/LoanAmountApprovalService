package org.swed.loan.preparator.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/*
 * A 'hack' to minimize response time for the statistics, collected in the last X seconds.
 */
@Getter
@Setter
public class ApprovedLoan {
    private BigDecimal amount;
    private Map<ApprovedLoan, LocalTime> loanMap;

    public ApprovedLoan(BigDecimal amount, Map<ApprovedLoan, LocalTime> loanMap, long lifeSpan) {
        this.amount = amount;
        this.loanMap = loanMap;
        beginLifeCycle((int) lifeSpan);
    }

    private void suicide() {
        loanMap.remove(this);
    }

    private void beginLifeCycle(int lifeSpan) {
        TimerTask task = new TimerTask() {
            public void run() {
                suicide();
            }
        };

        new Timer().schedule(task, lifeSpan * 1000);
    }
}
