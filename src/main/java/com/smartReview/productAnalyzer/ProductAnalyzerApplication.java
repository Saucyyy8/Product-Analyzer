package com.smartReview.productAnalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication

@EnableCaching
public class ProductAnalyzerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductAnalyzerApplication.class, args);
	}

}
