package com.licenta.horeca.product.repository;

import com.licenta.horeca.product.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByAvailableTrue();

    List<Product> findByCategoryId(Long categoryId);
}