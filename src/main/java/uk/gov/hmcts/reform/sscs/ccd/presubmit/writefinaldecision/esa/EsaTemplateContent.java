package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios.EsaScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class EsaTemplateContent extends WriteFinalDecisionTemplateContent {

    public String getAllowedOrRefusedSentence(boolean allowed) {
        return "The appeal is " + (allowed ? "allowed" : "refused") + ".";
    }

    public String getConfirmedOrSetAsideSentence(boolean setAside, String decisionDate) {
        return "The decision made by the Secretary of State on " + decisionDate + " is "
            + (!setAside ? "confirmed." : "set aside.");
    }

    public String getDoesNotHaveLimitedCapabilityForWorkSentence(String appellantName) {
        return appellantName + " does not have limited capability for work and cannot be treated as having limited capability for work.";
    }

    public String getDoesNotHaveLimitedCapabilityForWorkNoSchedule3Sentence(String appellantName) {
        return appellantName + " does not have limited capability for work-related activity because no descriptor from Schedule 3 applied.  Regulation 35 did not apply.";
    }

    public String getDoesHaveLimitedCapabilityForWorkSentence(String appellantName) {
        return appellantName + " is to be treated as having limited capability for work.";
    }

    public String getSchedule2InsufficientPointsSentence(Integer points, Boolean regulation29Applies) {
        return "In applying the work capability assessment " + points + (points == 1 ? " point was" : " points were")
            + " scored from the activities and descriptors in Schedule "
            + "2 of the ESA Regulations 2008" + (regulation29Applies != null && regulation29Applies.booleanValue() ? "made up as follows:"
            : ". This is insufficient to meet the "
            + "threshold for the test. Regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 did not apply.");
    }

    public String getInsufficientPointsSentenceRegulation29Applied(Integer points, Boolean regulation29Applies) {
        return "This is because insufficient points were scored to meet the threshold for the work capability assessment, " +
                "but regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 applied.";
    }

    public String getRegulation29DiseaseOrDisablementSentence(String appellantName) {
        //FIXME: Replace disease or disablement as part of future ticket
        return "The tribunal applied regulation 29 because it found that " + appellantName + " suffers from "
        + "[insert disease or disablement] and, by reasons of such disease or disablement, there would "
        + "be a substantial risk to the mental or physical health of any person if they were found not to have limited capability for work.";
    }

    public String getHearingTypeSentence(String appellantName, String bundlePage) {
        // Placeholder for SSCS-8033 (Chris D)
        return "This has been an oral (face to face) hearing. "
        + appellantName + "attended the hearing today and the tribunal considered the appeal bundle to page " + bundlePage
        + ". A Presenting Officer attended on behalf of the Respondent.";
    }

    public String getRecommendationSentence() {
        // Placeholder for SSCS-8308 (Ryan)
        return "";
    }



    public abstract EsaScenario getScenario();
}
