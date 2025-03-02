package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EvidenceDescription {
    @ApiModelProperty(example = "this is a description of the evidence", required = false)
    @JsonProperty(value = "body")
    private String body;

    @JsonProperty(value = "idamEmail")
    private String idamEmail;

    // needed for Jackson
    private EvidenceDescription() {
    }

    public EvidenceDescription(String body, String idamEmail) {
        this.body = body;
        this.idamEmail = idamEmail;
    }

    public String getBody() {
        return body;
    }

    public String getIdamEmail() {
        return idamEmail;
    }
}
