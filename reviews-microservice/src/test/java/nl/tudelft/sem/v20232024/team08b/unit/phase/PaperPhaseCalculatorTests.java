package nl.tudelft.sem.v20232024.team08b.unit.phase;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.phase.PaperPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.communicators.SubmissionsMicroserviceCommunicator;
import nl.tudelft.sem.v20232024.team08b.domain.*;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperStatus;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.repos.PaperRepository;
import nl.tudelft.sem.v20232024.team08b.repos.ReviewRepository;
import nl.tudelft.sem.v20232024.team08b.repos.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class PaperPhaseCalculatorTests {
    private final PaperRepository paperRepository = Mockito.mock(PaperRepository.class);
    private final TrackRepository trackRepository = Mockito.mock(TrackRepository.class);
    private final SubmissionsMicroserviceCommunicator submissionsCommunicator =
        Mockito.mock(SubmissionsMicroserviceCommunicator.class);
    private final ReviewRepository reviewRepository = Mockito.mock(ReviewRepository.class);

    private final PaperPhaseCalculator paperPhaseCalculator = Mockito.spy(
            new PaperPhaseCalculator(
                    paperRepository,
                    trackRepository,
                    submissionsCommunicator,
                    reviewRepository
            )
    );

    private Track track;
    private TrackID trackID;
    private List<Paper> papers;
    private List<Review> reviews;

    @BeforeEach
    void init() throws NotFoundException {
        // Create 3 papers for the single track
        Paper paper1 = new Paper(3L, null, PaperStatus.NOT_DECIDED, false);
        Paper paper2 = new Paper(4L, null, PaperStatus.NOT_DECIDED, false);
        Paper paper3 = new Paper(4L, null, PaperStatus.NOT_DECIDED, true);

        papers = List.of(paper1, paper2, paper3);

        // Create the track with these papers
        trackID = new TrackID(1L, 2L);
        track = new Track(
                trackID,
                Date.valueOf(LocalDate.of(2023, 1, 27)),
                false,
                papers
        );

        // Create 2 reviews for the first track
        ReviewID reviewID1 = new ReviewID(paper1.getId(), 5L);
        Review review1 = new Review(
                reviewID1,
                ConfidenceScore.KNOWLEDGEABLE,
                "Comment",
                RecommendationScore.STRONG_ACCEPT,
                "Comment",
                List.of()
        );

        ReviewID reviewID2 = new ReviewID(paper1.getId(), 6L);
        Review review2 = new Review(
                reviewID2,
                ConfidenceScore.EXPERT,
                "Comment2",
                RecommendationScore.WEAK_ACCEPT,
                "Comment2",
                List.of()
        );
        reviews = List.of(review1, review2);

        // Create a fake submission corresponding to paper1
        Submission submission1 = new Submission();
        submission1.setEventId(trackID.getConferenceID());
        submission1.setTrackId(trackID.getTrackID());
        when(submissionsCommunicator.getSubmission(paper1.getId())).thenReturn(submission1);

    }

    @Test
    void checkIfReviewersAreAssignedToTrack_NoSuchTrack() {
        when(trackRepository.findById(trackID)).thenReturn(Optional.empty());
        boolean result = paperPhaseCalculator.checkIfReviewersAreAssignedToTrack(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );
        assertThat(result).isFalse();
    }

    @Test
    void checkIfReviewersAreAssignedToTrack_No() {
        // By default, we have set track.reviewersHaveBeenFinalized = false,
        // therefore, the method should return false.
        when(trackRepository.findById(trackID)).thenReturn(Optional.of(track));
        boolean result = paperPhaseCalculator.checkIfReviewersAreAssignedToTrack(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );
        assertThat(result).isFalse();
    }

    @Test
    void checkIfReviewersAreAssignedToTrack_Yes() {
        track.setReviewersHaveBeenFinalized(true);
        when(trackRepository.findById(trackID)).thenReturn(Optional.of(track));
        boolean result = paperPhaseCalculator.checkIfReviewersAreAssignedToTrack(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );
        assertThat(result).isTrue();
    }

    @Test
    void checkIfEveryReviewerHasSubmitted_Yes() {
        Long paperID = papers.get(0).getId();
        when(reviewRepository.findByReviewIDPaperID(paperID)).thenReturn(reviews);
        boolean result = paperPhaseCalculator.checkIfEveryReviewerHasSubmitted(paperID);

        // Since both reviews review1 and review2 are not empty reviews, then we expect the method
        // to say that all reviewers have submitted
        assertThat(result).isTrue();
    }

    @Test
    void checkIfEveryReviewerHasSubmitted_No() {

        // Let's change review1 to be not-submitted. To do that, we need to take review1,
        // and change it's confidenceScore to null.
        Review review1 = reviews.get(0);
        review1.setConfidenceScore(null);

        Long paperID = papers.get(0).getId();
        when(reviewRepository.findByReviewIDPaperID(paperID)).thenReturn(reviews);
        boolean result = paperPhaseCalculator.checkIfEveryReviewerHasSubmitted(paperID);

        // Since both reviews review1 IS an empty review, we expect the method to
        // return false
        assertThat(result).isFalse();
    }

    @Test
    void checkIfPaperIsFinalized_NoSuchPaper() {
        when(paperRepository.findById(0L)).thenReturn(Optional.empty());

        // If such paper is not even in our local DB, it is not finalized for sure, since
        // when finalize is called to our microservice, the paper is put into the local DB
        boolean result = paperPhaseCalculator.checkIfPaperIsFinalized(0L);
        assertThat(result).isFalse();
    }

    @Test
    void checkIfPaperIsFinalized_No() {
        // Get paper1
        Paper paper1 = papers.get(0);
        Long paper1ID = paper1.getId();
        when(paperRepository.findById(paper1ID)).thenReturn(Optional.of(paper1));

        // By default, paper1 is set to be NOT finalized. So we expect the method to return false
        boolean result = paperPhaseCalculator.checkIfPaperIsFinalized(paper1ID);
        assertThat(result).isFalse();
    }

    @Test
    void checkIfPaperIsFinalized_Yes() {
        // Get paper3
        Paper paper3 = papers.get(2);
        Long paper3ID = paper3.getId();
        when(paperRepository.findById(paper3ID)).thenReturn(Optional.of(paper3));

        // By default, paper1 is set to be finalized. So we expect the method to return true
        boolean result = paperPhaseCalculator.checkIfPaperIsFinalized(paper3ID);
        assertThat(result).isTrue();
    }

    @Test
    void getPaperPhase_NotAllReviewersAssignedToTrack() throws NotFoundException {
        Paper paper1 = papers.get(0);
        Long paper1ID = paper1.getId();

        // Assume that reviewers have not yet been assigned in the track
        doReturn(false).when(paperPhaseCalculator).checkIfReviewersAreAssignedToTrack(
                trackID.getTrackID(),
                trackID.getConferenceID()
        );

        // If the reviewers are not assigned to track, then the paper is in before-review phase
        PaperPhase result = paperPhaseCalculator.getPaperPhase(paper1ID);
        assertThat(result).isEqualTo(PaperPhase.BEFORE_REVIEW);
    }

    @Test
    void getPaperPhase_NotEveryReviewerHasSubmitted() throws NotFoundException {
        Paper paper1 = papers.get(0);
        Long paper1ID = paper1.getId();

        // Assume that reviewers have been assigned in the track
        doReturn(true).when(paperPhaseCalculator).checkIfReviewersAreAssignedToTrack(
                trackID.getTrackID(),
                trackID.getConferenceID()
        );

        // Assume that not every reviewer has submitted at least once
        doReturn(false).when(paperPhaseCalculator).checkIfEveryReviewerHasSubmitted(
                paper1ID
        );

        // If not every reviewer has submitted, then the discussion phase has not started
        // yet, so the paper is in the review phase
        PaperPhase result = paperPhaseCalculator.getPaperPhase(paper1ID);
        assertThat(result).isEqualTo(PaperPhase.IN_REVIEW);
    }

    @Test
    void getPaperPhase_PaperIsNotFinalized() throws NotFoundException {
        Paper paper1 = papers.get(0);
        Long paper1ID = paper1.getId();

        // Assume that reviewers have been assigned in the track
        doReturn(true).when(paperPhaseCalculator).checkIfReviewersAreAssignedToTrack(
                trackID.getTrackID(),
                trackID.getConferenceID()
        );

        // Assume that every reviewer has submitted at least once
        doReturn(true).when(paperPhaseCalculator).checkIfEveryReviewerHasSubmitted(
                paper1ID
        );

        // Assume that ethe paper result is not yet finalized
        doReturn(false).when(paperPhaseCalculator).checkIfPaperIsFinalized(
                paper1ID
        );

        // If the paper is not yet finalized, then it is in the discussion phase
        PaperPhase result = paperPhaseCalculator.getPaperPhase(paper1ID);
        assertThat(result).isEqualTo(PaperPhase.IN_DISCUSSION);
    }

    @Test
    void getPaperPhase_PaperIsFinalized() throws NotFoundException {
        Paper paper1 = papers.get(0);
        Long paper1ID = paper1.getId();

        // Assume that reviewers have been assigned in the track
        doReturn(true).when(paperPhaseCalculator).checkIfReviewersAreAssignedToTrack(
                trackID.getTrackID(),
                trackID.getConferenceID()
        );

        // Assume that every reviewer has submitted at least once
        doReturn(true).when(paperPhaseCalculator).checkIfEveryReviewerHasSubmitted(
                paper1ID
        );

        // Assume that ethe paper result finalized
        doReturn(true).when(paperPhaseCalculator).checkIfPaperIsFinalized(
                paper1ID
        );

        // If the paper finalized, then it is in the final phase, i.e., it has
        // been fully reviewed
        PaperPhase result = paperPhaseCalculator.getPaperPhase(paper1ID);
        assertThat(result).isEqualTo(PaperPhase.REVIEWED);
    }
}
