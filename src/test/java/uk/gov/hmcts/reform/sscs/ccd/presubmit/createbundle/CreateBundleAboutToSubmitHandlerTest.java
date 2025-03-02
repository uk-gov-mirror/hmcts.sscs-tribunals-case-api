package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_EVIDENCE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_RESPONSE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;
import uk.gov.hmcts.reform.sscs.service.bundle.BundleAudioVideoPdfService;

@RunWith(JUnitParamsRunner.class)
public class CreateBundleAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private CreateBundleAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private ServiceRequestExecutor serviceRequestExecutor;

    @Mock
    private BundleAudioVideoPdfService bundleAudioVideoPdfService;

    private SscsCaseData sscsCaseData;

    private DwpDocumentService dwpDocumentService;


    @Before
    public void setUp() {
        openMocks(this);
        dwpDocumentService = new DwpDocumentService();
        handler = new CreateBundleAboutToSubmitHandler(serviceRequestExecutor, dwpDocumentService, bundleAudioVideoPdfService, "bundleUrl.com", "bundleEnglishConfig", "bundleWelshConfig",
                "bundleEnglishEditedConfig", "bundleWelshEditedConfig");

        when(callback.getEvent()).thenReturn(EventType.CREATE_BUNDLE);

        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(serviceRequestExecutor.post(any(), any())).thenReturn(new PreSubmitCallbackResponse<>(sscsCaseData));
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"Yes, bundleWelshConfig", " No, bundleEnglishConfig"})
    public void givenCaseWithLanguagePreference_thenPopulateConfigFileName(String languagePreference, String expectedConfigFile) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        caseData.setDwpDocuments(dwpDocuments);

        caseData.setLanguagePreferenceWelsh(languagePreference);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(expectedConfigFile, response.getData().getMultiBundleConfiguration().get(0).getValue());
    }

    @Test
    public void givenEnglishCaseWithEdited_thenPopulateEnglishEditedAndUneditedConfigFileName() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        callback.getCaseDetails().getCaseData().setDwpPhme("Yes");
        callback.getCaseDetails().getCaseData().setPhmeGranted(YesNo.YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("No");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals("bundleEnglishEditedConfig", response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("bundleEnglishConfig", response.getData().getMultiBundleConfiguration().get(1).getValue());
    }

    @Test
    public void givenWelshCaseWithEdited_thenPopulateWelshEditedAndUneditedConfigFileName() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        callback.getCaseDetails().getCaseData().setDwpPhme("Yes");
        callback.getCaseDetails().getCaseData().setPhmeGranted(YesNo.YES);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("Yes");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        assertEquals(2, response.getData().getMultiBundleConfiguration().size());
        assertEquals("bundleWelshEditedConfig", response.getData().getMultiBundleConfiguration().get(0).getValue());
        assertEquals("bundleWelshConfig", response.getData().getMultiBundleConfiguration().get(1).getValue());
    }

    @Test
    @Parameters({"Yes, bundleWelshConfig", " No, bundleEnglishConfig"})
    public void givenCaseWithEditedDwpDocsAndPhmeNotGranted_thenReturnErrorMessageAndDoNotSendRequestToBundleService(String languagePreference, String expectedConfigFile) {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh(languagePreference);
        callback.getCaseDetails().getCaseData().setDwpPhme("Yes");
        callback.getCaseDetails().getCaseData().setPhmeGranted(YesNo.NO);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        assertEquals(1, response.getData().getMultiBundleConfiguration().size());
        assertEquals(expectedConfigFile, response.getData().getMultiBundleConfiguration().get(0).getValue());
    }

    @Test
    public void givenDwpResponseDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DWP_RESPONSE.getLabel(), response.getData().getDwpDocuments().stream().filter(e -> e.getValue().getDocumentType().equals(DWP_RESPONSE.getValue())).collect(toList()).get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenDwpEvidenceDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DWP_EVIDENCE_BUNDLE.getLabel(), response.getData().getDwpDocuments().stream().filter(e -> e.getValue().getDocumentType().equals(DWP_EVIDENCE_BUNDLE.getValue())).collect(toList()).get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenSscsDocumentHasEmptyFileName_thenPopulateFileName() {

        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName(null).documentLink(
            DocumentLink.builder().documentFilename("test.com").build()).build()).build();
        List<SscsDocument> docs = new ArrayList<>();

        docs.add(sscsDocument);

        callback.getCaseDetails().getCaseData().setSscsDocument(docs);

        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("test.com", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenCreateBundleEventWithAudioVideoEvidence_thenTriggerTheExternalCreateBundleEvent() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        List<SscsDocument> audioVideoEvidences = new ArrayList<>();
        audioVideoEvidences.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentDateAdded(LocalDate.now().toString())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename.mp3").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build()).build())
                .build());
        caseDetails.getCaseData().setSscsDocument(audioVideoEvidences);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(bundleAudioVideoPdfService).createAudioVideoPdf(sscsCaseData);
        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
    }

    @Test
    public void givenEmptyDwpEvidenceBundleDocumentLinkWithDwpDocumentsPattern_thenReturnError() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
        verifyNoInteractions(serviceRequestExecutor);

    }

    @Test
    public void givenEmptyDwpEvidenceBundleDocumentLinkWithOldPattern_thenReturnError() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenEmptyDwpResponseDocumentLinkWithDwpDocumentsPattern_thenReturnError() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenEmptyDwpResponseDocumentLinkWithOldPattern_thenReturnError() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenCaseWithEditedDwpDocsAndPhmeUnderReview_thenReturnErrorMessageAndDoNotSendRequestToBundleService() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("No");
        callback.getCaseDetails().getCaseData().setDwpPhme("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("There is a pending PHME request on this case", error);
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenCaseWithPreviouslyCreatedBundles_thenClearAllBundles() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).editedDocumentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("No");
        callback.getCaseDetails().getCaseData().setPhmeGranted(YesNo.YES);

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(Bundle.builder().value(BundleDetails.builder().build()).build());
        callback.getCaseDetails().getCaseData().setCaseBundles(bundles);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        assertNull(response.getData().getCaseBundles());
    }

}