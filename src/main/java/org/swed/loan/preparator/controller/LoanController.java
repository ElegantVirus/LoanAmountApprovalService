package org.swed.loan.preparator.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.swed.loan.preparator.entity.Loan;
import org.swed.loan.preparator.service.LoanService;

import java.math.BigDecimal;

@Api(value = "Loans preparator")
@RequestMapping("loans")
@Controller
public class LoanController {
    LoanService loanService;

    @Autowired
    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @ApiOperation(value = "Creating a loan amount approval request", response = String.class)
    @PostMapping("request-approval")
    public ResponseEntity<?> requestLoanApproval(@ApiParam(value = "Customer's id (XX-XXXX-XXX)", required = true)
                                                 @RequestParam String id,
                                                 @ApiParam(value = "Loan's amount", required = true)
                                                 @RequestParam BigDecimal loanAmount,
                                                 @ApiParam(value = "Approver 1", required = true)
                                                 @RequestParam String approverId1,
                                                 @ApiParam(value = "Approver 2")
                                                 @RequestParam(required = false) String approverId2,
                                                 @ApiParam(value = "Approver 3")
                                                 @RequestParam(required = false) String approverId3) {
        try {
            loanService.requestLoanApproval(id, loanAmount, approverId1, approverId2, approverId3);
            return new ResponseEntity<>("A loan approval request for customer " +
                    id + " has been made", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation(value = "Approve a loan", response = String.class)
    @PostMapping("approve")
    public ResponseEntity<?> approveLoan(@ApiParam(value = "Customer's id (XX-XXXX-XXX)", required = true)
                                         @RequestParam String id,
                                         @ApiParam(value = "Manager's username", required = true)
                                         @RequestParam String username) {
        try {
            Loan loan = loanService.approveLoan(id, username);
            return new ResponseEntity<>(loan, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation(value = "Get statistics", response = String.class)
    @GetMapping
    public ResponseEntity<?> getStatistics() {
        return new ResponseEntity<>(loanService.getStatistics(), HttpStatus.OK);
    }

    @ApiOperation(value = "Get all pending loans", response = String.class)
    @GetMapping("all")
    public ResponseEntity<?> getLoans() {
        return new ResponseEntity<>(loanService.getPendingLoans(), HttpStatus.OK);
    }

}
