package com.tca.dao;

import com.tca.model.Product;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class ProductDao {

    private List<Product> list = Arrays.asList(
            new Product(101,"Samsung",80000.0),
            new Product(102,"Apple",150000.0),
            new Product(103,"Redmi",40000.0)
    );



    public List<Product> getAllProduct(){
        return list;
    }

}
