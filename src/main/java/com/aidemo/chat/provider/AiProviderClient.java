package com.aidemo.chat.provider;

import com.aidemo.chat.dto.AiReply;
import reactor.core.publisher.Flux;

/**
 * AI provider client contract.
 */
public interface AiProviderClient {

    String name();

    String modelName();

    String chat(String message);

    Flux<String> chatStream(String message);

    AiReply chatStructured(String message);
}
