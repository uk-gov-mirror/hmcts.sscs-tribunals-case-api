package uk.gov.hmcts.reform.sscs.model.docassembly;

import static org.junit.Assert.*;

import org.junit.Test;

public class DescriptorTest {

    @Test
    public void testDescriptorBuilder() {
        Descriptor descriptor = Descriptor.builder()
            .activityQuestionValue("questionValue")
            .activityAnswerValue("answerValue")
            .activityAnswerLetter("answerLetter")
            .activityQuestionNumber("1")
            .activityAnswerPoints(10)
            .build();

        assertEquals("questionValue", descriptor.getActivityQuestionValue());
        assertEquals("answerValue", descriptor.getActivityAnswerValue());
        assertEquals("1", descriptor.getActivityQuestionNumber());
        assertEquals(10, descriptor.getActivityAnswerPoints());
        assertEquals("answerLetter", descriptor.getActivityAnswerLetter());
    }

}
