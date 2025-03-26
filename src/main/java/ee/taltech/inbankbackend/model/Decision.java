package ee.taltech.inbankbackend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Holds the response data of the REST endpoint.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Decision {
    private Integer loanAmount;
    private Integer loanPeriod;
    private String errorMessage;
}
