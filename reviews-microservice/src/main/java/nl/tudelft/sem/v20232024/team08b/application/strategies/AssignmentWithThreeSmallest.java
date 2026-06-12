package nl.tudelft.sem.v20232024.team08b.application.strategies;

import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithSubmissionMicroservice;
import nl.tudelft.sem.v20232024.team08b.domain.Bid;
import nl.tudelft.sem.v20232024.team08b.domain.Paper;
import nl.tudelft.sem.v20232024.team08b.domain.Review;
import nl.tudelft.sem.v20232024.team08b.domain.ReviewID;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.repos.BidRepository;

import nl.tudelft.sem.v20232024.team08b.repos.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssignmentWithThreeSmallest implements AutomaticAssignmentStrategy {
    private final BidRepository bidRepository;
    private final ReviewRepository reviewRepository;
    private final CommunicationWithSubmissionMicroservice submissionsMicroservice;

    /**
    * Method that adds automatically the three reviewers with the least amount of papers.
    *
    * @param bidRepository repository for storing the bids
    * @param reviewRepository repository for storing the reviews
    * @param submissionsMicroservice repository for storing the external database
    */
    @Autowired
    public AssignmentWithThreeSmallest(BidRepository bidRepository,
                                     ReviewRepository reviewRepository,
                                       CommunicationWithSubmissionMicroservice submissionsMicroservice) {

        this.bidRepository = bidRepository;
        this.reviewRepository = reviewRepository;
        this.submissionsMicroservice = submissionsMicroservice;
    }

    @Override
    public void automaticAssignment(TrackID trackID, List<Paper> papers) {
        for (Paper paper : papers) {
            List<Bid> bids = bidRepository.findByPaperID(paper.getId());
            List<Long> users = bids.stream().map(Bid::getBidderID).collect(Collectors.toList());
            if (users.isEmpty()) {
                throw new IllegalArgumentException("At least One reviewer needed");
            }
            List<Integer> numberOfPapers = new ArrayList<>();
            for (Long user : users) {
                var reviews = reviewRepository.findByReviewIDReviewerID(user);
                var paperAmount = reviews.stream().map(x -> {
                    try {
                        return new TrackID(submissionsMicroservice.getSubmission(x.getReviewID().getPaperID())
                            .getEventId(), submissionsMicroservice.getSubmission(x.getReviewID().getPaperID())
                            .getTrackId());
                    } catch (NotFoundException e) {
                        throw new RuntimeException("Track does not exist!");
                    }

                })
                    .filter(x -> x.getTrackID().equals(trackID.getTrackID())
                    && x.getConferenceID().equals(trackID.getConferenceID())).toArray();
                numberOfPapers.add(paperAmount.length);
            }
            savingSmallest(gettingSmallest(numberOfPapers), paper, users);

        }
    }


    /**
     * Saves the values of the list into the repository.
     *
     * @param threeSmallest position of members that will review
     * @param paper to review
     * @param users that might review
    */
    private void savingSmallest(List<Integer> threeSmallest, Paper paper, List<Long> users) {
        for (Integer index : threeSmallest) {
            ReviewID reviewID = new ReviewID(paper.getId(), users.get(index));
            Review toSave = new Review();
            toSave.setReviewID(reviewID);
            reviewRepository.save(toSave);
        }
    }
    /**
     * Method that gets the three smallest elements of an array.
     *
     * @param numberOfPapers A list corresponding to the number of papers of every reviewer
     * @return a list with the 3 smallest elements
     */

    private List<Integer> gettingSmallest(List<Integer> numberOfPapers) {
        List<Integer> smallest = new ArrayList<>();
        int numberOfRepeats = min(3, numberOfPapers.size());


        for (int j = 0; j < numberOfRepeats; j++) {
            int minIndex = 0;
            // Find the index of the minimum element
            for (int k = 1; k < numberOfPapers.size(); k++) {
                if (numberOfPapers.get(k) < numberOfPapers.get(minIndex)) {
                    minIndex = k;
                }
            }
            // Store the indices of the smallest elements and set the chosen minimum to a large value
            smallest.add(minIndex);
            numberOfPapers.set(minIndex, Integer.MAX_VALUE);
        }
        return smallest;

    }

}
