package org.swed.loan.preparator.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Setter
@Getter
@AllArgsConstructor
public class Loan {
    private String id;
    private BigDecimal amount;
    private Map<String, Boolean> approves;
}
