package nl.tudelft.sem.v20232024.team08b.communicators;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.utils.HttpRequestSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SubmissionsMicroserviceCommunicator implements CommunicationWithSubmissionMicroservice {

    private final Long ourID = -1L;
    private final String submissionsURL = "http://localhost:8081";
    private final ObjectMapper objectMapper;
    private final HttpRequestSender httpRequestSender;


    /**
     * Constructor used for testing purposes.
     *
     * @param httpRequestSender class used for sending HTTP requests
     * @param objectMapper class used to map objects to json
     */
    public SubmissionsMicroserviceCommunicator(ObjectMapper objectMapper, HttpRequestSender httpRequestSender) {
        this.objectMapper = objectMapper;
        this.httpRequestSender = httpRequestSender;
    }


    /**
     * Default constructor.
     *
     * @param httpRequestSender class used for sending HTTP requests
     */
    @Autowired
    public SubmissionsMicroserviceCommunicator(HttpRequestSender httpRequestSender) {
        this.objectMapper = new ObjectMapper();
        this.httpRequestSender = httpRequestSender;
    }


    /**
     * Gets a paper (called submission) from the Submissions microservice.
     *
     * @param paperID the ID of the paper to get
     * @return the gotten Submission object
     */
    @Override
    public Submission getSubmission(Long paperID) throws NotFoundException {
        try {
            String url = submissionsURL + "/submission/" + paperID + "/" + ourID;
            String response = httpRequestSender.sendGetRequest(url);
            Submission submission;
            submission = objectMapper.readValue(response, Submission.class);
            return submission;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse the HTTP response");
        }
    }


    /**
     * Gets all submissions in a track using the default requester ID (ourID).
     *
     * @param trackID the ID of the track
     * @return a list of submissions in the track
     */
    @Override
    public List<Submission> getSubmissionsInTrack(Long conferenceID, Long trackID) throws NotFoundException {
        return getSubmissionsInTrack(conferenceID, trackID, ourID);
    }

    /**
     * Gets all submissions in a track.
     *
     * @param trackID     the ID of the track
     * @param requesterID the ID of the requester
     * @return a list of submissions in the track
     */
    @Override
    public List<Submission> getSubmissionsInTrack(Long conferenceID, Long trackID, Long requesterID)
            throws NotFoundException {
        try {
            String url = submissionsURL + "/submission/event/" + conferenceID
                + "/track/" + trackID + "/" + requesterID;
            String response = httpRequestSender.sendGetRequest(url);
            List<Submission> submissions;
            submissions = objectMapper.readValue(response, List.class);
            return submissions;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse the HTTP response");
        }
    }
}
