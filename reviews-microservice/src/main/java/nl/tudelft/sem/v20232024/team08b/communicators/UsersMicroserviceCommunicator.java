package nl.tudelft.sem.v20232024.team08b.communicators;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.dtos.users.RolesOfUser;
import nl.tudelft.sem.v20232024.team08b.dtos.users.Track;
import nl.tudelft.sem.v20232024.team08b.utils.HttpRequestSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UsersMicroserviceCommunicator implements CommunicationWithUsersMicroservice {
    private final Long ourID = -1L;
    private final String usersURL = "http://localhost:8082";
    private final ObjectMapper objectMapper;
    private final HttpRequestSender httpRequestSender;


    /**
     * Default constructor.
     *
     * @param httpRequestSender class used for sending HTTP requests
     */
    @Autowired
    public UsersMicroserviceCommunicator(HttpRequestSender httpRequestSender) {
        this.httpRequestSender = httpRequestSender;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Constructor used for testing purposes.
     *
     * @param httpRequestSender class used for sending HTTP requests
     * @param objectMapper class used to map objects to json
     */
    public UsersMicroserviceCommunicator(ObjectMapper objectMapper, HttpRequestSender httpRequestSender) {
        this.objectMapper = objectMapper;
        this.httpRequestSender = httpRequestSender;
    }

    /**
     * Gets a track from the Users microservice.
     *
     * @param conferenceID the ID of the conference the track is in
     * @param trackID the ID of the conference the track is in
     * @return the track object, from the Users microservice
     */
    @Override
    public Track getTrack(Long conferenceID, Long trackID) throws NotFoundException {
        try {
            String url = usersURL + "/track/" + conferenceID + "/" + trackID;
            String response = httpRequestSender.sendGetRequest(url);
            Track track;
            track = objectMapper.readValue(response, Track.class);
            return track;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse the HTTP response");
        }
    }


    /**
     * Gets from the Users microservice all the roles of a user.
     *
     * @param userID the ID of the user
     * @return a list of roles of that user
     */
    @Override
    public RolesOfUser getRolesOfUser(Long userID) throws NotFoundException {
        try {
            String url = usersURL + "/user/" + userID + "/tracks/role";
            String response = httpRequestSender.sendGetRequest(url);
            RolesOfUser rolesOfUser;
            rolesOfUser = objectMapper.readValue(response, RolesOfUser.class);
            return rolesOfUser;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse the HTTP response");
        }
    }
}
