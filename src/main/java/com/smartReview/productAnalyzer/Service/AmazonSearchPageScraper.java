package com.smartReview.productAnalyzer.Service;

import com.smartReview.productAnalyzer.Exception.InvalidUrlException;
import com.smartReview.productAnalyzer.Exception.ProductNotFound;
import com.smartReview.productAnalyzer.Exception.ScrapingException;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class AmazonSearchPageScraper {

    private final WebDriver driver;

    public AmazonSearchPageScraper(WebDriver driver) {
        this.driver = driver;
    }


    public List<String> scrapeAmazonOnUrl(String url) {

        log.info("Starting to Scrape Products from url : {}", url);
        validateUrl(url);
        try {
            ;
            driver.get(url);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int durationToWait = 10;
            WebDriverWait webDriverWait = new WebDriverWait(driver, Duration.ofSeconds(durationToWait));

            List<String> productLinks = extractProductLinks();
            log.info("Size of extracted product links : {}", productLinks.size());
            return productLinks;

        } catch (Exception e) {
            log.error("Error in scraping products : ", e);
            throw new ScrapingException("Error in scraping products", e);
        }
    }

    public void validateUrl(String url) {
        try {
            URL urlToParse = new URL(url);
            String host = urlToParse.getHost().toLowerCase();
            log.info("This is the url : {} and this is the host : {}", url, host);
            Set<String> amazonUrls = new HashSet<>();
            amazonUrls.add("www.amazon.in");
            amazonUrls.add("www.amazon.com");
            if (!amazonUrls.contains(host)) {
                throw new InvalidUrlException("Invalid Url");
            }
        } catch (Exception e) {
            log.error("Error in parsing url, invalid url : ", e);
            throw new InvalidUrlException("Invalid Url" + e.getMessage());
        }

    }

    public List<String> extractProductLinks() {
        // Corrected, simplified, and combined CSS selectors
        String[] selectors = {
                "a.a-link-normal.s-underline-text.s-underline-link-text.s-link-style.a-text-normal", // Primary selector for most search results

                "a-link-normal s-line-clamp-2 s-line-clamp-3-for-col-12 s-link-style a-text-normal",
                ".a-link-normal.s-line-clamp-3.s-link-style.a-text-normal",
                "a.a-link-normal.s-no-outline"

        };

        List<String> productLinks = new ArrayList<>();

        for (String selector : selectors) {
            try {
                log.debug("Trying selector: {}", selector);
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                log.debug("Found {} elements with selector '{}'", elements.size(), selector);

                for (WebElement element : elements) {
                    String href = element.getAttribute("href"); // Use getAttribute for href
                    if (href != null && !href.trim().isEmpty()) {
                        String productUrl = completeUrl(href);
                        // Ensure it's a product link and not a duplicate
                        if (productUrl.contains("/dp/") && !productLinks.contains(productUrl)) {
                            productLinks.add(productUrl);
                        }
                    }
                }

                // If we've found links with the primary selector, we can stop
                if (!productLinks.isEmpty()) {
                    log.info("Found {} valid product links. Returning results.", productLinks.size());
                    return productLinks;
                }

            } catch (Exception e) {
                log.warn("Error processing selector '{}': {}", selector, e.getMessage());
                // Don't re-throw here; allow the loop to try the next selector
            }
        }

        // After trying all selectors, if the list is still empty, then throw an exception
        if (productLinks.isEmpty()) {
            log.error("Product Not Found: No product links found on the search page after trying all selectors.");
            throw new ProductNotFound("No products found on the search page");
        }

        return productLinks; // Should not be reached if links are found earlier, but good practice
    }

    private String completeUrl(String href) {
        if (href.startsWith("http")) {
            return href;
        }
        return "https://www.amazon.in" + href;
    }

}
//String[] selectors = {};