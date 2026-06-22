package com.interniq.ai;

import com.interniq.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AiProviderFactory {

    private static final String MOCK_PROVIDER = "mock";
    private static final String OPENAI_COMPATIBLE_PROVIDER = "openai-compatible";

    private final MockAiProvider mockAiProvider;
    private final OpenAiCompatibleProvider openAiCompatibleProvider;

    @Value("${application.ai.provider:mock}")
    private String configuredProvider;

    @Value("${application.ai.api-key:}")
    private String apiKey;

    public AiProvider getProvider() {
        String provider = normalize(configuredProvider);

        if (provider.isBlank() || MOCK_PROVIDER.equals(provider)) {
            return mockAiProvider;
        }

        if (OPENAI_COMPATIBLE_PROVIDER.equals(provider)) {
            if (apiKey == null || apiKey.isBlank()) {
                throw new BadRequestException("AI_PROVIDER=openai-compatible requires AI_API_KEY. Keep AI_PROVIDER=mock for local mocked AI.");
            }
            return openAiCompatibleProvider;
        }

        throw new BadRequestException("Unsupported AI_PROVIDER: " + configuredProvider);
    }

    public String configuredProviderName() {
        String provider = normalize(configuredProvider);
        return provider.isBlank() ? "MOCK" : provider.toUpperCase(Locale.ROOT);
    }

    private String normalize(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    }
}
