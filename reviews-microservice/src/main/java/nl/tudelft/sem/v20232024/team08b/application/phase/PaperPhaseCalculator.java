package nl.tudelft.sem.v20232024.team08b.application.phase;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithSubmissionMicroservice;
import nl.tudelft.sem.v20232024.team08b.domain.Review;
import nl.tudelft.sem.v20232024.team08b.domain.Track;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperPhase;
import nl.tudelft.sem.v20232024.team08b.repos.PaperRepository;
import nl.tudelft.sem.v20232024.team08b.repos.ReviewRepository;
import nl.tudelft.sem.v20232024.team08b.repos.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PaperPhaseCalculator {
    private final PaperRepository paperRepository;
    private final TrackRepository trackRepository;
    private final CommunicationWithSubmissionMicroservice submissionsCommunicator;
    private final ReviewRepository reviewRepository;

    /**
     * Default constructor.
     *
     * @param paperRepository repository storing papers
     * @param trackRepository repository storing tracks
     * @param submissionsCommunicator class that talks with submissions microservice
     * @param reviewRepository repository storing reviews
     */
    @Autowired
    public PaperPhaseCalculator(PaperRepository paperRepository,
                         TrackRepository trackRepository,
                         CommunicationWithSubmissionMicroservice submissionsCommunicator,
                         ReviewRepository reviewRepository) {
        this.paperRepository = paperRepository;
        this.trackRepository = trackRepository;
        this.submissionsCommunicator = submissionsCommunicator;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Checks if a track is present in our DB, and if it is, whether
     * reviewers have already been assigned to the track.
     *
     * @param conferenceID the ID of the conference the track is in
     * @param trackID the ID of the track
     * @return true, iff the reviewers are already assigned in a track
     */
    public boolean checkIfReviewersAreAssignedToTrack(Long conferenceID,
                                                      Long trackID) {
        Optional<Track> optional = trackRepository.findById(new TrackID(conferenceID, trackID));
        if (optional.isEmpty()) {
            return false;
        }
        return optional.get().getReviewersHaveBeenFinalized();
    }

    /**
     * Checks if in a paper, every reviewer has submitted.
     *
     * @param paperID the ID of the paper to check.
     * @return true, iff every reviewer has submitted a paper at least once
     */
    public boolean checkIfEveryReviewerHasSubmitted(Long paperID) {
        List<Review> reviews = reviewRepository.findByReviewIDPaperID(paperID);

        for (Review review : reviews) {
            if (review.getConfidenceScore() == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if finalize has been called on this paper before. I.e.,
     * if the reviews of this paper are done.
     * Also, if this paper does not exist in our local DB, that means
     * that likely, finalize was not yet called, so we return false.
     *
     * @param paperID the ID of the paper to check.
     * @return true, iff the reviews for this paper have been finalized
     */
    public boolean checkIfPaperIsFinalized(Long paperID) {
        var optional = paperRepository.findById(paperID);
        if (optional.isEmpty()) {
            // If we have not stored the paper in our DB, it for sure
            // was not finalized yet
            return false;
        }
        return optional.get().getReviewsHaveBeenFinalized();
    }

    /**
     * Calculates current phase of the paper.
     * The logic of calculation is the following:
     * - If the reviewers of the track the paper is in have not yet been
     *   assigned -> BEFORE_REVIEW.
     * - else, if at least one reviewer has not yet submitted a review -> IN_REVIEW
     * - else, if the reviews have not yet been finalized -> IN_DISCUSSION
     * - finally, if all reviews have been finalized (flag reviewsHaveBeenFinalized is
     *  true) in the paper object is set to true -> REVIEWED.
     *
     * @param paperID the ID of the paper
     * @return current phase of the given paper.
     */
    public PaperPhase getPaperPhase(Long paperID) throws NotFoundException {
        // Technically the following line could throw, but this should not be the
        // case, since the calling function will first verify if paper exists
        var submission = submissionsCommunicator.getSubmission(paperID);
        Long trackID = submission.getTrackId();
        Long conferenceID = submission.getEventId();

        // Check if the reviewers of the containing track have not yet been assigned
        if (!checkIfReviewersAreAssignedToTrack(trackID, conferenceID)) {
            return PaperPhase.BEFORE_REVIEW;
        }

        // Check if every reviewer has submitted a review
        if (!checkIfEveryReviewerHasSubmitted(paperID)) {
            return PaperPhase.IN_REVIEW;
        }

        // Check if reviews for this paper have been finalized
        if (!checkIfPaperIsFinalized(paperID)) {
            return PaperPhase.IN_DISCUSSION;
        }

        return PaperPhase.REVIEWED;
    }
}
