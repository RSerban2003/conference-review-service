package nl.tudelft.sem.v20232024.team08b.demo;

import nl.tudelft.sem.v20232024.team08b.domain.Paper;
import nl.tudelft.sem.v20232024.team08b.domain.Track;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.User;
import nl.tudelft.sem.v20232024.team08b.repos.TrackRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Seeds the local database for the "demo" profile: one track
 * (conference 1, track 1) containing two papers, plus the matching
 * submissions in {@link DemoSubmissionsStub}. Together with
 * {@link DemoUsersStub} this makes the full review lifecycle walkable
 * without the external Users and Submissions microservices
 * (see docs/demo.http).
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {
    private final TrackRepository trackRepository;
    private final DemoSubmissionsStub submissionsStub;

    public DemoDataSeeder(TrackRepository trackRepository, DemoSubmissionsStub submissionsStub) {
        this.trackRepository = trackRepository;
        this.submissionsStub = submissionsStub;
    }

    @Override
    public void run(String... args) {
        Track track = new Track();
        track.setTrackID(new TrackID(DemoUsersStub.CONFERENCE_ID, DemoUsersStub.TRACK_ID));
        // Bidding stays open for two hours; the demo script moves the
        // deadline into the past to advance the track to ASSIGNING.
        track.setBiddingDeadline(new Date(Instant.now().plus(2, ChronoUnit.HOURS).toEpochMilli()));
        track.setReviewersHaveBeenFinalized(false);
        track.setPapers(List.of(new Paper(), new Paper()));

        Track saved = trackRepository.save(track);
        List<Paper> papers = saved.getPapers();

        submissionsStub.registerSubmission(submission(
                papers.get(0).getId(),
                "Self-Correcting Transformers for Low-Resource Translation",
                "We present a transformer architecture that detects and corrects its own "
                        + "translation errors at decoding time, improving BLEU scores on six "
                        + "low-resource language pairs without additional training data."
        ));
        submissionsStub.registerSubmission(submission(
                papers.get(1).getId(),
                "Gradient-Free Hyperparameter Search by Simulated Annealing",
                "We revisit simulated annealing for hyperparameter optimisation and compare "
                        + "it against Bayesian methods on standard vision benchmarks, analysing "
                        + "when gradient-free search remains competitive."
        ));
    }

    private static Submission submission(Long paperID, String title, String abstractText) {
        User author = new User();
        author.setUserId(5L);
        author.setName("Ada");
        author.setSurname("Lovelace");
        author.setEmail("ada@example.org");

        Submission submission = new Submission();
        submission.setSubmissionId(paperID);
        submission.setTitle(title);
        submission.setAbstract(abstractText);
        submission.setEventId(DemoUsersStub.CONFERENCE_ID);
        submission.setTrackId(DemoUsersStub.TRACK_ID);
        submission.setAuthors(List.of(author));
        submission.setConflictsOfInterest(List.of());
        return submission;
    }
}
