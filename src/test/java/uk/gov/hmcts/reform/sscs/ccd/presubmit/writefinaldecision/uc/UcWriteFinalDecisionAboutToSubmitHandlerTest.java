package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionAboutToSubmitHandlerTestBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.UcDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.UcDecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class UcWriteFinalDecisionAboutToSubmitHandlerTest extends WriteFinalDecisionAboutToSubmitHandlerTestBase<UcDecisionNoticeQuestionService> {

    public UcWriteFinalDecisionAboutToSubmitHandlerTest() throws IOException {
        super(new UcDecisionNoticeQuestionService());
    }

    @NamedParameters("schedule3ActivityAndSchedule9Paragraph4Combinations")
    @SuppressWarnings("unused")
    private Object[] schedule3ActivityAndSchedule9Paragraph4Combinations() {
        return new Object[]{
            new Boolean[]{null, null},
            new Boolean[]{false, null},
            new Boolean[]{true, null},
            new Boolean[]{null, false},
            new Boolean[]{false, false},
            new Boolean[]{true, false},
            new Boolean[]{null, true},
            new Boolean[]{false, true},
            new Boolean[]{true, true},
        };
    }

    @NamedParameters("schedule3ActivityCombinations")
    @SuppressWarnings("unused")
    private Object[] schedule3ActivityCombinations() {
        return new Object[]{
            new Boolean[]{null},
            new Boolean[]{false},
            new Boolean[]{true},
        };
    }

    @Test
    @Parameters(named = "schedule3ActivityAndSchedule9Paragraph4Combinations")
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreTooHigh_thenOnlyDisplayAnErrorIfSchedule7ActivitiesNotPopulated(Boolean schedule7Activities, Boolean schedule9Regulation4) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);

        if (schedule7Activities != null) {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(schedule7Activities.booleanValue() ? "Yes" : "No");
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(schedule7Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        } else {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
        }
        if (schedule9Regulation4 != null) {
            sscsCaseData.setDoesSchedule9Paragraph4Apply(schedule9Regulation4.booleanValue() ? YesNo.YES : YesNo.NO);
        }
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - too high for schedule 8 paragraph 4 to apply
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if ((schedule7Activities != null && schedule7Activities.booleanValue())
            || schedule7Activities != null && !schedule7Activities.booleanValue() && schedule9Regulation4 != null) {
            Assert.assertEquals(0, response.getErrors().size());

        } else {
            Assert.assertEquals(1, response.getErrors().size());

            String error = response.getErrors().stream().findFirst().orElse("");
            if (schedule7Activities == null) {
                if (schedule9Regulation4 == null) {
                    assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                } else {
                    if (!schedule9Regulation4.booleanValue()) {
                        assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    }
                }
            } else if (!schedule7Activities.booleanValue()) {
                if (schedule9Regulation4 == null) {
                    assertEquals(
                        "You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have made no selections for the Schedule 7 Activities question. Please review your previous selection.",
                        error);
                } else {
                    if (!schedule9Regulation4.booleanValue()) {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    }
                }
            } else {
                if (schedule9Regulation4 == null) {
                    assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                } else {
                    if (!schedule9Regulation4.booleanValue()) {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    }
                }
            }

        }
    }

    @Test
    @Parameters(named = "schedule3ActivityAndSchedule9Paragraph4Combinations")
    public void givenSchedule8Paragraph4FieldIsPopulatedWithNoAndPointsAreTooHigh_thenOnlyDisplayAnErrorIfSchedule3ActivitiesNotPopulated(Boolean schedule3Activities, Boolean regulation35) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.NO);

        if (schedule3Activities != null) {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(schedule3Activities.booleanValue() ? "Yes" : "No");
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        } else {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
        }
        if (regulation35 != null) {
            sscsCaseData.setDoesSchedule9Paragraph4Apply(regulation35.booleanValue() ? YesNo.YES : YesNo.NO);
        }
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points - too high for schedule 8 paragraph 4 to apply
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if ((schedule3Activities != null && schedule3Activities.booleanValue())
            || schedule3Activities != null && !schedule3Activities.booleanValue() && regulation35 != null) {
            Assert.assertEquals(0, response.getErrors().size());

        } else {
            Assert.assertEquals(1, response.getErrors().size());

            String error = response.getErrors().stream().findFirst().orElse("");
            if (schedule3Activities == null) {
                if (regulation35 == null) {
                    assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                } else {
                    if (!regulation35.booleanValue()) {
                        assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    }
                }
            } else if (!schedule3Activities.booleanValue()) {
                if (regulation35 == null) {
                    assertEquals(
                        "You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have made no selections for the Schedule 7 Activities question. Please review your previous selection.",
                        error);
                } else {
                    if (!regulation35.booleanValue()) {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    }
                }
            } else {
                if (regulation35 == null) {
                    assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                } else {
                    if (!regulation35.booleanValue()) {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    } else {
                        assertEquals("You have awarded 15 points or more, but have submitted an unexpected answer for the Schedule 8 Paragraph 4 question and have a missing answer for the Schedule 7 Activities question. Please review your previous selection.", error);
                    }
                }
            }

        }
    }


    @Test
    @Parameters(named = "schedule3ActivityAndSchedule9Paragraph4Combinations")
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreLowAndRequireItToBePopulated_thenDisplayAnError(Boolean schedule3Activities, Boolean regulation35) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWcaAppeal("Yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        if (schedule3Activities != null) {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        }
        if (regulation35 != null) {
            sscsCaseData.setDoesSchedule9Paragraph4Apply(regulation35.booleanValue() ? YesNo.YES : YesNo.NO);
        }
        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1w");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");

        assertEquals("You have awarded less than 15 points, but have a missing answer for the Schedule 8 Paragraph 4 question. Please review your previous selection.", error);
    }

    @Test
    @Parameters(named = "schedule3ActivityCombinations")
    public void givenSchedule9Paragraph4FieldIsPopulatedWithYesAndSchedule8Paragraph4FieldIsPopulatedWithNoAndPointsAreCorrectForSchedule8Paragraph4ButIncorrectForSchedule9Paragraph4_thenDoNoDisplayAnError(Boolean schedule3Activities) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.NO);
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.YES);
        sscsCaseData.setWcaAppeal(YesNo.YES.getValue());
        if (schedule3Activities != null) {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        }
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());

    }

    @Test
    @Parameters(named = "schedule3ActivityCombinations")
    public void givenSchedule9Paragraph4FieldIsPopulatedWithNoAndSchedule8Paragraph4FieldIsPopulatedWithNoAndPointsAreCorrectForSchedule8Paragraph4ButIncorrectForSchedule9Paragraph4_thenDisplayAnError(Boolean schedule3Activities) {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.NO);
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);
        sscsCaseData.setWcaAppeal(YesNo.YES.getValue());
        if (schedule3Activities != null) {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(schedule3Activities.booleanValue() ? "Yes" : "No");
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(schedule3Activities.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>());
        } else {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
        }

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if (sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply() != null) {
            Assert.assertEquals(1, response.getErrors().size());

            String error = response.getErrors().stream().findFirst().orElse("");
            if (schedule3Activities == null) {
                assertEquals(
                    "You have awarded less than 15 points and specified that Schedule 8 Paragraph 4 does not apply, but have submitted an unexpected answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.",
                    error);
            } else if (!schedule3Activities.booleanValue()) {
                assertEquals(
                    "You have awarded less than 15 points and specified that Schedule 8 Paragraph 4 does not apply, but have submitted an unexpected answer for the Schedule 9 Paragraph 4 question and submitted an unexpected answer for the Schedule 7 Activities question. Please review your previous selection.",
                    error);
            } else {
                assertEquals(
                    "You have awarded less than 15 points and specified that Schedule 8 Paragraph 4 does not apply, but have submitted an unexpected answer for the Schedule 7 Activities question. Please review your previous selection.",
                    error);
            }
        } else {
            Assert.assertEquals(0, response.getErrors().size());

        }
    }

    @Test
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndActivitiesSelectedOnly_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("someQuestion"));

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndActivitiesSelectedAndSchedule9Paragraph4SetToNo_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertNull(sscsCaseData.getSchedule9Paragraph4Selection());
        Assert.assertNotNull(sscsCaseData.getSchedule7Selections());
        Assert.assertFalse(sscsCaseData.getSchedule7Selections().isEmpty());

        Assert.assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndActivitiesSelectedAndSchedule9Paragraph4SetToYes_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.YES);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertNull(sscsCaseData.getSchedule9Paragraph4Selection());
        Assert.assertNotNull(sscsCaseData.getSchedule7Selections());
        Assert.assertFalse(sscsCaseData.getSchedule7Selections().isEmpty());

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToYes_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.YES);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenSchedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4IsNotSet_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have awarded less than 15 points, specified that Schedule 8 Paragraph 4 applies "
                + "and not provided an answer to the Schedule 9 Paragraph 4 question, but have made "
                + "no selections for the Schedule 7 Activities question. Please review your previous selection.", error);
    }

    @Test
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndActivitiesSelectedOnly_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("someQuestion"));

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndActivitiesSelectedAndSchedule9Paragraph4SetToNo_thenDoNotDisplayAnErrorButResetSchedule9Paragraph4OnSubmit() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertNull(sscsCaseData.getSchedule9Paragraph4Selection());
        Assert.assertNotNull(sscsCaseData.getSchedule7Selections());
        Assert.assertFalse(sscsCaseData.getSchedule7Selections().isEmpty());

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndActivitiesSelectedAndSchedule9Paragraph4SetToYes_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("someQuestion"));
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertNull(sscsCaseData.getSchedule9Paragraph4Selection());
        Assert.assertNotNull(sscsCaseData.getSchedule7Selections());
        Assert.assertFalse(sscsCaseData.getSchedule7Selections().isEmpty());

        Assert.assertEquals(0,  response.getErrors().size());
    }

    @Test
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }




    @Test
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToYes_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.YES);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenSchedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4IsNotSet_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have made no selections for the Schedule 7 Activities question. Please review your previous selection.", error);
    }

    @Override
    protected DecisionNoticeOutcomeService createOutcomeService(UcDecisionNoticeQuestionService decisionNoticeQuestionService) {
        return new UcDecisionNoticeOutcomeService(decisionNoticeQuestionService);
    }

    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.NO);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(
            Arrays.asList("mobilisingUnaided"));
        sscsCaseData.setWcaAppeal(descriptorFlowValue);

        // < 15 points - correct for these fields
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1b");
    }

    @Override
    public void givenDraftFinalDecisionAlreadyExistsOnCase_thenOverwriteExistingDraft() {
        SscsDocument doc = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("oldDraft.doc").documentType(DRAFT_DECISION_NOTICE.getValue()).build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(doc);
        callback.getCaseDetails().getCaseData().setSscsDocument(docs);
        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        // Why do we not need to set valid scenario ?

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getSscsDocument().size());
        assertEquals((String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))), response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    // Refused scenario 1
    @Test
    public void givenNonSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsPopulatedWithNoAndPointsAreCorrectForSchedule8Paragraph4AndNoOtherFieldsPopulated_WhenRefused_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    // Refused scenario 1 with error due to explicitly allowed
    @Test
    public void givenNonSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsPopulatedWithNoAndPointsAreCorrectForSchedule8Paragraph4AndNoOtherFieldsPopulated_WhenIncorrectlyAllowed_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have awarded less than 15 points, specified that the appeal is allowed and specified that Support Group Only Appeal does not apply, but have answered No for the Schedule 8 Paragraph 4 question. Please review your previous selection.", error);
    }

    // Refused scenario 1 with error due to support group being set
    @Test
    public void givenNonSupportGroupScenario_Schedule8Paragraph4FieldIsPopulatedWithNoAndPointsAreCorrectForSchedule8Paragraph4AndNoOtherFieldsPopulated_WhenIncorrectlySupportGroup_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have awarded less than 15 points, specified that the appeal is refused and specified that Support Group Only Appeal applies, but have answered No for the Schedule 8 Paragraph 4 question, have a missing answer for the Schedule 7 Activities question and a missing answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.", error);
    }

    // Refused scenario 2
    @Test
    public void givenSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_WhenRefused_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    // Refused scenario 2 - with error due to explicitly allowed.
    @Test
    public void givenSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_WhenIncorrectlyAllowed_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have specified that the appeal is allowed, specified that Support Group Only Appeal applies and made no selections for the Schedule 7 Activities question, but have answered No for the Schedule 9 Paragraph 4 question. Please review your previous selection.", error);

    }

    // Refused scenario 2 - with error due to support group not being set.
    @Test
    public void givenSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsPopulatedWithYesAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_WhenIncorrectlyNotSupportGroup_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.setDoesSchedule8Paragraph4Apply(YesNo.YES);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 0 points awarded for this question - low, which means schedule 8 paragraph 4 must be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have answered Yes for the Schedule 8 Paragraph 4 question, submitted an unexpected answer for the Schedule 7 Activities question and submitted an unexpected answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.", error);

    }


    // Refused Scenario 3
    @Test
    public void givenSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_WhenRefused_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 does not need to apply.
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    // Refused Scenario 3 - with error due to explictly allowed
    @Test
    public void givenSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_WhenIncorrectlyAllowed_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 does not need to apply.
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have specified that the appeal is allowed, specified that Support Group Only Appeal applies and made no selections for the Schedule 7 Activities question, but have answered No for the Schedule 9 Paragraph 4 question. Please review your previous selection.", error);

    }

    // Refused Scenario 3 - with error due to non support group answer
    @Test
    public void givenSupportGroupRefusedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoActivitiesSelectedAndSchedule9Paragraph4SetToNo_WhenIncorrectlyNotSupportGroup_thenDoDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(new ArrayList<>());
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 does not need to apply.
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Schedule 8 Paragraph 4 question, submitted an unexpected answer for the Schedule 7 Activities question and submitted an unexpected answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.", error);

    }

    // Allowed scenario 1
    @Test
    public void givenNonSupportGroupAllowedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoSchedule3ActivitesAndSchedule9Paragraph4False_WhenAllowed_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    // Allowed scenario 1 - with error due to incorrect setting of refused
    @Test
    public void givenNonSupportGroupAllowedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoSchedule3ActivitesAndSchedule9Paragraph4False_WhenRefused_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.setDoesSchedule9Paragraph4Apply(YesNo.NO);

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Schedule 8 Paragraph 4 question, submitted an unexpected answer for the Schedule 7 Activities question and submitted an unexpected answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.", error);
    }

    // Allowed scenario 1 - with error due to missing regulation 35
    @Test
    public void givenNonSupportGroupAllowedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoSchedule3ActivitesAndSchedule9Paragraph4NotSpecified_WhenAllowed_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have made no selections for the Schedule 7 Activities question. Please review your previous selection.", error);
    }

    // Allowed scenario 2
    @Test
    public void givenNonSupportGroupAllowedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndSchedule3ActivitesAndSchedule9Paragraph4NotSet_WhenAllowed_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("schedule3MobilisingUnaided"));

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(0, response.getErrors().size());
    }

    // Allowed scenario 2 - with error due to incorrectly refused
    @Test
    public void givenNonSupportGroupAllowedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndSchedule3ActivitesAndSchedule9Paragraph4NotSet_WhenRefused_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList("schedule3MobilisingUnaided"));

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Schedule 8 Paragraph 4 question and submitted an unexpected answer for the Schedule 7 Activities question. Please review your previous selection.", error);
    }

    // Allowed scenario 2 - with error due to no schedule 3 answers
    @Test
    public void givenNonSupportGroupAllowedScenario_Schedule8Paragraph4FieldIsNotPopulatedAndPointsAreCorrectForSchedule8Paragraph4AndNoSchedule3ActivitesAndSchedule9Paragraph4NotSet_WhenAllowed_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList(""));

        sscsCaseData.setWcaAppeal("Yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));

        // 15 points awarded for this question - high, which means schedule 8 paragraph 4 is not applicable
        // and does not need to be populated
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().iterator().next();

        Assert.assertEquals("You have awarded 15 points or more and not provided an answer to the Schedule 9 Paragraph 4 question, but have made no selections for the Schedule 7 Activities question. Please review your previous selection.", error);
    }
}
