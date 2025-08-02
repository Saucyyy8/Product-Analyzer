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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AmazonScraperService {
    @Autowired
    private Product product;

    @Autowired
    private WebDriver driver;

    public Product scrapeAmazonOnUrl(String url){

        try{
            driver.get(url);
            Thread.sleep(10);

            try {
                String title = driver.findElement(By.cssSelector("span.a-size-large.product-title-word-break")).getText().trim();
                product.setProductName(title);
                product.setProductLink(url);
            } catch (Exception e) {
                throw new ProductNotFound("Product name not found");
            }

            try {
                String price = driver.findElement(By.cssSelector("span.a-price-whole")).getText().trim();
                product.setProductPrice(price);
            }
            catch (Exception e) {
                throw new ProductNotFound("Product price not found");
            }

            try {
                List<WebElement> reviewElements = driver.findElements(
                        By.cssSelector("div.a-expander-content.reviewText.review-text-content.a-expander-partial-collapse-content")
                );
                List<String> reviews = new ArrayList<>();
                for (WebElement el : reviewElements) {
                    reviews.add(el.getText().trim());
                }
                if(!reviews.isEmpty()) product.setProductPros(reviews);
                else product.setProductPros(new ArrayList<>(List.of("Not found")));

            }
            catch (Exception e) {
                product.setProductPros(Collections.singletonList("Not found"));
            }

        }
        catch (Exception e) {
            throw new ProductNotFound("Product not found");
        }


        return product;
    }
}
