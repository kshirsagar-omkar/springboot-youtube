package com.org.stem_project.service;

import com.org.stem_project.model.Product;
import com.org.stem_project.repository.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImp implements ProductService {

    @Autowired
    private ProductRepo productRepo;

    @Override
    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        return productRepo.findById(id);
    }

    @Override
    public Product createProduct(Product product) {
        return productRepo.save(product);
    }

    @Override
    public Product updateProduct(Long id, Product product) {
        if (productRepo.existsById(id)) {
            product.setId(id);
            return productRepo.save(product);
        }
        throw new RuntimeException("Product not found with id: " + id);
    }

    @Override
    public void deleteProduct(Long id) {
        if (!productRepo.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepo.deleteById(id);
    }

    @Override
    public List<Product> getProductsByCategory(String category) {
        return productRepo.findByCategory(category);
    }
}

