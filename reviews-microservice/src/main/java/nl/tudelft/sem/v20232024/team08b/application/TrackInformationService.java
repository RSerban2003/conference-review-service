package nl.tudelft.sem.v20232024.team08b.application;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.phase.TrackPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.application.verification.TracksVerification;
import nl.tudelft.sem.v20232024.team08b.application.verification.UsersVerification;
import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithSubmissionMicroservice;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperSummaryWithID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.exceptions.ForbiddenAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TrackInformationService {
    private final TracksVerification tracksVerification;
    private final UsersVerification usersVerification;
    private final TrackPhaseCalculator trackPhaseCalculator;
    private final CommunicationWithSubmissionMicroservice submissionsCommunicator;


    /**
     * Default constructor for the service.
     *
     * @param trackPhaseCalculator object responsible for getting the current phase
     *                             of a track
     * @param tracksVerification object responsible for verifying track information
     * @param usersVerification object responsible for verifying user information
     * @param submissionsCommunicator gets objects from submissions microservice
     */
    @Autowired
    public TrackInformationService(TrackPhaseCalculator trackPhaseCalculator,
                                   TracksVerification tracksVerification,
                                   UsersVerification usersVerification,
                                   CommunicationWithSubmissionMicroservice submissionsCommunicator) {
        this.trackPhaseCalculator = trackPhaseCalculator;
        this.tracksVerification = tracksVerification;
        this.usersVerification = usersVerification;
        this.submissionsCommunicator = submissionsCommunicator;
    }

    /**
     * Returns the current phase of a given track. Also checks if the
     * requesting user has access to the track.
     *
     * @param conferenceID the ID of the conference the track is in
     * @param trackID the ID of the track
     * @return the current phase of the track
     * @throws NotFoundException if such track does not exist
     * @throws IllegalAccessException if the requesting user does not have permissions
     */
    public TrackPhase getTrackPhase(Long requesterID,
                                    Long conferenceID,
                                    Long trackID) throws NotFoundException, IllegalAccessException {
        // Verify if the user and track exist, and if the user is reviewer
        // or chair of the track. Throws respective exceptions
        tracksVerification.verifyIfUserCanAccessTrack(requesterID, conferenceID, trackID);

        return trackPhaseCalculator.getTrackPhase(conferenceID, trackID);
    }


    /**
     * Gets the submissions of a track and transforms them into an instance of PaperSummaryWithID.
     *
     * @param requesterID the ID of the requester
     * @param conferenceID the ID of the conference
     * @param trackID the ID of the track
     * @return A list of PaperSummaryWithID
     * @throws ForbiddenAccessException if the track does not exist
     * @throws NotFoundException if the user is not a pc chair
     */
    public List<PaperSummaryWithID> getPapers(Long requesterID,
                                              Long conferenceID,
                                              Long trackID) throws ForbiddenAccessException,
            NotFoundException {
        if (!usersVerification.verifyRoleFromTrack(requesterID, conferenceID, trackID, UserRole.CHAIR)) {
            throw new ForbiddenAccessException();
        }
        if (!tracksVerification.verifyTrack(conferenceID, trackID)) {
            throw new NotFoundException(
                    "Not Found. The requested track or conference was not found."
            );
        }
        var submissions = submissionsCommunicator.getSubmissionsInTrack(conferenceID, trackID, requesterID);

        final List<PaperSummaryWithID> papers = new ArrayList<>();
        for (Submission submission : submissions) {
            PaperSummaryWithID paperSummaryWithID = new PaperSummaryWithID();
            paperSummaryWithID.setPaperID(submission.getSubmissionId());
            paperSummaryWithID.setTitle(submission.getTitle());
            paperSummaryWithID.setAbstractSection(submission.getAbstract());
            papers.add(paperSummaryWithID);
        }
        return papers;
    }
}
