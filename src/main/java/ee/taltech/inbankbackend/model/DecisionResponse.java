package ee.taltech.inbankbackend.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Holds the response data of the REST endpoint.
 */
@Getter
@Setter
public class DecisionResponse {
    private Integer loanAmount;
    private Integer loanPeriod;
    private String errorMessage;
}
