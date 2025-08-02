package com.smartReview.productAnalyzer.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
@Data
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String productName;
    private String productPrice;
    private String productLink;
    private List<String> productPros;
    private List<String> productCons;
    private String verdict;
    private double rating;

}
