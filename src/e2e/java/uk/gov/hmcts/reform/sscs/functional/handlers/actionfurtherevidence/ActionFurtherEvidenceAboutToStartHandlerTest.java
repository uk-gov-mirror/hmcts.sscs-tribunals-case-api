package uk.gov.hmcts.reform.sscs.functional.handlers.actionfurtherevidence;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class ActionFurtherEvidenceAboutToStartHandlerTest extends BaseHandler {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Test
    @Parameters({
        ", sendToInterlocReviewByJudge, sendToInterlocReviewByTcw",
        "anyValue, informationReceivedForInterlocJudge, informationReceivedForInterlocTcw"
    })
    public void givenAboutToStartCallback_shouldSetItemsInFurtherActionDropdownMenu(
        String interlocReviewState, String expectedItem1, String expectedItem2) throws Exception {

        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest(
            "handlers/actionfurtherevidence/actionFurtherEvidenceAboutToStartCallback.json");
        jsonCallbackForTest = jsonCallbackForTest.replace("INTERLOC_REVIEW_STATE", interlocReviewState);

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(jsonCallbackForTest)
            .post("/ccdAboutToStart")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true)
            .assertThat().body("data.furtherEvidenceAction.value.code", equalTo("issueFurtherEvidence"))
            .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "issueFurtherEvidence")))
            .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", "otherDocumentManual")))
            .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", expectedItem1)))
            .assertThat().body("data.furtherEvidenceAction.list_items", hasItem(hasEntry("code", expectedItem2)))
            .assertThat().body("data.furtherEvidenceAction.list_items", hasSize(4));

    }
}
