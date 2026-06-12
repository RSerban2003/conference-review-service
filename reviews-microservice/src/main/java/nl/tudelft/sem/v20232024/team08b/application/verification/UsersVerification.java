package nl.tudelft.sem.v20232024.team08b.application.verification;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithSubmissionMicroservice;
import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithUsersMicroservice;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.User;
import nl.tudelft.sem.v20232024.team08b.dtos.users.RolesOfUser;
import nl.tudelft.sem.v20232024.team08b.dtos.users.RolesOfUserTracksInner;
import nl.tudelft.sem.v20232024.team08b.repos.ReviewRepository;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

@Service
public class UsersVerification {
    private final CommunicationWithUsersMicroservice usersCommunicator;
    private final CommunicationWithSubmissionMicroservice submissionsCommunicator;
    private final ReviewRepository reviewRepository;

    /**
     * Default constructor for the user verification.
     *
     * @param submissionsCommunicator class, that talks to submissions microservice
     * @param usersCommunicator class, that talks to submissions microservice
     * @param reviewRepository repository storing all reviews
     */
    public UsersVerification(CommunicationWithUsersMicroservice usersCommunicator,
                             CommunicationWithSubmissionMicroservice submissionsCommunicator,
                             ReviewRepository reviewRepository) {
        this.usersCommunicator = usersCommunicator;
        this.reviewRepository = reviewRepository;
        this.submissionsCommunicator = submissionsCommunicator;
    }

    /**
     * Checks whether a user exists.
     *
     * @param userID the ID of the user
     * @return true, iff the given user exists
     */
    public boolean verifyIfUserExists(Long userID) {
        try {
            // getRolesOfUser method will throw an error when this user does not exist
            usersCommunicator.getRolesOfUser(userID);
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * Checks whether a user with a given ID exists and is in the same conference and track as a paper with a give ID.
     *
     * @param userID the ID of the user.
     * @param trackID the ID of the track.
     * @param conferenceID the ID of the conference.
     * @param role the role to check for that user.
     * @return true, iff the given user exists with the given role.
     */
    public boolean verifyRoleFromTrack(Long userID, Long conferenceID, Long trackID, UserRole role) {
        try {
            // Get all roles of each track from other microservice
            RolesOfUser roles = usersCommunicator.getRolesOfUser(userID);
            boolean ret = false;

            // Iterate over each role/track
            for (RolesOfUserTracksInner thisTrack : roles.getTracks()) {

                // Cast from Integer to Long
                Long thisTrackID = thisTrack.getTrackId();
                Long thisConferenceID = thisTrack.getEventId();

                // If we do not care about this track, continue
                if (!thisTrackID.equals(trackID) || !thisConferenceID.equals(conferenceID)) {
                    continue;
                }

                // Parse the role string into enum
                UserRole thisRole = UserRole.parse(thisTrack.getRoleName());

                boolean rolesAreEqual = thisRole.equals(role);
                ret = ret || rolesAreEqual;
            }
            return ret;
        } catch (NotFoundException e) {
            return false;
        }
    }


    /**
     * Checks whether a user with a given ID exists and is in the same conference and track as a paper with a give ID.
     *
     * @param userID the ID of the user.
     * @param paperID the ID of the paper.
     * @param role the role to check for that user.
     * @return true, iff the given user exists with the given role.
     */
    public boolean verifyRoleFromPaper(Long userID, Long paperID, UserRole role) {
        try {
            Long trackID = submissionsCommunicator.getSubmission(paperID).getTrackId();
            Long conferenceID = submissionsCommunicator.getSubmission(paperID).getEventId();
            return verifyRoleFromTrack(userID, conferenceID, trackID, role);
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * Checks whether a user is assigned as a reviewer to a specific paper.
     *
     * @param reviewerID the ID of the user.
     * @param paperID the ID of the paper.
     * @return whether the user is assigned to the paper or not.
     */
    public boolean isReviewerForPaper(Long reviewerID, Long paperID) {
        return reviewRepository.isReviewerForPaper(reviewerID, paperID);
    }

    /**
     *  Checks whether a user given by userID is an author of a paper given by paperID.
     *
     * @param userID the ID of the user
     * @param paperID the ID of the paper
     * @return true if the user is an author of the paper, false otherwise
     */
    public boolean isAuthorToPaper(Long userID, Long paperID) {
        try {
            var submission = submissionsCommunicator.getSubmission(paperID);
            List<@Valid User> authors = submission.getAuthors();
            for (@Valid User author : authors) {
                if (Objects.equals(author.getUserId(), userID)) {
                    return true;
                }
            }
            return false;
        } catch (NotFoundException e) {
            return false;
        }
    }

}
