package nl.tudelft.sem.v20232024.team08b.unit.communicators;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.communicators.UsersMicroserviceCommunicator;
import nl.tudelft.sem.v20232024.team08b.dtos.users.RolesOfUser;
import nl.tudelft.sem.v20232024.team08b.dtos.users.Track;
import nl.tudelft.sem.v20232024.team08b.utils.HttpRequestSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class UsersMicroserviceCommunicatorTests {
    final HttpRequestSender httpRequestSender = Mockito.mock(HttpRequestSender.class);

    final ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);

    final UsersMicroserviceCommunicator usersCommunicator =
        new UsersMicroserviceCommunicator(objectMapper, httpRequestSender);

    @Test
    void getRolesOfUserJsonFail() throws NotFoundException, IOException {
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any())).thenReturn("json");
        when(
            objectMapper.readValue("json", RolesOfUser.class)
        ).thenThrow(new RuntimeException(""));
        assertThrows(RuntimeException.class, () -> usersCommunicator.getRolesOfUser(1L));
    }

    @Test
    void getRolesOfUserUserNotFound() throws NotFoundException {
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any()))
            .thenThrow(new NotFoundException(""));
        assertThrows(NotFoundException.class, () -> usersCommunicator.getRolesOfUser(1L));
    }

    @Test
    void getRolesOfUserUserSuccessful() throws NotFoundException, IOException {
        RolesOfUser fakeRoles = new RolesOfUser();
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any())).thenReturn("json");
        when(
            objectMapper.readValue("json", RolesOfUser.class)
        ).thenReturn(fakeRoles);
        assertThat(usersCommunicator.getRolesOfUser(1L)).isEqualTo(fakeRoles);
    }


    @Test
    void getTrack_Fail() throws NotFoundException, IOException {
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any())).thenReturn("json");
        when(
            objectMapper.readValue("json", Track.class)
        ).thenThrow(new RuntimeException(""));
        assertThrows(RuntimeException.class, () -> usersCommunicator.getTrack(1L, 2L));
    }

    @Test
    void getTrack_NotFound() throws NotFoundException {
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any()))
            .thenThrow(new NotFoundException(""));
        assertThrows(NotFoundException.class, () -> usersCommunicator.getTrack(1L, 2L));
    }

    @Test
    void getTrack_Successful() throws NotFoundException, IOException {
        Track fakeTrack = new Track();
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any())).thenReturn("json");
        when(
            objectMapper.readValue("json", Track.class)
        ).thenReturn(fakeTrack);
        assertThat(usersCommunicator.getTrack(1L, 2L)).isEqualTo(fakeTrack);
    }

}
