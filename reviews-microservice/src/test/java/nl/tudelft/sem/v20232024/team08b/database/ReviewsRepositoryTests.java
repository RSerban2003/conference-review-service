package nl.tudelft.sem.v20232024.team08b.database;

import nl.tudelft.sem.v20232024.team08b.domain.Review;
import nl.tudelft.sem.v20232024.team08b.domain.ReviewID;
import nl.tudelft.sem.v20232024.team08b.repos.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

@DataJpaTest
public class ReviewsRepositoryTests {
    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    public void test() {
        Review review = new Review();
        ReviewID reviewId = new ReviewID(10L, 2L);
        review.setReviewID(reviewId);
        reviewRepository.save(review);

        Optional<Review> got = reviewRepository.findById(reviewId);
        assertThat(got).isNotEmpty();
        if (got.isPresent()) {
            assertThat(got.get().getReviewID()).isEqualTo(reviewId);
        } else {
            fail("Got is not present");
        }

    }

    @Test
    public void testCustomGetterByPartialID() {
        // Create fake review 1
        Review review1 = new Review();
        ReviewID reviewId1 = new ReviewID(10L, 2L);
        review1.setReviewID(reviewId1);

        // Create fake review 2
        Review review2 = new Review();
        ReviewID reviewId2 = new ReviewID(11L, 2L);
        review2.setReviewID(reviewId2);

        // Add them to repo
        reviewRepository.save(review1);
        reviewRepository.save(review2);

        // Get them back using the custom method and assert behaviour
        List<Review> got = reviewRepository.findByReviewIDPaperID(10L);
        assertThat(got).isEqualTo(List.of(review1));
    }

    @Test
    public void testIsReviewerByPaper() {
        // Create fake review 1
        Review review1 = new Review();
        ReviewID reviewId1 = new ReviewID(10L, 2L);
        review1.setReviewID(reviewId1);

        // Create fake review 2
        Review review2 = new Review();
        ReviewID reviewId2 = new ReviewID(11L, 2L);
        review2.setReviewID(reviewId2);

        // Add them to repo
        reviewRepository.save(review1);
        reviewRepository.save(review2);

        assertThat(reviewRepository.isReviewerForPaper(2L, 10L)).isTrue();
        assertThat(reviewRepository.isReviewerForPaper(2L, 11L)).isTrue();
        assertThat(reviewRepository.isReviewerForPaper(3L, 11L)).isFalse();

    }

    @Test
    public void findPapersByReviewer() {
        // Create fake review 1
        Review review1 = new Review();
        ReviewID reviewId1 = new ReviewID(10L, 2L);
        review1.setReviewID(reviewId1);

        // Create fake review 2
        Review review2 = new Review();
        ReviewID reviewId2 = new ReviewID(11L, 2L);
        review2.setReviewID(reviewId2);

        // Add them to repo
        reviewRepository.save(review1);
        reviewRepository.save(review2);

        List<Long> list = reviewRepository.findPapersByReviewer(2L);
        assertThat(list).isEqualTo(List.of(10L, 11L));
    }
}