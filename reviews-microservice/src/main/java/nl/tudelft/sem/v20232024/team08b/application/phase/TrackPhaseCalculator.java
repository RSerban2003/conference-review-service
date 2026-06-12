package nl.tudelft.sem.v20232024.team08b.application.phase;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithUsersMicroservice;
import nl.tudelft.sem.v20232024.team08b.domain.Paper;
import nl.tudelft.sem.v20232024.team08b.domain.Track;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperPhase;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackPhase;
import nl.tudelft.sem.v20232024.team08b.repos.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Component
public class TrackPhaseCalculator {
    private final TrackRepository trackRepository;
    private final CommunicationWithUsersMicroservice usersCommunicator;
    private final PaperPhaseCalculator paperPhaseCalculator;
    private Clock clock;
    /**
     * Default constructor for the phase calculator.
     *
     * @param trackRepository repository storing the tracks
     * @param usersCommunicator class that talks with users microservice
     * @param paperPhaseCalculator object that calculates phase of the paper
     */
    @Autowired
    public TrackPhaseCalculator(TrackRepository trackRepository,
                                CommunicationWithUsersMicroservice usersCommunicator,
                                PaperPhaseCalculator paperPhaseCalculator) {
        this.trackRepository = trackRepository;
        this.usersCommunicator = usersCommunicator;
        this.paperPhaseCalculator = paperPhaseCalculator;
        clock = Clock.systemUTC();
    }

    /**
     * A setter for clock, so that we can inject it during testing.
     *
     * @param clock clock object
     */
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Method that gets bidding deadline of the track and returns it as a UNIX
     * timestamp, as Integer.
     * If the deadline is not set, returns NULL.
     * Note that this method is not setting the default bidding deadline
     * (if the optional is empty) which it might actually have to do.
     * But it should be fine.
     *
     * @param conferenceID the ID of the conference the track is in
     * @param trackID the ID of the track
     * @return bidding deadline as an integer
     */
    public Long getBiddingDeadlineAsLong(Long conferenceID,
                                         Long trackID) {
        Optional<Track> optional = trackRepository.findById(new TrackID(conferenceID, trackID));
        if (optional.isEmpty()) {
            return null;
        } else {
            return optional.get().getBiddingDeadline().getTime();
        }
    }

    /**
     * Checks if all papers in a given track have been finalized.
     *
     * @param conferenceID the ID of the conference the track is in
     * @param trackID the ID of the track
     * @return true, iff all papers in a given track have been finalized
     */
    public boolean checkIfAllPapersFinalized(Long conferenceID,
                                             Long trackID) throws NotFoundException {
        // Check if such track exists
        usersCommunicator.getTrack(conferenceID, trackID);

        // Get the track
        Optional<Track> trackOptional = trackRepository.findById(
                new TrackID(conferenceID, trackID)
        );
        if (trackOptional.isEmpty()) {
            // In theory, this method should be called only after reviews have
            // been added, so this should not be reached.
            return false;
        }

        // Get the papers of the track
        List<Paper> papers = trackOptional.get().getPapers();

        // Check if all the papers are finalized
        boolean ret = true;
        for (var paper : papers) {
            Long paperID = paper.getId();

            // We could also use paper.reviewsHaveBeenFinalized flag, instead of calling the
            // getPaperPhase method. But calling the method gives less space for bugs to appear,
            // since it performs many additional checks.
            ret &= paperPhaseCalculator.getPaperPhase(paperID) == PaperPhase.REVIEWED;
        }
        return ret;
    }

    /**
     * Calculates the current phase of a track.
     * The logic of calculation is the following:
     * - If the submission deadline has not yet passed -> SUBMITTING
     * - else, if the bidding deadline is not set or has not passed -> BIDDING
     * - else, if the reviewers have not yet been assigned to papers -> ASSIGNING
     * - else, if all papers of the track have not been finalized -> REVIEWING
     * - finally, if all papers have been finalized -> FINAL
     *
     * @param conferenceID the ID of the conference the track is in
     * @param trackID the ID of the track
     * @return the current phase of the track
     */
    public TrackPhase getTrackPhase(Long conferenceID,
                                    Long trackID) throws NotFoundException {

        // Get the track from external repository. Such track should always
        // exist, since we verified, so the following line will not throw.
        nl.tudelft.sem.v20232024.team08b.dtos.users.Track track =
                usersCommunicator.getTrack(conferenceID, trackID);

        // Get the current timestamp
        long currentTime = clock.instant().toEpochMilli();

        // Check if submission deadline has not yet passed
        Long submissionDeadline = track.getDeadline();
        if (currentTime <= submissionDeadline) {
            return TrackPhase.SUBMITTING;
        }

        // Get the bidding deadline
        Long biddingDeadline = getBiddingDeadlineAsLong(conferenceID, trackID);
        if (biddingDeadline == null || currentTime <= biddingDeadline) {
            return TrackPhase.BIDDING;
        }

        // Check if the reviewers haven't yet been assigned
        boolean reviewersAssigned = paperPhaseCalculator
                .checkIfReviewersAreAssignedToTrack(conferenceID, trackID);
        if (!reviewersAssigned) {
            return TrackPhase.ASSIGNING;
        }

        // Check if all papers in a track have been finalized
        boolean allPapersFinalized = checkIfAllPapersFinalized(conferenceID, trackID);
        if (!allPapersFinalized) {
            return TrackPhase.REVIEWING;
        }

        return TrackPhase.FINAL;
    }
}
