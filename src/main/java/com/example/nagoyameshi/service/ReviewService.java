package com.example.nagoyameshi.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nagoyameshi.entity.Category;
import com.example.nagoyameshi.entity.Restaurant;
import com.example.nagoyameshi.entity.Review;
import com.example.nagoyameshi.entity.User;
import com.example.nagoyameshi.form.EditForm;
import com.example.nagoyameshi.form.RegisterForm;
import com.example.nagoyameshi.repository.RestaurantRepository;
import com.example.nagoyameshi.repository.ReviewRepository;
import com.example.nagoyameshi.repository.UserRepository;

@Service
public class ReviewService {
	private Logger logger  = LoggerFactory.getLogger(ReviewService.class);
	private final ReviewRepository reviewRepository;
	private final RestaurantRepository restaurantRepository;
	private final UserRepository userRepository;
	private record RestaurantOrder(int order, Restaurant restaurant) {} 

	public ReviewService(ReviewRepository reviewRepository, RestaurantRepository restaurantRepository,
			UserRepository userRepository) {
		this.reviewRepository = reviewRepository;
		this.restaurantRepository = restaurantRepository;
		this.userRepository = userRepository;
	}
	
	//新規レビューをDBに保存
	@Transactional
	public void create(RegisterForm RegisterForm) {
		Review review = new Review();
		Restaurant restaurant = restaurantRepository.getReferenceById(RegisterForm.getRestaurantId());
		User user = userRepository.getReferenceById(RegisterForm.getUserId());
		review.setRestaurant(restaurant);
		review.setUser(user);
		review.setStar(RegisterForm.getStar());
		review.setReview(RegisterForm.getReview());
		reviewRepository.save(review);
	}

	//レビューの変更を保存
	@Transactional
	public void update(EditForm EditForm) {
		Review review = reviewRepository.getReferenceById(EditForm.getId());
		review.setStar(EditForm.getStar());
		review.setReview(EditForm.getReview());
		reviewRepository.save(review);
	}

	// レストランをレビュー評価の高い順に取得する
	public Page<Restaurant> getRestaurantByReviewOrder(Pageable pageable) {
		var restaurants = sortedByStar(reviewRepository.findAll());
		return new PageImpl<Restaurant>(restaurants, pageable, restaurants.size());
	}
	// カテゴリに該当するレストランをレビュー評価の高い順に取得する
	public Page<Restaurant> getRestaurantByReviewOrder(Category category, Pageable pageable) {
		var restaurants = sortedByStar(reviewRepository.findByRestaurantCategory(category));
		return new PageImpl<Restaurant>(restaurants, pageable, restaurants.size());
	}
	private List<Restaurant> sortedByStar(List<Review> records) {
		var reviews = records.stream().collect(Collectors.groupingBy(it -> it.getRestaurant().getId())); // レストランのID毎にグルーピング
		var temps = reviews.entrySet().stream().map(e -> {
			var totalStar = e.getValue().stream().map(v -> Integer.valueOf(v.getStar())).reduce(0, Integer::sum); // スター数を算出
			var rate = totalStar / e.getValue().size(); // スター数の平均を算出（要変更）
			logger.info("order: {} {}",rate, e.getValue().get(0).getRestaurant().getName());
			return new RestaurantOrder(rate, e.getValue().get(0).getRestaurant()); // 平均、レストランのレコードを作成
		}).sorted(Comparator.comparing(RestaurantOrder::order).reversed()); // 平均を降順にソート
		return temps
				.map(it -> it.restaurant) // レストランのみに変形
				.collect(Collectors.toList());
	}

}

    