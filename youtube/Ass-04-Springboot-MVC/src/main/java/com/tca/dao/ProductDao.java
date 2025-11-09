package com.tca.dao;

import com.tca.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductDao extends JpaRepository<Product, Integer> {


    @Query("SELECT p FROM Product p WHERE p.price >= :price")
    public List<Product> findExpensiveProduct(Double price);

}
