package nl.tudelft.sem.v20232024.team08b.database;

import nl.tudelft.sem.v20232024.team08b.domain.Bid;
import nl.tudelft.sem.v20232024.team08b.repos.BidRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

@DataJpaTest
public class BidsRepositoryTests {
    @Autowired
    private BidRepository bidRepository;

    @Test
    public void testFindByPaperID() {
        Bid bid = new Bid(5L, 2L, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.NOT_REVIEW);
        bidRepository.save(bid);
        bid = new Bid(5L, 2L, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.CAN_REVIEW);
        bidRepository.save(bid);
        bid = new Bid(5L, 3L, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.NOT_REVIEW);
        bidRepository.save(bid);
        bid = new Bid(5L, 1L, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.NEUTRAL);
        bidRepository.save(bid);
        bid = new Bid(6L, 2L, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.CAN_REVIEW);
        bidRepository.save(bid);
        bid = new Bid(6L, 2L, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.NOT_REVIEW);
        bidRepository.save(bid);

        var result = bidRepository.findByPaperID(5L);

        var expected = List.of(
                new Bid(5L, 2L, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.CAN_REVIEW),
                new Bid(5L, 3L, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.NOT_REVIEW),
                new Bid(5L, 1L, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.NEUTRAL)
        );

        Assertions.assertEquals(expected.size(), result.size());
        Assertions.assertTrue(result.containsAll(expected));
        Assertions.assertTrue(expected.containsAll(result));
    }
}
