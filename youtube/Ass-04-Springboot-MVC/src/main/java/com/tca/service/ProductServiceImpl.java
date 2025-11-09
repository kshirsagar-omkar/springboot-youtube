package com.tca.service;

import com.tca.dao.ProductDao;
import com.tca.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService{


    @Autowired
    private ProductDao productDao;

    public List<Product> findAll(){
        return productDao.findAll();
    }

    @Override
    public Product save(Product product) {
        return productDao.save(product);
    }

    @Override
    public List<Product> findExpensiveProduct(Double price){
        return productDao.findExpensiveProduct(price);
    }
}
