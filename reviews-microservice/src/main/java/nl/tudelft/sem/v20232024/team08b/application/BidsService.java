package nl.tudelft.sem.v20232024.team08b.application;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.verification.BidsVerification;
import nl.tudelft.sem.v20232024.team08b.domain.Bid;
import nl.tudelft.sem.v20232024.team08b.domain.BidID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.BidByReviewer;
import nl.tudelft.sem.v20232024.team08b.exceptions.ForbiddenAccessException;
import nl.tudelft.sem.v20232024.team08b.repos.BidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BidsService {
    private final BidRepository bidRepository;
    private final BidsVerification bidsVerification;

    /**
     * Default constructor for the service.
     *
     * @param bidRepository repository storing the bids
     * @param bidsVerification service responsible for bid verification
     */
    @Autowired
    public BidsService(
            BidRepository bidRepository,
            BidsVerification bidsVerification
    ) {
        this.bidRepository = bidRepository;
        this.bidsVerification = bidsVerification;
    }

    /**
     * Retrieves the bid for a paper by a reviewer.
     *
     * @param requesterID The ID of the requester.
     * @param paperID     The ID of the paper.
     * @param reviewerID  The ID of the reviewer.
     * @return The bid for the paper.
     * @throws NotFoundException        If the bid doesn't exist.
     * @throws ForbiddenAccessException If the requester doesn't have the required role. They must
     *                                  be a chair or a reviewer of the track the paper is in.
     */
    public nl.tudelft.sem.v20232024.team08b.dtos.review.Bid getBidForPaperByReviewer(Long requesterID,
                                                                                     Long paperID,
                                                                                     Long reviewerID)
            throws NotFoundException, ForbiddenAccessException {
        bidsVerification.verifyPermissionToAccessBidsOfPaper(requesterID, paperID);
        Optional<Bid> bid = bidRepository.findById(new BidID(paperID, reviewerID));
        if (bid.isEmpty()) {
            throw new NotFoundException("The bid doesn't exist");
        }
        return bid.get().getBid();
    }

    /**
     * Retrieves a list of bids made by reviewers for a specific paper.
     *
     * @param requesterID the ID of the requester
     * @param paperID     the ID of the paper
     * @return a list of BidByReviewer objects representing the bids for the paper
     * @throws NotFoundException        if the paper is not found
     * @throws ForbiddenAccessException if the requester is not a chair of the track the paper is in
     */
    public List<BidByReviewer> getBidsForPaper(Long requesterID, Long paperID)
            throws NotFoundException, ForbiddenAccessException {
        bidsVerification.verifyPermissionToAccessAllBids(requesterID, paperID);
        var bids = bidRepository.findByPaperID(paperID);
        return bids.stream().map(bid -> new BidByReviewer(bid.getBidderID(), bid.getBid())).collect(Collectors.toList());
    }

    /**
     * Saves the preference (based on expertise) of the requester regarding reviewing the given paper.
     *
     * @param requesterID the ID of the requester (the reviewer)
     * @param paperID     the ID of the paper
     * @param bid         the preference of the reviewer in regard to reviewing the paper
     * @throws ForbiddenAccessException if the requester is not a reviewer of the track the paper is in
     * @throws NotFoundException        if the paper/track doesn't exist
     * @throws IllegalStateException    if the bidding phase has passed or if it hasn't started
     */
    public void bid(Long requesterID,
                    Long paperID,
                    nl.tudelft.sem.v20232024.team08b.dtos.review.Bid bid)
            throws ForbiddenAccessException, NotFoundException, IllegalStateException {
        bidsVerification.verifyPermissionToSubmitBid(requesterID, paperID);
        bidRepository.save(new Bid(paperID, requesterID, bid));
    }
}
