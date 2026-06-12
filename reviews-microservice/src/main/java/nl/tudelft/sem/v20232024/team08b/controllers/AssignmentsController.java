package nl.tudelft.sem.v20232024.team08b.controllers;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.api.AssignmentsAPI;
import nl.tudelft.sem.v20232024.team08b.application.AssignmentsService;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperSummaryWithID;
import nl.tudelft.sem.v20232024.team08b.exceptions.ConflictOfInterestException;
import nl.tudelft.sem.v20232024.team08b.exceptions.ForbiddenAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AssignmentsController implements AssignmentsAPI {
    private final AssignmentsService assignmentsService;

    /**
     * Default constructor for the controller.
     *
     * @param assignmentsService the respective service to inject
     */
    @Autowired
    public AssignmentsController(AssignmentsService assignmentsService) {
        this.assignmentsService = assignmentsService;
    }

    /**
     * Manually assigns reviewer to a specific paper. At least 3
     * reviewers must be assigned to a paper.
     *
     * @param requesterID the ID of the requesting user
     * @param reviewerID the ID of the reviewer
     * @param paperID the ID of the paper
     * @return response entity with the result
     */
    @Override
    public ResponseEntity<Void> assignManual(Long requesterID,
                                               Long reviewerID,
                                               Long paperID) {
        try {
            assignmentsService.assignManually(requesterID, reviewerID, paperID);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .build();
        } catch (IllegalCallerException | NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalAccessException e) {
            // The requester must be a pc chair
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (ConflictOfInterestException e) {
            // There is a COI
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (Exception e) {
            // Internal server error
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * Automatically assigns a reviewer to a specific paper.
     *
     * @param requesterID the ID of the requesting user the ID of the requesting user
     * @param conferenceID the ID of the conference
     * @param trackID the ID of the track
     * @return response entity with the result
     */
    @Override
    public ResponseEntity<Void> assignAuto(Long requesterID,
                                           Long conferenceID,
                                           Long trackID) {
        try {
            assignmentsService.assignAuto(requesterID, conferenceID, trackID);
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .build();

        } catch (IllegalCallerException | NotFoundException e) {
            // The requested track or user was not found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalAccessException e) {
            // The requester must be a reviewer assigned to the given paper or a chair,
            // and the review phase must have started
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            // The requester must be a reviewer assigned to the given paper or a chair,
            // and the review phase must have started
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (Exception e) {
            // Internal server error
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Finalises the assignment of reviewers, so they can no longer be changed
     * manually or automatically.
     *
     * @param requesterID the ID of the requesting user
     * @param conferenceID the ID of the conference
     * @param trackID the ID of the track
     * @return response entity with the result
     */
    @Override
    public ResponseEntity<Void> finalization(Long requesterID, Long conferenceID, Long trackID) {
        try {
            assignmentsService.finalization(requesterID, conferenceID, trackID);
            return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).build();
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (ForbiddenAccessException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Get current assignments for a paper.
     *
     * @param requesterID the ID of the requesting user
     * @param paperID the ID of the paper
     * @return response entity with the result
     */
    @Override
    public ResponseEntity<List<Long>> assignments(Long requesterID,
                                                  Long paperID) {
        try {
            return ResponseEntity.ok(assignmentsService.assignments(requesterID, paperID));
        } catch (IllegalCallerException | NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalAccessException e) {
            // The requester must be a pc chair
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            // The requester must be a pc chair
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (Exception e) {
            // Internal server error
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Removes a reviewer from a paper.
     *
     * @param requesterID the ID of the requesting user
     * @param paperID the ID of the paper
     * @param reviewerID the ID of the reviewer
     * @return response entity with the result
     */
    @Override
    public ResponseEntity<Void> remove(Long requesterID,
                                       Long paperID,
                                       Long reviewerID) {
        try {
            assignmentsService.remove(requesterID, paperID, reviewerID);
            return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .build();
        } catch (IllegalCallerException | NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalAccessException e) {
            // The requester must be a pc chair
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            // Internal server error
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Gets all papers a reviewer (the requester) is assigned to.
     *
     * @param requesterID the ID of the requesting user
     * @return response entity with the result
     */
    @Override
    public ResponseEntity<List<PaperSummaryWithID>> getAssignedPapers(Long requesterID) {
        try {
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(assignmentsService.getAssignedPapers(requesterID));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
