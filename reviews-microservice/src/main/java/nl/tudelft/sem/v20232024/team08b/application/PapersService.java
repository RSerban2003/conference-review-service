package nl.tudelft.sem.v20232024.team08b.application;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.phase.PaperPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.application.verification.PapersVerification;
import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithSubmissionMicroservice;
import nl.tudelft.sem.v20232024.team08b.domain.Paper;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperStatus;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperSummary;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.repos.PaperRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PapersService {
    private final CommunicationWithSubmissionMicroservice submissionsCommunicator;
    private final PaperRepository paperRepository;
    private final PapersVerification papersVerification;
    private final PaperPhaseCalculator paperPhaseCalculator;

    /**
     * Default constructor for the service.
     *
     * @param submissionsCommunicator Class that talks with submission microservice
     * @param paperRepository repository storing papers
     * @param paperPhaseCalculator class that calculates the phase of a paper
     * @param papersVerification object responsible for verifying paper information
     */
    @Autowired
    public PapersService(CommunicationWithSubmissionMicroservice submissionsCommunicator,
                         PaperRepository paperRepository,
                         PaperPhaseCalculator paperPhaseCalculator,
                         PapersVerification papersVerification) {
        this.submissionsCommunicator = submissionsCommunicator;
        this.paperRepository = paperRepository;
        this.paperPhaseCalculator = paperPhaseCalculator;
        this.papersVerification = papersVerification;
    }


    /**
     * Returns the content of the paper from the repository.
     *
     * @param reviewerID ID of the reviewer requesting the paper
     * @param paperID ID of the paper being requested
     * @return the paper, if all conditions are met
     */
    public nl.tudelft.sem.v20232024.team08b.dtos.review.Paper getPaper(Long reviewerID,
                                                                       Long paperID)
            throws NotFoundException, IllegalAccessException {
        // Verify that user has permission to view the paper, while also checking phase (true)
        papersVerification.verifyPermissionToGetPaper(reviewerID, paperID);

        Submission submission = submissionsCommunicator.getSubmission(paperID);

        return new nl.tudelft.sem.v20232024.team08b.dtos.review.Paper(submission);
    }


    /**
     * Returns the title and abstract of the paper from the external repository.
     *
     * @param reviewerID ID of the reviewer requesting the paper
     * @param paperID ID of the paper being requested
     * @return the paper, if all conditions are met
     */
    public PaperSummary getTitleAndAbstract(Long reviewerID, Long paperID) throws NotFoundException,
                                                                           IllegalAccessException {
        papersVerification.verifyPermissionToAccessPaper(reviewerID, paperID);

        // Track phase verification for reading an abstract of a paper is not necessary,
        // since title and abstract can be read during all phases

        return new PaperSummary(submissionsCommunicator.getSubmission(paperID));
    }

    /**
     * Gets the phase of the requested paper.
     *
     * @param requesterID the ID of the requesting user
     * @param paperID the ID of the paper
     * @return current phase of the paper
     * @throws NotFoundException if such paper does not exist
     * @throws IllegalAccessException if the user is not allowed to view the paper
     */
    public PaperPhase getPaperPhase(Long requesterID,
                                    Long paperID) throws NotFoundException, IllegalAccessException {
        papersVerification.verifyPermissionToAccessPaper(requesterID, paperID);
        return paperPhaseCalculator.getPaperPhase(paperID);
    }


    /**
     * Shows the status of a paper given by paperID to a user given by userID
     * after verifying if the user has the appropriate permissions.
     *
     * @param requesterID the ID of the user
     * @param paperID the ID of the paper
     * @return the status of the paper
     * @throws NotFoundException if the paper does not exist
     * @throws IllegalAccessException if the user does not have the required permissions
     */
    public PaperStatus getState(Long requesterID,
                                Long paperID) throws NotFoundException, IllegalAccessException {
        papersVerification.verifyPermissionToViewStatus(requesterID, paperID);
        Optional<Paper> optional = paperRepository.findById(paperID);
        if (optional.isPresent()) {
            return optional.get().getStatus();
        } else {
            throw new NotFoundException("The paper could not be found");
        }
    }
}
