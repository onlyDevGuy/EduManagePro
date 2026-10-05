package com.edumanage.web;

import com.edumanage.service.InvoiceService;
import com.edumanage.service.StripeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripeService stripeService;
    private final InvoiceService invoiceService;
    private final ObjectMapper objectMapper;

    /**
     * Public endpoint — authenticated by Stripe's HMAC signature, not by JWT.
     * Configure in Stripe CLI:  stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
     *
     * We read the fields we need straight from the raw JSON rather than Stripe's typed
     * deserializer, because that deserializer returns empty whenever the event's API
     * version is newer than the stripe-java SDK on the classpath.
     */
    @PostMapping("/stripe")
    public ResponseEntity<Map<String, Object>> handle(@RequestBody String payload,
                                                      @RequestHeader("Stripe-Signature") String signature) {
        Event event = stripeService.constructEvent(payload, signature); // verifies the signature
        log.info("Stripe webhook received: {} ({})", event.getType(), event.getId());

        switch (event.getType()) {
            case "checkout.session.completed" -> {
                JsonNode session = objectNode(payload);
                String invoiceId    = text(session.path("metadata").path("invoiceId"));
                String tenantId     = text(session.path("metadata").path("tenantId"));
                String paymentIntent = text(session.path("payment_intent"));
                if (invoiceId != null && tenantId != null) {
                    invoiceService.markPaid(invoiceId, tenantId, paymentIntent);
                    log.info("Invoice {} marked paid from checkout session", invoiceId);
                } else {
                    log.warn("checkout.session.completed missing invoice/tenant metadata");
                }
            }
            case "checkout.session.expired" ->
                log.info("Checkout session expired: {}", event.getId());
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok(Map.of("received", true));
    }

    /** Pulls {@code data.object} out of the raw webhook JSON. */
    private JsonNode objectNode(String payload) {
        try {
            return objectMapper.readTree(payload).path("data").path("object");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed webhook payload");
        }
    }

    private static String text(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }
}
