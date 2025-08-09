package com.smartReview.productAnalyzer.Controller;

import com.smartReview.productAnalyzer.Model.Product;
import com.smartReview.productAnalyzer.Model.ProductAnalysisRequest;
import com.smartReview.productAnalyzer.Service.ProductService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Builder
@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {


    private final ProductService service;

    @Autowired
    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("health")
    public ResponseEntity<String> healthCheck(){
        log.info("Health Check endpoint called");
        return new ResponseEntity<>("Smart Product Analyzer Running", HttpStatus.OK);
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeProduct(@Valid @RequestBody ProductAnalysisRequest request){
        log.info("Analyze endpoint called for {}",request);
        try{
            Product productResult;
            if(request.isUrl()){
                productResult = service.analyzeLink(request.getInput());
            }
            else productResult = service.analyzeProduct(request.getInput());
            log.info("Analyzed Product with response : {}", productResult);
            return ResponseEntity.ok(productResult);
        }
        catch(Exception e){
            log.error("Error found during Product Analysis in /analyze endpoint", e);
            Map<String, Object> errorFormat = new HashMap<>();
            errorFormat.put("timestamp: ",LocalDateTime.now());
            errorFormat.put("HttpStatus :", 505);
            errorFormat.put("error :", "Internal Server Error");
            errorFormat.put("message :", e);
            return new ResponseEntity<>(errorFormat,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/analyze/url")
    public ResponseEntity<?> analyzeProductUrl(@Valid @RequestBody ProductAnalysisRequest request) {
        log.info("Product URL analysis request received: {}", request.getInput());

        try {
            Product result = service.analyzeLink(request.getInput());
            log.info("Product URL analysis completed successfully for: {}", result.getName());
            return ResponseEntity.ok(result);

        }
        catch (Exception e) {
            log.error("Error found during Product Analysis in /analyze/url endpoint", e);
            Map<String, Object> errorFormat = new HashMap<>();
            errorFormat.put("timestamp: ",LocalDateTime.now());
            errorFormat.put("HttpStatus :", 505);
            errorFormat.put("error :", "Internal Server Error");
            errorFormat.put("message :", e);
            return new ResponseEntity<>(errorFormat,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/analyze/description")
    public ResponseEntity<?> analyzeProductDescription(@Valid @RequestBody ProductAnalysisRequest request) {
        log.info("Product description analysis request received: {}", request.getInput());

        try {
            Product result = service.analyzeProduct(request.getInput());
            log.info("Product description analysis completed successfully for: {}", result.getName());
            return ResponseEntity.ok(result);

        }
        catch (Exception e) {
            log.error("Error found during Product Analysis in /analyze/description endpoint", e);
            Map<String, Object> errorFormat = new HashMap<>();
            errorFormat.put("timestamp: ",LocalDateTime.now());
            errorFormat.put("HttpStatus :", 505);
            errorFormat.put("error :", "Internal Server Error");
            errorFormat.put("message :", e);
            return new ResponseEntity<>(errorFormat,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
