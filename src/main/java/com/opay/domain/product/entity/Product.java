package com.opay.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)   
    private Long price;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(length = 50)
    private String category;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Product(String name, String description, Long price, Integer stock, 
                   String category, String imageUrl, Double averageRating, Integer reviewCount) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock != null ? stock : 0;
        this.category = category;
        this.imageUrl = imageUrl;
        this.averageRating = averageRating != null ? averageRating : 0.0;
        this.reviewCount = reviewCount != null ? reviewCount : 0;
    }

    public void update(String name, String description, Long price, Integer stock, 
                      String category, String imageUrl) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (price != null) this.price = price;
        if (stock != null) this.stock = stock;
        if (category != null) this.category = category;
        if (imageUrl != null) this.imageUrl = imageUrl;
    }

    public void updateStock(Integer quantity) {
        if (this.stock + quantity < 0) {
            throw new IllegalArgumentException("재고가 부족합니다");
        }
        this.stock += quantity;
    }

    public void updateRating(Double averageRating, Integer reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }
}
