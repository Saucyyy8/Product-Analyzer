package com.smartReview.productAnalyzer.Service;

import com.smartReview.productAnalyzer.Exception.ProductNotFound;
import com.smartReview.productAnalyzer.Model.Product;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AmazonSearchPageScraper {

    @Autowired
    private Product product;

    @Autowired
    private WebDriver driver;

    public List<String> scrapeAmazonOnUrl(String url){

        try{
            driver.get(url);
            Thread.sleep(500);
            try {
                List<WebElement> productElements = driver.findElements(
                        By.cssSelector("a.a-link-normal.s-no-outline")
                );

                List<String> productLinks = new ArrayList<>();
                System.out.println("Product links:"+productElements.size());
                for (int i = 0; i < Math.min(10, productElements.size()); i++) {
                    WebElement element = productElements.get(i);
                    String href = element.getDomAttribute("href");
                    if (href != null) {
                        if (!href.startsWith("https://")) {
                            href = "https://www.amazon.in/" + href;
                        }
                        System.out.println("href : " + href);
                        productLinks.add(href);
                    }
                }
                return productLinks;
            }
            catch(Exception e) {
                throw new ProductNotFound("No products found");
            }
        }
        catch (Exception e) {
            throw new ProductNotFound("No products found");
        }
    }
}
