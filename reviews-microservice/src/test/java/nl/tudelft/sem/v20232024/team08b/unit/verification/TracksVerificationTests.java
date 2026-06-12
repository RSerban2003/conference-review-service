package nl.tudelft.sem.v20232024.team08b.unit.verification;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.phase.TrackPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.application.verification.TracksVerification;
import nl.tudelft.sem.v20232024.team08b.application.verification.UsersVerification;
import nl.tudelft.sem.v20232024.team08b.communicators.SubmissionsMicroserviceCommunicator;
import nl.tudelft.sem.v20232024.team08b.communicators.UsersMicroserviceCommunicator;
import nl.tudelft.sem.v20232024.team08b.domain.Track;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.repos.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class TracksVerificationTests {
    private final TrackRepository trackRepository = Mockito.mock(TrackRepository.class);
    private final SubmissionsMicroserviceCommunicator submissionsCommunicator =
        Mockito.mock(SubmissionsMicroserviceCommunicator.class);
    private final UsersMicroserviceCommunicator usersCommunicator = Mockito.mock(UsersMicroserviceCommunicator.class);
    private final TrackPhaseCalculator trackPhaseCalculator = Mockito.mock(TrackPhaseCalculator.class);
    private final UsersVerification usersVerification = Mockito.mock(UsersVerification.class);

    private final TracksVerification tracksVerification = Mockito.spy(new TracksVerification(
            trackRepository,
            submissionsCommunicator,
            usersCommunicator,
            trackPhaseCalculator,
            usersVerification
    ));

    private Submission fakeSubmission;
    private final Long requesterID = 0L;
    private final Long conferenceID = 1L;
    private final Long trackID = 2L;

    @BeforeEach
    void init() {

        fakeSubmission = new Submission();
        fakeSubmission.setTrackId(3L);

        // Assume that the user has no role
        when(
                usersVerification.verifyRoleFromTrack(requesterID, conferenceID, trackID, UserRole.REVIEWER)
        ).thenReturn(false);
        when(
                usersVerification.verifyRoleFromTrack(requesterID, conferenceID, trackID, UserRole.CHAIR)
        ).thenReturn(false);
        when(
                usersVerification.verifyRoleFromTrack(requesterID, conferenceID, trackID, UserRole.AUTHOR)
        ).thenReturn(false);
    }

    @Test
    public void testVerifyIfTrackExists() throws NotFoundException {
        Long paperID = 123L;
        Submission submission = new Submission();
        submission.setTrackId(456L);
        submission.setEventId(789L);

        when(submissionsCommunicator.getSubmission(paperID)).thenReturn(submission);
        when(trackRepository.findById(any())).thenReturn(Optional.of(new Track()));

        tracksVerification.verifyIfTrackExists(paperID);

        verify(submissionsCommunicator, times(1)).getSubmission(paperID);
        verify(trackRepository, times(1)).findById(any());
        verify(tracksVerification, never()).insertTrack(anyLong(), anyLong());
    }

    @Test
    public void testVerifyIfTrackDoesNotExist() throws NotFoundException {
        Long paperID = 123L;
        Submission submission = new Submission();
        submission.setTrackId(456L);
        submission.setEventId(789L);

        when(submissionsCommunicator.getSubmission(paperID)).thenReturn(submission);
        when(trackRepository.findById(any())).thenReturn(Optional.empty());

        tracksVerification.verifyIfTrackExists(paperID);
        verify(tracksVerification, times(1)).insertTrack(anyLong(), anyLong());

    }

    @Test
    void testInsertTrack() {

        Long conferenceID = 1L;
        Long trackID = 2L;

        tracksVerification.insertTrack(conferenceID, trackID);

        Track expectedTrack = new Track();
        expectedTrack.setTrackID(new TrackID(conferenceID, trackID));
        expectedTrack.setReviewersHaveBeenFinalized(false);
        expectedTrack.setBiddingDeadline(null);

        verify(trackRepository).save(expectedTrack);
    }

    @Test
    void verifyTrackPhase_NoSuchTrack() throws NotFoundException {
        when(
                trackPhaseCalculator.getTrackPhase(0L, 1L)
        ).thenThrow(new NotFoundException(""));
        assertThrows(NotFoundException.class, () ->
                tracksVerification.verifyTrackPhase(0L, 1L, List.of()));
    }

    @Test
    void verifyTrackPhase_NoSuchPhase() throws NotFoundException {
        // Assume current phase is bidding
        when(
                trackPhaseCalculator.getTrackPhase(0L, 1L)
        ).thenReturn(TrackPhase.BIDDING);

        // Assume allowed phases are reviewing and final. Then the method should throw
        assertThrows(IllegalAccessException.class, () ->
                tracksVerification.verifyTrackPhase(
                        0L, 1L,
                        List.of(TrackPhase.REVIEWING, TrackPhase.FINAL)
                ));
    }

    @Test
    void verifyTrackPhase_PhaseMatches() throws NotFoundException {
        // Assume current phase is bidding
        when(
                trackPhaseCalculator.getTrackPhase(0L, 1L)
        ).thenReturn(TrackPhase.BIDDING);

        // Assume allowed phases are reviewing and bidding. Then the method should not throw
        assertDoesNotThrow(() -> tracksVerification.verifyTrackPhase(
                0L, 1L,
                List.of(TrackPhase.REVIEWING, TrackPhase.BIDDING)
        ));
    }

    @Test
    void verifyTrackPhaseThePaperIsIn_Throws() throws NotFoundException, IllegalAccessException {
        Long conferenceID = fakeSubmission.getEventId();
        Long trackID = fakeSubmission.getTrackId();
        Long paperID = fakeSubmission.getSubmissionId();
        List<TrackPhase> acceptable = List.of(TrackPhase.REVIEWING);

        // When we want to figure out paper parent ID, we ask external repository
        when(
                submissionsCommunicator.getSubmission(paperID)
        ).thenReturn(fakeSubmission);

        // Assume verifyTrackPhase throws
        doThrow(new NotFoundException(""))
                .when(tracksVerification).verifyTrackPhase(conferenceID, trackID, acceptable);
        // Assume allowed phases are reviewing and bidding. Then the method should not throw
        assertThrows(NotFoundException.class, () ->
                tracksVerification.verifyTrackPhaseThePaperIsIn(
                        paperID,
                        acceptable
                ));
    }

    @Test
    void verifyTrackPhaseThePaperIsIn_AllWell() throws NotFoundException, IllegalAccessException {
        Long conferenceID = fakeSubmission.getEventId();
        Long trackID = fakeSubmission.getTrackId();
        Long paperID = fakeSubmission.getSubmissionId();
        List<TrackPhase> acceptable = List.of(TrackPhase.REVIEWING);

        // When we want to figure out paper parent ID, we ask external repository
        when(
                submissionsCommunicator.getSubmission(paperID)
        ).thenReturn(fakeSubmission);

        // Assume verifyTrackPhase throws
        doNothing()
                .when(tracksVerification).verifyTrackPhase(conferenceID, trackID, acceptable);
        // Assume allowed phases are reviewing and bidding. Then the method should not throw
        assertDoesNotThrow(() ->
                tracksVerification.verifyTrackPhaseThePaperIsIn(
                        paperID,
                        acceptable
                )
        );
    }

    @Test
    void verifyTrack_Yes() throws NotFoundException {
        when(usersCommunicator.getTrack(1L, 2L)).thenReturn(null);
        assertThat(tracksVerification.verifyTrack(1L, 2L)).isTrue();
    }

    @Test
    void verifyTrack_No() throws NotFoundException {
        when(usersCommunicator.getTrack(1L, 2L)).thenThrow(
                new NotFoundException("")
        );
        assertThat(tracksVerification.verifyTrack(1L, 2L)).isFalse();
    }


    @Test
    void verifyIfUserCanAccessTrackNoSuchTrack() {
        doReturn(false).when(tracksVerification).verifyTrack(conferenceID, trackID);

        assertThrows(NotFoundException.class, () ->
                tracksVerification.verifyIfUserCanAccessTrack(requesterID, conferenceID, trackID));
    }


    void applyRole(UserRole role) {
        when(
                usersVerification.verifyRoleFromTrack(requesterID, conferenceID, trackID, role)
        ).thenReturn(true);
    }

    @Test
    void verifyIfUserCanAccessTrack_Reviewer() {
        doReturn(true).when(tracksVerification).verifyTrack(conferenceID, trackID);

        // Assume the user is a reviewer
        applyRole(UserRole.REVIEWER);

        assertDoesNotThrow(() ->
                tracksVerification.verifyIfUserCanAccessTrack(requesterID, conferenceID, trackID));
    }

    @Test
    void verifyIfUserCanAccessTrack_Chair() {
        doReturn(true).when(tracksVerification).verifyTrack(conferenceID, trackID);

        // Assume the user is a chair
        applyRole(UserRole.CHAIR);

        assertDoesNotThrow(() ->
                tracksVerification.verifyIfUserCanAccessTrack(requesterID, conferenceID, trackID));
    }

    @Test
    void verifyIfUserCanAccessTrack_Author() {
        doReturn(true).when(tracksVerification).verifyTrack(conferenceID, trackID);


        // Assume the user is a chair
        applyRole(UserRole.AUTHOR);

        assertDoesNotThrow(() ->
                tracksVerification.verifyIfUserCanAccessTrack(requesterID, conferenceID, trackID));
    }

    @Test
    void verifyIfUserCanAccessTrack_NoOne() {
        doReturn(true).when(tracksVerification).verifyTrack(conferenceID, trackID);

        // Assume the user is neither chair nor reviewer

        assertThrows(IllegalAccessException.class, () ->
                tracksVerification.verifyIfUserCanAccessTrack(requesterID, conferenceID, trackID));
    }

}
