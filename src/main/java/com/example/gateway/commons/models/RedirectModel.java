package com.example.gateway.commons.models;

import lombok.Data;

@Data
public class RedirectModel {
    private String successUrl;
    private String failedUrl;
}
