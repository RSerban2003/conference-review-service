package nl.tudelft.sem.v20232024.team08b.unit.services;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.TrackInformationService;
import nl.tudelft.sem.v20232024.team08b.application.phase.TrackPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.application.verification.TracksVerification;
import nl.tudelft.sem.v20232024.team08b.application.verification.UsersVerification;
import nl.tudelft.sem.v20232024.team08b.communicators.SubmissionsMicroserviceCommunicator;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperSummaryWithID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.exceptions.ForbiddenAccessException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TrackInformationServicesTests {
    private final TracksVerification tracksVerification = Mockito.mock(TracksVerification.class);
    private final UsersVerification usersVerification = Mockito.mock(UsersVerification.class);
    private final TrackPhaseCalculator trackPhaseCalculator = Mockito.mock(TrackPhaseCalculator.class);
    private final SubmissionsMicroserviceCommunicator submissionsCommunicator =
            Mockito.mock(SubmissionsMicroserviceCommunicator.class);


    private final TrackInformationService trackInformationService = Mockito.spy(
            new TrackInformationService(
                    trackPhaseCalculator,
                    tracksVerification,
                    usersVerification,
                    submissionsCommunicator
            )
    );

    @Test
    void getTrackPhase() throws NotFoundException, IllegalAccessException {
        // Assume that the current phase is bidding
        Long conferenceID = 1L;
        Long trackID = 2L;
        when(trackPhaseCalculator.getTrackPhase(conferenceID, trackID)).thenReturn(TrackPhase.BIDDING);

        // Assume that the provided input to function is valid
        Long requesterID = 0L;
        doNothing().when(tracksVerification).verifyIfUserCanAccessTrack(requesterID, conferenceID, trackID);

        // Make sure, that the service returns the same result that the calculator returns
        assertThat(
                trackInformationService.getTrackPhase(requesterID, conferenceID, trackID)
        ).isEqualTo(TrackPhase.BIDDING);
        verify(tracksVerification).verifyIfUserCanAccessTrack(requesterID, conferenceID, trackID);
    }

    @Test
    void getPaperNoPermission() {
        Long requesterID = 3L;
        Long conferenceID = 4L;
        Long trackID = 5L;
        when(usersVerification.verifyRoleFromTrack(requesterID, conferenceID,
                trackID, UserRole.CHAIR)).thenReturn(false);
        assertThrows(ForbiddenAccessException.class, () ->
                trackInformationService.getPapers(requesterID, conferenceID, trackID));
    }

    @Test
    void getPaperNoTrack() {
        Long requesterID = 3L;
        Long conferenceID = 4L;
        Long trackID = 5L;
        when(usersVerification.verifyRoleFromTrack(requesterID, conferenceID,
                trackID, UserRole.CHAIR)).thenReturn(true);
        when(tracksVerification.verifyTrack(conferenceID,
                trackID)).thenReturn(false);
        assertThrows(NotFoundException.class, () ->
                trackInformationService.getPapers(requesterID, conferenceID, trackID));
    }

    @Test
    void getZeroPaper() throws NotFoundException, ForbiddenAccessException {
        Long requesterID = 3L;
        Long conferenceID = 4L;
        Long trackID = 5L;
        when(usersVerification.verifyRoleFromTrack(requesterID, conferenceID,
                trackID, UserRole.CHAIR)).thenReturn(true);
        when(tracksVerification.verifyTrack(conferenceID,
                trackID)).thenReturn(true);
        when(submissionsCommunicator.getSubmissionsInTrack(conferenceID, trackID, requesterID))
                .thenReturn(new ArrayList<>());
        List<PaperSummaryWithID> papers = new ArrayList<>();
        assertThat(trackInformationService.getPapers(requesterID, conferenceID, trackID)).isEqualTo(papers);
    }

    @Test
    void getOnePaper() throws NotFoundException, ForbiddenAccessException {
        Long requesterID = 3L;
        Long conferenceID = 4L;
        Long trackID = 5L;
        when(usersVerification.verifyRoleFromTrack(requesterID, conferenceID,
                trackID, UserRole.CHAIR)).thenReturn(true);
        when(tracksVerification.verifyTrack(conferenceID,
                trackID)).thenReturn(true);
        Submission submission1 = new Submission();
        submission1.setTitle("abc");
        submission1.setAbstract("def");
        submission1.setSubmissionId(1L);
        List<Submission> submissions = new ArrayList<>();
        submissions.add(submission1);
        when(submissionsCommunicator.getSubmissionsInTrack(conferenceID, trackID, requesterID)).thenReturn(submissions);
        var paper1 = new PaperSummaryWithID();
        paper1.setPaperID(1L);
        paper1.setTitle("abc");
        paper1.setAbstractSection("def");
        List<PaperSummaryWithID> papers = new ArrayList<>();
        papers.add(paper1);
        var papersSolution = trackInformationService.getPapers(requesterID, conferenceID, trackID);
        var idExample = papers.stream().map(PaperSummaryWithID::getPaperID).toArray();
        var idSolution = papersSolution.stream().map(PaperSummaryWithID::getPaperID).toArray();
        assertThat(idSolution).isEqualTo(idExample);
        var titleExample = papers.stream().map(PaperSummaryWithID::getTitle).toArray();
        var titleSolution = papersSolution.stream().map(PaperSummaryWithID::getTitle).toArray();
        assertThat(titleSolution).isEqualTo(titleExample);
        var abstractExample = papers.stream().map(PaperSummaryWithID::getAbstractSection).toArray();
        var abstractSolution = papersSolution.stream().map(PaperSummaryWithID::getAbstractSection).toArray();
        assertThat(abstractSolution).isEqualTo(abstractExample);
    }

    @Test
    void getTwoPapers() throws NotFoundException, ForbiddenAccessException {
        Long requesterID = 3L;
        Long conferenceID = 4L;
        Long trackID = 5L;
        when(usersVerification.verifyRoleFromTrack(requesterID, conferenceID,
                trackID, UserRole.CHAIR)).thenReturn(true);
        when(tracksVerification.verifyTrack(conferenceID,
                trackID)).thenReturn(true);
        Submission submission1 = new Submission();
        submission1.setTitle("abc");
        submission1.setAbstract("def");
        submission1.setSubmissionId(1L);
        Submission submission2 = new Submission();
        submission2.setTitle("zyx");
        submission2.setAbstract("wvu");
        submission2.setSubmissionId(2L);
        List<Submission> submissions = new ArrayList<>();
        submissions.add(submission1);
        submissions.add(submission2);
        when(submissionsCommunicator.getSubmissionsInTrack(conferenceID, trackID, requesterID)).thenReturn(submissions);
        var paper1 = new PaperSummaryWithID();
        paper1.setPaperID(1L);
        paper1.setTitle("abc");
        paper1.setAbstractSection("def");
        var paper2 = new PaperSummaryWithID();
        paper2.setPaperID(2L);
        paper2.setTitle("zyx");
        paper2.setAbstractSection("wvu");
        List<PaperSummaryWithID> papers = new ArrayList<>();
        papers.add(paper1);
        papers.add(paper2);
        var papersSolution = trackInformationService.getPapers(requesterID, conferenceID, trackID);
        var idExample = papers.stream().map(PaperSummaryWithID::getPaperID).toArray();
        var idSolution = papersSolution.stream().map(PaperSummaryWithID::getPaperID).toArray();
        assertThat(idSolution).isEqualTo(idExample);
        var titleExample = papers.stream().map(PaperSummaryWithID::getTitle).toArray();
        var titleSolution = papersSolution.stream().map(PaperSummaryWithID::getTitle).toArray();
        assertThat(titleSolution).isEqualTo(titleExample);
        var abstractExample = papers.stream().map(PaperSummaryWithID::getAbstractSection).toArray();
        var abstractSolution = papersSolution.stream().map(PaperSummaryWithID::getAbstractSection).toArray();
        assertThat(abstractSolution).isEqualTo(abstractExample);
    }
}
