package com.tca.service;

import com.tca.dao.ProductDao;
import com.tca.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ProductService {


    @Autowired
    private ProductDao productDao;

    public List<Product> getAllProduct(){
        return productDao.getAllProduct();
    }
}
