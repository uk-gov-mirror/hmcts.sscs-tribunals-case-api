package uk.gov.hmcts.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AirLookupServiceException extends UnknownErrorCodeException {
    public AirLookupServiceException(Throwable cause) {
        super(AlertLevel.P3, cause);
    }
}