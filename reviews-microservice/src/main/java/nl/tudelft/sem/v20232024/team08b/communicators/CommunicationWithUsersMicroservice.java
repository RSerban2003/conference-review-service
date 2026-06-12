package nl.tudelft.sem.v20232024.team08b.communicators;

import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.dtos.users.RolesOfUser;
import nl.tudelft.sem.v20232024.team08b.dtos.users.Track;

public interface CommunicationWithUsersMicroservice {
    Track getTrack(Long conferenceID, Long trackID) throws NotFoundException;

    RolesOfUser getRolesOfUser(Long userID) throws NotFoundException;
}
