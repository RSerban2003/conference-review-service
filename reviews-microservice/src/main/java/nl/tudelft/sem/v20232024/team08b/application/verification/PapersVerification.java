package nl.tudelft.sem.v20232024.team08b.application.verification;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.phase.PaperPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithSubmissionMicroservice;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.User;
import nl.tudelft.sem.v20232024.team08b.exceptions.ConflictOfInterestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.List;

@Service
public class PapersVerification {

    private final CommunicationWithSubmissionMicroservice submissionsCommunicator;
    private final UsersVerification usersVerification;
    private final TracksVerification tracksVerification;
    private final PaperPhaseCalculator paperPhaseCalculator;

    /**
     * Default constructor for this verification service.
     *
     * @param submissionsCommunicator class that talks with submissions microservice
     * @param usersVerification object used for verifying user information
     * @param tracksVerification object used for verifying track information
     */
    @Autowired
    public PapersVerification(CommunicationWithSubmissionMicroservice submissionsCommunicator,
                              UsersVerification usersVerification,
                              TracksVerification tracksVerification,
                              PaperPhaseCalculator paperPhaseCalculator) {
        this.submissionsCommunicator = submissionsCommunicator;
        this.usersVerification = usersVerification;
        this.tracksVerification = tracksVerification;
        this.paperPhaseCalculator = paperPhaseCalculator;
    }

    /**
     * Checks whether a paper with a given ID exists.
     *
     * @param paperID the ID of the paper.
     * @return true, iff the given paper exists.
     */
    public boolean verifyPaper(Long paperID) {
        try {
            submissionsCommunicator.getSubmission(paperID);
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * This method checks for the conflict of interests.
     *
     * @param paperID ID of a candidate paper to be reviewed
     * @param reviewerID ID of a candidate reviewer
     * @throws NotFoundException if there is no such a submission
     * @throws ConflictOfInterestException if the reviewer cannot be assigned
     */
    public void verifyCOI(Long paperID, Long reviewerID) throws NotFoundException, ConflictOfInterestException {
        Submission submission = submissionsCommunicator.getSubmission(paperID);
        List<@Valid User> conflictsOfInterest = submission.getConflictsOfInterest();
        if (conflictsOfInterest == null) {
            return;
        }
        for (User user : conflictsOfInterest) {
            if (user.getUserId().equals(reviewerID)) {
                throw new ConflictOfInterestException("The reviewer has COI with this paper");
            }
        }
    }

    /**
     * Verifies the permission of a user given by requesterID
     * to view the status of a paper given by paperID.
     *
     * @param requesterID the ID of the user
     * @param paperID the ID of the paper
     * @throws IllegalAccessException if the user does not have permission to view the status of the paper
     */
    public void verifyPermissionToViewStatus(Long requesterID,
                                             Long paperID) throws IllegalAccessException {
        boolean isReviewer = usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.REVIEWER) &&
                usersVerification.isReviewerForPaper(requesterID, paperID);
        boolean isAuthor = usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.AUTHOR) &&
                usersVerification.isAuthorToPaper(requesterID, paperID);
        boolean isChair = usersVerification.verifyRoleFromPaper(requesterID, paperID, UserRole.CHAIR);

        if (!isReviewer && !isAuthor && !isChair) {
            throw new IllegalAccessException("User does not have permission to view the status of this paper");
        }
    }

    /**
     * Verifies if the given user can even access a paper. Used for getting paper and
     * getting the phase of paper.
     *
     * @param reviewerID the ID of the requesting user.
     * @param paperID the ID of the paper
     * @throws NotFoundException thrown if such user could not be found
     * @throws IllegalAccessException thrown if the user has no access to the paper
     */
    public void verifyPermissionToAccessPaper(Long reviewerID,
                                              Long paperID) throws NotFoundException, IllegalAccessException {
        if (!verifyPaper(paperID)) {
            throw new NotFoundException("No such paper exists");
        }
        boolean isChair = usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.CHAIR);
        boolean isReviewer = usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.REVIEWER);
        boolean isReviewerForPaper = usersVerification.isReviewerForPaper(reviewerID, paperID);
        if (!isChair && !isReviewer) {
            throw new IllegalCallerException("No such user exists");
        }
        if (isReviewer && !isReviewerForPaper) {
            throw new IllegalAccessException("The user is not a reviewer for this paper.");
        }
    }

    /**
     * Verifies whether the user has permission to view the paper. This method
     * does not and SHOULD NOT do any phase checking.
     *
     * @param reviewerID the ID of the requesting user
     * @param paperID the ID of the paper that is requested
     * @throws NotFoundException if such paper is not found
     * @throws IllegalCallerException if the user is not assigned as a reviewer to the paper
     * @throws IllegalAccessException if the user is not reviewer or chair in the track of the paper
     *
     */
    public void verifyPermissionToGetPaper(Long reviewerID,
                                            Long paperID) throws NotFoundException, IllegalAccessException {
        // Verify that the current track phase allows for reading full papers
        tracksVerification.verifyTrackPhaseThePaperIsIn(
                paperID,
                List.of(TrackPhase.SUBMITTING, TrackPhase.REVIEWING, TrackPhase.FINAL)
        );
        verifyPermissionToAccessPaper(reviewerID, paperID);
    }

    /**
     * Verifies whether the paper is in a specified phase.
     *
     * @param paperID - The ID of the paper
     * @param paperPhase - The phase to check against
     * @throws NotFoundException if the paper does not exist.
     */
    public boolean verifyPhasePaperIsIn(Long paperID, PaperPhase paperPhase) throws NotFoundException {
        return paperPhaseCalculator.getPaperPhase(paperID) == paperPhase;
    }

}
