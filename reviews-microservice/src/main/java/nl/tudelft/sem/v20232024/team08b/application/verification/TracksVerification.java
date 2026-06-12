package nl.tudelft.sem.v20232024.team08b.application.verification;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.phase.TrackPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithSubmissionMicroservice;
import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithUsersMicroservice;
import nl.tudelft.sem.v20232024.team08b.domain.Track;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.repos.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TracksVerification {
    private final TrackRepository trackRepository;
    private final CommunicationWithSubmissionMicroservice submissionsCommunicator;
    private final CommunicationWithUsersMicroservice usersCommunicator;
    private final TrackPhaseCalculator trackPhaseCalculator;
    private final UsersVerification usersVerification;

    /**
     * Default constructor for the track verification.
     *
     * @param trackRepository repository storing all tracks
     * @param submissionsCommunicator class, that talks to submissions microservice
     * @param usersCommunicator class, that talks to submissions microservice
     * @param trackPhaseCalculator object responsible for calculating track phases
     * @param usersVerification object that handles user verification
     */
    @Autowired
    public TracksVerification(TrackRepository trackRepository,
                              CommunicationWithSubmissionMicroservice submissionsCommunicator,
                              CommunicationWithUsersMicroservice usersCommunicator,
                              TrackPhaseCalculator trackPhaseCalculator,
                              UsersVerification usersVerification) {
        this.trackRepository = trackRepository;
        this.submissionsCommunicator = submissionsCommunicator;
        this.usersCommunicator = usersCommunicator;
        this.trackPhaseCalculator = trackPhaseCalculator;
        this.usersVerification = usersVerification;
    }

    /**
     * Checks for existence of a track in our database, if it does not exist it adds it.
     *
     * @param paperID paper ID to look for
     * @throws NotFoundException if paper is not found in database
     */
    public void verifyIfTrackExists(Long paperID) throws NotFoundException {
        Submission submission = submissionsCommunicator.getSubmission(paperID);
        Long trackID = submission.getTrackId();
        Long conferenceID = submission.getEventId();
        if (trackRepository.findById(new TrackID(conferenceID, trackID)).isPresent()) {
            return;
        }
        insertTrack(conferenceID, trackID);

    }

    /**
     * Inserts track to our database.
     *
     * @param conferenceID ID of a conference the track is in
     * @param trackID ID of a track
     */
    public void insertTrack(Long conferenceID, Long trackID) {
        Track toSave = new Track();
        toSave.setTrackID(new TrackID(conferenceID, trackID));
        // We do not need to set the defaults, since they are set in the domain class

        trackRepository.save(toSave);
    }

    /**
     * Finds the track a paper is in and checks if the current phase of that track
     * is one of the listed.
     *
     * @param paperID the ID of the paper
     * @param acceptablePhases a list of all acceptable phases
     * @throws IllegalAccessException exception thrown if the current phase is not in the list
     * @throws NotFoundException exception thrown if the track couldn't be found
     */
    public void verifyTrackPhaseThePaperIsIn(Long paperID,
                                             List<TrackPhase> acceptablePhases) throws IllegalAccessException,
                                                                                       NotFoundException {
        Submission submission = submissionsCommunicator.getSubmission(paperID);
        Long conferenceID = submission.getEventId();
        Long trackID = submission.getTrackId();
        verifyTrackPhase(conferenceID, trackID, acceptablePhases);
    }


    /**
     * Method that checks if the current phase is one of the given phases.
     *
     * @param conferenceID the ID of the conference of the track.
     * @param trackID the ID of the conference.
     * @param acceptablePhases a list of all acceptable phases
     * @throws IllegalAccessException exception thrown if the current phase is not in the list
     * @throws NotFoundException exception thrown if the track couldn't be found
     */
    public void verifyTrackPhase(Long conferenceID,
                                 Long trackID,
                                 List<TrackPhase> acceptablePhases) throws IllegalAccessException, NotFoundException {
        try {
            TrackPhase currentPhase = trackPhaseCalculator.getTrackPhase(conferenceID, trackID);
            if (!acceptablePhases.contains(currentPhase)) {
                throw new IllegalAccessException("This action is not allowed in the current phase");
            }
        } catch (NotFoundException e) {
            throw new NotFoundException("No such track exists");
        }
    }

    /**
     * Verifies if a given track exists.
     *
     * @param conferenceID the ID of the conference of the track
     * @param trackID the ID of the track
     * @return true, iff such track exists
     */
    public boolean verifyTrack(Long conferenceID,
                               Long trackID) {
        try {
            usersCommunicator.getTrack(conferenceID, trackID);
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }


    /**
     * Checks if a given track exists, and if the given user is either a
     * reviewer or a chair of that track.
     *
     * @param requesterID the ID of the requesting user
     * @param conferenceID the ID of the conference the track is in
     * @param trackID the ID of the track
     * @throws NotFoundException if such track does not exist
     * @throws IllegalAccessException if the user is not a chair/reviewer of the track
     */
    public void verifyIfUserCanAccessTrack(Long requesterID,
                                           Long conferenceID,
                                           Long trackID) throws NotFoundException, IllegalAccessException {
        // Verify such track exists
        if (!verifyTrack(conferenceID, trackID)) {
            throw new NotFoundException("Such track could not be found");
        }

        boolean isReviewer = usersVerification.verifyRoleFromTrack(requesterID, conferenceID,
                trackID, UserRole.REVIEWER);
        boolean isChair = usersVerification.verifyRoleFromTrack(requesterID, conferenceID,
                trackID, UserRole.CHAIR);
        boolean isAuthor = usersVerification.verifyRoleFromTrack(requesterID, conferenceID,
                trackID, UserRole.AUTHOR);

        // Check if the requesting user is either a chair or a reviewer in that conference
        if (!isReviewer && !isChair && !isAuthor) {
            throw new IllegalAccessException("The requester is not allowed to access the track");
        }
    }

}
