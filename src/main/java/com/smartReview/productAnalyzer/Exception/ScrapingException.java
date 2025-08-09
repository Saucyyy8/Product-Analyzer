package com.smartReview.productAnalyzer.Exception;

public class ScrapingException extends RuntimeException{
    public ScrapingException(String errorInScrapingProducts, Exception e) {
        super(errorInScrapingProducts);
    }
}
