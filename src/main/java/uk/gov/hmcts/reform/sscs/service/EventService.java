package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_APPEAL_PDF;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class EventService {

    private final SscsPdfService sscsPdfService;
    private final RoboticsService roboticsService;
    private final EvidenceManagementService evidenceManagementService;
    private final EmailService emailService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final IdamService idamService;

    @Autowired
    EventService(SscsPdfService sscsPdfService,
                 RoboticsService roboticsService,
                 RegionalProcessingCenterService regionalProcessingCenterService,
                 IdamService idamService,
                 EvidenceManagementService evidenceManagementService,
                 EmailService emailService) {
        this.sscsPdfService = sscsPdfService;
        this.roboticsService = roboticsService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.idamService = idamService;
        this.evidenceManagementService = evidenceManagementService;
        this.emailService = emailService;
    }

    public boolean handleEvent(EventType eventType, SscsCaseData caseData) {

        if (CREATE_APPEAL_PDF == eventType) {
            createAppealPdfAndSendToRobotics(caseData);
            return true;
        }

        return false;
    }

    private void handleEvent(Runnable eventHandler) {
        Executors.newSingleThreadExecutor().submit(eventHandler);
    }

    private void createAppealPdfAndSendToRobotics(SscsCaseData caseData) {

        if (!hasPdfDocument(caseData)) {
            updateAppointeeNullIfNotPresent(caseData);
            caseData.setEvidencePresent(hasEvidence(caseData));
            String firstHalfOfPostcode = regionalProcessingCenterService.getFirstHalfOfPostcode(
                    caseData.getAppeal().getAppellant().getAddress().getPostcode());

            IdamTokens idamTokens = idamService.getIdamTokens();

            byte[] pdf = sscsPdfService.generateAndSendPdf(caseData, Long.parseLong(caseData.getCcdCaseId()),
                    idamTokens,"appellantEvidence");

            Map<String, byte[]> additionalEvidence = downloadEvidence(caseData);

            roboticsService.sendCaseToRobotics(caseData, Long.parseLong(caseData.getCcdCaseId()),
                    firstHalfOfPostcode, pdf, additionalEvidence);
        }
    }

    private void updateAppointeeNullIfNotPresent(SscsCaseData caseData) {
        if (caseData != null && caseData.getAppeal() != null && caseData.getAppeal().getAppellant() != null) {
            Appointee appointee = caseData.getAppeal().getAppellant().getAppointee();
            if (appointee != null && appointee.getName() == null) {
                caseData.getAppeal().getAppellant().setAppointee(null);
            }
        }
    }

    public boolean sendEvent(EventType eventType, SscsCaseData caseData) {

        if (CREATE_APPEAL_PDF == eventType) {
            handleEvent(eventHandler(eventType, caseData));
            return true;
        }

        return false;
    }

    private Runnable eventHandler(EventType eventType, SscsCaseData caseData) {
        return () -> handleEvent(eventType, caseData);
    }

    private boolean hasPdfDocument(SscsCaseData caseData) {
        String fileName = emailService.generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";
        for (SscsDocument document : caseData.getSscsDocument()) {
            if (document != null && fileName.equals(document.getValue().getDocumentFileName())) {
                return true;
            }
        }
        return false;
    }

    private String hasEvidence(SscsCaseData caseData) {
        String fileName = emailService.generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";
        if (caseData.getSscsDocument() != null) {
            for (SscsDocument document : caseData.getSscsDocument()) {
                if (document != null && !fileName.equals(document.getValue().getDocumentFileName())) {
                    return "Yes";
                }
            }
        }
        return "No";
    }

    private Map<String, byte[]> downloadEvidence(SscsCaseData caseData) {
        Map<String, byte[]> map = new LinkedHashMap<>();
        if (caseData.getSscsDocument() != null) {
            String appealPdfName = emailService.generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";
            for (SscsDocument document : caseData.getSscsDocument()) {
                if (!appealPdfName.equals(document.getValue().getDocumentFileName())) {
                    map.put(document.getValue().getDocumentFileName(), downloadBinary(document));
                }
            }
        }
        return map;
    }

    private byte[] downloadBinary(SscsDocument document) {
        return evidenceManagementService.download(URI.create(document.getValue().getDocumentLink().getDocumentUrl()),
                SubmitAppealService.DM_STORE_USER_ID);
    }

}
