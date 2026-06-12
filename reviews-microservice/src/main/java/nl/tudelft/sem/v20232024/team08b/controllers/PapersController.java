package nl.tudelft.sem.v20232024.team08b.controllers;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.api.PapersAPI;
import nl.tudelft.sem.v20232024.team08b.application.PapersService;
import nl.tudelft.sem.v20232024.team08b.dtos.review.Paper;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperStatus;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PapersController implements PapersAPI {
    private final PapersService papersService;

    /**
     * Default constructor for the controller.
     *
     * @param papersService the respective service to inject
     */
    @Autowired
    public PapersController(PapersService papersService) {
        this.papersService = papersService;
    }

    /**
     * Retrieves a summary view of a paper, which includes only its title and abstract, for a specific requester.
     * The method also checks if the requester has the appropriate permissions.
     * All caught exceptions are handled and converted into appropriate HTTP status responses.
     * - NotFoundException results in an HTTP NOT_FOUND status if the paper with the specified ID is not found.
     * - IllegalAccessException results in an HTTP FORBIDDEN status
     * if the requester does not have the appropriate permissions.
     * - Other general exceptions result in an HTTP INTERNAL_SERVER_ERROR status, indicating an internal server issue.
     *
     * @param requesterID The unique identifier of the user making the request.
     * @param paperID The unique identifier of the paper whose title and abstract are being requested.
     * @return A ResponseEntity containing the paper summary, which includes the title and abstract of the paper.
     */
    @Override
    public ResponseEntity<PaperSummary> getTitleAndAbstract(Long requesterID,
                                                            Long paperID) {
        try {
            return ResponseEntity.ok(papersService.getTitleAndAbstract(requesterID, paperID));
        } catch (IllegalCallerException | NotFoundException e) {
            // The requested paper was not found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalAccessException e) {
            // The requester must be a reviewer assigned to the given paper or a chair,
            // and the review phase must have started
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            // Internal server error
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves a paper by its ID, omitting author names, for a specific requester.
     * It also checks whether the requester has the appropriate permissions.
     * All caught exceptions are handled and converted into appropriate HTTP status responses.
     * - NotFoundException results in an HTTP NOT_FOUND status if the paper with the specified ID is not found.
     * - IllegalAccessException results in an HTTP FORBIDDEN status
     * if the requester does not have the appropriate permissions.
     * - Other general exceptions result in an HTTP INTERNAL_SERVER_ERROR status, indicating an internal server issue.
     *
     * @param requesterID the ID of the requesting user
     * @param paperID the ID of the paper
     * @return A ResponseEntity indicating the success of the request or the type of failure if an error occurs.
     */
    @Override
    public ResponseEntity<Paper> get(Long requesterID,
                                     Long paperID) {
        try {
            return ResponseEntity.ok(papersService.getPaper(requesterID, paperID));
        } catch (IllegalCallerException | NotFoundException e) {
            // The requested paper was not found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalAccessException e) {
            // The requester must be a reviewer assigned to the given paper or a chair,
            // and the review phase must have started
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            // Internal server error
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Responds with the status of the paper (whether it was accepted, rejected or neither so far).
     * The requesting user has to be a reviewer of the paper, an author of the paper,
     * or a chair in the track of the paper.
     *
     * @param requesterID the ID of the requesting user
     * @param paperID the ID of the paper
     * @return response entity with the status of the paper
     */
    @Override
    public ResponseEntity<PaperStatus> getState(Long requesterID,
                                                Long paperID) {
        try {
            return ResponseEntity.ok(papersService.getState(requesterID, paperID));
        } catch (NotFoundException e) {
            // The requested paper was not found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalAccessException e) {
            // The requester must be a reviewer assigned to the given paper,
            // or a chair of the track the paper is in,
            // or an author of the paper.
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            // Internal server error
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
