package nl.tudelft.sem.v20232024.team08b.unit.services;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.PapersService;
import nl.tudelft.sem.v20232024.team08b.application.phase.PaperPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.application.verification.PapersVerification;
import nl.tudelft.sem.v20232024.team08b.communicators.SubmissionsMicroserviceCommunicator;
import nl.tudelft.sem.v20232024.team08b.dtos.review.Paper;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperStatus;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperSummary;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.repos.PaperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class PapersServiceTests {
    private final SubmissionsMicroserviceCommunicator submissionsCommunicator =
        Mockito.mock(SubmissionsMicroserviceCommunicator.class);
    private final PaperRepository paperRepository = Mockito.mock(PaperRepository.class);
    private final PapersVerification papersVerification = Mockito.mock(PapersVerification.class);
    private final PaperPhaseCalculator paperPhaseCalculator = Mockito.mock(PaperPhaseCalculator.class);

    private PapersService papersService;

    private final Long reviewerID = 1L;
    private final Long paperID = 2L;
    private Submission fakeSubmission;

    @BeforeEach
    void setUp() {
        papersService = Mockito.spy(
                new PapersService(
                        submissionsCommunicator,
                        paperRepository,
                        paperPhaseCalculator,
                        papersVerification
                )
        );

        fakeSubmission = new Submission();
        fakeSubmission.setEventId(3L);
        fakeSubmission.setTrackId(4L);
    }

    @Test
    void getTitleAndAbstract_NoPaperFound() throws NotFoundException, IllegalAccessException {
        doThrow(new NotFoundException("")).when(papersVerification).verifyPermissionToAccessPaper(reviewerID, paperID);

        assertThrows(NotFoundException.class, () -> papersService.getTitleAndAbstract(reviewerID, paperID));
    }

    @Test
    void getTitleAndAbstract_Successful() throws NotFoundException, IllegalAccessException {
        doNothing().when(papersVerification).verifyPermissionToAccessPaper(reviewerID, paperID);
        when(submissionsCommunicator.getSubmission(paperID)).thenReturn(fakeSubmission);

        fakeSubmission.setTitle("Title");
        fakeSubmission.setKeywords(List.of("Keywords"));
        fakeSubmission.setAbstract("Abstract");
        fakeSubmission.setPaper("Content".getBytes());

        PaperSummary expectedPaper = new PaperSummary();
        expectedPaper.setTitle("Title");
        expectedPaper.setAbstractSection("Abstract");


        assertThat(papersService.getTitleAndAbstract(reviewerID, paperID)).isEqualTo(expectedPaper);
    }

    @Test
    void getPaperPhase() throws NotFoundException, IllegalAccessException {
        // Assume that the current phase is bidding
        when(paperPhaseCalculator.getPaperPhase(1L)).thenReturn(PaperPhase.REVIEWED);

        // Assume that the provided input to function is valid
        doNothing().when(papersVerification).verifyPermissionToAccessPaper(0L, 1L);

        // Make sure, that the service returns the same result that the calculator returns
        assertThat(
                papersService.getPaperPhase(0L, 1L)
        ).isEqualTo(PaperPhase.REVIEWED);
        verify(papersVerification).verifyPermissionToAccessPaper(0L, 1L);
    }


    @Test
    void getState_Success() throws NotFoundException, IllegalAccessException {
        doNothing().when(papersVerification).verifyPermissionToViewStatus(reviewerID, paperID);
        nl.tudelft.sem.v20232024.team08b.domain.Paper domainPaper = new nl.tudelft.sem.v20232024.team08b.domain.Paper();
        domainPaper.setStatus(PaperStatus.ACCEPTED);
        Optional<nl.tudelft.sem.v20232024.team08b.domain.Paper> optionalPaper = Optional.of(domainPaper);
        when(paperRepository.findById(paperID)).thenReturn(optionalPaper);

        assertThat(papersService.getState(reviewerID, paperID)).isEqualTo(PaperStatus.ACCEPTED);
    }

    @Test
    void getState_NoPermission() throws IllegalAccessException {
        doThrow(new IllegalAccessException("")).when(papersVerification).verifyPermissionToViewStatus(reviewerID, paperID);

        assertThrows(IllegalAccessException.class, () -> papersService.getState(reviewerID, paperID));
    }

    @Test
    void getState_NoPaperFound() throws IllegalAccessException {
        doNothing().when(papersVerification).verifyPermissionToViewStatus(reviewerID, paperID);
        when(paperRepository.findById(paperID)).thenReturn(Optional.empty());
        when(paperRepository.findById(paperID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> papersService.getState(reviewerID, paperID));
    }

    @Test
    void getPaper_Successful() throws NotFoundException, IllegalAccessException {

        doNothing().when(papersVerification).verifyPermissionToGetPaper(reviewerID, paperID);

        fakeSubmission.setTitle("Title");
        fakeSubmission.setKeywords(List.of("Keywords"));
        fakeSubmission.setAbstract("Abstract");
        fakeSubmission.setPaper("Content".getBytes());

        Paper expectedPaper = new Paper();
        expectedPaper.setTitle("Title");
        expectedPaper.setKeywords(List.of("Keywords"));
        expectedPaper.setAbstractSection("Abstract");
        expectedPaper.setMainText(new String(fakeSubmission.getPaper()));

        when(submissionsCommunicator.getSubmission(paperID)).thenReturn(fakeSubmission);

        assertThat(papersService.getPaper(reviewerID, paperID)).isEqualTo(expectedPaper);
    }

    @Test
    void getPaper_Unsuccessful() throws NotFoundException, IllegalAccessException {

        doThrow(new NotFoundException(""))
                .when(papersVerification).verifyPermissionToGetPaper(reviewerID, paperID);

        assertThrows(NotFoundException.class, () -> papersService.getPaper(reviewerID, paperID));
    }

}