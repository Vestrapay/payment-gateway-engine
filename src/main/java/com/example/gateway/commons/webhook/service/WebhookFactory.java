package com.example.gateway.commons.webhook.service;

import com.example.gateway.commons.webhook.interfaces.IWebhookInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookFactory {
    private final List<IWebhookInterface> webhookInterfaces;

    public IWebhookInterface getImplementation(String provider){
        return webhookInterfaces.stream().filter(iWebhookInterface -> iWebhookInterface.getProvider().equalsIgnoreCase(provider)).findFirst()
                .orElseThrow(() -> new RuntimeException("provider integration not found"));
    }

}
