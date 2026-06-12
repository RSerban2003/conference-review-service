package nl.tudelft.sem.v20232024.team08b.unit.services;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.DiscussionService;
import nl.tudelft.sem.v20232024.team08b.application.ReviewsService;
import nl.tudelft.sem.v20232024.team08b.application.phase.PaperPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.application.verification.DiscussionVerification;
import nl.tudelft.sem.v20232024.team08b.application.verification.UsersVerification;
import nl.tudelft.sem.v20232024.team08b.domain.Comment;
import nl.tudelft.sem.v20232024.team08b.domain.Paper;
import nl.tudelft.sem.v20232024.team08b.domain.RecommendationScore;
import nl.tudelft.sem.v20232024.team08b.domain.Review;
import nl.tudelft.sem.v20232024.team08b.dtos.review.DiscussionComment;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperStatus;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import nl.tudelft.sem.v20232024.team08b.repos.PaperRepository;
import nl.tudelft.sem.v20232024.team08b.repos.ReviewRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DiscussionServiceTests {
    private final PaperRepository paperRepository = Mockito.mock(PaperRepository.class);
    private final PaperPhaseCalculator paperPhaseCalculator = Mockito.mock(PaperPhaseCalculator.class);
    private final UsersVerification usersVerification = Mockito.mock(UsersVerification.class);
    private final ReviewRepository reviewRepository = Mockito.mock(ReviewRepository.class);
    private final ReviewsService reviewsService = Mockito.mock(ReviewsService.class);
    private final DiscussionVerification discussionVerification = Mockito.mock(DiscussionVerification.class);

    private final DiscussionService discussionService = new DiscussionService(
            reviewRepository,
            reviewsService,
            paperRepository,
            discussionVerification
    );

    private Review fakeReview;
    private final Long requesterID = 0L;
    private final Long reviewerID = 3L;
    private final Long paperID = 4L;

    @BeforeEach
    void prepare() {
        fakeReview = new Review();
        List<Comment> comments = new ArrayList<>();
        comments.add(new Comment(2L, "comment"));
        comments.add(new Comment(3L, "comment"));
        fakeReview.setConfidentialComments(comments);
    }

    @Test
    public void finalizeDiscussion_InvalidPaperPhase() throws Exception {
        doThrow(new IllegalStateException()).when(discussionVerification)
                .verifyIfUserCanFinalizeDiscussionPhase(requesterID, paperID);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> discussionService.finalizeDiscussionPhase(requesterID, paperID));
        verify(reviewRepository, never()).findByReviewIDPaperID(paperID);
    }

    @Test
    public void finalizeDiscussion_MixedReviews() throws Exception {
        List<Review> reviews = List.of(
                new Review(null, null, null, RecommendationScore.STRONG_ACCEPT, null, null),
                new Review(null, null, null, RecommendationScore.STRONG_REJECT, null, null),
                new Review(null, null, null, RecommendationScore.WEAK_ACCEPT, null, null)
        );
        doNothing().when(discussionVerification).verifyIfUserCanFinalizeDiscussionPhase(requesterID, paperID);
        when(reviewRepository.findByReviewIDPaperID(paperID)).thenReturn(reviews);
        Exception e = assertThrows(IllegalStateException.class, () ->
                discussionService.finalizeDiscussionPhase(requesterID, paperID));
        assertEquals("Reviews are not all positive nor all negative.", e.getMessage());
    }

    @Test
    public void finalizeDiscussion_NullReviews() throws Exception {
        when(paperPhaseCalculator.getPaperPhase(paperID)).thenReturn(PaperPhase.IN_DISCUSSION);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        when(reviewRepository.findByReviewIDPaperID(paperID)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> discussionService.finalizeDiscussionPhase(requesterID, paperID));
    }

    @Test
    public void finalizeDiscussionSuccessful_AllAccept() throws Exception {
        List<Review> reviews = List.of(
                new Review(null, null, null, RecommendationScore.STRONG_ACCEPT, null, null),
                new Review(null, null, null, RecommendationScore.WEAK_ACCEPT, null, null),
                new Review(null, null, null, RecommendationScore.STRONG_ACCEPT, null, null)
        );
        Paper paper = new Paper(paperID, null, PaperStatus.NOT_DECIDED, false);
        doNothing().when(discussionVerification)
                .verifyIfUserCanFinalizeDiscussionPhase(requesterID, paperID);
        when(reviewRepository.findByReviewIDPaperID(paperID)).thenReturn(reviews);
        when(paperRepository.findById(paperID)).thenReturn(Optional.of(paper));
        when(reviewsService.getReview(reviewerID, paperID)).thenReturn(fakeReview);
        discussionService.finalizeDiscussionPhase(requesterID, paperID);
        assertTrue(paper.getReviewsHaveBeenFinalized());
        verify(paperRepository, times(1)).save(paper);
    }

    @Test
    void testGetDomainPaperSuccess() throws NotFoundException {
        Paper expectedPaper = new Paper();
        when(paperRepository.findById(paperID)).thenReturn(Optional.of(expectedPaper));

        Paper paper = discussionService.getDomainPaper(paperID);

        Assertions.assertNotNull(paper);
        Assertions.assertEquals(expectedPaper, paper);
    }

    @Test
    void testGetDomainPaperNotFoundException() {
        when(paperRepository.findById(paperID)).thenReturn(Optional.empty());

        Exception e = assertThrows(NotFoundException.class, () ->
                discussionService.getDomainPaper(paperID));
        assertEquals("Paper was not found", e.getMessage());
    }

    @Test
    void testFinalizeDiscussionPhaseRejectedStrongRejects()
            throws IllegalAccessException, NotFoundException, IllegalStateException {
        Paper paper = new Paper();
        paper.setId(paperID);
        List<Review> reviews = List.of(
                new Review(null, null, null, RecommendationScore.STRONG_REJECT, null, null),
                new Review(null, null, null, RecommendationScore.STRONG_REJECT, null, null),
                new Review(null, null, null, RecommendationScore.STRONG_REJECT, null, null)
        );

        when(paperPhaseCalculator.getPaperPhase(paperID)).thenReturn(PaperPhase.IN_DISCUSSION);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        when(reviewRepository.findByReviewIDPaperID(paperID)).thenReturn(reviews);
        when(paperRepository.findById(paperID)).thenReturn(Optional.of(paper));

        discussionService.finalizeDiscussionPhase(requesterID, paperID);
        Assertions.assertEquals(PaperStatus.REJECTED, paper.getStatus());
        assertTrue(paper.getReviewsHaveBeenFinalized());
        verify(paperRepository, times(1)).save(paper);
    }

    @Test
    void testFinalizeDiscussionPhaseRejectedWeakReject() throws Exception {
        Paper paper = new Paper();
        paper.setId(paperID);
        List<Review> reviews = List.of(
                new Review(null, null, null, RecommendationScore.WEAK_REJECT, null, null),
                new Review(null, null, null, RecommendationScore.WEAK_REJECT, null, null),
                new Review(null, null, null, RecommendationScore.WEAK_REJECT, null, null)
        );

        when(paperPhaseCalculator.getPaperPhase(paperID)).thenReturn(PaperPhase.IN_DISCUSSION);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        when(reviewRepository.findByReviewIDPaperID(paperID)).thenReturn(reviews);
        when(paperRepository.findById(paperID)).thenReturn(Optional.of(paper));

        discussionService.finalizeDiscussionPhase(requesterID, paperID);
        Assertions.assertEquals(PaperStatus.REJECTED, paper.getStatus());
        assertTrue(paper.getReviewsHaveBeenFinalized());
        verify(paperRepository, times(1)).save(paper);
    }

    @Test
    void testFinalizeDiscussionPhaseAccepted() throws IllegalAccessException, NotFoundException, IllegalStateException {
        Paper paper = new Paper();
        paper.setId(paperID);

        List<Review> reviews = List.of(
                new Review(null, null, null, RecommendationScore.STRONG_ACCEPT, null, null),
                new Review(null, null, null, RecommendationScore.STRONG_ACCEPT, null, null),
                new Review(null, null, null, RecommendationScore.WEAK_ACCEPT, null, null)
        );

        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        when(paperPhaseCalculator.getPaperPhase(paperID)).thenReturn(PaperPhase.IN_DISCUSSION);
        when(reviewRepository.findByReviewIDPaperID(paperID)).thenReturn(reviews);
        when(paperRepository.findById(paperID)).thenReturn(Optional.of(paper));

        discussionService.finalizeDiscussionPhase(requesterID, paperID);

        assertEquals(PaperStatus.ACCEPTED, paper.getStatus());
    }


























    @Test
    void submitConfidentialComment_NotFoundException() throws NotFoundException, IllegalAccessException {
        doThrow(new NotFoundException(""))
                .when(discussionVerification).verifySubmitDiscussionComment(requesterID, reviewerID, paperID);

        assertThrows(NotFoundException.class, () ->
                discussionService.submitDiscussionComment(requesterID, reviewerID, paperID, "text"));
    }

    @Test
    void submitConfidentialComment_IllegalAccessException() throws NotFoundException, IllegalAccessException {
        doThrow(new IllegalAccessException(""))
                .when(discussionVerification).verifySubmitDiscussionComment(requesterID, reviewerID, paperID);

        assertThrows(IllegalAccessException.class, () ->
                discussionService.submitDiscussionComment(requesterID, reviewerID, paperID, "text"));
    }

    @Test
    void submitConfidentialComment_Successful() throws NotFoundException, IllegalAccessException {
        doNothing().when(discussionVerification).verifySubmitDiscussionComment(requesterID, reviewerID, paperID);
        when(reviewsService.getReview(reviewerID, paperID)).thenReturn(fakeReview);

        discussionService.submitDiscussionComment(requesterID, reviewerID, paperID, "text");
        Comment comment = new Comment(requesterID, "text");
        assertThat(fakeReview.getConfidentialComments().contains(comment)).isTrue();
        verify(reviewRepository).save(fakeReview);

    }

    @Test
    void getDiscussionComments_NotFoundException() throws NotFoundException, IllegalAccessException {
        doThrow(new NotFoundException(""))
                .when(discussionVerification).verifyGetDiscussionComments(requesterID, reviewerID, paperID);

        assertThrows(NotFoundException.class, () ->
                discussionService.getDiscussionComments(requesterID, reviewerID, paperID));
    }

    @Test
    void getDiscussionComments_IllegalAccessException() throws NotFoundException, IllegalAccessException {
        doThrow(new IllegalAccessException(""))
                .when(discussionVerification).verifyGetDiscussionComments(requesterID, reviewerID, paperID);

        assertThrows(IllegalAccessException.class, () ->
                discussionService.getDiscussionComments(requesterID, reviewerID, paperID));
    }


    @Test
    void getDiscussionComments_Successful() throws NotFoundException, IllegalAccessException {
        List<DiscussionComment> expectedComments = new ArrayList<>();
        expectedComments.add(new DiscussionComment(2L, "comment"));
        expectedComments.add(new DiscussionComment(3L, "comment"));

        doNothing().when(discussionVerification).verifyGetDiscussionComments(requesterID, reviewerID, paperID);
        when(reviewsService.getReview(reviewerID, paperID)).thenReturn(fakeReview);

        List<DiscussionComment> actualComments = discussionService.getDiscussionComments(requesterID, reviewerID, paperID);

        assertThat(actualComments).isEqualTo(expectedComments);
    }

}
