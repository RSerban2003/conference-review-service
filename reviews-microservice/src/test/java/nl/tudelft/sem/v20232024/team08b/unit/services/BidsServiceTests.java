package nl.tudelft.sem.v20232024.team08b.unit.services;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.BidsService;
import nl.tudelft.sem.v20232024.team08b.application.verification.BidsVerification;
import nl.tudelft.sem.v20232024.team08b.domain.Bid;
import nl.tudelft.sem.v20232024.team08b.domain.BidID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.BidByReviewer;
import nl.tudelft.sem.v20232024.team08b.exceptions.ForbiddenAccessException;
import nl.tudelft.sem.v20232024.team08b.repos.BidRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class BidsServiceTests {
    private final BidRepository bidRepository = Mockito.mock(BidRepository.class);
    private final BidsVerification bidsVerification = Mockito.mock(BidsVerification.class);

    private final BidsService bidsService = new BidsService(bidRepository, bidsVerification);

    @Test
    public void testGetBidForPaperByReviewer() throws NotFoundException, ForbiddenAccessException {
        Long requesterID = 1L;
        Long paperID = 2L;
        Long reviewerID = 3L;
        BidID bidID = new BidID(paperID, reviewerID);
        Bid bid = new Bid(paperID, reviewerID, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.CAN_REVIEW);

        when(bidRepository.findById(bidID)).thenReturn(Optional.of(bid));
        doNothing().when(bidsVerification).verifyPermissionToAccessBidsOfPaper(paperID, paperID);

        var result = bidsService.getBidForPaperByReviewer(requesterID, paperID, reviewerID);

        assertEquals(bid.getBid(), result);
    }

    @Test
    public void testGetBidForPaperByReviewerBidNotFound() throws ForbiddenAccessException {
        Long requesterID = 1L;
        Long paperID = 2L;
        Long reviewerID = 3L;
        BidID bidID = new BidID(paperID, reviewerID);

        when(bidRepository.findById(bidID)).thenReturn(Optional.empty());
        doNothing().when(bidsVerification).verifyPermissionToAccessBidsOfPaper(paperID, paperID);

        assertThrows(NotFoundException.class, () -> bidsService.getBidForPaperByReviewer(requesterID, paperID, reviewerID));
        verify(bidRepository, times(1)).findById(bidID);
    }

    @Test
    public void testGetBidForPaperByReviewerForbiddenAccess() throws ForbiddenAccessException {
        doThrow(new ForbiddenAccessException()).when(bidsVerification)
                .verifyPermissionToAccessBidsOfPaper(1L, 2L);
        assertThrows(ForbiddenAccessException.class, () -> bidsService.getBidForPaperByReviewer(1L, 2L, 3L));

    }

    @Test
    void testGetBidsForPaperForbiddenAccess() throws NotFoundException, ForbiddenAccessException {
        doThrow(new ForbiddenAccessException())
                .when(bidsVerification).verifyPermissionToAccessAllBids(6L, 1L);
        assertThrows(ForbiddenAccessException.class, () -> bidsService.getBidsForPaper(6L, 1L));
    }

    @Test
    void testGetBidsForPaperValidRequesterAndPaperReturnsBidsByReviewer()
            throws NotFoundException, ForbiddenAccessException {
        List<Bid> bids = new ArrayList<>();
        bids.add(new Bid(1L, 5L, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.CAN_REVIEW));
        bids.add(new Bid(1L, 4L, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.NOT_REVIEW));
        doNothing().when(bidsVerification).verifyPermissionToAccessAllBids(6L, 1L);

        when(bidRepository.findByPaperID(1L)).thenReturn(bids);

        var expected = new ArrayList<BidByReviewer>();
        expected.add(new BidByReviewer(4L,
                nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.NOT_REVIEW));
        expected.add(new BidByReviewer(5L,
                nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.CAN_REVIEW));

        List<BidByReviewer> expectedResult = bidsService.getBidsForPaper(6L, 1L);
        Assertions.assertEquals(expected.size(), expectedResult.size());
        Assertions.assertTrue(expected.containsAll(expectedResult));
        Assertions.assertTrue(expectedResult.containsAll(expected));
    }

    @Test
    void testBidValidRequestSavesBid() throws ForbiddenAccessException, NotFoundException, IllegalStateException {
        Long paperID = 5L;
        Long requesterID = 1L;
        doNothing().when(bidsVerification).verifyPermissionToSubmitBid(requesterID, paperID);

        var bid = nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.CAN_REVIEW;
        bidsService.bid(requesterID, paperID, bid);

        verify(bidRepository, times(1)).save(new Bid(paperID, requesterID, bid));
    }

    @Test
    void testBidForbiddedAccess() throws ForbiddenAccessException, NotFoundException, IllegalStateException {
        Long paperID = 5L;
        Long requesterID = 1L;
        var bid = nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.CAN_REVIEW;

        doThrow(new ForbiddenAccessException())
                .when(bidsVerification).verifyPermissionToSubmitBid(requesterID, paperID);
        assertThrows(ForbiddenAccessException.class, () -> bidsService.bid(requesterID, paperID, bid));
    }
}
