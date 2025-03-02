package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen;

import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerTestBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;

@RunWith(JUnitParamsRunner.class)
public class GenWriteFinalDecisionMidEventValidationHandlerTest extends WriteFinalDecisionMidEventValidationHandlerTestBase {

    @Override
    protected WriteFinalDecisionMidEventValidationHandlerBase createValidationHandler(Validator validator, DecisionNoticeService decisionNoticeService) {
        return new GenWriteFinalDecisionMidEventValidationHandler(validator, decisionNoticeService);
    }

    @Override
    protected String getBenefitType() {
        return "GEN";
    }

    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);
    }

    @Override
    protected void setNoAwardsScenario(SscsCaseData sscsCaseData) {
        // N/A for GEN
    }

    @Override
    protected void setEmptyActivitiesListScenario(SscsCaseData caseData) {
        // N/A for GEN
    }

    @Override
    protected void setNullActivitiesListScenario(SscsCaseData caseData) {
        // N/A for GEN
    }

    @Override
    protected void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelected() {
        // N/A for GEN
    }

    @Override
    protected void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsSetEndDate() {
        // N/A for GEN
    }

    @Override
    protected void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsIndefinite() {
        // N/A for GEN
    }


    @Override
    protected void shouldExhibitBenefitSpecificBehaviourWhenAnAnAwardIsGivenAndNoActivitiesSelected(AwardType dailyLiving, AwardType mobility) {
        // N/A for GEN
    }

}

