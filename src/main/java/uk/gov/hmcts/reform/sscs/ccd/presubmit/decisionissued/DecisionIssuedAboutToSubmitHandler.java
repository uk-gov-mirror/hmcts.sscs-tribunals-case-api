package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DecisionType.STRIKE_OUT;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.isFileAPdf;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Service
@Slf4j
public class DecisionIssuedAboutToSubmitHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;

    @Autowired
    public DecisionIssuedAboutToSubmitHandler(FooterService footerService) {
        this.footerService = footerService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
                && (callback.getEvent() == EventType.DECISION_ISSUED
                || callback.getEvent() == EventType.DECISION_ISSUED_WELSH)
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        final PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        DocumentLink url = null;
        if (nonNull(callback.getCaseDetails().getCaseData().getPreviewDocument())) {
            url = caseData.getPreviewDocument();
        } else if (caseData.getSscsInterlocDecisionDocument() != null) {
            url = caseData.getSscsInterlocDecisionDocument().getDocumentLink();
            caseData.setDateAdded(caseData.getSscsInterlocDecisionDocument().getDocumentDateAdded());
            if (!isFileAPdf(caseData.getSscsInterlocDecisionDocument().getDocumentLink())) {
                sscsCaseDataPreSubmitCallbackResponse.addError("You need to upload PDF documents only");
                return sscsCaseDataPreSubmitCallbackResponse;
            }
        } else if (callback.getEvent() == EventType.DECISION_ISSUED_WELSH) {
            Optional<SscsWelshDocument> document = caseData.getLatestWelshDocumentForDocumentType(DocumentType.DECISION_NOTICE);
            url = document.isPresent() ? document.get().getValue().getDocumentLink() : null;
        }
        if (isNull(url)) {
            sscsCaseDataPreSubmitCallbackResponse.addError("You need to upload a PDF document");
            return sscsCaseDataPreSubmitCallbackResponse;
        }

        SscsDocumentTranslationStatus documentTranslationStatus = caseData.isLanguagePreferenceWelsh() && callback.getEvent() == EventType.DECISION_ISSUED ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null;

        footerService.createFooterAndAddDocToCase(url, caseData, DocumentType.DECISION_NOTICE,
                Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now()).format(DateTimeFormatter.ofPattern("dd-MM-YYYY")),
                caseData.getDateAdded(), null, documentTranslationStatus);

        if (!SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(documentTranslationStatus)) {
            State beforeState = callback.getCaseDetailsBefore().map(CaseDetails::getState).orElse(null);
            clearTransientFields(caseData,beforeState);
            caseData.setDwpState(DwpState.STRUCK_OUT.getId());
            caseData.setDirectionDueDate(null);

            if (STRIKE_OUT.getValue().equals(caseData.getDecisionType())) {
                if (State.INTERLOCUTORY_REVIEW_STATE.equals(beforeState)) {
                    caseData.setOutcome("nonCompliantAppealStruckout");
                } else {
                    caseData.setOutcome("struckOut");
                }
                caseData.setInterlocReviewState(null);
            }
            caseData.setState(State.DORMANT_APPEAL_STATE);
        } else {
            clearBasicTransientFields(caseData);
            caseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION.getId());
            caseData.setTranslationWorkOutstanding("Yes");
        }

        log.info("Saved the new interloc decision document for case id: " + caseData.getCcdCaseId());

        return sscsCaseDataPreSubmitCallbackResponse;
    }
}
