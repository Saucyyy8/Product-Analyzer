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


    public List<String> scrapeAmazonOnUrl(String url){

        log.info("Starting to Scrape Products from url : {}",url);
        validateUrl(url);
        try{
            ;
            driver.get(url);
            try{
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int durationToWait = 10;
            WebDriverWait webDriverWait = new WebDriverWait(driver, Duration.ofSeconds(durationToWait));

            List<String> productLinks = extractProductLinks();
            log.info("Size of extracted product links : {}",productLinks.size());
            return productLinks;

        }
        catch (Exception e){
            log.error("Error in scraping products : ",e);
            throw new ScrapingException("Error in scraping products", e);
        }
    }
    public void validateUrl(String url){
        try{
            URL urlToParse = new URL(url);
            String host = urlToParse.getHost().toLowerCase();
            log.info("This is the url : {} and this is the host : {}",url,host);
            Set<String> amazonUrls = new HashSet<>();
            amazonUrls.add("www.amazon.in");
            amazonUrls.add("www.amazon.com");
            if(!amazonUrls.contains(host)){
                throw new InvalidUrlException("Invalid Url");
            }
        }
        catch(Exception e){
            log.error("Error in parsing url, invalid url : ",e);
            throw new InvalidUrlException("Invalid Url"+ e.getMessage());
        }

    }
    public List<String> extractProductLinks(){
        try{
            List<WebElement> elements1 = driver.findElements(
                    By.cssSelector(".a-link-normal.s-line-clamp-3.s-link-style.a-text-normal")
            );
            log.debug("Found {} elements with primary selector", elements1.size());
            Set<WebElement> productElements = new HashSet<>(elements1);

            log.debug("Total found {} product elements", productElements.size());
            List<String> productLinks = new ArrayList<>();
            int nullLinks = 0;
            int count = 0;

            for(WebElement element: productElements){
                String href = element.getDomAttribute("href");
                if(href!= null && !href.trim().isEmpty()){
                    String productUrl =completeUrl(href);
                    if(productUrl.contains("/dp/")){
                        productLinks.add(productUrl);
                        count++;
                    }
                    else nullLinks++;
                }
                else nullLinks++;
            }
            log.info("Processed {} products, found {} valid links, {} null hrefs",
                    count, productLinks.size(), nullLinks);

            if (productLinks.isEmpty()) {
                log.warn("No product links found on the search page - returning empty list");
                return productLinks;
            }

            return productLinks;
        }
        catch(Exception e) {
            log.error("Product Not Found on search page :",e);
            throw new ProductNotFound("No products found on the search page");
        }


    }

    public String completeUrl(String url){
        if(url.startsWith("http")){
            return url;
        }
        return "https://www.amazon.in"+url;
    }

}
