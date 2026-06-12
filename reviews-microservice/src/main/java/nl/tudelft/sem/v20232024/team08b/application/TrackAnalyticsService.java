package nl.tudelft.sem.v20232024.team08b.application;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.verification.UsersVerification;
import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithSubmissionMicroservice;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperStatus;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackAnalytics;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import nl.tudelft.sem.v20232024.team08b.exceptions.ForbiddenAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrackAnalyticsService {
    private final UsersVerification usersVerification;
    private final CommunicationWithSubmissionMicroservice submissionsCommunicator;
    private final PapersService papersService;

    /**
     * Creates a calculator for track analytics.
     *
     * @param usersVerification object that performs user verification
     * @param submissionsCommunicator object storing external objects
     * @param papersService service that handles paper operations
     */
    @Autowired
    public TrackAnalyticsService(UsersVerification usersVerification,
                                 CommunicationWithSubmissionMicroservice submissionsCommunicator,
                                 PapersService papersService) {
        this.usersVerification = usersVerification;
        this.submissionsCommunicator = submissionsCommunicator;
        this.papersService = papersService;
    }

    /**
     * Verifies if the requesting user is a chair of the given track.
     *
     * @param trackID the ID of the track
     * @param requesterID the ID of the requesting user
     * @throws ForbiddenAccessException if the user is not a chair of the track
     */
    private void verifyIfChair(TrackID trackID, Long requesterID) throws ForbiddenAccessException {
        // Ensure the requester is a chair of the track
        if (!usersVerification.verifyRoleFromTrack(requesterID, trackID.getConferenceID(),
                trackID.getTrackID(), UserRole.CHAIR)) {
            throw new ForbiddenAccessException();
        }
    }

    /**
     * Gets the analytics of a track. Analytics that provide a summary for a particular track of
     * the amount of papers that have been accepted, rejected and those that haven't yet been decided
     *
     * @param trackID     the ID of the track
     * @param requesterID the ID of the requester
     * @return the numbers of accepted, rejected and undecided papers
     * @throws NotFoundException        if the track does not exist
     * @throws ForbiddenAccessException if the requester is not a chair of the track
     */
    public TrackAnalytics getAnalytics(TrackID trackID, Long requesterID)
            throws NotFoundException, ForbiddenAccessException {
        // Verify if the user is a chair
        verifyIfChair(trackID, requesterID);

        var submissions = submissionsCommunicator.getSubmissionsInTrack(
                trackID.getConferenceID(), trackID.getTrackID(), requesterID
        );
        var accepted = 0;
        var rejected = 0;
        var undecided = 0;
        for (var submission : submissions) {
            PaperStatus status;
            try {
                status = papersService.getState(requesterID, submission.getSubmissionId());
            } catch (IllegalAccessException e) {
                // We have already checked that the requester is a chair of the track
                // so this shouldn't happen
                throw new RuntimeException(e);
            }

            switch (status) {
                case ACCEPTED -> accepted++;
                case REJECTED -> rejected++;
                default -> undecided++;
            }
        }
        return new TrackAnalytics(accepted, rejected, undecided);
    }
}
