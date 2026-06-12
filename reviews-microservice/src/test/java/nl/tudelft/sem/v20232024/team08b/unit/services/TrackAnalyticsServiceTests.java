package nl.tudelft.sem.v20232024.team08b.unit.services;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.PapersService;
import nl.tudelft.sem.v20232024.team08b.application.TrackAnalyticsService;
import nl.tudelft.sem.v20232024.team08b.application.verification.UsersVerification;
import nl.tudelft.sem.v20232024.team08b.communicators.SubmissionsMicroserviceCommunicator;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperStatus;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackAnalytics;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.exceptions.ForbiddenAccessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

public class TrackAnalyticsServiceTests {
    private final UsersVerification usersVerification = Mockito.mock(UsersVerification.class);
    private final SubmissionsMicroserviceCommunicator submissionsCommunicator =
            Mockito.mock(SubmissionsMicroserviceCommunicator.class);
    private final PapersService papersService = Mockito.mock(PapersService.class);

    private final TrackAnalyticsService trackAnalyticsService = Mockito.spy(
            new TrackAnalyticsService(
                    usersVerification,
                    submissionsCommunicator,
                    papersService
            )
    );

    @Test
    void testGetAnalyticsSuccess() throws NotFoundException, ForbiddenAccessException, IllegalAccessException {
        TrackID trackID = new TrackID();
        trackID.setConferenceID(1L);
        trackID.setTrackID(2L);
        final Long requesterID = 3L;

        List<Submission> submissions = new ArrayList<>();
        Submission submission1 = new Submission();
        submission1.setSubmissionId(1L);
        submissions.add(submission1);

        Submission submission2 = new Submission();
        submission2.setSubmissionId(2L);
        submissions.add(submission2);

        Submission submission3 = new Submission();
        submission3.setSubmissionId(3L);
        submissions.add(submission3);

        Submission submission4 = new Submission();
        submission4.setSubmissionId(4L);
        submissions.add(submission4);

        Submission submission5 = new Submission();
        submission5.setSubmissionId(5L);
        submissions.add(submission5);

        Submission submission6 = new Submission();
        submission6.setSubmissionId(6L);
        submissions.add(submission6);

        when(usersVerification.verifyRoleFromTrack(requesterID, trackID.getConferenceID(),
                trackID.getTrackID(), UserRole.CHAIR)).thenReturn(true);

        when(submissionsCommunicator.getSubmissionsInTrack(trackID.getConferenceID(), trackID.getTrackID(), requesterID))
                .thenReturn(submissions);

        when(papersService.getState(requesterID, 1L)).thenReturn(PaperStatus.ACCEPTED);
        when(papersService.getState(requesterID, 2L)).thenReturn(PaperStatus.REJECTED);
        when(papersService.getState(requesterID, 3L)).thenReturn(PaperStatus.NOT_DECIDED);
        when(papersService.getState(requesterID, 4L)).thenReturn(PaperStatus.ACCEPTED);
        when(papersService.getState(requesterID, 5L)).thenReturn(PaperStatus.ACCEPTED);
        when(papersService.getState(requesterID, 6L)).thenReturn(PaperStatus.NOT_DECIDED);

        TrackAnalytics result = trackAnalyticsService.getAnalytics(trackID, requesterID);

        Assertions.assertEquals(3, result.getAccepted());
        Assertions.assertEquals(1, result.getRejected());
        Assertions.assertEquals(2, result.getUnknown());
    }

    @Test
    void testGetAnalyticsNotFoundException() throws NotFoundException {
        TrackID trackID = new TrackID();
        trackID.setConferenceID(1L);
        trackID.setTrackID(2L);
        Long requesterID = 3L;

        when(usersVerification.verifyRoleFromTrack(requesterID, trackID.getConferenceID(),
                trackID.getTrackID(), UserRole.CHAIR)).thenReturn(true);

        when(submissionsCommunicator.getSubmissionsInTrack(trackID.getConferenceID(), trackID.getTrackID(), requesterID))
                .thenThrow(NotFoundException.class);

        Assertions.assertThrows(NotFoundException.class, () -> trackAnalyticsService.getAnalytics(trackID, requesterID));
    }

    @Test
    void testGetAnalyticsForbiddenAccessException() {
        TrackID trackID = new TrackID();
        trackID.setConferenceID(1L);
        trackID.setTrackID(2L);
        Long requesterID = 3L;

        when(usersVerification.verifyRoleFromTrack(requesterID, trackID.getConferenceID(),
                trackID.getTrackID(), UserRole.CHAIR)).thenReturn(false);

        Assertions.assertThrows(ForbiddenAccessException.class, () ->
                trackAnalyticsService.getAnalytics(trackID, requesterID));
    }

    @Test
    void testGetAnalyticsRuntimeException()
            throws NotFoundException, IllegalAccessException {
        TrackID trackID = new TrackID();
        trackID.setConferenceID(1L);
        trackID.setTrackID(1L);
        Long requesterID = 1L;

        List<Submission> submissions = new ArrayList<>();
        Submission submission1 = new Submission();
        submission1.setSubmissionId(1L);
        submissions.add(submission1);

        when(usersVerification.verifyRoleFromTrack(requesterID, trackID.getConferenceID(),
                trackID.getTrackID(), UserRole.CHAIR)).thenReturn(true);

        when(submissionsCommunicator.getSubmissionsInTrack(trackID.getConferenceID(), trackID.getTrackID(), requesterID))
                .thenReturn(submissions);

        when(papersService.getState(1L, requesterID)).thenThrow(new IllegalAccessException());

        Assertions.assertThrows(RuntimeException.class, () -> trackAnalyticsService.getAnalytics(trackID, requesterID));
    }
}
