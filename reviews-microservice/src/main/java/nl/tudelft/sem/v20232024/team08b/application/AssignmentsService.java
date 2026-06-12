package nl.tudelft.sem.v20232024.team08b.application;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.strategies.AutomaticAssignmentStrategy;
import nl.tudelft.sem.v20232024.team08b.application.verification.AssignmentsVerification;
import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithSubmissionMicroservice;
import nl.tudelft.sem.v20232024.team08b.domain.Paper;
import nl.tudelft.sem.v20232024.team08b.domain.Review;
import nl.tudelft.sem.v20232024.team08b.domain.Track;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperSummaryWithID;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.exceptions.ConflictOfInterestException;
import nl.tudelft.sem.v20232024.team08b.exceptions.ForbiddenAccessException;
import nl.tudelft.sem.v20232024.team08b.repos.ReviewRepository;
import nl.tudelft.sem.v20232024.team08b.repos.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AssignmentsService {
    private final ReviewRepository reviewRepository;

    private final CommunicationWithSubmissionMicroservice submissionCommunicator;

    private final TrackRepository trackRepository;
    private final AssignmentsVerification assignmentsVerification;
    private AutomaticAssignmentStrategy automaticAssignmentStrategy;

    /**
     * Default constructor for the service.
     *
     * @param reviewRepository repository storing the reviews
     * @param submissionCommunicator class, that talks to submissions microservice
     * @param trackRepository repository storing the tracks
     * @param assignmentsVerification object responsible for verifying assignments
     *
     */
    @Autowired
    public AssignmentsService(
            ReviewRepository reviewRepository,
            CommunicationWithSubmissionMicroservice submissionCommunicator,
            TrackRepository trackRepository,
            AssignmentsVerification assignmentsVerification
    ) {
        this.reviewRepository = reviewRepository;
        this.submissionCommunicator = submissionCommunicator;
        this.trackRepository = trackRepository;
        this.assignmentsVerification = assignmentsVerification;
    }

    /**
     * This method assigns manually reviewer to a paper.
     *
     * @param requesterID ID of a requester
     * @param reviewerID ID of a reviewer to be assigned
     * @param paperID ID of a paper to which the reviewer will be assigned
     * @throws IllegalAccessException If the requester does not have a permission to assign
     * @throws NotFoundException If the reviewer is not in the track of paper
     * @throws ConflictOfInterestException If reviewer can not be assigned due to conflict of interest
     */
    public void assignManually(Long requesterID, Long reviewerID, Long paperID)
            throws IllegalAccessException, NotFoundException, ConflictOfInterestException {

        assignmentsVerification.verifyIfManualAssignmentIsPossible(requesterID, paperID, reviewerID);

        Review toSave = new Review(paperID, reviewerID);
        reviewRepository.save(toSave);
    }

    /**
     * Method returns the list of all ID's of a reviewers for requested paper.
     *
     * @param requesterID ID of a requester
     * @param paperID ID of a requested paper
     * @return List of ID's of reviewers
     * @throws IllegalAccessException If the requester does not have permissions to see the assignments
     */
    public List<Long> assignments(Long requesterID, Long paperID) throws IllegalAccessException, NotFoundException {
        assignmentsVerification.verifyPermissionToGetAssignments(requesterID, paperID);
        List<Long> userIds = new ArrayList<>();
        List<Review> reviews = reviewRepository.findByReviewIDPaperID(paperID);
        for (Review review : reviews) {
            userIds.add(review.getReviewID().getReviewerID());
        }
        return userIds;
    }

    /**
     * This method assigns automatically reviewers to papers.
     *
     * @param requesterID ID of a requester
     * @param conferenceID ID of a conferenceID
     * @param trackID ID of a trackID
     * @throws IllegalAccessException If the requester does not have a permission to assign
     * @throws NotFoundException If the reviewer is not in the track of paper
     * @throws IllegalArgumentException If reviewer can not be assigned due to conflict of interest
     */
    public void assignAuto(Long requesterID, Long conferenceID, Long trackID)
            throws NotFoundException, IllegalAccessException {
        assignmentsVerification.verifyAutoAssignmentIsPossible(conferenceID, trackID, requesterID);

        TrackID trackID1 = new TrackID(conferenceID, trackID);
        Optional<Track> opTrack = trackRepository.findById(trackID1);
        if (opTrack.isEmpty()) {
            throw new NotFoundException("Track was not found");
        }
        Track track = opTrack.get();
        List<Paper> papers = track.getPapers();
        automaticAssignmentStrategy.automaticAssignment(trackID1, papers);
    }

    @Autowired
    public void setAutomaticAssignmentStrategy(
            AutomaticAssignmentStrategy automaticAssignmentStrategy) {
        this.automaticAssignmentStrategy = automaticAssignmentStrategy;
    }

    /**
     * Removes assignment from paper.
     *
     * @param requesterID ID of a user making the request
     * @param paperID ID of a paper for which there is an assignment
     * @param reviewerID ID of a reviewer assigned to the paper
     * @throws NotFoundException when the paper does not exist or there is no such an assignment
     * @throws IllegalAccessException when the requester is not a pc chair
     */
    public void remove(Long requesterID, Long paperID, Long reviewerID) throws NotFoundException, IllegalAccessException {
        assignmentsVerification.verifyPermissionToRemoveAssignment(requesterID, paperID);
        List<Review> reviews = reviewRepository.findByReviewIDPaperID(paperID);
        if (reviews.isEmpty()) {
            throw new NotFoundException("there are no reviewers assigned to this paper");
        }
        for (Review r : reviews) {
            if (r.getReviewID().getReviewerID().equals(reviewerID)) {
                reviewRepository.delete(r);
                return;
            }
        }
        throw new NotFoundException("There is no such a assignment");
    }

    /**
     * Gets assigned papers for a reviewer.
     *
     * @param requesterID ID of a user making a request
     * @return the PaperSummaryWithID objects related to the requester
     * @throws NotFoundException if user is not found
     */
    public List<PaperSummaryWithID> getAssignedPapers(Long requesterID) throws NotFoundException {
        assignmentsVerification.verifyIfUserCanGetAssignedPapers(requesterID);
        List<Long> paperIDs = reviewRepository.findPapersByReviewer(requesterID);
        List<PaperSummaryWithID> list = new ArrayList<>();
        for (Long paperID : paperIDs) {
            PaperSummaryWithID summaryWithID = new PaperSummaryWithID();
            Submission paper = submissionCommunicator.getSubmission(paperID);
            summaryWithID.setPaperID(paperID);
            summaryWithID.setTitle(paper.getTitle());
            summaryWithID.setAbstractSection(paper.getAbstract());
            list.add(summaryWithID);
        }
        return list;
    }

    /**
     * This method finalizes the assignment of reviewers, so they can no longer be changed
     * manually or automatically. It moves the track into the REVIEWING phase.
     *
     * @param requesterID  ID of a requester
     * @param conferenceID ID of the conference of the track
     * @param trackID      ID of a track
     * @throws ForbiddenAccessException If the requester is not a PC chair
     * @throws NotFoundException        If the track does not exist
     * @throws IllegalStateException    If there is less than 3 reviewers assigned to a paper
     *                                  or the track is not in ASSIGNING phase
     */
    public void finalization(Long requesterID, Long conferenceID, Long trackID)
            throws ForbiddenAccessException, NotFoundException, IllegalStateException {
        assignmentsVerification.verifyPermissionToFinalize(requesterID, conferenceID, trackID);
        // Ensure there is at least 3 reviewers assigned to each paper
        var submissions = submissionCommunicator.getSubmissionsInTrack(conferenceID, trackID, requesterID);
        if (submissions.stream().anyMatch(submission ->
                reviewRepository.findByReviewIDPaperID(submission.getSubmissionId()).size() < 3
        )) {
            throw new IllegalStateException();
        }
        // Ensure the track is in our repository
        Optional<Track> optional =  trackRepository.findById(conferenceID, trackID);
        if (optional.isEmpty()) {
            throw new NotFoundException("");
        }
        // Get the track from our database
        Track track = optional.get();
        track.setReviewersHaveBeenFinalized(true);
        trackRepository.save(track);
    }
}
