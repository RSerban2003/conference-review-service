package nl.tudelft.sem.v20232024.team08b.unit.phase;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.phase.PaperPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.application.phase.TrackPhaseCalculator;
import nl.tudelft.sem.v20232024.team08b.communicators.UsersMicroserviceCommunicator;
import nl.tudelft.sem.v20232024.team08b.domain.Paper;
import nl.tudelft.sem.v20232024.team08b.domain.Track;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperStatus;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackPhase;
import nl.tudelft.sem.v20232024.team08b.repos.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class TrackPhaseCalculatorTests {
    private final TrackRepository trackRepository = Mockito.mock(TrackRepository.class);
    private final UsersMicroserviceCommunicator usersCommunicator = Mockito.mock(UsersMicroserviceCommunicator.class);
    private final PaperPhaseCalculator paperPhaseCalculator = Mockito.mock(PaperPhaseCalculator.class);
    private final Clock clock = Mockito.mock(Clock.class);

    private final TrackPhaseCalculator trackPhaseCalculator = Mockito.spy(
            new TrackPhaseCalculator(
                    trackRepository,
                    usersCommunicator,
                    paperPhaseCalculator
            )
    );

    private Track track;
    private TrackID trackID;
    private List<Paper> papers;
    nl.tudelft.sem.v20232024.team08b.dtos.users.Track trackDTO;


    /**
     * Helper method that sets fake phase for a paper.
     *
     * @param paperIndex the index of the paper in our papers array
     * @param paperPhase the phase to be set
     * @throws NotFoundException should never be thrown
     */
    private void setPaperPhase(int paperIndex, PaperPhase paperPhase) throws NotFoundException {
        Long id = papers.get(paperIndex).getId();
        when(paperPhaseCalculator.getPaperPhase(id)).thenReturn(paperPhase);
    }

    @BeforeEach
    void init() throws NotFoundException {
        trackPhaseCalculator.setClock(clock);

        // Set current time to 10ms since start of time
        when(clock.instant()).thenReturn(Instant.ofEpochMilli(10L));
        // Set up a fake track with fake papers

        // Create 2 papers for the single track
        Paper paper1 = new Paper(3L, null, PaperStatus.NOT_DECIDED, false);
        Paper paper2 = new Paper(4L, null, PaperStatus.NOT_DECIDED, false);


        papers = List.of(paper1, paper2);

        // Create the track with these papers. By default, track has bidding deadline
        // of 20.
        trackID = new TrackID(1L, 2L);
        track = new Track(
                trackID,
                Date.from(Instant.ofEpochMilli(20L)),
                false,
                papers
        );

        // Set up a fake track DTO
        trackDTO = new nl.tudelft.sem.v20232024.team08b.dtos.users.Track();

        when(
                usersCommunicator.getTrack(
                        trackID.getConferenceID(),
                        trackID.getTrackID()
                )
        ).thenReturn(
            trackDTO
        );

    }

    @Test
    void getTrackPhase_BeforeSubmissionDeadline() throws NotFoundException {
        // Assume that the current time (10) is before the submission deadline
        trackDTO.setDeadline(11L);

        TrackPhase result = trackPhaseCalculator.getTrackPhase(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        assertThat(result).isEqualTo(TrackPhase.SUBMITTING);
    }

    @Test
    void getTrackPhase_ExactlyAtSubmissionDeadline() throws NotFoundException {
        // Assume that the current time (10) is at the time of submission deadline
        trackDTO.setDeadline(10L);

        TrackPhase result = trackPhaseCalculator.getTrackPhase(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        assertThat(result).isEqualTo(TrackPhase.SUBMITTING);
    }

    @Test
    void getTrackPhase_NoBiddingDeadline() throws NotFoundException {
        // Assume that the current time (10) is after the submission deadline
        trackDTO.setDeadline(5L);

        // Assume that the bidding deadline is not set yet
        doReturn(null).when(trackPhaseCalculator).getBiddingDeadlineAsLong(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        TrackPhase result = trackPhaseCalculator.getTrackPhase(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        assertThat(result).isEqualTo(TrackPhase.BIDDING);
    }

    @Test
    void getTrackPhase_BeforeBiddingDeadline() throws NotFoundException {
        // Assume that the current time (10) is after the submission deadline
        trackDTO.setDeadline(5L);

        // Assume that the bidding deadline has not yet passed
        doReturn(11L).when(trackPhaseCalculator).getBiddingDeadlineAsLong(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        TrackPhase result = trackPhaseCalculator.getTrackPhase(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        assertThat(result).isEqualTo(TrackPhase.BIDDING);
    }

    @Test
    void getTrackPhase_ExactlyAtBiddingDeadline() throws NotFoundException {
        // Assume that the current time (10) is after the submission deadline
        trackDTO.setDeadline(5L);

        // Assume that the bidding deadline has not yet passed
        doReturn(10L).when(trackPhaseCalculator).getBiddingDeadlineAsLong(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        TrackPhase result = trackPhaseCalculator.getTrackPhase(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        assertThat(result).isEqualTo(TrackPhase.BIDDING);
    }


    @Test
    void getTrackPhase_ReviewersAreNotAssigned() throws NotFoundException {
        // Assume that the current time (10) is after the submission deadline
        trackDTO.setDeadline(5L);

        // Assume that the bidding deadline has passed
        doReturn(6L).when(trackPhaseCalculator).getBiddingDeadlineAsLong(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        // Assume the reviewers have not yet been assigned in the track
        when(paperPhaseCalculator.checkIfReviewersAreAssignedToTrack(
                trackID.getConferenceID(),
                trackID.getTrackID()
        )).thenReturn(false);

        TrackPhase result = trackPhaseCalculator.getTrackPhase(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        assertThat(result).isEqualTo(TrackPhase.ASSIGNING);
    }

    @Test
    void getTrackPhase_NotAllPapersHaveBeenFinalized() throws NotFoundException {
        // Assume that the current time (10) is after the submission deadline
        trackDTO.setDeadline(5L);

        // Assume that the bidding deadline has passed
        doReturn(6L).when(trackPhaseCalculator).getBiddingDeadlineAsLong(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        // Assume the reviewers have already been assigned in the track
        when(paperPhaseCalculator.checkIfReviewersAreAssignedToTrack(
                trackID.getConferenceID(),
                trackID.getTrackID()
        )).thenReturn(true);

        // Assume that not all papers have been finalized
        doReturn(false).when(trackPhaseCalculator).checkIfAllPapersFinalized(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        TrackPhase result = trackPhaseCalculator.getTrackPhase(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        assertThat(result).isEqualTo(TrackPhase.REVIEWING);
    }

    @Test
    void getTrackPhase_AllPapersHaveBeenFinalized() throws NotFoundException {
        // Assume that the current time (10) is after the submission deadline
        trackDTO.setDeadline(5L);

        // Assume that the bidding deadline has passed
        doReturn(6L).when(trackPhaseCalculator).getBiddingDeadlineAsLong(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        // Assume the reviewers have already been assigned in the track
        when(paperPhaseCalculator.checkIfReviewersAreAssignedToTrack(
                trackID.getConferenceID(),
                trackID.getTrackID()
        )).thenReturn(true);

        // Assume not all papers have been finalized
        doReturn(true).when(trackPhaseCalculator).checkIfAllPapersFinalized(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        TrackPhase result = trackPhaseCalculator.getTrackPhase(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        assertThat(result).isEqualTo(TrackPhase.FINAL);
    }

    @Test
    void checkIfAllPapersFinalized_NoTrackPresent() throws NotFoundException {
        // Assume that such track exists
        when(
                usersCommunicator.getTrack(
                        trackID.getConferenceID(),
                        trackID.getTrackID()
                )
        ).thenReturn(trackDTO);

        // Assume that the track is not in our local DB, i.e., no reviews have been
        // assigned
        when(trackRepository.findById(trackID)).thenReturn(Optional.empty());

        boolean result = trackPhaseCalculator.checkIfAllPapersFinalized(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        // If the track is not in our DB, then no reviews have been added
        assertThat(result).isFalse();
    }

    @Test
    void checkIfAllPapersFinalized_NoPapersFinalized() throws NotFoundException {
        // Set both papers as not-yet-finalized
        setPaperPhase(0, PaperPhase.BEFORE_REVIEW);
        setPaperPhase(1, PaperPhase.BEFORE_REVIEW);

        // Assume that such track exists
        when(
                usersCommunicator.getTrack(
                        trackID.getConferenceID(),
                        trackID.getTrackID()
                )
        ).thenReturn(trackDTO);

        // Assume that the track is stored in our local DB
        when(trackRepository.findById(trackID)).thenReturn(Optional.of(track));

        boolean result = trackPhaseCalculator.checkIfAllPapersFinalized(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        // We have set all papers to be not finalized, so we should expect the
        // method to return false
        assertThat(result).isFalse();
    }

    @Test
    void checkIfAllPapersFinalized_SomePapersFinalized() throws NotFoundException {
        // Set second paper as finalized, first as not
        setPaperPhase(0, PaperPhase.BEFORE_REVIEW);
        setPaperPhase(1, PaperPhase.REVIEWED);

        papers.get(0).setReviewsHaveBeenFinalized(true);
        // Assume that such track exists
        when(
                usersCommunicator.getTrack(
                        trackID.getConferenceID(),
                        trackID.getTrackID()
                )
        ).thenReturn(trackDTO);

        // Assume that the track is stored in our local DB
        when(trackRepository.findById(trackID)).thenReturn(Optional.of(track));

        boolean result = trackPhaseCalculator.checkIfAllPapersFinalized(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        // One paper is finalized, the other is not. So the method should return false
        assertThat(result).isFalse();
    }


    @Test
    void checkIfAllPapersFinalized_AllPapersFinalized() throws NotFoundException {
        // Set both papers as finalized
        setPaperPhase(0, PaperPhase.REVIEWED);
        setPaperPhase(1, PaperPhase.REVIEWED);

        // Assume that such track exists
        when(
                usersCommunicator.getTrack(
                        trackID.getConferenceID(),
                        trackID.getTrackID()
                )
        ).thenReturn(trackDTO);

        // Assume that the track is stored in our local DB
        when(trackRepository.findById(trackID)).thenReturn(Optional.of(track));

        boolean result = trackPhaseCalculator.checkIfAllPapersFinalized(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );

        // Both papers are finalized, so we expect the method to return true
        assertThat(result).isTrue();
    }

    @Test
    void getBiddingDeadlineAsLong_NotInRepo() {
        // Assume track is not in repo
        when(trackRepository.findById(trackID)).thenReturn(Optional.empty());
        Long result = trackPhaseCalculator.getBiddingDeadlineAsLong(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );
        assertThat(result).isNull();
    }

    @Test
    void getBiddingDeadlineAsLong_Successful() {
        java.util.Date date = Date.valueOf(LocalDate.of(2012, 10, 10));
        Long dateUnix = date.getTime();
        track.setBiddingDeadline(date);

        // Assume track is in repo
        when(trackRepository.findById(trackID)).thenReturn(Optional.of(track));
        Long result = trackPhaseCalculator.getBiddingDeadlineAsLong(
                trackID.getConferenceID(),
                trackID.getTrackID()
        );
        assertThat(result).isEqualTo(dateUnix);
    }
}
