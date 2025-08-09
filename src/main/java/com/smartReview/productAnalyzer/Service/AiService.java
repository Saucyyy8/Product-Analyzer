package com.smartReview.productAnalyzer.Service;


import com.smartReview.productAnalyzer.Config.PromptLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final ChatClient chatClient;

    private final PromptLoader promptLoader;

    @Autowired
    public AiService(ChatClient.Builder chatClientBuilder,PromptLoader promptLoader) {
        this.chatClient = chatClientBuilder.build();
        this.promptLoader = promptLoader;
    }
    public String getProductAnalysisResponse(String reviewsText) {
        try{
            String prompt = promptLoader.loadPromptFile("productAnalyzer.txt");
            String fullPrompt = prompt.replace("{reviews}", reviewsText);
            System.out.println("Sending prompt to AI: " + fullPrompt);
            
            String response = chatClient.prompt()
                    .user(fullPrompt)
                    .call()
                    .content();
            System.out.println("Received AI response: " + response);
            return response;
        }
        catch (Exception e){
            System.err.println("AI service error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("AI Service Error: " + e.getMessage(), e);
        }
    }
    public String getSearchQueryResponse(String query) {
        try{
        String prompt = promptLoader.loadPromptFile("searchQueryPrompt.txt").replace("{query}",query);
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        }
        catch (Exception e){
            throw new RuntimeException("File Handling Error..");
        }
    }
}