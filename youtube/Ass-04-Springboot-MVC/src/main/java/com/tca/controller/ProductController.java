package com.tca.controller;

import com.tca.model.Product;
import com.tca.service.ProductService;
import com.tca.service.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/products")
    public List<Product> productsList(){
        return productService.findAll();
    }


    @PostMapping("/addproduct")
    public Product saveProduct(@RequestBody Product product){
        return productService.save(product);
    }


    @GetMapping("products/expensive/{price}")
    public List<Product> findExpensiveProduct(@PathVariable Double price){
        return productService.findExpensiveProduct(price);
    }
}
