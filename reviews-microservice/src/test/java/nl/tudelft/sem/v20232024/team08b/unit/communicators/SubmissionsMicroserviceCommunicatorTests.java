package nl.tudelft.sem.v20232024.team08b.unit.communicators;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.communicators.SubmissionsMicroserviceCommunicator;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.utils.HttpRequestSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class SubmissionsMicroserviceCommunicatorTests {
    final HttpRequestSender httpRequestSender = Mockito.mock(HttpRequestSender.class);

    final ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
    final SubmissionsMicroserviceCommunicator submissionsCommunicator = new
        SubmissionsMicroserviceCommunicator(objectMapper, httpRequestSender);

    @Test
    void getSubmissionJsonFail() throws NotFoundException, IOException {
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any())).thenReturn("json");
        when(
            objectMapper.readValue("json", Submission.class)
        ).thenThrow(new RuntimeException(""));
        assertThrows(RuntimeException.class, () -> submissionsCommunicator.getSubmission(1L));
    }

    @Test
    void getSubmissionNotFound() throws NotFoundException {
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any()))
            .thenThrow(new NotFoundException(""));
        assertThrows(NotFoundException.class, () -> submissionsCommunicator.getSubmission(1L));
    }

    @Test
    void getSubmissionSuccessful() throws NotFoundException, IOException {
        Submission fakeSubmission = new Submission();
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any())).thenReturn("json");
        when(
            objectMapper.readValue("json", Submission.class)
        ).thenReturn(fakeSubmission);
        assertThat(submissionsCommunicator.getSubmission(1L)).isEqualTo(fakeSubmission);
    }





    @Test
    void getSubmissionsInTrackSuccessful() throws NotFoundException, JsonProcessingException {
        var expected = List.of(new Submission(), new Submission());
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any())).thenReturn("json");
        when(
            objectMapper.readValue("json", List.class)
        ).thenReturn(expected);
        assertThat(submissionsCommunicator.getSubmissionsInTrack(1L, 2L,
            3L)).isEqualTo(expected);
    }

    @Test
    void getSubmissionsInTrackNotFound() throws NotFoundException {
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any()))
            .thenThrow(new NotFoundException(""));
        assertThrows(NotFoundException.class, () ->
            submissionsCommunicator.getSubmissionsInTrack(1L, 2L, 3L));
    }

    @Test
    void getSubmissionsInTrackOverload() throws NotFoundException, JsonProcessingException {
        var expected = List.of(new Submission(), new Submission());
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any())).thenReturn("json");
        when(
            objectMapper.readValue("json", List.class)
        ).thenReturn(expected);
        assertThat(submissionsCommunicator.getSubmissionsInTrack(1L, 2L))
            .isEqualTo(expected);
    }

    @Test
    void getSubmissionsInTrackFail() throws NotFoundException, IOException {
        when(httpRequestSender.sendGetRequest(ArgumentMatchers.any())).thenReturn("json");
        when(
            objectMapper.readValue("json", List.class)
        ).thenThrow(new RuntimeException(""));
        assertThrows(RuntimeException.class, () ->
            submissionsCommunicator.getSubmissionsInTrack(1L, 2L, 3L));
    }
}
