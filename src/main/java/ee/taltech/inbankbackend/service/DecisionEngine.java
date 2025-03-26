package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.constants.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import ee.taltech.inbankbackend.model.Decision;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();

    private static final Map<String, Integer> AGE_LIMITS = Map.of(
            "estonia", DecisionEngineConstants.MAXIMUM_AGE_ESTONIA,
            "latvia", DecisionEngineConstants.MAXIMUM_AGE_LATVIA,
            "lithuania", DecisionEngineConstants.MAXIMUM_AGE_LITHUANIA);

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws NoValidLoanException If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, Integer loanAmount, Integer loanPeriod, String country, Integer age)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod, country, age);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }
        int creditModifier = getCreditModifier(personalCode);

        int outputLoanAmount;

        if (creditModifier == 0) {
            throw new NoValidLoanException("Loans are not available with debt!");
        }

         //O(1)
        loanPeriod = Math.max(loanPeriod, (int) Math.ceil((double) DecisionEngineConstants.MINIMUM_LOAN_AMOUNT / creditModifier));

        while(calculateCreditScore(creditModifier, loanAmount, loanPeriod) < 0.1 && loanPeriod < DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            loanPeriod++;
        }
        //    O(n)
//        while (highestValidLoanAmount(creditModifier, loanPeriod) < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
//            loanPeriod++;
//        }

        if (loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            outputLoanAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, highestValidLoanAmount(creditModifier, loanPeriod));
        } else {
            throw new NoValidLoanException("No valid loan period found!");
        }

        if(calculateCreditScore(creditModifier, loanAmount, loanPeriod) < 0.1){
            throw new NoValidLoanException("Credit score too low, Loan is not available for your bracket!");
        }

        return new Decision(outputLoanAmount, loanPeriod, null);
    }

    /**
     * Calculates the largest valid loan for the current credit modifier and loan period.
     *
     * @return Largest valid loan amount
     */
    private int highestValidLoanAmount(int creditModifier, int loanPeriod) {
        return creditModifier * loanPeriod;
    }

    /**
     * Calculates the credit modifier of the customer according to the last four digits of their ID code.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));

        if (segment < 2500) {
            return 0;
        } else if (segment < 5000) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (segment < 7500) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        }

        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @param country Requested country
     * @param age Requested age
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws NoValidLoanException If the requested age or country do not meet the requirements
     */
    private void verifyInputs(String personalCode, Integer loanAmount, int loanPeriod, String country, int age)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException, NoValidLoanException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount)
                || !(loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod)
                || !(loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }
        if(DecisionEngineConstants.MINIMUM_AGE > age || getMaximumAgeLimit(country) < age) {
            throw new NoValidLoanException(age < DecisionEngineConstants.MINIMUM_AGE ?
                    "Age does not meet minimum age requirements" :
                    "Age exceeds maximum age requirements!"
            );
        }
    }
    /**
     * Calculates Credit Score based on creditModifier, loanAmount and loanPeriod
     *
     * @return creditScore
     * **/
    private Double calculateCreditScore(Integer creditModifier,Integer loanAmount, Integer loanPeriod) {
        return ((((double)creditModifier / loanAmount) * loanPeriod) / 10.0);
    }

    private int getMaximumAgeLimit(String country) throws NoValidLoanException {
         return Optional.ofNullable(AGE_LIMITS.get(country.toLowerCase()))
                 .orElseThrow(() -> new NoValidLoanException("Loans are not available for your Country"));

    }
}
