package nl.tudelft.sem.v20232024.team08b.demo;

import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithSubmissionMicroservice;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory stand-in for the Submissions microservice, active only under
 * the "demo" profile. {@link DemoDataSeeder} registers the demo submissions
 * here after it has created the matching local paper records.
 */
@Component
@Primary
@Profile("demo")
public class DemoSubmissionsStub implements CommunicationWithSubmissionMicroservice {
    private final Map<Long, Submission> submissionsById = new LinkedHashMap<>();

    /**
     * Registers a submission so it can be served to the application.
     *
     * @param submission the submission to register
     */
    public void registerSubmission(Submission submission) {
        submissionsById.put(submission.getSubmissionId(), submission);
    }

    @Override
    public Submission getSubmission(Long paperID) throws NotFoundException {
        Submission submission = submissionsById.get(paperID);
        if (submission == null) {
            throw new NotFoundException("404, not found");
        }
        return submission;
    }

    @Override
    public List<Submission> getSubmissionsInTrack(Long conferenceID, Long trackID) throws NotFoundException {
        return getSubmissionsInTrack(conferenceID, trackID, -1L);
    }

    @Override
    public List<Submission> getSubmissionsInTrack(Long conferenceID,
                                                  Long trackID,
                                                  Long requesterID) throws NotFoundException {
        List<Submission> result = new ArrayList<>();
        for (Submission submission : submissionsById.values()) {
            if (Objects.equals(submission.getEventId(), conferenceID)
                    && Objects.equals(submission.getTrackId(), trackID)) {
                result.add(submission);
            }
        }
        return result;
    }
}
