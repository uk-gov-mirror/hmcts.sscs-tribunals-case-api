package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.reissuefurtherevidence;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.userFriendlyName;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class ReissueFurtherEvidenceAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.REISSUE_FURTHER_EVIDENCE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();


        List<? extends AbstractDocument> allSscsDocs = Stream.of(sscsCaseData.getSscsDocument(), sscsCaseData.getSscsWelshDocuments()).flatMap(x -> x == null ? null : x.stream()).filter(doc -> StringUtils.isNotBlank(doc.getValue().getDocumentType())).collect(Collectors.toList());

        ArrayList<? extends AbstractDocument> availableDocumentsToReIssue =
                Optional.ofNullable(allSscsDocs).map(Collection::stream)
                        .orElse(Stream.empty()).filter(f ->
                        APPELLANT_EVIDENCE.getValue().equals(f.getValue().getDocumentType())
                                || REPRESENTATIVE_EVIDENCE.getValue().equals(f.getValue().getDocumentType())
                                || DWP_EVIDENCE.getValue().equals(f.getValue().getDocumentType())
        ).collect(Collectors.toCollection(ArrayList::new));


        if (CollectionUtils.isNotEmpty(availableDocumentsToReIssue)) {
            setDocumentDropdown(sscsCaseData, availableDocumentsToReIssue);
            sscsCaseData.setResendToAppellant(null);
            sscsCaseData.setResendToRepresentative(null);
            sscsCaseData.setResendToDwp(null);
            sscsCaseData.setOriginalSender(null);
        }

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (CollectionUtils.isEmpty(availableDocumentsToReIssue)) {
            response.addError("There are no evidence documents in the appeal. Cannot reissue further evidence.");
        }
        return response;
    }

    private void setDocumentDropdown(SscsCaseData sscsCaseData, List<? extends AbstractDocument> availableDocumentsToReIssue) {
        List<DynamicListItem> listCostOptions = new ArrayList<>();

        for (AbstractDocument doc: availableDocumentsToReIssue) {
            String label = buildFormattedLabel(doc);
            if (doc.getValue().getDocumentLink() != null) {
                listCostOptions.add(new DynamicListItem(doc.getValue().getDocumentLink().getDocumentUrl(), label));
            }
        }

        sscsCaseData.setReissueFurtherEvidenceDocument(new DynamicList(listCostOptions.get(0), listCostOptions));
    }

    private String buildFormattedLabel(AbstractDocument doc) {
        String filenameLabel = doc.getValue().getDocumentFileName();
        if (doc instanceof  SscsWelshDocument ){
            filenameLabel = getBilingualLabel(doc);
        }
        return String.format("%s -  %s", filenameLabel, userFriendlyName(doc.getValue().getDocumentType()));
    }

    @NotNull
    private String getBilingualLabel(AbstractDocument doc) {
        StringBuilder sb = new StringBuilder("Bilingual - ");
        if (doc.getValue().getDocumentLink().getDocumentFilename() != null ) {
            sb.append(doc.getValue().getDocumentLink().getDocumentFilename());
        }
        sb.append(" received on ");
        sb.append(doc.getValue().getDocumentDateAdded());
        return sb.toString();
    }
}
