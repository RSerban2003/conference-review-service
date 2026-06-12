package nl.tudelft.sem.v20232024.team08b.unit.verification;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.phase.PaperPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.application.verification.DiscussionVerification;
import nl.tudelft.sem.v20232024.team08b.application.verification.PapersVerification;
import nl.tudelft.sem.v20232024.team08b.application.verification.UsersVerification;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DiscussionVerificationTests {
    private final UsersVerification usersVerification = Mockito.mock(UsersVerification.class);
    private final PapersVerification papersVerification = Mockito.mock(PapersVerification.class);
    private final PaperPhaseCalculator paperPhaseCalculator = Mockito.mock(PaperPhaseCalculator.class);
    private final DiscussionVerification discussionVerification = new DiscussionVerification(
            papersVerification,
            usersVerification,
            paperPhaseCalculator
    );
    private final Long requesterID = 0L;
    private final Long reviewerID = 3L;
    private final Long paperID = 4L;

    @Test
    void verifySubmitConfidentialComment_NotFoundException() {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(true);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(true);
        when(papersVerification.verifyPaper(paperID)).thenReturn(false);

        assertThrows(NotFoundException.class, () ->
                discussionVerification.verifySubmitDiscussionComment(requesterID, reviewerID, paperID));
    }

    @Test
    void verifySubmitConfidentialComment_IllegalAccessException_NotReviewerNotAssigned() {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(false);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(false);
        when(papersVerification.verifyPaper(paperID)).thenReturn(true);

        assertThrows(IllegalAccessException.class, () ->
                discussionVerification.verifySubmitDiscussionComment(requesterID, reviewerID, paperID));
    }

    @Test
    void verifySubmitConfidentialComment_IllegalAccessException_NotReviewer() {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(false);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(true);
        when(papersVerification.verifyPaper(paperID)).thenReturn(true);

        assertThrows(IllegalAccessException.class, () ->
                discussionVerification.verifySubmitDiscussionComment(requesterID, reviewerID, paperID));
    }

    @Test
    void verifySubmitConfidentialComment_IllegalAccessException_NotAssigned() {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(true);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(false);
        when(papersVerification.verifyPaper(paperID)).thenReturn(true);

        assertThrows(IllegalAccessException.class, () ->
                discussionVerification.verifySubmitDiscussionComment(requesterID, reviewerID, paperID));
    }

    @Test
    void verifySubmitConfidentialComment_Successful() throws NotFoundException, IllegalAccessException {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(true);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(true);
        when(papersVerification.verifyPaper(paperID)).thenReturn(true);

        discussionVerification.verifySubmitDiscussionComment(requesterID, reviewerID, paperID);
    }

    @Test
    void verifyGetDiscussionComments_NotFoundException() {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(true);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(true);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        when(papersVerification.verifyPaper(paperID)).thenReturn(false);

        assertThrows(NotFoundException.class, () ->
                discussionVerification.verifyGetDiscussionComments(requesterID, reviewerID, paperID));
    }

    @Test
    void verifyGetDiscussionComments_IllegalAccessException() {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(false);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(false);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(false);
        when(papersVerification.verifyPaper(paperID)).thenReturn(true);

        assertThrows(IllegalAccessException.class, () ->
                discussionVerification.verifyGetDiscussionComments(requesterID, reviewerID, paperID));
    }

    @Test
    void verifyGetDiscussionComments_IllegalAccessException_NotChairNotAssigned() {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(true);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(false);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(false);
        when(papersVerification.verifyPaper(paperID)).thenReturn(true);

        assertThrows(IllegalAccessException.class, () ->
                discussionVerification.verifyGetDiscussionComments(requesterID, reviewerID, paperID));
    }

    @Test
    void verifyGetDiscussionComments_IllegalAccessException_NotChairNotReviewer() {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(false);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(true);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(false);
        when(papersVerification.verifyPaper(paperID)).thenReturn(true);

        assertThrows(IllegalAccessException.class, () ->
                discussionVerification.verifyGetDiscussionComments(requesterID, reviewerID, paperID));
    }

    @Test
    void verifyGetDiscussionComments_Successful_isReviewer() throws NotFoundException, IllegalAccessException {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(true);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(true);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(false);
        when(papersVerification.verifyPaper(paperID)).thenReturn(true);

        discussionVerification.verifyGetDiscussionComments(requesterID, reviewerID, paperID);
    }

    @Test
    void verifyGetDiscussionComments_Successful_isChair() throws NotFoundException, IllegalAccessException {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(false);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(false);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        when(papersVerification.verifyPaper(paperID)).thenReturn(true);

        discussionVerification.verifyGetDiscussionComments(requesterID, reviewerID, paperID);
    }

    @Test
    void verifyGetDiscussionComments_Successful_isChairAndReviewer() throws NotFoundException, IllegalAccessException {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(true);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(false);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        when(papersVerification.verifyPaper(paperID)).thenReturn(true);

        discussionVerification.verifyGetDiscussionComments(requesterID, reviewerID, paperID);
    }

    @Test
    void verifyGetDiscussionComments_Successful_isChairAndAssigned() throws NotFoundException, IllegalAccessException {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(false);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(true);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        when(papersVerification.verifyPaper(paperID)).thenReturn(true);

        discussionVerification.verifyGetDiscussionComments(requesterID, reviewerID, paperID);
    }

    @Test
    void verifyGetDiscussionComments_Successful() throws NotFoundException, IllegalAccessException {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER)).thenReturn(true);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(true);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        when(papersVerification.verifyPaper(paperID)).thenReturn(true);

        discussionVerification.verifyGetDiscussionComments(requesterID, reviewerID, paperID);
    }

    @Test
    public void verifyIfUserCanFinalizeDiscussionPhase_PaperNotFound() throws Exception {
        when(paperPhaseCalculator.getPaperPhase(paperID)).thenThrow(new NotFoundException("No such paper found"));
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        assertThrows(NotFoundException.class, () ->
                discussionVerification.verifyIfUserCanFinalizeDiscussionPhase(requesterID, paperID));
    }

    @Test
    public void verifyIfUserCanFinalizeDiscussionPhase_InvalidRequester() {
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(false);
        assertThrows(IllegalAccessException.class, () ->
                discussionVerification.verifyIfUserCanFinalizeDiscussionPhase(requesterID, paperID));
    }

    @Test
    public void verifyIfUserCanFinalizeDiscussionPhase_InvalidPaperPhase() throws Exception {
        when(paperPhaseCalculator.getPaperPhase(paperID)).thenReturn(PaperPhase.IN_REVIEW);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        assertThrows(IllegalStateException.class, () ->
                discussionVerification.verifyIfUserCanFinalizeDiscussionPhase(requesterID, paperID));
    }

    @Test
    public void verifyIfUserCanFinalizeDiscussionPhase_Success() throws Exception {
        when(paperPhaseCalculator.getPaperPhase(paperID)).thenReturn(PaperPhase.IN_DISCUSSION);
        when(usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)).thenReturn(true);
        assertDoesNotThrow(() -> discussionVerification.verifyIfUserCanFinalizeDiscussionPhase(requesterID, paperID));
    }
}
