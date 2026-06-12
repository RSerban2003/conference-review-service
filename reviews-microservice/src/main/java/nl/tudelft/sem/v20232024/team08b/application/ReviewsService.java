package nl.tudelft.sem.v20232024.team08b.application;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.verification.PapersVerification;
import nl.tudelft.sem.v20232024.team08b.application.verification.TracksVerification;
import nl.tudelft.sem.v20232024.team08b.application.verification.UsersVerification;
import nl.tudelft.sem.v20232024.team08b.domain.Review;
import nl.tudelft.sem.v20232024.team08b.domain.ReviewID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import nl.tudelft.sem.v20232024.team08b.repos.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewsService {
    private final ReviewRepository reviewRepository;
    private final PapersVerification papersVerification;
    private final TracksVerification tracksVerification;
    private final UsersVerification usersVerification;

    /**
     * Default constructor for the service.
     *
     * @param reviewRepository repository storing the reviews
     * @param tracksVerification object responsible for verifying track information
     * @param usersVerification object responsible for verifying user information
     * */
    @Autowired
    public ReviewsService(ReviewRepository reviewRepository,
                          PapersVerification papersVerification,
                          TracksVerification tracksVerification,
                          UsersVerification usersVerification) {
        this.reviewRepository = reviewRepository;
        this.papersVerification = papersVerification;
        this.tracksVerification = tracksVerification;
        this.usersVerification = usersVerification;
    }

    /**
     * Verifies if: given user exists, given paper exists, the user is a reviewer of that paper.
     *
     * @param requesterID the ID of the requesting user
     * @param paperID the ID of the paper that is accessed
     * @throws NotFoundException if such paper is not found
     * @throws IllegalAccessException if the user is not allowed to access the paper
     */
    private void verifyIfReviewerCanSubmitReview(Long requesterID, Long paperID) throws NotFoundException,
            IllegalAccessException {
        // Check if such paper exists
        if (!papersVerification.verifyPaper(paperID)) {
            throw new NotFoundException("No such paper exists");
        }

        // Check if such user exists and has correct privileges
        if (!usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)) {
            throw new IllegalAccessException("No such user exists");
        }

        // Check if the user is allowed to review this paper
        if (!usersVerification.isReviewerForPaper(requesterID, paperID)) {
            throw new IllegalAccessException("The user is not a reviewer for this paper.");
        }

        // Verify that the track is in the reviewing phase
        tracksVerification.verifyTrackPhaseThePaperIsIn(paperID,
                List.of(TrackPhase.REVIEWING)
        );
    }

    /**
     * Adds or updates a review by a user for a specific paper.
     *
     * @param reviewDTO the review object that was given by the user
     * @param requesterID the ID of the submitter
     * @param paperID the ID of the for which the review is submitted
     */
    public void submitReview(nl.tudelft.sem.v20232024.team08b.dtos.review.Review reviewDTO,
                             Long requesterID,
                             Long paperID) throws Exception {
        verifyIfReviewerCanSubmitReview(requesterID, paperID);

        ReviewID reviewId = new ReviewID(paperID, requesterID);
        Review review = new Review(reviewDTO, reviewId);
        reviewRepository.save(review);
    }

    /**
     * Returns a review from the repository.
     *
     * @param reviewerID the ID of the reviewer
     * @param paperID the ID of the paper
     * @return the review
     */
    public Review getReview(Long reviewerID, Long paperID) throws NotFoundException {
        ReviewID reviewId = new ReviewID(paperID, reviewerID);
        Optional<Review> optional = reviewRepository.findById(reviewId);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            throw new NotFoundException("The review could not be found");
        }
    }

    /**
     * This method is designed to retrieve a list of reviewer IDs associated with a given paper.
     * It is intended for use by users who are either chairs or reviewers of the paper in question.
     *
     * @param requesterID the ID of the user making the request
     * @param paperID the ID of the paper for which reviewers are being requested
     * @return The method returns a list of Long values, each representing the ID of a reviewer associated with the paper.
     * @throws NotFoundException if the paper is not found
     * @throws IllegalAccessException if the requester is neither a chair nor a reviewer of the paper
     */
    public List<Long> getReviewersFromPaper(long requesterID, long paperID)
            throws NotFoundException, IllegalAccessException {
        boolean isChair = usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR);
        boolean isReviewer = usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER);
        if (!isChair && !isReviewer) {
            throw new IllegalAccessException("Not a chair or reviewer of paper");
        }
        tracksVerification.verifyTrackPhaseThePaperIsIn(paperID, List.of(TrackPhase.REVIEWING, TrackPhase.FINAL));
        List<Review> reviews = reviewRepository.findByReviewIDPaperID(paperID);
        List<Long> reviewers = new ArrayList<>();
        for (Review review : reviews) {
            reviewers.add(review.getReviewID().getReviewerID());
        }
        return reviewers;
    }

    /**
     * Verifies if a user can access a review. The user either has to be a chair
     * of the track of the review, or the reviewer himself.
     *
     * @param requesterID the ID of the requesting user.
     * @param reviewerID the ID of the reviewer who wrote that review.
     * @param paperID the ID of the paper reviewer.
     * @throws NotFoundException if requested review does not exist.
     * @throws IllegalAccessException if the requester is not allowed to access the review.
     */
    public void verifyIfUserCanAccessReview(Long requesterID,
                                            Long reviewerID,
                                            Long paperID) throws NotFoundException, IllegalAccessException {
        if (!papersVerification.verifyPaper(paperID)) {
            throw new NotFoundException("No such paper exists");
        }

        boolean isReviewer = usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER);
        boolean isChair = usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR);
        boolean isAuthor = usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.AUTHOR);

        // Check if such review even exists (the method throws if it doesn't)
        getReview(reviewerID, paperID);

        if (isAuthor) {
            // If the requesting user is author, then he can only access the paper during final phase
            tracksVerification.verifyTrackPhaseThePaperIsIn(paperID, List.of(TrackPhase.FINAL));
        } else if (isChair || isReviewer) {
            // If the requesting user is chair or reviewer, they cannot access the review before the reviewing phase
            tracksVerification.verifyTrackPhaseThePaperIsIn(paperID, List.of(TrackPhase.REVIEWING, TrackPhase.FINAL));
        } else {
            throw new IllegalAccessException("The requester is not a member of the track");
        }
    }

    /**
     * Gets a review from the local repository. Also performs all checks: checks if the
     * user is allowed to access the review, if such review exists, etc.
     *
     * @param requesterID ID of requesting user
     * @param reviewerID ID of the reviewer
     * @param paperID ID of the reviewed paper
     * @return review DTO object to return
     * @throws NotFoundException if such review was not found
     * @throws IllegalAccessException if the requester is not allowed to access the paper
     * @throws IllegalCallerException if such requester does not exist
     */
    public nl.tudelft.sem.v20232024.team08b.dtos.review.Review getReview(Long requesterID,
                                                                         Long reviewerID,
                                                                         Long paperID) throws NotFoundException,
            IllegalAccessException,
            IllegalCallerException {
        // Verify if the requesting user is allowed to access and if the given review exists. Also, verifies phase
        verifyIfUserCanAccessReview(requesterID, reviewerID, paperID);

        // Get the review from local repository.
        Review review = getReview(reviewerID, paperID);
        if (usersVerification.isAuthorToPaper(requesterID, paperID)) {
            review.setCommentForReviewers(null);
        }

        // Map the review from local domain object to a DTO and return it
        return new nl.tudelft.sem.v20232024.team08b.dtos.review.Review(review);
    }
}
