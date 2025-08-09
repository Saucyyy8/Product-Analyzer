package com.smartReview.productAnalyzer.Service;

import com.smartReview.productAnalyzer.Exception.AiServiceException;
import com.smartReview.productAnalyzer.Exception.InvalidUrlException;
import com.smartReview.productAnalyzer.Exception.ProductNotFound;
import com.smartReview.productAnalyzer.Exception.ScrapingException;
import com.smartReview.productAnalyzer.Model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class ProductService {

    private final AmazonScraperService scraperService;
    private final AmazonSearchPageScraper searchScraperService;
    private final AiService aiService;


    @Autowired
    public ProductService(AmazonScraperService scraperService, AmazonSearchPageScraper searchScraperService, AiService aiService) {
        this.scraperService = scraperService;
        this.searchScraperService = searchScraperService;
        this.aiService = aiService;
    }
    public Product analyzeProduct(String productDescription) {
        log.info("Starting product analysis for description: {}", productDescription);

        try {

            SearchCriteria criteria = parseProductDescription(productDescription);

            String searchUrl = buildSearchUrl(criteria, productDescription);
            log.debug("Built search URL: {}", searchUrl);

            // Try to get product links from search
            List<String> productLinks ;
            boolean used = false;

            try {
                productLinks = searchScraperService.scrapeAmazonOnUrl(searchUrl);

                // If no products found and we have price filters, try without price filters
                if (productLinks.isEmpty() && (criteria.getPriceMin() !=null || criteria.getPriceMax()!= null)) {
                    log.info("No products found with price filters, trying without price filters...");
                    SearchCriteria newCriteria = new SearchCriteria();
                    newCriteria.setKeywords(criteria.getKeywords());
                    newCriteria.setBrand(criteria.getBrand());
                    newCriteria.setSort(criteria.getSort());

                    String newUrl = buildSearchUrl(newCriteria, productDescription);
                    log.debug("Built fallback search URL: {}", newUrl);

                    try {
                        productLinks = searchScraperService.scrapeAmazonOnUrl(newUrl);
                        used =true;
                        log.info("Fallback search successful, found {} products", productLinks.size());
                    }
                    catch (ProductNotFound fallbackException) {
                        log.warn("Fallback search also failed: {}", fallbackException.getMessage());
                        throw new ProductNotFound("No products found even without price restrictions");
                    }
                }

            }
            catch (ProductNotFound e) {
                // If no products found and we have price filters, try without price filters
                if (criteria.getPriceMin() != null || criteria.getPriceMax() != null) {
                    log.info("No products found with price filters, trying without price filters...");
                    SearchCriteria newCriteria = new SearchCriteria();
                    newCriteria.setKeywords(criteria.getKeywords());
                    newCriteria.setBrand(criteria.getBrand());
                    newCriteria.setSort(criteria.getSort());

                    String newUrl = buildSearchUrl(newCriteria, productDescription);
                    log.debug("Built fallback search URL: {}", newUrl);

                    try {
                        productLinks = searchScraperService.scrapeAmazonOnUrl(newUrl);
                        used = true;
                        log.info("Fallback search successful, found {} products", productLinks.size());
                    }
                    catch (ProductNotFound error) {
                        log.warn("Fallback search also failed: {}", error.getMessage());
                        throw e;
                    }
                }
                else {
                    throw e; // Re-throw if no price filters were used
                }
            }

            Product bestProduct = findBestProduct(productLinks);

            if (bestProduct == null) {
                String message = used ?
                        "No suitable products found even without price restrictions" :
                        "No suitable products found for the given description";
                throw new ProductNotFound(message);
            }

            log.info("Successfully analyzed product: {}", bestProduct.getName());
            return bestProduct;

        }
        catch (Exception e) {
            log.error("Error analyzing product: ", e);
            throw new ScrapingException("Failed to analyze product: " + e.getMessage(), e);
        }
    }

    public Product analyzeLink(String link) {
        log.info("Starting product analysis for URL: {}", link);

        validateAmazonUrl(link);

        try {
            // Scrape the product
            Product product = scraperService.scrapeAmazonOnUrl(link);

            analyzeProductReviews(product);

            log.info("Successfully analyzed product from link: {}", product.getName());
            return product;

        }
        catch (Exception e) {
            log.error("Error analyzing product from link: ", e);
            throw new ScrapingException("Failed to analyze product from link: " + e.getMessage(), e);
        }
    }

    private void validateAmazonUrl(String link) {
        if (link == null || link.trim().isEmpty()) {
            throw new InvalidUrlException("URL cannot be null or empty");
        }

        if (!link.toLowerCase().contains("amazon")) {
            throw new InvalidUrlException("Only Amazon URLs are supported");
        }
    }

    private SearchCriteria parseProductDescription(String description) {
        try {
            String aiResponse = aiService.getSearchQueryResponse(description);
            log.debug("AI response for search criteria: {}", aiResponse);

            return parseSearchCriteria(aiResponse);

        }
        catch (Exception e) {
            log.error("Error parsing product description: ", e);
            throw new AiServiceException("Failed to parse product description:");
        }
    }

    private SearchCriteria parseSearchCriteria(String aiResponse) {
        SearchCriteria criteria = new SearchCriteria();

        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            log.warn("AI response is null or empty");
            return criteria;
        }

        log.debug("Parsing AI response: {}", aiResponse);

        String[] lines = aiResponse.trim().split("\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            log.debug("Processing line: '{}'", trimmedLine);

            if (trimmedLine.startsWith("keywords: ")) {
                String keywords = extractValue(trimmedLine);
                log.debug("Extracted keywords: '{}'", keywords);
                if (keywords != null && !keywords.isEmpty()) {
                    criteria.setKeywords(keywords);
                }
            }
            else if (trimmedLine.startsWith("price_max: ")) {
                Long priceMax = extractPriceValue(trimmedLine);
                log.debug("Extracted price_max: {}", priceMax);
                criteria.setPriceMax(priceMax);
            }
            else if (trimmedLine.startsWith("price_min: ")) {
                Long priceMin = extractPriceValue(trimmedLine);
                log.debug("Extracted price_min: {}", priceMin);
                criteria.setPriceMin(priceMin);
            }
            else if (trimmedLine.startsWith("brand: ")) {
                String brand = extractValue(trimmedLine);
                log.debug("Extracted brand: '{}'", brand);
                criteria.setBrand(brand);
            }
            else if (trimmedLine.startsWith("sort: ")) {
                String sort = extractValue(trimmedLine);
                log.debug("Extracted sort: '{}'", sort);
                criteria.setSort(sort);
            }
        }

        log.info("Parsed search criteria - keywords: '{}', price_min: {}, price_max: {}, brand: '{}', sort: '{}'",
                criteria.getKeywords(), criteria.getPriceMin(), criteria.getPriceMax(), criteria.getBrand(), criteria.getSort());

        return criteria;
    }

    private String extractValue(String line) {
        // Handle both formats: "keywords: value" and "Line 1: value"
        String[] parts = line.split(": ", 2);
        if (parts.length < 2) {
            return null;
        }
        String value = parts[1].trim();
        return "null".equalsIgnoreCase(value) ? null : value;
    }

    private Long extractPriceValue(String line) {
        String value = extractValue(line);
        if (value == null) return null;

        try {

            String cleanValue = value.replaceAll("[^0-9]", "");
            if (cleanValue.isEmpty()) {
                return null;
            }
            return Long.parseLong(cleanValue);
        }
        catch (NumberFormatException e) {
            log.warn("Invalid price value: {}", value);
            return null;
        }
    }

    private String buildSearchUrl(SearchCriteria criteria, String originalDescription) {
        StringBuilder url = new StringBuilder("https://www.amazon.in/s");

        // Add keywords - use original description as fallback if AI didn't provide keywords
        String keywords = criteria.getKeywords();
        if (keywords == null || keywords.isEmpty()) {
            log.warn("AI didn't provide keywords, using original description as fallback");
            keywords = originalDescription; // Use the original description as fallback
        }

        log.debug("Using keywords: '{}'", keywords);

        if (keywords != null && !keywords.isEmpty()) {
            url.append("?k=").append(URLEncoder.encode(keywords, StandardCharsets.UTF_8));
        }

        // Build refinements - only add if they make sense
        StringBuilder refinements = new StringBuilder();

        // Add price filter - only if the range is reasonable
        if (criteria.getPriceMin() != null || criteria.getPriceMax() != null) {
            long minPrice = criteria.getPriceMin() != null ? criteria.getPriceMin() : 0;
            long maxPrice = criteria.getPriceMax() != null ? criteria.getPriceMax() : 999999999;

            refinements.append("p_36:").append(minPrice).append("-").append(maxPrice);
            log.debug("Added price filter: p_36:{}-{}", minPrice, maxPrice);
        }

        // Add brand filter
        if (criteria.getBrand() != null && !"null".equals(criteria.getBrand())) {
            if (!refinements.isEmpty()) {
                refinements.append(",");
            }
            refinements.append("p_89:").append(URLEncoder.encode(criteria.getBrand(), StandardCharsets.UTF_8));
            log.debug("Added brand filter: p_89:{}", criteria.getBrand());
        }

        // Add refinements to URL
        if (!refinements.isEmpty()) {
            url.append("&rh=").append(refinements.toString());
        }

        // Add sort parameter - only if it's not the default
        if (criteria.getSort() != null && !"review-rank".equals(criteria.getSort())) {
            url.append("&s=").append(criteria.getSort());
            log.debug("Added sort parameter: s={}", criteria.getSort());
        }

        String finalUrl = url.toString();
        log.info("Built search URL: {}", finalUrl);
        return finalUrl;
    }

    private Product findBestProduct(List<String> productLinks) {
        List<Product> validProducts = new ArrayList<>();
        int count = 0;
        int max = 10;
        for (String link : productLinks) {
            try {
                Product product = scraperService.scrapeAmazonOnUrl(link);
                if(count>max) break;
                // Ensure rating is initialized
                if (product.getRating() == null) {
                    product.setRating(0.0);
                }
                
                // Analyze the product first to get rating and other analysis
                analyzeProductReviews(product);

                // Now check if the product is valid after analysis
                System.out.println(product.getRating());
                if (product.isValid() && product.getRating()!=0.0) {
                    validProducts.add(product);
                    count++;
                    log.debug("Added valid product: {} with rating: {}", product.getName(), product.getRating());
                } else {
                    log.debug("Product failed validation: {} (name: {}, rating: {})",
                            product.getName(), product.getName() != null, product.getRating());
                }
            } catch (Exception e) {
                log.warn("Failed to analyze product at {}: {}", link, e.getMessage());
            }
        }

        log.info("Found {} valid products out of {} links", validProducts.size(), productLinks.size());

        if (validProducts.isEmpty()) {
            return null;
        }

        // Sort by rating and return the best one
        validProducts.sort((p1, p2) -> {
            Double rating1 = p1.getRating();
            Double rating2 = p2.getRating();
            
            // Handle null ratings by treating them as 0.0
            if (rating1 == null) rating1 = 0.0;
            if (rating2 == null) rating2 = 0.0;
            
            return Double.compare(rating2, rating1);
        });
        for(Product product : validProducts){
            System.out.println(product);
        }

        Product bestProduct = validProducts.getFirst();
        log.info("Selected best product: {} with rating: {}", bestProduct.getName(), bestProduct.getRating());
        return bestProduct;
    }

    private void analyzeProductReviews(Product product) {
        try {
            List<String> reviews = product.getPros();
            log.info("Analyzing product: {} with {} reviews", product.getName(),
                    reviews != null ? reviews.size() : 0);

            if (reviews == null || reviews.isEmpty() || reviews.getFirst().equals("No reviews found")) {
                log.warn("No reviews found for product: {}, setting default analysis", product.getName());
                setDefaultAnalysis(product);
                return;
            }

            String reviewsText = String.join("\n", reviews);
            log.info("Sending {} reviews to AI for analysis", reviews.size());
            log.debug("Reviews text: {}", reviewsText);

            String aiResponse = aiService.getProductAnalysisResponse(reviewsText);
            log.info("Received AI analysis response for product: {}", product.getName());
            log.debug("AI response: {}", aiResponse);

            parseAnalysisResponse(product, aiResponse);
            log.info("Successfully analyzed product: {} with rating: {}", product.getName(), product.getRating());

        }
        catch (Exception e) {
            log.error("Error analyzing product reviews for {}: ", product.getName(), e);
            setDefaultAnalysis(product);
        }
    }

    private void parseAnalysisResponse(Product product, String aiResponse) {
        log.info("Parsing AI response: {}", aiResponse);
        
        String[] sections = aiResponse.split("(?=PROS:|CONS:|VERDICT:|RATING:)");
        log.info("Found {} sections in AI response", sections.length);

        for (String section : sections) {
            String trimmedSection = section.trim();
            log.debug("Processing section: {}", trimmedSection);

            if (trimmedSection.startsWith("PROS: ")) {
                List<String> pros = parseListItems(trimmedSection.replace("PROS: ", "").trim());
                product.setPros(pros);
                log.info("Set pros: {}", pros);
            }
            else if (trimmedSection.startsWith("CONS: ")) {
                List<String> cons = parseListItems(trimmedSection.replace("CONS: ", "").trim());
                product.setCons(cons);
                log.info("Set cons: {}", cons);
            }
            else if (trimmedSection.startsWith("VERDICT: ")) {
                String verdict = trimmedSection.replace("VERDICT: ", "").trim();
                product.setVerdict(verdict);
                log.info("Set verdict: {}", verdict);
            }
            else if (trimmedSection.startsWith("RATING: ")) {
                try {
                    String ratingStr = trimmedSection.replace("RATING: ", "").trim();
                    Double rating = Double.parseDouble(ratingStr);
                    product.setRating(rating);
                    log.info("Set rating: {}", rating);
                }
                catch (NumberFormatException e) {
                    log.warn("Invalid rating value: {}", trimmedSection);
                    product.setRating(0.0);
                }
            }
        }
    }

    private List<String> parseListItems(String text) {
        List<String> items = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("-") || trimmedLine.startsWith("*") || trimmedLine.startsWith("•")) {
                trimmedLine = trimmedLine.substring(1).trim();
            }
            else if (trimmedLine.matches("^\\d+\\..*")) {
                trimmedLine = trimmedLine.replaceFirst("^\\d+\\.", "").trim();
            }

            if (!trimmedLine.isEmpty()) {
                items.add(trimmedLine);
            }
        }

        return items.isEmpty() ? List.of("No items found") : items;
    }

    private void setDefaultAnalysis(Product product) {
        product.setPros(List.of("No reviews available"));
        product.setCons(List.of("No reviews available"));
        product.setVerdict("Unable to provide analysis due to lack of reviews");
        product.setRating(0.0);
    }

    // Inner class to hold search criteria
    private static class SearchCriteria {
        private String keywords;
        private Long priceMin;
        private Long priceMax;
        private String brand;
        private String sort;

        // Getters and setters
        public String getKeywords() { return keywords; }
        public void setKeywords(String keywords) { this.keywords = keywords; }

        public Long getPriceMin() { return priceMin; }
        public void setPriceMin(Long priceMin) { this.priceMin = priceMin; }

        public Long getPriceMax() { return priceMax; }
        public void setPriceMax(Long priceMax) { this.priceMax = priceMax; }

        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }

        public String getSort() { return sort; }
        public void setSort(String sort) { this.sort = sort; }
    }
}
