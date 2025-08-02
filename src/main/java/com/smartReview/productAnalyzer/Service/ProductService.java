package com.smartReview.productAnalyzer.Service;

import com.smartReview.productAnalyzer.Exception.InvalidUrlException;
import com.smartReview.productAnalyzer.Model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class ProductService {
    @Autowired
    private AmazonScraperService scraperService;
    @Autowired
    private AmazonSearchPageScraper scraperSearchService;

    @Autowired
    private AiService openAiService;

    public Product analyzeProduct(String productDescription) {
        String q = openAiService.getAiResponse("\nYou are an expert at translating product requirements into Amazon.in search URLs.\n" +

                "User request: "+productDescription+"\n" +
                "\n" +
                "Build a valid Amazon India search URL (base https://www.amazon.in/s) that:\n" +
                "  • Encodes the main keywords (For example :“nike red running shoes”).  \n" +
                "  • Applies any explicit price‑filter (For price : “under 5000” → rh=p_36%3A-500000).  \n" +
                "  • Includes other filters if mentioned (For other things : brand, color, RAM, storage…).\n" +
                "\n" +
                "Return only the full URL—nothing else, not even a single line apart from url.\n" +
                "Example Response : https://www.amazon.in/s?k=nike+red+running+shoes&rh=p_36%3A-500000\n");
        System.out.println(q);
        String url = q + "&s=review-rank";
        List<String> productLinks = scraperSearchService.scrapeAmazonOnUrl(url);
        List<Product> products = new ArrayList<>();

        for(String link: productLinks){
            Product currentProduct = analyzeLink(link);
            if(currentProduct!=null && !Objects.equals(currentProduct.getProductName(), "Not found") && currentProduct.getProductPrice()!="Not found"){
                products.add(currentProduct);
            }
        }
        products.sort((product1, product2) -> Double.compare(product2.getRating(), product1.getRating()));

        if(!products.isEmpty()){
            return products.getFirst();
        }
        return null;

    }

    public Product analyzeLink(String link) {
        if(link == null || !link.contains("amazon")){
            throw new InvalidUrlException("Not a valid Amazon.in Url");
        }
        Product product = scraperService.scrapeAmazonOnUrl(link);

        List<String> allReviews = product.getProductPros();
        if (allReviews == null || allReviews.isEmpty() || allReviews.getFirst().equals("Not found")) {
            product.setProductName("Not found");
            product.setProductPrice("Not found");
            product.setProductPros(Collections.singletonList("Not found"));
            product.setProductCons(Collections.singletonList("Not found"));
            product.setVerdict("Not found");
            product.setRating(0.0);
            return product;
        }

// Single consolidated prompt
        String consolidatedPrompt = "I have a product from Amazon with the following reviews:\n" +
                allReviews.toString() + "\n\n" +
                "Please analyze these reviews and provide the following information in this exact format:\n\n" +
                "PROS:\n" +
                "- [list each pro as a short bullet point]\n\n" +
                "CONS:\n" +
                "- [list each con as a short bullet point]\n\n" +
                "VERDICT:\n" +
                "[one-line verdict whether the product is worth buying or not]\n\n" +
                "RATING:\n" +
                "[rating out of 10 as a number only]\n\n" +
                "Do not include any introductory statements or additional text.";

        String response = openAiService.getAiResponse(consolidatedPrompt);

// Parse the consolidated response
        String[] sections = response.split("(?=PROS:|CONS:|VERDICT:|RATING:)");

// Extract pros
        List<String> prosOfProduct = new ArrayList<>();
        for (String section : sections) {
            if (section.trim().startsWith("PROS:")) {
                String prosSection = section.replace("PROS:", "").trim();
                prosOfProduct = parseToString(prosSection);
                break;
            }
        }

// Extract cons
        List<String> consOfProduct = new ArrayList<>();
        for (String section : sections) {
            if (section.trim().startsWith("CONS:")) {
                String consSection = section.replace("CONS:", "").trim();
                consOfProduct = parseToString(consSection);
                break;
            }
        }

// Extract verdict
        String verdict = "";
        for (String section : sections) {
            if (section.trim().startsWith("VERDICT:")) {
                verdict = section.replace("VERDICT:", "").trim();
                break;
            }
        }

// Extract rating
        double rating = 0.0;
        for (String section : sections) {
            if (section.trim().startsWith("RATING:")) {
                String ratingStr = section.replace("RATING:", "").trim();
                rating = Double.parseDouble(ratingStr);
                break;
            }
        }

// Set all values
        product.setProductPros(prosOfProduct);
        product.setProductCons(consOfProduct);
        product.setVerdict(verdict);
        product.setRating(rating);

        return product;
    }
    private List<String> parseToString(String raw) {
        List<String> result = new ArrayList<>();
        for (String line : raw.split("\n")) {
            line = line.trim();
            if (line.startsWith("-") || line.startsWith("*") || line.startsWith("•")) {
                line = line.substring(1).trim();
            }
            else if (line.matches("^\\d+\\..*")) {
                line = line.replaceFirst("^\\d+\\.", "").trim();
            }
            if (!line.isEmpty()) {
                result.add(line);
            }
        }
        return result;
    }

}
