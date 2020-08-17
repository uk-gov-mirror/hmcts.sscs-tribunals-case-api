package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Validation;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseMidEventValidationHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private DwpUploadResponseMidEventValidationHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamClient idamClient;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        handler = new DwpUploadResponseMidEventValidationHandler(Validation.buildDefaultValidatorFactory().getValidator());

        when(callback.getEvent()).thenReturn(EventType.DWP_UPLOAD_RESPONSE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .appeal(Appeal.builder().build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonDwpUploadResponseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenElementsDisputedGeneralPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedGeneral(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("General element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedSanctionsPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedSanctions(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Standard allowance - sanctions element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedOverpaymentPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedOverpayment(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Standard allowance - overpayment element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedHousingPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedHousing(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Housing element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedChildcarePopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedChildCare(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Childcare element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedCarePopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedCare(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Carer element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedChildElementPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedChildElement(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Child element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedChildDisabledPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedChildDisabled(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Disabled child addition element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedSelectedWithNoDuplicates_thenDoNotDisplayError() {

        sscsCaseData.setElementsDisputedChildDisabled(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenElementsDisputedGeneralPopulatedWithDuplicateIssueCodeButDifferentOutcomes_thenDoShowError() {
        sscsCaseData.setElementsDisputedGeneral(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").outcome("Test").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").outcome("Different").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("General element contains duplicate issue codes", error);
    }

    @Test
    public void givenNoJointPartyDob_thenDoNotDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenDobWithYearInPast_thenDoNotDisplayError() {

        String dateToTest = LocalDate.now().minusYears(1).toString();

        sscsCaseData.setJointPartyDob(dateToTest);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenDobWithYearOfThisYear_thenDoShowError() {

        String dateToTest = LocalDate.now().toString();

        sscsCaseData.setJointPartyDob(dateToTest);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You’ve entered an invalid date of birth", error);
    }

    @Test
    public void givenDobWithYearOfFutureYear_thenDoShowError() {

        String dateToTest = LocalDate.now().plusYears(1).toString();

        sscsCaseData.setJointPartyDob(dateToTest);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You’ve entered an invalid date of birth", error);
    }

    @Test
    public void givenNoJointPartyNino_thenDoNotDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenValidJointPartyNinoNoSpaces_thenDoNotDisplayError() {

        sscsCaseData.setJointPartyNino("BB000000B");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenValidJointPartyNinoWithSpaces_thenDoNotDisplayError() {

        sscsCaseData.setJointPartyNino("BB 00 00 00 B");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenInvalidJointPartyNino_thenDoDisplayError() {

        sscsCaseData.setJointPartyNino("blah");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Invalid National Insurance number", error);
    }

    @Test
    public void givenInvalidJointPartyValidAddress_thenDoNoDisplayError() {

        Address jointPartyAddress = Address.builder()
            .line1("some text")
            .line2("some text")
            .town("some text")
            .county("some text")
            .postcode("w1p 4df").build();

        sscsCaseData.setJointPartyAddress(jointPartyAddress);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenInvalidJointPartyAddressInvalidCharacterInLine1_thenDoDisplayError() {

        Address jointPartyAddress = Address.builder().line1("some $ text").build();

        sscsCaseData.setJointPartyAddress(jointPartyAddress);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Line 1 must not contain special characters", error);
    }

    @Test
    public void givenInvalidJointPartyAddressInvalidCharacterInLine2_thenDoDisplayError() {

        Address jointPartyAddress = Address.builder().line2("some $ text").build();

        sscsCaseData.setJointPartyAddress(jointPartyAddress);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Line 2 must not contain special characters", error);
    }

    @Test
    public void givenInvalidJointPartyAddressInvalidCharacterInTown_thenDoDisplayError() {

        Address jointPartyAddress = Address.builder().town("some $ text").build();

        sscsCaseData.setJointPartyAddress(jointPartyAddress);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Town must not contain special characters", error);
    }

    @Test
    public void givenInvalidJointPartyAddressInvalidCharacterInCounty_thenDoDisplayError() {

        Address jointPartyAddress = Address.builder().county("some $ text").build();

        sscsCaseData.setJointPartyAddress(jointPartyAddress);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("County must not contain special characters", error);
    }

    @Test
    public void givenInvalidJointPartyAddressInvalidPostcode_thenDoDisplayError() {

        Address jointPartyAddress = Address.builder().postcode("invalid postcode").build();

        sscsCaseData.setJointPartyAddress(jointPartyAddress);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Please enter a valid postcode", error);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheEvent() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }
}
