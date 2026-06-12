package nl.tudelft.sem.v20232024.team08b.communicators;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;

import java.util.List;

public interface CommunicationWithSubmissionMicroservice {
    Submission getSubmission(Long paperID) throws NotFoundException;

    List<Submission> getSubmissionsInTrack(Long conferenceID, Long trackID) throws NotFoundException;

    List<Submission> getSubmissionsInTrack(Long conferenceID, Long trackID, Long requesterID)
            throws NotFoundException;
}
