package com.edumanage.service;

import com.edumanage.config.AppProperties;
import com.edumanage.domain.Invoice;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final AppProperties appProperties;
    private StripeClient client;

    @PostConstruct
    void init() {
        if (appProperties.stripe().isConfigured()) {
            this.client = new StripeClient(appProperties.stripe().secretKey());
        } else {
            log.warn("Stripe is not configured (STRIPE_SECRET_KEY blank). "
                   + "Billing endpoints will return 503 until it is set.");
        }
    }

    public boolean isConfigured() {
        return client != null;
    }

    public String createCheckoutSession(Invoice invoice, String tenantId) {
        requireConfigured();

        long minorUnits = invoice.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(appProperties.stripe().successUrl()
                        + "?invoice=" + invoice.getId())
                .setCancelUrl(appProperties.stripe().cancelUrl()
                        + "?invoice=" + invoice.getId())
                .setClientReferenceId(invoice.getId())
                .putMetadata("invoiceId", invoice.getId())
                .putMetadata("tenantId", tenantId)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(invoice.getCurrency().toLowerCase())
                                .setUnitAmount(minorUnits)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(invoice.getNumber() + " — " + invoice.getDescription())
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            Session session = client.checkout().sessions().create(params);
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Stripe checkout creation failed for invoice {}", invoice.getId(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Payment provider error: " + e.getUserMessage());
        }
    }

    public com.stripe.model.Event constructEvent(String payload, String signatureHeader) {
        requireConfigured();
        try {
            return client.constructEvent(payload, signatureHeader,
                    appProperties.stripe().webhookSecret());
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature");
        }
    }

    private void requireConfigured() {
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Billing is not configured on this environment");
        }
    }
}