package nl.tudelft.sem.v20232024.team08b.database;

import nl.tudelft.sem.v20232024.team08b.domain.Track;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.repos.TrackRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
public class TrackRepositoryTests {

    @Autowired
    private TrackRepository trackRepository;

    @Test
    public void findByIdPair() {
        Long conferenceID = 1L;
        Long trackID = 2L;
        TrackID id = new TrackID(conferenceID, trackID);

        Track track = new Track(
                id,
                Date.valueOf(LocalDate.of(2012, 11, 20)),
                false,
                null
        );

        trackRepository.save(track);

        Optional<Track> optional = trackRepository.findById(conferenceID, trackID);
        assertThat(optional).isEqualTo(Optional.of(track));
    }
}
