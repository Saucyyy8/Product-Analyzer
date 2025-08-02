package com.smartReview.productAnalyzer.Controller;

import com.smartReview.productAnalyzer.Model.Product;
import com.smartReview.productAnalyzer.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService service;

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Suhas!";
    }

    @PostMapping("/analyze")
    public Product linkSummarizer(@RequestBody String link){

        return service.analyzeLink(link);

    }
    @PostMapping("/recommend")
    public Product productInfo(@RequestBody String input){

        return service.analyzeProduct(input);
    }
}
