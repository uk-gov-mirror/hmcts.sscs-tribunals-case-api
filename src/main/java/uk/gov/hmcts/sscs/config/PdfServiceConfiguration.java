package uk.gov.hmcts.sscs.config;

import static java.net.URI.create;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;

@Configuration
public class PdfServiceConfiguration {

    @Value("${pdf.api.url}")
    private String pdfApiUrl;

    @Bean
    public PDFServiceClient pdfServiceClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        AuthTokenGenerator authTokenGenerator
    ) {
        return PDFServiceClient.builder()
            .restOperations(restTemplate)
            .objectMapper(objectMapper)
            .build(authTokenGenerator::generate, create(pdfApiUrl));
    }
}
