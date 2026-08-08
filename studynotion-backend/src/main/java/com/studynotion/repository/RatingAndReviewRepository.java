package com.studynotion.repository;

import com.studynotion.entity.RatingAndReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatingAndReviewRepository extends JpaRepository<RatingAndReview, Long> {

    Optional<RatingAndReview> findByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT AVG(r.rating) FROM RatingAndReview r WHERE r.course.id = :courseId")
    Optional<Double> findAverageRatingByCourseId(@Param("courseId") Long courseId);

    List<RatingAndReview> findByCourseId(Long courseId);

    @Query("SELECT r FROM RatingAndReview r ORDER BY r.rating DESC")
    List<RatingAndReview> findTopRatings();
}
