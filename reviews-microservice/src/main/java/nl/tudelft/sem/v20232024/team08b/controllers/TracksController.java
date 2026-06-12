package nl.tudelft.sem.v20232024.team08b.controllers;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.api.TracksAPI;
import nl.tudelft.sem.v20232024.team08b.application.TrackAnalyticsService;
import nl.tudelft.sem.v20232024.team08b.application.TrackDeadlineService;
import nl.tudelft.sem.v20232024.team08b.application.TrackInformationService;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperSummaryWithID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackAnalytics;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackPhase;
import nl.tudelft.sem.v20232024.team08b.exceptions.ForbiddenAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
public class TracksController implements TracksAPI {
    private final TrackInformationService trackInformationService;
    private final TrackAnalyticsService trackAnalyticsService;
    private final TrackDeadlineService trackDeadlineService;

    /**
     * Default constructor for the controller.
     *
     * @param trackInformationService service that manages track information
     * @param trackAnalyticsService service that manages the analytics of tracks
     * @param trackDeadlineService service that manages the deadlines of tracks
     */
    @Autowired
    public TracksController(TrackInformationService trackInformationService,
                            TrackAnalyticsService trackAnalyticsService,
                            TrackDeadlineService trackDeadlineService) {
        this.trackInformationService = trackInformationService;
        this.trackAnalyticsService = trackAnalyticsService;
        this.trackDeadlineService = trackDeadlineService;
    }

    /**
     * Returns all the papers in the given track of a conference.
     *
     * @param requesterID the ID of the requesting user
     * @param conferenceID the ID of the conference
     * @param trackID the ID of the track
     * @return response entity with the result
     */
    @Override
    public ResponseEntity<List<PaperSummaryWithID>> getPapers(Long requesterID,
                                                              Long conferenceID,
                                                              Long trackID) {
        try {
            return ResponseEntity.ok(
                trackInformationService.getPapers(requesterID, conferenceID, trackID)
            );
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (ForbiddenAccessException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Returns the numbers of accepted, rejected and not-yet-decided papers.
     *
     * @param requesterID the ID of the requesting user
     * @param conferenceID the ID of the conference
     * @param trackID the ID of the track
     * @return response entity with the result
     */
    @Override
    public ResponseEntity<TrackAnalytics> getAnalytics(
            Long requesterID, Long conferenceID, Long trackID
    ) {
        try {
            return ResponseEntity.ok(
                    trackAnalyticsService.getAnalytics(new TrackID(conferenceID, trackID), requesterID)
            );
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (ForbiddenAccessException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Changes or sets the bidding deadline of a track.
     *
     * @param requesterID the ID of the requesting user
     * @param conferenceID the ID of the conference
     * @param trackID the ID of the track
     * @param newDeadline the new deadline to be set
     * @return response entity with the result
     */
    @Override
    public ResponseEntity<Void> setBiddingDeadline(Long requesterID,
                                                   Long conferenceID,
                                                   Long trackID,
                                                   Date newDeadline) {
        try {
            trackDeadlineService.setBiddingDeadline(
                    requesterID,
                    conferenceID,
                    trackID,
                    newDeadline
            );
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .build();
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalAccessException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Gets the bidding deadline of a track.
     *
     * @param requesterID the ID of the requesting user
     * @param conferenceID the ID of the conference
     * @param trackID the ID of the track
     * @return response entity with the result
     */
    @Override
    public ResponseEntity<Date> getBiddingDeadline(Long requesterID,
                                                   Long conferenceID,
                                                   Long trackID) {
        try {
            Date biddingDeadline = trackDeadlineService.getBiddingDeadline(
                    requesterID,
                    conferenceID,
                    trackID
            );
            return ResponseEntity.ok(biddingDeadline);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalAccessException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Responds with the review phase of the track.
     *
     * @param requesterID the ID of the requesting user
     * @param conferenceID the ID of the conference
     * @param trackID the ID of the track
     * @return response entity with the result
     */
    @Override
    public ResponseEntity<TrackPhase> getPhase(Long requesterID,
                                               Long conferenceID,
                                               Long trackID) {
        try {
            TrackPhase trackPhase = trackInformationService.getTrackPhase(
                    requesterID,
                    conferenceID,
                    trackID
            );
            return ResponseEntity.ok(trackPhase);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IllegalAccessException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
