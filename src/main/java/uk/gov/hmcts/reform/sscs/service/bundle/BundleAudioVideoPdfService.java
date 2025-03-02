package uk.gov.hmcts.reform.sscs.service.bundle;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.PdfTableDescriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.PdfTemplateContent;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.PdfService;

@Service
public class BundleAudioVideoPdfService {
    private final PdfService pdfService;
    private static final String TEMPLATE = "TB-SCS-GNO-ENG-00670.docx";
    private final EvidenceManagementService evidenceManagementService;
    private static final String DM_STORE_USER_ID = "sscs";
    private String dmGatewayUrl;
    private String documentManagementUrl;
    protected static DateTimeFormatter DATEFORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public BundleAudioVideoPdfService(
            @Qualifier("docmosisPdfService") PdfService pdfService,
            EvidenceManagementService evidenceManagementService,
            @Value("${dm_gateway.url}") String dmGatewayUrl,
            @Value("${document_management.url}") String documentManagementUrl) {
        this.pdfService = pdfService;
        this.evidenceManagementService = evidenceManagementService;
        this.dmGatewayUrl = dmGatewayUrl;
        this.documentManagementUrl = documentManagementUrl;
    }

    public void createAudioVideoPdf(SscsCaseData sscsCaseData) {

        List<PdfTableDescriptor> descriptors = buildAudioVideoEvidenceDescriptorsForTable(sscsCaseData);

        if (descriptors != null && descriptors.size() > 0) {
            PdfTemplateContent pdfTemplateContent = PdfTemplateContent.builder().content(descriptors).build();

            byte[] content = pdfService.createPdf(pdfTemplateContent, TEMPLATE);

            ByteArrayMultipartFile file = ByteArrayMultipartFile.builder()
                    .content(content)
                    .name("Audio-video-bundle-document.pdf")
                    .contentType(APPLICATION_PDF).build();

            UploadResponse uploadResponse = evidenceManagementService.upload(singletonList(file), DM_STORE_USER_ID);

            if (uploadResponse != null) {
                String location = uploadResponse.getEmbedded().getDocuments().get(0).links.self.href;
                DocumentLink newDocLink = DocumentLink.builder().documentFilename(file.getOriginalFilename()).documentUrl(location).documentBinaryUrl(location + "/binary").build();

                sscsCaseData.setAudioVideoEvidenceBundleDocument(AudioVideoEvidenceBundleDocument.builder().documentLink(newDocLink).documentFileName("Audio/video document").build());
            }
        }
    }

    private List<PdfTableDescriptor> buildAudioVideoEvidenceDescriptorsForTable(SscsCaseData caseData) {

        List<PdfTableDescriptor> pdfTableDescriptorList = new ArrayList<>();

        if (caseData.getSscsDocument() != null) {
            pdfTableDescriptorList.addAll(buildDescriptorsUsingDocuments(caseData.getSscsDocument()));
        }

        if (caseData.getDwpDocuments() != null) {
            pdfTableDescriptorList.addAll(buildDescriptorsUsingDocuments(caseData.getDwpDocuments()));
        }

        return pdfTableDescriptorList;
    }

    public List<PdfTableDescriptor> buildDescriptorsUsingDocuments(List<? extends AbstractDocument> abstractDocument) {

        return abstractDocument.stream()
                .filter(e -> e.getValue().getDocumentLink().getDocumentFilename().endsWith(".mp3") || e.getValue().getDocumentLink().getDocumentFilename().endsWith(".mp4"))
                .map(audioVideoDocument -> buildDescriptorsFromAudioVideoEvidence(audioVideoDocument))
                .collect(Collectors.toList());

    }

    private PdfTableDescriptor buildDescriptorsFromAudioVideoEvidence(AbstractDocument document) {

        if (document != null) {

            String docUrl = document.getValue().getDocumentLink().getDocumentBinaryUrl().replace(documentManagementUrl, dmGatewayUrl);

            return PdfTableDescriptor.builder().documentType(DocumentType.fromValue(document.getValue().getDocumentType()).getLabel())
                    .documentUrl(document.getValue().getDocumentLink().getDocumentFilename() + "|" + docUrl)
                    .dateAdded(DATEFORMATTER.format(LocalDate.parse(document.getValue().getDocumentDateAdded())))
                    .dateApproved(DATEFORMATTER.format(LocalDate.parse(document.getValue().getDateApproved())))
                    .uploadParty(document.getValue().getPartyUploaded().getLabel())
                    .build();
        }
        return null;
    }
}
