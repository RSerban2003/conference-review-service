package nl.tudelft.sem.v20232024.team08b.application.verification;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.phase.PaperPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DiscussionVerification {
    private final PapersVerification papersVerification;
    private final UsersVerification usersVerification;
    private final PaperPhaseCalculator paperPhaseCalculator;

    /**
     * Constructs discussion verification service class.
     *
     * @param papersVerification object that does paper verification
     * @param usersVerification object that does user verification
     * @param paperPhaseCalculator object that caclulates the phases of papers
     */
    public DiscussionVerification(PapersVerification papersVerification,
                                  UsersVerification usersVerification,
                                  PaperPhaseCalculator paperPhaseCalculator) {
        this.papersVerification = papersVerification;
        this.usersVerification = usersVerification;
        this.paperPhaseCalculator = paperPhaseCalculator;
    }

    /**
     * Verifies if the user has permission to submit a confidential comment on a review.
     *
     * @param requesterID The ID of the user submitting the comment
     * @param reviewerID The ID of the reviewer associated with the paper
     * @param paperID The ID of the paper being reviewed
     * @throws NotFoundException if the paper does not exist
     * @throws IllegalAccessException if the user does not have the required permissions
     */
    public void verifySubmitDiscussionComment(Long requesterID,
                                              Long reviewerID,
                                              Long paperID) throws NotFoundException,
            IllegalAccessException {
        boolean isReviewer = usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER);
        boolean isAssignedToPaper = usersVerification.isReviewerForPaper(reviewerID, paperID);

        if (!papersVerification.verifyPaper(paperID)) {
            throw new NotFoundException("Paper does not exist");
        }

        if (!isReviewer || !isAssignedToPaper) {
            throw new IllegalAccessException("The user does not have permission to submit this comment");
        }

        papersVerification.verifyPhasePaperIsIn(paperID, PaperPhase.IN_DISCUSSION);

    }


    /**
     * Verifies if the user has the permission to view discussion comments on a review.
     *
     * @param requesterID The ID of the user requesting to view the comments
     * @param reviewerID The ID of the reviewer associated with the paper
     * @param paperID The ID of the paper
     * @throws NotFoundException if the paper does not exist
     * @throws IllegalAccessException if the user does not have the required permissions
     */
    public void verifyGetDiscussionComments(Long requesterID,
                                            Long reviewerID,
                                            Long paperID) throws NotFoundException,
            IllegalAccessException {
        boolean isChair = usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR);
        boolean isReviewer = usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER);
        boolean isAssignedToPaper = usersVerification.isReviewerForPaper(reviewerID, paperID);

        if (!papersVerification.verifyPaper(paperID)) {
            throw new NotFoundException("Paper does not exist");
        }

        if (!isChair && !(isReviewer && isAssignedToPaper)) {
            throw new IllegalAccessException("The user does not have permission to view these comments");
        }

        papersVerification.verifyPhasePaperIsIn(paperID, PaperPhase.IN_DISCUSSION);
    }

    /**
     * Method that verifies if a user can finalize the discussion phase.
     *
     * @param requesterID the ID of the requesting user
     * @param paperID the ID of the paper that is being finalized
     * @throws IllegalAccessException if the user has no permissions to finalize
     * @throws NotFoundException if such paper does not exist
     */
    public void verifyIfUserCanFinalizeDiscussionPhase(Long requesterID, Long paperID) throws IllegalAccessException,
                                                                                              NotFoundException {
        // Check if requester is a valid chair of this track which contains this paper.
        if (!usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR)) {
            throw new IllegalAccessException("User is not a chair for this track!");
        }
        PaperPhase phase = paperPhaseCalculator.getPaperPhase(paperID);
        // Check if paper is in discussion phase.
        if (!Objects.equals(phase, PaperPhase.IN_DISCUSSION)) {
            throw new IllegalStateException("Paper is not in discussion phase!");
        }
    }
}
