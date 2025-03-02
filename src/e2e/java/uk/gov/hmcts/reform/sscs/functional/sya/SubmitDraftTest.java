package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.JsonAssert.whenIgnoringPaths;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.sscs.functional.sya.SubmitAppealTest.getCcdIdFromLocationHeader;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.*;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserDetailsTransformer;
import uk.gov.hmcts.reform.sscs.util.SyaServiceHelper;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
@Slf4j
public class SubmitDraftTest {

    private static final String CLIENT_ID = "sscs";
    private static final String BASIC_AUTHORIZATION = "Basic ";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String RESPONSE_TYPE = "code";
    private static final String LOCATION_HEADER_NAME = "Location";

    @Value("${test-url}")
    private String testUrl;

    @Autowired
    private IdamClient idamApiClient;

    @Autowired
    private CitizenCcdService citizenCcdService;

    @Value("${idam.client.secret}")
    private String idamOauth2ClientSecret;

    @Value("${idam.oauth2.redirectUrl}")
    private String idamOauth2RedirectUrl;

    @Value("${idam.oauth2.citizen.email}")
    private String username;

    @Value("${idam.oauth2.citizen.password}")
    private String password;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private IdamService idamService;

    private String citizenToken;

    private IdamTokens citizenIdamTokens;

    private IdamTokens userIdamTokens;

    private SyaCaseWrapper draftAppeal;

    @Autowired
    private SubmitHelper submitHelper;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
        citizenToken = getIdamOauth2Token(username, password);
        UserDetails userDetails = getUserDetails(citizenToken);
        citizenIdamTokens = IdamTokens.builder()
            .idamOauth2Token(citizenToken)
            .serviceAuthorization(idamService.generateServiceAuthorization())
            .userId(userDetails.getId())
            .roles(userDetails.getRoles())
            .email(userDetails.getEmail())
            .build();

        userIdamTokens = idamService.getIdamTokens();

