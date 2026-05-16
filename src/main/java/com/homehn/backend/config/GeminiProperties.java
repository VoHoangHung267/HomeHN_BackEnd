package com.homehn.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.gemini")
public class GeminiProperties {
    private boolean enabled = false;
    private String apiKey;
    private String model = "gemini-2.5-flash-lite";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
}
