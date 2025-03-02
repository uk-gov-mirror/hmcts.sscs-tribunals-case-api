package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaMrn;
import uk.gov.hmcts.reform.sscs.exception.ApplicationErrorException;
import uk.gov.hmcts.reform.sscs.exception.DuplicateCaseException;
import uk.gov.hmcts.reform.sscs.helper.EmailHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAIntoBService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.ResourceManager;

@RunWith(JUnitParamsRunner.class)
public class SubmitAppealServiceTest {
    private static final String TEMPLATE_PATH = "/templates/appellant_appeal_template.html";
    private static final String WELSH_TEMPLATE_PATH = "/templates/appellant_appeal_welsh_template.html";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    @Mock
    private CcdService ccdService;

    @Mock
    private CitizenCcdService citizenCcdService;

    @Mock
    private CcdPdfService ccdPdfService;

    @Mock
    private PDFServiceClient pdfServiceClient;

    @Mock
    private EmailHelper emailHelper;

    @Mock
    private IdamService idamService;

    @Mock
    private ResourceManager resourceManager;

    private SubmitAppealService submitAppealService;

    private final SyaCaseWrapper appealData = getSyaCaseWrapper();

    private final String userToken = "user token";

    @Captor
    private ArgumentCaptor<SscsCaseData> capture;

    @Mock
    private ConvertAIntoBService<SscsCaseData, SessionDraft> convertAIntoBService;

    private static final RegionalProcessingCenterService regionalProcessingCenterService;

    private static AirLookupService airLookupService;

    private SscsPdfService sscsPdfService;

    static {
        airLookupService = new AirLookupService();
        airLookupService.init();
        regionalProcessingCenterService = new RegionalProcessingCenterService(airLookupService);
        regionalProcessingCenterService.init();
    }

    public static final String BIRMINGHAM_RPC = "{\n"
        + "    \"name\" : \"BIRMINGHAM\",\n"
        + "    \"address1\" : \"HM Courts & Tribunals Service\",\n"
        + "    \"address2\" : \"Social Security & Child Support Appeals\",\n"
        + "    \"address3\" : \"Administrative Support Centre\",\n"
        + "    \"address4\" : \"PO Box 14620\",\n"
        + "    \"city\" : \"BIRMINGHAM\",\n"
        + "    \"postcode\" : \"B16 6FR\",\n"
        + "    \"phoneNumber\" : \"0300 123 1142\",\n"
        + "    \"faxNumber\" : \"0126 434 7983\",\n"
        + "    \"email\" : \"Birmingham-SYA-Receipts@justice.gov.uk\"\n"
        + "  }";

    public static final String BRADFORD_RPC = "{\n"
        + "    \"name\" : \"BRADFORD\",\n"
        + "    \"address1\": \"HM Courts & Tribunals Service\",\n"
        + "    \"address2\": \"Social Security & Child Support Appeals\",\n"
        + "    \"address3\": \"Phoenix House\",\n"
        + "    \"address4\": \"Rushton Avenue\",\n"
        + "    \"city\": \"BRADFORD\",\n"
        + "    \"postcode\": \"BD3 7BH\",\n"
        + "    \"phoneNumber\" : \"0300 123 1142\",\n"
        + "    \"faxNumber\" : \"0126 434 7983\",\n"
        + "    \"email\" : \"SSCS_Bradford@justice.gov.uk\"\n"
        + "  }";

    public static final String SUTTON_RPC = "{\n"
        + "    \"name\" : \"SUTTON\",\n"
        + "    \"address1\" : \"HM Courts & Tribunals Service\",\n"
        + "    \"address2\" : \"Social Security & Child Support Appeals\",\n"
        + "    \"address3\" : \"Copthall House\",\n"
        + "    \"address4\" : \"9 The Pavement, Grove Road\",\n"
        + "    \"city\" : \"SUTTON\",\n"
        + "    \"postcode\" : \"SM1 1DA\",\n"
        + "    \"phoneNumber\" : \"0300 123 1142\",\n"
        + "    \"faxNumber\" : \"0870 739 4229\",\n"
        + "    \"email\" : \"Sutton_SYA_Respons@justice.gov.uk\"\n"
        + "  }";

