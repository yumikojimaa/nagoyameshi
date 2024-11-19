package com.example.nagoyameshi.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.nagoyameshi.entity.Category;
import com.example.nagoyameshi.entity.Restaurant;
import com.example.nagoyameshi.entity.Review;
import com.example.nagoyameshi.entity.User;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
	 Review getReferenceById(Review review);
	 public Page<Review> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
	 Page<Review> findByRestaurantId(Integer restaurantId, Pageable pageable);
	 Page<Review> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant, Pageable pageable);
	 Page<Review> findByRestaurantCategory(Category category, Pageable pageable);
	 Page<Review> findByRestaurantLowestPriceLessThanEqual(Integer price, Pageable pageable);
	 List<Review> findByRestaurantCategory(Category category);
	 List<Review> findByRestaurantLowestPriceLessThanEqual(Integer price);
}