package com.tca.service;

import com.tca.model.Product;

import java.util.List;

public interface ProductService {


    public List<Product> findAll();
    public Product save(Product product);
    public List<Product> findExpensiveProduct(Double price);
}
