package com.hospital.billing.claim.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Production adapter (profile "prod"). Real Medicare ECLIPSE / fund gateways require a PRODA
 * B2B device certificate and a Services Australia-certified message format; this class shows
 * the integration seam: an HTTP call to a claims gateway (for example a clearing-house
 * such as Tyro Health or HICAPS) with transient errors mapped to PayerCommunicationException
 * so the caller's retry policy applies.
 */
@Slf4j
@Component
@Profile("prod")
public class ProductionEclipseClaimAdapter implements InsuranceClaimAdapter {

    private final RestClient restClient;

    public ProductionEclipseClaimAdapter(RestClient.Builder builder,
                                         @Value("${app.claims.gateway.base-url}") String baseUrl,
                                         @Value("${app.claims.gateway.api-key}") String apiKey) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Api-Key", apiKey)
                .build();
    }

    @Override
    public SubmissionReceipt submit(ClaimSubmission submission) {
        try {
            return restClient.post()
                    .uri("/claims")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(submission)
                    .retrieve()
                    .body(SubmissionReceipt.class);
        } catch (RestClientException ex) {
            throw new PayerCommunicationException("Claims gateway unavailable for " + submission.claimNo(), ex);
        }
    }

    @Override
    public PayerDecision fetchDecision(String payerReference, ClaimSubmission submission) {
        try {
            return restClient.get()
                    .uri("/claims/{reference}/decision", payerReference)
                    .retrieve()
                    .body(PayerDecision.class);
        } catch (RestClientException ex) {
            throw new PayerCommunicationException("Could not fetch decision for " + payerReference, ex);
        }
    }
}
