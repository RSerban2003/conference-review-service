package nl.tudelft.sem.v20232024.team08b.demo;

import nl.tudelft.sem.v20232024.team08b.communicators.CommunicationWithUsersMicroservice;
import nl.tudelft.sem.v20232024.team08b.dtos.users.RolesOfUser;
import nl.tudelft.sem.v20232024.team08b.dtos.users.RolesOfUserTracksInner;
import nl.tudelft.sem.v20232024.team08b.dtos.users.Track;
import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory stand-in for the Users microservice, active only under the
 * "demo" profile. It serves one conference track (conference 1, track 1)
 * whose submission deadline passed one hour before start-up, and five users:
 * user 1 is the PC chair (and PC member), users 2-4 are reviewers and
 * user 5 is an author.
 */
@Component
@Primary
@Profile("demo")
public class DemoUsersStub implements CommunicationWithUsersMicroservice {
    public static final long CONFERENCE_ID = 1L;
    public static final long TRACK_ID = 1L;

    private final Track demoTrack;
    private final Map<Long, RolesOfUser> rolesByUser = new HashMap<>();

    /**
     * Builds the demo track and the role table.
     */
    public DemoUsersStub() {
        demoTrack = new Track();
        demoTrack.setId(TRACK_ID);
        demoTrack.setName("Demo Track: Machine Learning");
        demoTrack.setDescription("Seeded track used by the demo profile.");
        // The submission deadline passed an hour ago, so the track starts
        // in the BIDDING phase (until the chair moves the bidding deadline).
        demoTrack.setDeadline(Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli());

        rolesByUser.put(1L, rolesOf("PC Chair", "PC Member"));
        rolesByUser.put(2L, rolesOf("PC Member"));
        rolesByUser.put(3L, rolesOf("PC Member"));
        rolesByUser.put(4L, rolesOf("PC Member"));
        rolesByUser.put(5L, rolesOf("Author"));
    }

    private static RolesOfUser rolesOf(String... roleNames) {
        RolesOfUser roles = new RolesOfUser();
        List<RolesOfUserTracksInner> tracks = new java.util.ArrayList<>();
        for (String roleName : roleNames) {
            RolesOfUserTracksInner entry = new RolesOfUserTracksInner();
            entry.setEventId(CONFERENCE_ID);
            entry.setTrackId(TRACK_ID);
            entry.setRoleName(roleName);
            tracks.add(entry);
        }
        roles.setTracks(tracks);
        return roles;
    }

    @Override
    public Track getTrack(Long conferenceID, Long trackID) throws NotFoundException {
        if (CONFERENCE_ID == conferenceID && TRACK_ID == trackID) {
            return demoTrack;
        }
        throw new NotFoundException("404, not found");
    }

    @Override
    public RolesOfUser getRolesOfUser(Long userID) throws NotFoundException {
        RolesOfUser roles = rolesByUser.get(userID);
        if (roles == null) {
            throw new NotFoundException("404, not found");
        }
        return roles;
    }
}
