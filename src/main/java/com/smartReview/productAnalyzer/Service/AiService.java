package com.smartReview.productAnalyzer.Service;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final ChatClient chatClient;

    @Autowired
    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    public String getAiResponse(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}