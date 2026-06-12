package nl.tudelft.sem.v20232024.team08b.unit.verification;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.phase.PaperPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.application.verification.PapersVerification;
import nl.tudelft.sem.v20232024.team08b.application.verification.TracksVerification;
import nl.tudelft.sem.v20232024.team08b.application.verification.UsersVerification;
import nl.tudelft.sem.v20232024.team08b.communicators.SubmissionsMicroserviceCommunicator;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.UserRole;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.User;
import nl.tudelft.sem.v20232024.team08b.exceptions.ConflictOfInterestException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class PapersVerificationTests {
    final SubmissionsMicroserviceCommunicator externalRepository = Mockito.mock(SubmissionsMicroserviceCommunicator.class);
    final UsersVerification usersVerification = Mockito.mock(UsersVerification.class);
    final TracksVerification tracksVerification = Mockito.mock(TracksVerification.class);
    final PaperPhaseCalculator paperPhaseCalculator = Mockito.mock(PaperPhaseCalculator.class);

    final PapersVerification papersVerification = Mockito.spy(new PapersVerification(
            externalRepository,
            usersVerification,
            tracksVerification,
            paperPhaseCalculator
    ));


    private Submission fakeSubmission;
    private final Long reviewerID = 1L;
    private final Long paperID = 2L;

    @BeforeEach
    void prepare() {
        fakeSubmission = new Submission();
        fakeSubmission.setTrackId(3L);
    }

    @Test
    void verifyPaperExists() throws NotFoundException {
        when(externalRepository.getSubmission(1L)).thenReturn(fakeSubmission);
        assertThat(papersVerification.verifyPaper(1L)).isEqualTo(true);
    }

    @Test
    void verifyPaperDoesNotExist() throws NotFoundException {
        when(externalRepository.getSubmission(1L)).thenThrow(new NotFoundException(""));
        assertThat(papersVerification.verifyPaper(1L)).isEqualTo(false);
    }

    @Test
    void verifyCOI() throws NotFoundException {
        when(externalRepository.getSubmission(1L)).thenReturn(fakeSubmission);
        //empty coi's list
        assertDoesNotThrow(() -> papersVerification.verifyCOI(1L, 1L));
        User u1 = new User();
        u1.userId(5L);
        User u2 = new User();
        u2.userId(6L);
        User u3 = new User();
        u3.userId((7L));
        List<@Valid User> users = new ArrayList<>();
        users.add(u1);
        users.add(u2);
        users.add(u3);
        fakeSubmission.setConflictsOfInterest(users);
        //not in coi;s list
        assertDoesNotThrow(() -> papersVerification.verifyCOI(1L, 1L));
        //coi exists
        assertThrows(ConflictOfInterestException.class, () -> papersVerification.verifyCOI(1L, 5L));
    }


    @Test
    void verifyPermissionToViewStatus_UserIsReviewer() throws IllegalAccessException {
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(true);
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.REVIEWER)).thenReturn(true);

        papersVerification.verifyPermissionToViewStatus(reviewerID, paperID);
    }

    @Test
    void verifyPermissionToViewStatus_UserIsReviewerButToDifferentPaper() {
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(false);
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.REVIEWER)).thenReturn(true);

        assertThrows(IllegalAccessException.class, () ->
                papersVerification.verifyPermissionToViewStatus(reviewerID, paperID));
    }

    @Test
    void verifyPermissionToViewStatus_UserIsAuthor() throws IllegalAccessException {
        when(usersVerification.isAuthorToPaper(reviewerID, paperID)).thenReturn(true);
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.AUTHOR)).thenReturn(true);

        papersVerification.verifyPermissionToViewStatus(reviewerID, paperID);
    }

    @Test
    void verifyPermissionToViewStatus_UserIsAuthorButToDifferentPaper() {
        when(usersVerification.isAuthorToPaper(reviewerID, paperID)).thenReturn(false);
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.AUTHOR)).thenReturn(true);

        assertThrows(IllegalAccessException.class, () ->
                papersVerification.verifyPermissionToViewStatus(reviewerID, paperID));
    }

    @Test
    void verifyPermissionToViewStatus_UserIsChair() throws IllegalAccessException {
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.CHAIR)).thenReturn(true);

        papersVerification.verifyPermissionToViewStatus(reviewerID, paperID);
    }

    @Test
    void verifyPermissionToViewStatus_UserDoesNotHavePermission() {
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(false);
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.REVIEWER)).thenReturn(false);
        when(usersVerification.isAuthorToPaper(reviewerID, paperID)).thenReturn(false);
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.AUTHOR)).thenReturn(false);
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.CHAIR)).thenReturn(false);

        assertThrows(IllegalAccessException.class, () ->
                papersVerification.verifyPermissionToViewStatus(reviewerID, paperID));
    }

    @Test
    void verifyPermissionToGetPaper_NoPaperFound() {
        doReturn(false).when(papersVerification).verifyPaper(paperID);
        assertThrows(NotFoundException.class, () -> papersVerification.verifyPermissionToGetPaper(reviewerID, paperID));
    }

    @Test
    void verifyPermissionToGetPaper_NoUserFound() throws Exception {
        doReturn(true).when(papersVerification).verifyPaper(paperID);
        when(externalRepository.getSubmission(paperID)).thenReturn(fakeSubmission);
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.REVIEWER)).thenReturn(false);

        assertThrows(IllegalCallerException.class, () -> papersVerification.verifyPermissionToGetPaper(reviewerID, paperID));
    }

    @Test
    void verifyPermissionToGetPaper_NotAReviewer() throws Exception {
        doReturn(true).when(papersVerification).verifyPaper(paperID);
        when(externalRepository.getSubmission(paperID)).thenReturn(fakeSubmission);
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.REVIEWER)).thenReturn(true);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(false);

        assertThrows(IllegalAccessException.class, () -> papersVerification.verifyPermissionToGetPaper(reviewerID, paperID));
    }

    @Test
    void verifyPermissionToGetPaper_NoSubmissionFound() throws NotFoundException {
        when(externalRepository.getSubmission(paperID)).thenThrow(new NotFoundException(""));

        assertThrows(NotFoundException.class, () -> papersVerification.verifyPermissionToGetPaper(reviewerID, paperID));
    }

    @Test
    void verifyPermissionToGetPaper_WrongPhase() throws NotFoundException, IllegalAccessException {
        // Make verifyPermissionToViewUser() passes nicely
        doReturn(true).when(papersVerification).verifyPaper(paperID);
        when(externalRepository.getSubmission(paperID)).thenReturn(fakeSubmission);
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.REVIEWER)).thenReturn(true);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(true);

        // Make sure exception is thrown when phase is checked
        doThrow(
                new IllegalAccessException("")
        ).when(tracksVerification).verifyTrackPhaseThePaperIsIn(
                paperID,
                List.of(TrackPhase.SUBMITTING, TrackPhase.REVIEWING, TrackPhase.FINAL)
        );

        // Assert that the method itself passes the exception upstream
        assertThrows(IllegalAccessException.class, () -> papersVerification.verifyPermissionToGetPaper(reviewerID, paperID));
    }

    @Test
    void verifyPermissionToGetPaper_Successful_Reviewer() throws NotFoundException {

        doReturn(true).when(papersVerification).verifyPaper(paperID);
        when(externalRepository.getSubmission(paperID)).thenReturn(fakeSubmission);
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.REVIEWER)).thenReturn(true);
        when(usersVerification.isReviewerForPaper(reviewerID, paperID)).thenReturn(true);

        assertDoesNotThrow(() -> papersVerification.verifyPermissionToGetPaper(reviewerID, paperID));
    }

    @Test
    void verifyPermissionToGetPaper_Successful_Chair() throws NotFoundException {

        doReturn(true).when(papersVerification).verifyPaper(paperID);
        when(externalRepository.getSubmission(paperID)).thenReturn(fakeSubmission);
        when(usersVerification.verifyRoleFromPaper(reviewerID, paperID, UserRole.CHAIR)).thenReturn(true);

        assertDoesNotThrow(() -> papersVerification.verifyPermissionToGetPaper(reviewerID, paperID));

    }

    @Test
    void testVerifyPhasePaperIsIn_True() throws NotFoundException {
        PaperPhase phase = PaperPhase.REVIEWED;

        when(paperPhaseCalculator.getPaperPhase(paperID)).thenReturn(PaperPhase.REVIEWED);

        Assertions.assertTrue(papersVerification.verifyPhasePaperIsIn(paperID, phase));
    }

    @Test
    void testVerifyPhasePaperIsIn_False() throws NotFoundException {
        PaperPhase phase = PaperPhase.BEFORE_REVIEW;

        when(paperPhaseCalculator.getPaperPhase(paperID)).thenReturn(PaperPhase.REVIEWED);

        Assertions.assertFalse(papersVerification.verifyPhasePaperIsIn(paperID, phase));
    }

    @Test
    void testVerifyPhasePaperIsIn_NotFoundException() throws NotFoundException {
        PaperPhase phase = PaperPhase.BEFORE_REVIEW;

        when(paperPhaseCalculator.getPaperPhase(paperID)).thenThrow(new NotFoundException("No such paper exists"));

        assertThrows(NotFoundException.class, () -> papersVerification.verifyPhasePaperIsIn(paperID, phase));
    }
}
