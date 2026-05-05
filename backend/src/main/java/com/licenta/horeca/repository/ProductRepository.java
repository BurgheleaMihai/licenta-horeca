package com.licenta.horeca.repository;

import com.licenta.horeca.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByAvailableTrue();

    List<Product> findByCategoryId(Long categoryId);
}