        draftAppeal = buildTestDraftAppeal();
    }

    @After
    public void tearDown() throws InterruptedException {
        List<SscsCaseData> savedDrafts = findCase(citizenIdamTokens);

        if (savedDrafts.size() > 0) {
            savedDrafts.stream().forEach(d -> archiveDraft(d));
        }
    }

    private UserDetails getUserDetails(String userToken) {
        return new UserDetailsTransformer(idamApiClient.getUserInfo(userToken)).asLocalUserDetails();
    }

    private SyaCaseWrapper buildTestDraftAppeal() {
        SyaCaseWrapper draftAppeal = new SyaCaseWrapper();
        draftAppeal.setCaseType("draft");
        draftAppeal.setBenefitType(new SyaBenefitType("Personal Independence Payment", "PIP"));
        return draftAppeal;
    }

    @Test
    public void givenDraftAppealIsSubmitted_shouldSetDwpRegionalCentre() throws InterruptedException {
        String expectedDwpRegionalCentre = "Newcastle";

        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header(AUTHORIZATION, citizenToken))
            .body(getAllDetailsDwpRegionalCentre("PIP", "DWP PIP (1)"))
            .put("/drafts");

        SscsCaseData draft = findCase(citizenIdamTokens).get(0);
        assertEquals(expectedDwpRegionalCentre, draft.getDwpRegionalCentre());
    }

    @Test
    public void givenValidDraftAppealIsSubmittedFromSaveAndReturn_thenCreateValidAppeal() throws InterruptedException {
        assertDraftCaseToSscsCaseResults("validAppeal", ALL_DETAILS_FROM_DRAFT_CCD.getSerializedMessage());
    }

    @Test
    public void givenIncompleteDraftAppealIsSubmittedFromSaveAndReturn_thenCreateIncompleteAppeal() throws InterruptedException {
        assertDraftCaseToSscsCaseResults("incompleteApplication", ALL_DETAILS_FROM_DRAFT_NO_MRN_DATE_CCD.getSerializedMessage());
    }

    @Test
    public void givenNonCompliantDraftAppealIsSubmittedFromSaveAndReturn_thenCreateNonCompliantAppeal() throws InterruptedException {
        assertDraftCaseToSscsCaseResults("interlocutoryReviewState", ALL_DETAILS_FROM_DRAFT_WITH_INTERLOC_CCD.getSerializedMessage());
    }

    private void assertDraftCaseToSscsCaseResults(String expectedState, String expectedResponse) throws InterruptedException {
        LocalDate now = LocalDate.now();
        LocalDate interlocutoryReviewDate = now.minusMonths(13).minusDays(1);
        LocalDate mrnDate = expectedState.equals("interlocutoryReviewState") ? interlocutoryReviewDate :
                expectedState.equals("incompleteApplication") ? null : now;
        String nino = submitHelper.getRandomNino();

        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .header(new Header(AUTHORIZATION, citizenToken))
                .body(setDraftCaseJson(mrnDate, nino))
                .put("/drafts");

        SscsCaseData draft = findCase(citizenIdamTokens).get(0);

        SyaCaseWrapper wrapper = ALL_DETAILS_FROM_DRAFT.getDeserializeMessage();
        wrapper.setCcdCaseId(draft.getCcdCaseId());
        wrapper.getMrn().setDate(mrnDate);
        wrapper.getAppellant().setNino(nino);

        String body = setDraftCaseJson(mrnDate, nino).replaceAll("CCD_CASE_ID", draft.getCcdCaseId());

        Response response = RestAssured.given()
                .body(body)
                .header("Content-Type", "application/json")
                .post("/appeals");

        response.then().statusCode(HttpStatus.SC_CREATED);

        final Long id = getCcdIdFromLocationHeader(response.getHeader("Location"));

        SscsCaseDetails sscsCaseDetails = submitHelper.findCaseInCcd(id, userIdamTokens);

        log.info(String.format("SYA created with CCD ID %s", id));

        assertJsonEquals(changeExpectedFields(expectedResponse, nino, mrnDate), sscsCaseDetails.getData(), whenIgnoringPaths("sscsDocument"));

        assertEquals(expectedState, sscsCaseDetails.getState());
    }

    private String changeExpectedFields(String serializedMessage, String nino, LocalDate mrnDate) {
        serializedMessage = serializedMessage.replace("ZRPVJDDBS", nino);
        serializedMessage = serializedMessage.replace("2021-04-13", LocalDate.now().toString());

        if (mrnDate != null) {
            serializedMessage = serializedMessage.replace("2018-02-01", mrnDate.toString());
        }


        return serializedMessage;
    }

    @Test
    public void givenAnUserSaveADraftMultipleTimes_shouldOnlyUpdateTheSameDraftForTheUser() {
        Response response = saveDraft(draftAppeal);
        response.then()
            .statusCode(anyOf(is(HttpStatus.SC_OK), is(HttpStatus.SC_CREATED)))
            .assertThat().header(LOCATION_HEADER_NAME, not(isEmptyOrNullString())).log().all(true);
        String responseHeader = response.getHeader(LOCATION_HEADER_NAME);

        Response response2 = saveDraft(draftAppeal);
        response2.then()
            .statusCode(HttpStatus.SC_OK)
            .assertThat().header(LOCATION_HEADER_NAME, not(isEmptyOrNullString())).log().all(true);
        String response2Header = response.getHeader(LOCATION_HEADER_NAME);

        assertEquals("the draft updated is not the same", responseHeader, response2Header);
    }

    @Test
    public void givenADraftExistsAndTheGetIsCalled_shouldReturn200AndTheDraft() {
        saveDraft(draftAppeal);
        RestAssured.given()
            .header(new Header(AUTHORIZATION, citizenToken))
            .get("/drafts")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .assertThat().body("BenefitType.benefitType", equalTo("Personal Independence Payment (PIP)"));
    }

    @Test
    public void givenGetDraftsIsCalledWithWrongCredentials_shouldReturn500Unauthorised() {
        RestAssured.given()
            .header(new Header(AUTHORIZATION, "thisTokenIsIncorrect"))
            .get("/drafts")
            .then()
            .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void onceADraftIsArchived_itCannotBeRetrievedByTheCitizenUser() throws InterruptedException {
        saveDraft(draftAppeal);

        List<SscsCaseData> savedDrafts = findCase(citizenIdamTokens);
        assertTrue(CollectionUtils.isNotEmpty(savedDrafts));
        SscsCaseData caseData = savedDrafts.get(0);

        archiveDraft(caseData);

        assertEquals(0, citizenCcdService.findCase(citizenIdamTokens).size());
    }

    private List<SscsCaseData> findCase(IdamTokens idamTokens) throws InterruptedException {
        List<SscsCaseData> savedDrafts = citizenCcdService.findCase(idamTokens);
        if (CollectionUtils.isEmpty(savedDrafts)) {
            Thread.sleep(5000);
            savedDrafts = citizenCcdService.findCase(citizenIdamTokens);
        }
        return savedDrafts;
    }

    private String setDraftCaseJson(LocalDate mrnDate, String nino) {
        String body = ALL_DETAILS_FROM_DRAFT.getSerializedMessage();
        body = submitHelper.setNino(body, nino);
        body = submitHelper.setLatestMrnDate(body, mrnDate);
        return body;
    }

    private String getAllDetailsDwpRegionalCentre(String benefitCode, String dwpIssuingOffice) {
        String body = ALL_DETAILS_DWP_REGIONAL_CENTRE.getSerializedMessage();
        String nino = submitHelper.getRandomNino();
        body = submitHelper.setNino(body, nino);
        body = submitHelper.setLatestMrnDate(body, LocalDate.now());
        body = submitHelper.setDwpIssuingOffice(body, dwpIssuingOffice);
        body = submitHelper.setBenefitCode(body, benefitCode);
        return body;
    }

    private Response saveDraft(SyaCaseWrapper draftAppeal) {
        return RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header(AUTHORIZATION, citizenToken))
            .body(SyaServiceHelper.asJsonString(draftAppeal))
            .put("/drafts");
    }

    private void archiveDraft(SscsCaseData draftAppeal) {
        citizenCcdService.archiveDraft(draftAppeal, citizenIdamTokens, Long.valueOf(draftAppeal.getCcdCaseId()));
    }

    public String getIdamOauth2Token(String username, String password) {
        return idamApiClient.getAccessToken(username, password);
    }
}
