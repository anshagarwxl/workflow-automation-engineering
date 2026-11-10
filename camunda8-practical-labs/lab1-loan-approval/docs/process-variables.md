# Lab 1 – Process-variable dictionary

Process `loan-approval-process`. "Set by" names the BPMN element that writes the variable. All values are synthetic.

## Input (start event / REST start)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `applicationId` | String | `LA-1001` | business key of the application (pattern `LA-0000`) |
| `applicantId` | String | `SYN-1001` | synthetic applicant id used for the bureau lookup (pattern `SYN-0000`, real ids rejected) |
| `applicantName` | String | `Asha Test` | display name (fictitious) |
| `applicantAge` | Number | `34` | must be 21–65 |
| `email` | String | `asha.test@example.com` | notification address (example.com) |
| `employmentType` | String | `SALARIED` | `SALARIED` / `SELF_EMPLOYED` / `UNEMPLOYED` (DMN input) |
| `employmentYears` | Number | `6` | years in current employment |
| `monthlyIncome` | Number | `120000` | monthly income in INR |
| `existingMonthlyEmi` | Number | `5000` | existing monthly loan repayments in INR |
| `loanAmount` | Number | `500000` | requested principal (10,000 – 50,00,000) |
| `loanTermMonths` | Number | `36` | term (6–84 months) |
| `loanPurpose` | String | `HOME_RENOVATION` | informational |
| `scenarioId` | String | `01-approved` | added by the scenario runner to link instance ↔ test case |
| `mockApiBaseUrl` | String (optional) | `http://host.docker.internal:8090` | overrides the REST Connector base URL |

## Set by *Validate applicant information* (worker `loan.validate-application`)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `validationStatus` | String | `VALID` / `INVALID` | routes the *Application valid?* gateway |
| `validationErrors` | List&lt;String&gt; | `["applicant age must be between 21 and 65"]` | reasons, used in the rejection notice |
| `proposedEmi` | Number | `16607.15` | EMI of the new loan at the assumed 12 % p.a. |
| `dtiRatio` | Number | `0.180` | (existing EMI + proposed EMI) / monthly income – DMN input |
| `loanToIncomeRatio` | Number | `0.347` | loan amount / annual income – DMN input |
| `assumedInterestRate` | Number | `12.0` | rate used for affordability |

## Set by *Request credit assessment* (REST Connector result expression / error boundary)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `creditCheckResponse` | Context | `{status: 200, headers: {…}, body: {creditScore: 780, …}}` | full HTTP response (connector *Result variable*), as in the course tutorial |
| `creditCheckStatus` | String | `COMPLETED` / `FAILED` | `FAILED` is set by the error boundary event output mapping |
| `creditCheckError` | String | `Read timed out` | error message when the bureau call failed |
| `creditAssessmentId` | String | `CBA-5001` | bureau reference |
| `creditScore` | Number | `780` | DMN input |
| `riskGrade` | String | `A` | bureau grade A–E |
| `bureauStatus` | String | `FOUND` / `NO_HISTORY` | whether the bureau knew the applicant |
| `activeLoans` | Number | `1` | informational |
| `defaultsLast24Months` | Number | `0` | DMN input |

## Set by *Evaluate loan eligibility* (DMN `loan-eligibility`, result variable `eligibility`)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `eligibility` | Context | `{decision: "APPROVED", reason: "...", riskBand: "LOW"}` | `eligibility.decision` routes the *Eligibility decision?* gateway; `riskBand` sets the offered rate |

## Set by *Manual underwriting review* (Tasklist form `loan-manual-review`)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `reviewDecision` | String | `APPROVE` / `REJECT` | routes the *Underwriter decision?* gateway |
| `approvedAmount` | Number (optional) | `700000` | sanctioned amount if lower than requested |
| `reviewerName` | String | `UW-07` | who decided |
| `reviewerComments` | String | `Stable job, DTI acceptable` | justification, copied into the rejection notice |

## Set by *Issue loan offer* / *Send rejection notice* (workers `loan.issue-offer`, `loan.send-rejection`)

| Variable | Type | Example | Purpose |
|---|---|---|---|
| `finalOutcome` | String | `APPROVED` / `REJECTED` | final business result (used in the test-case table) |
| `decidedBy` | String | `AUTOMATED_DMN` / `UNDERWRITER` / `VALIDATION` | who made the decision |
| `offerId` | String | `OFR-1001` | offer reference |
| `sanctionedAmount` | Number | `500000` | approved principal |
| `offeredInterestRate` | Number | `10.50` | by risk band: LOW 10.5 %, MEDIUM 12.5 %, HIGH 14.5 % |
| `offeredEmi` | Number | `16251.22` | EMI of the offer |
| `offerValidUntil` | String (date) | `2026-11-17` | today + 15 days |
| `rejectionReasons` | List&lt;String&gt; | `["Eligibility rules: Credit score below 550"]` | reasons sent to the applicant |
| `noticeId` | String | `REJ-1002` | rejection notice reference |
| `notificationStatus` | String | `SIMULATED_EMAIL_SENT` | notification is simulated (logged) |

## Gateway conditions

| Gateway | Flow | Condition |
|---|---|---|
| Application valid? | Valid | `=validationStatus = "VALID"` (default: Invalid) |
| Eligibility decision? | Approved | `=eligibility.decision = "APPROVED"` |
| | Rejected | `=eligibility.decision = "REJECTED"` |
| | Manual review | default flow (`MANUAL_REVIEW`) |
| Underwriter decision? | Approve | `=reviewDecision = "APPROVE"` (default: Reject) |
| Error boundary | Credit API error / timeout | BPMN error code `CREDIT_API_ERROR` |
