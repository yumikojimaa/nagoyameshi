package com.example.nagoyameshi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.example.nagoyameshi.service.StripeService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;

@Controller
public class StripeWebhookController {
	private Logger logger  = LoggerFactory.getLogger(StripeWebhookController.class);
	private final StripeService stripeService;
	 
    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public StripeWebhookController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Stripe.apiKey = stripeApiKey;
        Event event = null;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
          logger.info(event.toString());
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        logger.info("event: {}", event.getType());
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        if (dataObjectDeserializer.getObject().isEmpty()) {
            logger.info("data empty");
        	return new ResponseEntity<>("Success", HttpStatus.OK);
        	
        }
        
        StripeObject stripeObject = dataObjectDeserializer.getObject().get();
        
        if ("customer.subscription.created".equals(event.getType())) {
            logger.info("{}", stripeObject);
            stripeService.processSubscriptionComple((Subscription)stripeObject);
        } else if ("customer.created".equals(event.getType())) {
            stripeService.processCustomerCreated((Customer)stripeObject);
        } else if ("customer.subscription.deleted".equals(event.getType())) {
            stripeService.processSubscriptionDelete((Subscription)stripeObject);

        return new ResponseEntity<>("Success", HttpStatus.OK);
    }
}
}