    @Before
    public void setUp() {


        appealData.getMrn().setDate(LocalDate.now().minusMonths(1));

        submitAppealService = new SubmitAppealService(
            ccdService, citizenCcdService, regionalProcessingCenterService,
            idamService, convertAIntoBService, airLookupService);

        given(ccdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
            .willReturn(SscsCaseDetails.builder().id(123L).build());

        given(ccdService.updateCase(any(SscsCaseData.class), any(), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().id(123L).build());

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().roles(Arrays.asList("citizen")).build());
        given(emailHelper.generateUniqueEmailId(any(Appellant.class))).willReturn("Bloggs_33C");

    }

    @Test
    public void givenCaseDoesNotExistInCcd_shouldCreateCaseWithAppealDetailsWithValidAppealCreatedEvent() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("No", capture.getValue().getIsSaveAndReturn());
    }

    @Test
    public void givenDraftCaseDoesExistAndCaseSubmitted_shouldUpdateCaseWithAppealDetailsWithDraftToValidAppealCreatedEvent() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);
        given(ccdService.getByCaseId(eq(123L), any())).willReturn(SscsCaseDetails.builder().build());

        appealData.setCcdCaseId("123");
        appealData.setIsSaveAndReturn("Yes");
        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).updateCase(capture.capture(), eq(123L), eq(DRAFT_TO_VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("Yes", capture.getValue().getIsSaveAndReturn());

    }

    @Test
    public void givenAssociatedCaseAlreadyExistsInCcd_shouldCreateCaseWithAppealDetailsAndAssociatedCase() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any()))
                .willReturn(Collections.singletonList(
                        SscsCaseDetails.builder()
                                .id(12345678L)
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .benefitType(BenefitType.builder().code(appealData.getBenefitType().getCode()).build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build()
        ));

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals(1, capture.getValue().getAssociatedCase().size());
        assertEquals("12345678", capture.getValue().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void givenAssociatedCaseAlreadyExistsInCcd_shouldCreateCaseWithAppealDetailsAndAssociatedCaseWithoutMrn() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any()))
                .willReturn(Collections.singletonList(
                        SscsCaseDetails.builder()
                                .id(12345678L)
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .benefitType(BenefitType.builder().code(appealData.getBenefitType().getCode()).build())
                                                .mrnDetails(MrnDetails.builder().build())
                                                .build()).build()).build()
                ));

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(VALID_APPEAL_CREATED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals(1, capture.getValue().getAssociatedCase().size());
        assertEquals("12345678", capture.getValue().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void givenCaseDoesNotExistInCcdAndMrnDateIsMissing_shouldCreateCaseWithAppealDetailsWithIncompleteApplicationEvent() {
        byte[] expected = {};
        appealData.getMrn().setDate(null);

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(INCOMPLETE_APPLICATION_RECEIVED.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        verify(ccdService, times(0)).updateCase(any(SscsCaseData.class), eq(123L), eq(SEND_TO_DWP.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("No", capture.getValue().getIsSaveAndReturn());
    }

    @Test
    public void givenDraftCaseDoesExistAndMrnDateIsMissingAndCaseSubmitted_shouldUpdateCaseWithAppealDetailsWithDraftToIncompleteApplicationEvent() {
        byte[] expected = {};
        appealData.getMrn().setDate(null);

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        given(ccdService.getByCaseId(eq(123L), any())).willReturn(SscsCaseDetails.builder().build());

        appealData.setCcdCaseId("123");
        appealData.setIsSaveAndReturn("Yes");
        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).updateCase(capture.capture(), eq(123L), eq(DRAFT_TO_INCOMPLETE_APPLICATION.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("Yes", capture.getValue().getIsSaveAndReturn());

    }

    @Test
    public void givenCaseDoesNotExistInCcdAndMrnDateIsGreaterThan13Months_shouldCreateCaseWithAppealDetailsWithNonCompliantReceivedEvent() {
        byte[] expected = {};
        appealData.getMrn().setDate(LocalDate.now().minusMonths(13).minusDays(1));

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).createCase(capture.capture(), eq(NON_COMPLIANT.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("No", capture.getValue().getIsSaveAndReturn());
    }

    @Test
    public void givenDraftCaseDoesExistAndMrnDateIsGreaterThan13MonthsAndCaseSubmitted_shouldUpdateCaseWithAppealDetailsWithDraftToNonCompliantEvent() {
        byte[] expected = {};
        appealData.getMrn().setDate(LocalDate.now().minusMonths(13).minusDays(1));

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any())).willReturn(null);

        given(ccdService.getByCaseId(eq(123L), any())).willReturn(SscsCaseDetails.builder().build());

        appealData.setCcdCaseId("123");
        appealData.setIsSaveAndReturn("Yes");
        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService).updateCase(capture.capture(), eq(123L), eq(DRAFT_TO_NON_COMPLIANT.getCcdType()), any(String.class), any(String.class), any(IdamTokens.class));
        assertEquals("Yes", capture.getValue().getIsSaveAndReturn());
    }

    @Test
    public void shouldCreateDraftCaseWithAppealDetailsWithDraftEventNotForcedCreate() {
        given(citizenCcdService.saveCase(any(SscsCaseData.class), any(IdamTokens.class)))
            .willReturn(SaveCaseResult.builder()
                .caseDetailsId(123L)
                .saveCaseOperation(SaveCaseOperation.CREATE)
                .build());

        Optional<SaveCaseResult> result = submitAppealService.submitDraftAppeal("authorisation", appealData, false);

        verify(citizenCcdService).saveCase(any(SscsCaseData.class), any(IdamTokens.class));
        assertTrue(result.isPresent());

    }

    @Test
    public void shouldUpdateDraftCase() {

        uk.gov.hmcts.reform.ccd.client.model.CaseDetails caseDetails =
                uk.gov.hmcts.reform.ccd.client.model.CaseDetails.builder().id(12L).build();

        given(citizenCcdService.updateCase(any(SscsCaseData.class), any(), any(), any(), any(), any()))
                .willReturn(caseDetails);

        SyaCaseWrapper caseWrapper = getSyaCaseWrapper("json/sya_with_ccdId.json");
        Optional<SaveCaseResult> result = submitAppealService.updateDraftAppeal("authorisation", caseWrapper);

        verify(citizenCcdService).updateCase(any(SscsCaseData.class), any(), any(), any(), any(), any());
        assertTrue(result.isPresent());
    }

    @Test(expected = FeignException.class)
    public void shouldRaiseExceptionOnUpdateDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(404);
        given(citizenCcdService.updateCase(any(SscsCaseData.class), any(), any(), any(), any(), any()))
                .willThrow(feignException);

        Optional<SaveCaseResult> result = submitAppealService.updateDraftAppeal("authorisation", appealData);

        verify(citizenCcdService).archiveDraft(any(SscsCaseData.class), any(IdamTokens.class), any());
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldSuppressExceptionIfIts409OnUpdateDraftCaseWithAppealDetailsWithDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(409);
        given(citizenCcdService.updateCase(any(SscsCaseData.class), any(), any(), any(), any(), any()))
                .willThrow(feignException);

        Optional<SaveCaseResult> result = submitAppealService.updateDraftAppeal("authorisation", appealData);

        verify(citizenCcdService).updateCase(any(SscsCaseData.class), any(), any(), any(), any(), any());
        assertFalse(result.isPresent());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldRaisedExceptionOnUpdateDraftWhenCitizenRoleIsNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        Optional<SaveCaseResult> result = submitAppealService.updateDraftAppeal("authorisation", appealData);

        assertFalse(result.isPresent());
    }

    @Test
    public void shouldArchiveDraftCase() {
        given(citizenCcdService.archiveDraft(any(SscsCaseData.class), any(IdamTokens.class), any()))
                .willReturn(CaseDetails.builder().build());

        Optional<SaveCaseResult> result = submitAppealService.archiveDraftAppeal("authorisation", appealData, 112L);

        verify(citizenCcdService).archiveDraft(any(SscsCaseData.class), any(IdamTokens.class), any());
        assertTrue(result.isPresent());
    }

    @Test(expected = FeignException.class)
    public void shouldRaiseExceptionOnArchiveDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(404);
        given(citizenCcdService.archiveDraft(any(SscsCaseData.class), any(IdamTokens.class), any()))
                .willThrow(feignException);

        Optional<SaveCaseResult> result = submitAppealService.archiveDraftAppeal("authorisation", appealData, 112L);

        verify(citizenCcdService).archiveDraft(any(SscsCaseData.class), any(IdamTokens.class), any());
        assertFalse(result.isPresent());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldRaisedExceptionOnArchiveDraftWhenCitizenRoleIsNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        Optional<SaveCaseResult> result = submitAppealService.archiveDraftAppeal("authorisation", appealData, 121L);

        assertFalse(result.isPresent());
    }

    @Test
    public void shouldCreateDraftCaseWithAppealDetailsWithDraftEventForcedCreate() {
        given(citizenCcdService.createDraft(any(SscsCaseData.class), any(IdamTokens.class)))
                .willReturn(SaveCaseResult.builder()
                        .caseDetailsId(123L)
                        .saveCaseOperation(SaveCaseOperation.CREATE)
                        .build());

        Optional<SaveCaseResult> result = submitAppealService.submitDraftAppeal("authorisation", appealData, true);

        verify(citizenCcdService).createDraft(any(SscsCaseData.class), any(IdamTokens.class));
        assertTrue(result.isPresent());

    }

    @Test(expected = FeignException.class)
    public void shouldRaisedExceptionOnCreateDraftCaseWithAppealDetailsWithDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(404);
        given(citizenCcdService.saveCase(any(SscsCaseData.class), any(IdamTokens.class)))
            .willThrow(feignException);

        Optional<SaveCaseResult> result = submitAppealService.submitDraftAppeal("authorisation", appealData, false);

        verify(citizenCcdService).saveCase(any(SscsCaseData.class), any(IdamTokens.class));
        assertFalse(result.isPresent());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldRaisedExceptionOnCreateDraftWhenCitizenRoleIsNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        Optional<SaveCaseResult> result = submitAppealService.submitDraftAppeal("authorisation", appealData, false);

        assertFalse(result.isPresent());
    }

    @Test
    public void shouldSuppressExceptionIfIts409OnCreateDraftCaseWithAppealDetailsWithDraftEvent() {
        FeignException feignException = mock(FeignException.class);
        given(feignException.status()).willReturn(409);
        given(citizenCcdService.saveCase(any(SscsCaseData.class), any(IdamTokens.class)))
            .willThrow(feignException);

        Optional<SaveCaseResult> result = submitAppealService.submitDraftAppeal("authorisation", appealData, false);

        verify(citizenCcdService).saveCase(any(SscsCaseData.class), any(IdamTokens.class));
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldGetADraftIfItExists() {
        when(citizenCcdService.findCase(any())).thenReturn(Collections.singletonList(SscsCaseData.builder().build()));
        when(convertAIntoBService.convert(any(SscsCaseData.class))).thenReturn(SessionDraft.builder().build());
        Optional<SessionDraft> optionalSessionDraft = submitAppealService.getDraftAppeal("authorisation");
        assertTrue(optionalSessionDraft.isPresent());
    }

    @Test
    public void shouldGetNoDraftIfNoneExists() {
        when(citizenCcdService.findCase(any())).thenReturn(Collections.emptyList());
        Optional<SessionDraft> optionalSessionDraft = submitAppealService.getDraftAppeal("authorisation");
        assertFalse(optionalSessionDraft.isPresent());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldThrowExceptionOnGetDraftWhenCitizenRoleNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        Optional<SessionDraft> optionalSessionDraft = submitAppealService.getDraftAppeal("authorisation");
    }

    @Test
    public void shouldGetAllDraftsIfItExists() {

        SscsCaseData caseData =  SscsCaseData.builder().build();

        when(citizenCcdService.findCase(any())).thenReturn(List.of(caseData, caseData, caseData));
        when(convertAIntoBService.convert(any(SscsCaseData.class))).thenReturn(SessionDraft.builder().build());
        List<SessionDraft> sessionDrafts = submitAppealService.getDraftAppeals("authorisation");
        assertEquals(3, sessionDrafts.size());
    }

    @Test
    public void shouldGetEmptyListIfNoDraftsExists() {
        when(citizenCcdService.findCase(any())).thenReturn(Collections.emptyList());
        List<SessionDraft> sessionDrafts = submitAppealService.getDraftAppeals("authorisation");
        assertEquals(0, sessionDrafts.size());
    }

    @Test(expected = ApplicationErrorException.class)
    public void shouldThrowExceptionOnGetDraftsWhenCitizenRoleNotPresent() {
        given(idamService.getUserDetails(anyString())).willReturn(UserDetails.builder().build()); // no citizen role
        List<SessionDraft> sessionDrafts = submitAppealService.getDraftAppeals("authorisation");
    }

    @Test
    @Parameters(method = "generateDifferentRpcScenarios")
    public void givenAppellantPostCode_shouldSetRegionAndRpcCorrectly(String expectedRpc, String appellantPostCode)
        throws JsonProcessingException {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        appealData.getAppellant().getContactDetails().setPostCode(appellantPostCode);

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);

        RegionalProcessingCenter actualRpc = caseData.getRegionalProcessingCenter();
        RegionalProcessingCenter expectedRpcObject = getRpcObjectForGivenJsonRpc(expectedRpc);
        assertThat(actualRpc, is(expectedRpcObject));
        assertEquals(expectedRpcObject.getName(), caseData.getRegion());
    }

    private RegionalProcessingCenter getRpcObjectForGivenJsonRpc(String jsonRpc) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonRpc, RegionalProcessingCenter.class);
    }

    public Object[] generateDifferentRpcScenarios() {
        return new Object[]{
            new Object[]{BRADFORD_RPC, "TN32 6PL"},
            new Object[]{BRADFORD_RPC, "OX1 1AE"},
            new Object[]{BIRMINGHAM_RPC, "B1 1AA"},
            new Object[]{SUTTON_RPC, "EN1 1AA"},
            new Object[]{SUTTON_RPC, "KT19 0SZ"},
            new Object[]{BIRMINGHAM_RPC, "DE23 2PD"}
        };
    }

    @Test
    public void givenAPipCase_thenSetCreatedInGapsFromFieldToReadyToList() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        SyaBenefitType syaBenefitType = new SyaBenefitType("PIP", "PIP");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("2");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);
        assertEquals(READY_TO_LIST.getId(), caseData.getCreatedInGapsFrom());
    }

    @Test
    public void givenAEsaCase_thenSetCreatedInGapsFromToReadyToList() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        SyaBenefitType syaBenefitType = new SyaBenefitType("ESA", "ESA");
        appealData.setBenefitType(syaBenefitType);

        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("Chesterfield DRT");
        appealData.setMrn(mrn);

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);
        assertEquals(READY_TO_LIST.getId(), caseData.getCreatedInGapsFrom());
    }

    @Test(expected = CcdException.class)
    public void givenExceptionWhenSearchingForCaseInCcd_shouldThrowException() {
        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any(IdamTokens.class)))
            .willThrow(RuntimeException.class);

        submitAppealService.submitAppeal(appealData, userToken);
    }

    @Test(expected = CcdException.class)
    public void givenCaseDoesNotExistInCcdAndGivenExceptionWhenCreatingCaseInCcd_shouldThrowException() {
        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(anyString(), anyString(), anyString(), any(IdamTokens.class)))
            .willReturn(null);

        given(ccdService.createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class)))
            .willThrow(RuntimeException.class);

        submitAppealService.submitAppeal(appealData, userToken);
    }

    @Test(expected = DuplicateCaseException.class)
    public void givenCaseIsADuplicate_shouldNotResendEmails() {
        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any()))
                .willReturn(Collections.singletonList(
                        SscsCaseDetails.builder()
                                .id(12345678L)
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .benefitType(BenefitType.builder().code(appealData.getBenefitType().getCode()).build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build()
                ));

        submitAppealService.submitAppeal(appealData, userToken);

        then(pdfServiceClient).should(never()).generateFromHtml(any(byte[].class), anyMap());
    }

    @Test(expected = DuplicateCaseException.class)
    public void givenCaseAlreadyExistsInCcd_shouldNotCreateCaseWithAppealDetails() {
        given(ccdService.findCaseBy(eq("data.appeal.appellant.identity.nino"), eq(appealData.getAppellant().getNino()), any()))
                .willReturn(Arrays.asList(
                        SscsCaseDetails.builder()
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .benefitType(BenefitType.builder().code(appealData.getBenefitType().getCode()).build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build(),
                        SscsCaseDetails.builder()
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .benefitType(BenefitType.builder().code("ESA").build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(appealData.getMrn().getDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build(),
                        SscsCaseDetails.builder()
                                .data(SscsCaseData.builder()
                                        .appeal(Appeal.builder()
                                                .appellant(Appellant.builder().identity(Identity.builder().nino(appealData.getAppellant().getNino()).build()).build())
                                                .benefitType(BenefitType.builder().code(appealData.getBenefitType().getCode()).build())
                                                .mrnDetails(MrnDetails.builder().mrnDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                        .build()).build()).build()).build()
                ));

        submitAppealService.submitAppeal(appealData, userToken);

        verify(ccdService, never()).createCase(any(SscsCaseData.class), any(String.class), any(String.class), any(String.class), any(IdamTokens.class));
    }

    @Test
    public void givenAssociatedCase_thenAddAssociatedCaseLinkToCase() {
        SscsCaseDetails matchingCase = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase);

        SscsCaseData caseData = submitAppealService.addAssociatedCases(
            SscsCaseData.builder().ccdCaseId("00000000").build(),
            matchedByNinoCases);

        assertEquals(1, caseData.getAssociatedCase().size());
        assertEquals("Yes", caseData.getLinkedCasesBoolean());
        assertEquals("12345678", caseData.getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void givenMultipleAssociatedCases_thenAddAllAssociatedCaseLinksToCase() {
        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);

        SscsCaseData caseData = submitAppealService.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("00000000").build(),
                matchedByNinoCases);

        assertEquals(2, caseData.getAssociatedCase().size());
        assertEquals("Yes", caseData.getLinkedCasesBoolean());
        assertEquals("12345678", caseData.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("56765676", caseData.getAssociatedCase().get(1).getValue().getCaseReference());
    }

    @Test
    public void addNoAssociatedCases() {
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();

        SscsCaseData caseData = submitAppealService.addAssociatedCases(
            SscsCaseData.builder().ccdCaseId("00000000").build(),
            matchedByNinoCases);

        assertNull(caseData.getAssociatedCase());
        assertEquals("No", caseData.getLinkedCasesBoolean());
        verify(ccdService, times(0)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
    }

    @Test
    public void getMatchedCases() {
        given(ccdService.findCaseBy(any(), any(), any())).willReturn(Collections.singletonList(
            SscsCaseDetails.builder().id(12345678L).build()
        ));
        List<SscsCaseDetails> matchedCases = submitAppealService.getMatchedCases("ABCDEFG", idamService.getIdamTokens());

        assertEquals(1, matchedCases.size());
    }

    @Test
    @Parameters({
            "PIP, n1w1 wal, Birmingham, appellant",
            "ESA, n1w1 wal, Birmingham, appellant",
            "UC, n1w1 wal, Birmingham, appellant",
            "PIP, NN85 1ss, Northampton, appellant",
            "ESA, NN85 1ss, Northampton, appellant",
            "UC, NN85 1ss, Northampton, appellant",
            "PIP, n1w1 wal, Birmingham, appointee",
            "ESA, n1w1 wal, Birmingham, appointee",
            "UC, n1w1 wal, Birmingham, appointee",
            "PIP, NN85 1ss, Northampton, appointee",
            "ESA, NN85 1ss, Northampton, appointee",
            "UC, NN85 1ss, Northampton, appointee",
    })
    public void shouldSetProcessingVenueBasedOnBenefitTypeAndPostCode(String benefitCode, String postcode, String expectedVenue, String appellantOrAppointee) {
        boolean isAppellant = appellantOrAppointee.equals("appellant");
        SyaCaseWrapper appealData = getSyaCaseWrapper(isAppellant ? "json/sya.json" : "sya/allDetailsWithAppointeeWithDifferentAddress.json");
        SyaBenefitType syaBenefitType = new SyaBenefitType(benefitCode, benefitCode);
        appealData.setBenefitType(syaBenefitType);
        if (isAppellant) {
            appealData.getAppellant().getContactDetails().setPostCode(postcode);
        } else {
            appealData.getAppointee().getContactDetails().setPostCode(postcode);
        }

        SscsCaseData caseData = submitAppealService.convertAppealToSscsCaseData(appealData);
        assertEquals(expectedVenue, caseData.getProcessingVenue());
    }
}
