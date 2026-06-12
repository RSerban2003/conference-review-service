package nl.tudelft.sem.v20232024.team08b.application;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.verification.DiscussionVerification;
import nl.tudelft.sem.v20232024.team08b.domain.Comment;
import nl.tudelft.sem.v20232024.team08b.domain.Paper;
import nl.tudelft.sem.v20232024.team08b.domain.RecommendationScore;
import nl.tudelft.sem.v20232024.team08b.domain.Review;
import nl.tudelft.sem.v20232024.team08b.dtos.review.DiscussionComment;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperStatus;
import nl.tudelft.sem.v20232024.team08b.repos.PaperRepository;
import nl.tudelft.sem.v20232024.team08b.repos.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DiscussionService {
    private final ReviewRepository reviewRepository;
    private final ReviewsService reviewsService;
    private final PaperRepository paperRepository;
    private final DiscussionVerification discussionVerification;

    /**
     * Constructor for discussion service.
     *
     * @param reviewRepository repository storing the reviews
     * @param reviewsService service for reviews
     * @param paperRepository repository storing papers
     * @param discussionVerification object that verifies permissions
     */

    public DiscussionService(ReviewRepository reviewRepository,
                             ReviewsService reviewsService,
                             PaperRepository paperRepository,
                             DiscussionVerification discussionVerification) {
        this.reviewRepository = reviewRepository;
        this.reviewsService = reviewsService;
        this.paperRepository = paperRepository;
        this.discussionVerification = discussionVerification;
    }

    /**
     * Submits a confidential comment to a review.
     *
     * @param requesterID The ID of the user submitting the comment
     * @param reviewerID The ID of the reviewer associated with the paper
     * @param paperID The ID of the paper being commented on
     * @param text The confidential comment text
     * @throws NotFoundException if the paper does not exist
     * @throws IllegalAccessException if the user does not have the required permissions
     */
    public void submitDiscussionComment(Long requesterID,
                                        Long reviewerID,
                                        Long paperID,
                                        String text) throws NotFoundException, IllegalAccessException {
        discussionVerification.verifySubmitDiscussionComment(requesterID, reviewerID, paperID);

        Comment comment = new Comment(requesterID, text);

        Review review = reviewsService.getReview(reviewerID, paperID);
        review.getConfidentialComments().add(comment);
        reviewRepository.save(review);
    }

    /**
     * Retrieves discussion comments for a specific paper.
     *
     * @param requesterID The ID of the user requesting the comments
     * @param reviewerID The ID of the reviewer associated with the paper
     * @param paperID The ID of the paper whose comments are being retrieved
     * @throws NotFoundException if the review or paper does not exist
     * @throws IllegalAccessException if the user does not have the necessary permissions
     */
    public List<DiscussionComment> getDiscussionComments(Long requesterID,
                                                         Long reviewerID,
                                                         Long paperID) throws NotFoundException,
            IllegalAccessException {
        discussionVerification.verifyGetDiscussionComments(requesterID, reviewerID, paperID);

        Review review = reviewsService.getReview(reviewerID, paperID);
        //Retrieve the list of Comments assigned to the Review
        List<Comment> comments = review.getConfidentialComments();
        //Parse the list of Comments into a list of DiscussionComments
        List<DiscussionComment> discussionComments = new ArrayList<>();
        for (Comment comment : comments) {
            discussionComments.add(new DiscussionComment(comment));
        }
        return discussionComments;
    }

    /**
     * Finalizes the discussion phase for a paper.
     *
     * @param requesterID ID of requesting user
     * @param paperID ID of the paper
     * @throws IllegalAccessException if requester is not allowed to access the paper
     * @throws IllegalStateException if paper is not in discussion phase or reviews are not all positive nor negative
     * @throws NotFoundException if such requester does not exist or if the paper is not found
     */
    public void finalizeDiscussionPhase(Long requesterID, Long paperID) throws IllegalAccessException,
            IllegalStateException,
            NotFoundException {
        discussionVerification.verifyIfUserCanFinalizeDiscussionPhase(requesterID, paperID);
        //Check if all reviews are either in positive or negative.
        //Reviews should not be empty because paper is already in discussion phase
        //which means that there are 3 reviews for the paper.
        List<Review> reviews = reviewRepository.findByReviewIDPaperID(paperID);
        boolean isAgreed = isAgreed(reviews);
        if (!isAgreed) {
            throw new IllegalStateException("Reviews are not all positive nor all negative.");
        }
        RecommendationScore recommendationScore = reviews.get(0).getRecommendationScore();
        Paper paper = getDomainPaper(paperID);
        if (recommendationScore == RecommendationScore.STRONG_REJECT ||
                recommendationScore == RecommendationScore.WEAK_REJECT) {
            paper.setStatus(PaperStatus.REJECTED);
        } else {
            paper.setStatus(PaperStatus.ACCEPTED);
        }
        paper.setReviewsHaveBeenFinalized(true);
        paperRepository.save(paper);
    }

    /**
     * Gets the paper object from paperRepository.
     *
     * @param paperID the ID of the paper
     * @return the paper object
     * @throws NotFoundException if such paper does not exist
     */
    public Paper getDomainPaper(Long paperID) throws NotFoundException {
        Optional<Paper> paper = paperRepository.findById(paperID);
        if (paper.isEmpty()) {
            throw new NotFoundException("Paper was not found");
        }
        return paper.get();
    }

    /**
     * Checks if all reviews have made a uniform decision or not.
     *
     * @param reviews the list of reviews to check
     * @return true if all reviews are either agreed or disagreed. False otherwise.
     */
    public static boolean isAgreed(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            throw new IllegalStateException("No reviews found.");
        }

        boolean isAccept = reviews.get(0).getRecommendationScore() == RecommendationScore.STRONG_ACCEPT
                | reviews.get(0).getRecommendationScore() == RecommendationScore.WEAK_ACCEPT;

        for (int i = 1; i < reviews.size(); i++) { // Start from the second review
            RecommendationScore score = reviews.get(i).getRecommendationScore();
            boolean currentIsAccept = score == RecommendationScore.STRONG_ACCEPT
                    | score == RecommendationScore.WEAK_ACCEPT;

            if (currentIsAccept != isAccept) {
                return false;
            }
        }

        return true;
    }
}
