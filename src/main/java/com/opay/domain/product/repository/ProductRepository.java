package com.opay.domain.product.repository;

import com.opay.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // 카테고리로 검색
    Page<Product> findByCategory(String category, Pageable pageable);
    
    // 이름으로 검색 (부분 일치)
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    // 설명으로 검색 (부분 일치)
    Page<Product> findByDescriptionContainingIgnoreCase(String description, Pageable pageable);
    
    // 이름 또는 설명으로 검색
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 카테고리와 키워드로 검색
    @Query("SELECT p FROM Product p WHERE p.category = :category " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByCategoryAndKeyword(@Param("category") String category, 
                                            @Param("keyword") String keyword, 
                                            Pageable pageable);
    
    // 가격 범위로 검색
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceBetween(@Param("minPrice") Long minPrice, 
                                     @Param("maxPrice") Long maxPrice, 
                                     Pageable pageable);
    
    // 재고가 있는 상품만 조회
    Page<Product> findByStockGreaterThan(Integer stock, Pageable pageable);
    
    // 카테고리 목록 조회
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL ORDER BY p.category")
    List<String> findAllCategories();
}
