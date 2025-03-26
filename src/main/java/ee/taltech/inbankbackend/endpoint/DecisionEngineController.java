package ee.taltech.inbankbackend.endpoint;

import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import ee.taltech.inbankbackend.model.Decision;
import ee.taltech.inbankbackend.model.DecisionRequest;
import ee.taltech.inbankbackend.model.DecisionResponse;
import ee.taltech.inbankbackend.service.DecisionEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loan")
@CrossOrigin
public class DecisionEngineController {

    private final DecisionEngine decisionEngine;

    @Autowired
    DecisionEngineController(DecisionEngine decisionEngine) {
        this.decisionEngine = decisionEngine;
    }

    /**
     * A REST endpoint that handles requests for loan decisions.
     * The endpoint accepts POST requests with a request body containing the customer's personal ID code,
     * requested loan amount, and loan period.<br><br>
     * - If the loan amount or period is invalid, the endpoint returns a bad request response with an error message.<br>
     * - If the personal ID code is invalid, the endpoint returns a bad request response with an error message.<br>
     * - If an unexpected error occurs, the endpoint returns an internal server error response with an error message.<br>
     * - If no valid loans can be found, the endpoint returns a not found response with an error message.<br>
     * - If a valid loan is found, a DecisionResponse is returned containing the approved loan amount and period.
     *
     * @param request The request body containing the customer's personal ID code, requested loan amount, loan period, country and age
     * @return A ResponseEntity with a DecisionResponse body containing the approved loan amount and period, and an error message (if any)
     */
    @PostMapping("/decision")
    public ResponseEntity<DecisionResponse> requestDecision(@RequestBody DecisionRequest request) {
        System.out.println(request.getLoanAmount() + " " + request.getLoanPeriod() + " " + request.getPersonalCode() + " " + request.getCountry() + " " + request.getAge());
        try {
            Decision decision = decisionEngine.
                    calculateApprovedLoan(request.getPersonalCode(), request.getLoanAmount(), request.getLoanPeriod(), request.getCountry(),request.getAge());;
            return ResponseEntity.ok(getResponse(decision.getLoanAmount(), decision.getLoanPeriod(), decision.getErrorMessage()));
        } catch (InvalidPersonalCodeException | InvalidLoanAmountException | InvalidLoanPeriodException e) {
            return ResponseEntity.badRequest().body(getResponse(null, null, e.getMessage()));
        } catch (NoValidLoanException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(getResponse(null, null, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(getResponse(null, null,"An unexpected error occurred"));
        }
    }

    private DecisionResponse getResponse(Integer loanAmount, Integer loanPeriod, String errorMessage) {
        DecisionResponse response = new DecisionResponse();
        response.setLoanAmount(loanAmount);
        response.setLoanPeriod(loanPeriod);
        response.setErrorMessage(errorMessage);


        return response;
    }
}
