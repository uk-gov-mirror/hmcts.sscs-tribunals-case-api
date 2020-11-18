package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcScenario4Test {

    @Test
    public void testScenario4() {
        List<Descriptor> schedule7Descriptors =
                Arrays.asList(Descriptor.builder()
                        .activityQuestionValue("2. Transferring from one seated position to another.").build());

        WriteFinalDecisionTemplateBody body =
                WriteFinalDecisionTemplateBody.builder()
                        .isAllowed(true)
                        .isSetAside(true)
                        .dateOfDecision("2020-09-20")
                        .ucNumberOfPoints(15)
                        .appellantName("Felix Sydney")
                        .supportGroupOnly(true)
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else")
                        .ucSchedule7Descriptors(schedule7Descriptors).build();

        UcTemplateContent content = UcScenario.SCENARIO_4.getContent(body);

        String expectedContent = "The appeal is allowed.\n"
            + "\n"
            + "The decision made by the Secretary of State on 20/09/2020 is set aside.\n"
            + "\n"
            + "Felix Sydney has limited capability for work-related activity.\n"
            + "\n"
            + "The Secretary of State has accepted that Felix Sydney has limited capability for work. This was not an issue.\n"
            + "\n"
            + "The following activity and descriptor from Schedule 3 applied:\n"
            + "\n"
            + "2. Transferring from one seated position to another.\n"
            + "\n"
            + "\n"
            + "My first reasons\n"
            + "\n"
            + "My second reasons\n"
            + "\n"
            + "Something else\n"
            + "\n";

        Assert.assertEquals(9, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }
}
