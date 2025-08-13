package com.smartReview.productAnalyzer.Service;

import com.smartReview.productAnalyzer.Exception.ProductNotFound;
import com.smartReview.productAnalyzer.Exception.ScrapingException;
import com.smartReview.productAnalyzer.Model.Product;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class AmazonScraperService {

    private final WebDriver webDriver;


    @Autowired
    public AmazonScraperService(WebDriver webDriver){
        this.webDriver = webDriver;
    }

    public Product scrapeAmazonOnUrl(String url){
        log.info("Starting to scrape product from URL: {}", url);

        Product product = Product.builder()
                .url(url)
                .build();

        try{
            // Navigate to the URL first
            webDriver.get(url);
            log.info("Navigated to URL: {}", url);
            

            
            WebDriverWait wait = new WebDriverWait(webDriver,Duration.ofSeconds(10));
            product.setName(extractProductName(wait));

            product.setPrice(extractProductPrice(wait));

            List<String> reviews = extractProductReviews(wait);
            product.setPros(reviews);
            log.info("Successfully Scraped Product : {}",product.getName());
            return product;

        }
        catch (Exception e){
            log.error("Error in scraping product",e);
            throw new ScrapingException("Error in Scraping product", e);
        }
    }

    private String extractProductPrice(WebDriverWait wait) {
        try {
            // Try multiple CSS selectors for price
            String[] priceSelectors = {
                "span.a-price-whole"
            };
            
            for (String selector : priceSelectors) {
                try {
                    WebElement priceElement = webDriver.findElement(By.cssSelector(selector));
                    String price = priceElement.getText().trim();
                    if (!price.isEmpty()) {
                        // Clean up the price text
                        price = price.replaceAll("[^0-9,]", "").replace(",", "");
                        String priceWithCurrency = price + " INR";
                        log.debug("Extracted product price: {}", priceWithCurrency);
                        return priceWithCurrency;
                    }
                }
                catch (Exception e) {
                    log.debug("Price selector {} failed, trying next", selector);
                }
            }
            
            log.error("All price selectors failed");
            return null;
        }
        catch (Exception e) {
            log.error("Failed to extract product price: ", e);
            return null;
        }
    }

    private List<String> extractProductReviews(WebDriverWait wait) {
        try {
            //List<String> reviews = new ArrayList<>();
            log.info("Starting review extraction for URL: {}", webDriver.getCurrentUrl());
            
            // Simple approach: try to extract any text that looks like a review
            Set<String> reviewSet = new HashSet<>();
            extractReviewsSimple(wait, reviewSet);
            //extractReviewsFromReviewsPage(wait,reviewSet);

            List<String> reviews = new ArrayList<>(reviewSet);
            if (!reviewSet.isEmpty()) {
                log.info("Found {} reviews with simple method", reviewSet.size());
                return reviews;
            }

            log.warn("No reviews found for product");
            return Collections.singletonList("No reviews found");

        }
        catch (Exception e) {
            log.error("Failed to extract product reviews: ", e);
            return Collections.singletonList("Failed to extract reviews");
        }
    }

//    private void extractReviewsFromReviewsPage(WebDriverWait wait, Set<String> reviews) {
//        //List<String> reviews = new ArrayList<>();
//
//        log.info("Extracting reviews from reviews page: {}", webDriver.getCurrentUrl());
//
//        // Selectors for reviews on the reviews page
//        String[] reviewSelectors = {
//            "div.a-expander-content.reviewText.review-text-content span",
//            "div[data-hook='review-collapsed'] span",
//            "div.review-text-content span",
//            "span[data-hook='review-body']",
//            "div[data-hook='review'] div[data-hook='review-body']",
//            "div.review-text-content",
//            "div.a-expander-content.reviewText.review-text-content",
//            "div[data-hook='review'] .review-text-content span"
//        };
//
//        for (String selector : reviewSelectors) {
//            try {
//                List<WebElement> reviewElements = webDriver.findElements(By.cssSelector(selector));
//                log.info("Found {} review elements with selector: {}", reviewElements.size(), selector);
//
//                for (WebElement element : reviewElements) {
//                    String reviewText = element.getText().trim();
//                    if (isValidReview(reviewText)) {
//                        reviews.add(reviewText);
//                        log.debug("Added review: {}", reviewText);
//                    }
//                }
//
//                if (!reviews.isEmpty()) {
//                    log.info("Extracted {} reviews from reviews page using selector: {}", reviews.size(), selector);
//                    return;
//                }
//            }
//            catch (Exception e) {
//                log.debug("Review selector {} failed on reviews page, trying next", selector);
//            }
//        }
//
//    }

    private void extractReviewsSimple(WebDriverWait wait, Set<String> reviews) {


        log.info("Using simple method to extract reviews");

        // Try multiple approaches to find reviews
        try {
            // Method 1: Look for review containers
            String[] reviewSelectors = {
                    "div.a-expander-content.reviewText.review-text-content.a-expander-partial-collapse-content"
            };

            for (String selector : reviewSelectors) {
                try {
                    List<WebElement> elements = webDriver.findElements(By.cssSelector(selector));
                    log.info("Found {} elements with selector: {}", elements.size(), selector);

                    for (WebElement element : elements) {
                        String text = element.getText().trim();
                            if(isValidReview(text)){
                                reviews.add(text);
                                log.debug("Added review: {}", text);
                            }

                    }

                    if (!reviews.isEmpty()) {
                        log.info("Found {} reviews with selector: {}", reviews.size(), selector);
                        return;
                    }
                }
                catch (Exception e) {
                    log.debug("Selector {} failed", selector);
                }
            }

        }
        catch (Exception e) {
            log.error("Simple review extraction failed", e);
        }

    }

    private boolean isValidReview(String text) {
        return text.length() > 10 &&
                text.length() < 2000 &&
                !text.contains("Verified Purchase") &&
                !text.contains("Helpful") &&
                !text.contains("Report") &&
                !text.contains("stars") &&
                !text.contains("out of 5") &&
                !text.contains("customer reviews") &&
                !text.contains("See all") &&
                !text.contains("Write a review") ;

    }
    
    private @NotBlank(message = "Product name is required") String extractProductName(WebDriverWait wait) {
        try{
            // Try multiple CSS selectors for product name
            String[] nameSelectors = {
                "span.a-size-large.product-title-word-break"
            };
            
            for (String selector : nameSelectors) {
                try {
                    WebElement titleElement = webDriver.findElement(By.cssSelector(selector));
                    String name = titleElement.getText().trim();
                    if (!name.isEmpty()) {
                        log.info("Got product Name: {}", name);
                        return name;
                    }
                }
                catch (Exception e) {
                    log.debug("Name selector {} failed, trying next", selector);
                }
            }
            
            log.error("All name selectors failed");
            throw new ProductNotFound("Product Name not found");
        }
        catch (Exception e){
            throw new ProductNotFound("Product Name not found");
        }
    }
}
