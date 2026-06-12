package nl.tudelft.sem.v20232024.team08b.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.TrackAnalyticsService;
import nl.tudelft.sem.v20232024.team08b.application.TrackDeadlineService;
import nl.tudelft.sem.v20232024.team08b.application.TrackInformationService;
import nl.tudelft.sem.v20232024.team08b.controllers.TracksController;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperSummaryWithID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackAnalytics;
import nl.tudelft.sem.v20232024.team08b.dtos.review.TrackPhase;
import nl.tudelft.sem.v20232024.team08b.exceptions.ForbiddenAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TracksControllerTests {
    MockMvc mockMvc;
    final TrackInformationService trackInformationService = Mockito.mock(TrackInformationService.class);
    final TrackAnalyticsService trackAnalyticsService = Mockito.mock(TrackAnalyticsService.class);
    final TrackDeadlineService trackDeadlineService = Mockito.mock(TrackDeadlineService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(
                new TracksController(trackInformationService, trackAnalyticsService, trackDeadlineService)
        ).build();
    }

    /**
     * Simulates an exception inside getTrackPhase function and checks if
     * correct status code was returned.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    private void getPhase_WithException(Exception exception, int expected) throws Exception {
        Long requesterID = 1L;
        Long conferenceID = 2L;
        Long trackID = 3L;

        // Make sure correct exception is thrown when the respective call to service is called
        doThrow(exception).when(trackInformationService).getTrackPhase(requesterID, conferenceID, trackID);

        // Send the request to respective endpoint
        mockMvc.perform(
                MockMvcRequestBuilders.get("/conferences/{conferenceID}/tracks/{trackID}/phase",
                                conferenceID.toString(), trackID.toString())
                        .param("requesterID", requesterID.toString())
        ).andExpect(MockMvcResultMatchers.status().is(expected));

        // Make sure the required call to the service was made
        verify(trackInformationService).getTrackPhase(requesterID, conferenceID, trackID);
    }

    @Test
    void getPhase_NoSuchPaper() throws Exception {
        getPhase_WithException(new NotFoundException(""), 404);
    }

    @Test
    void getPhase_IllegalAccess() throws Exception {
        getPhase_WithException(new IllegalAccessException(""), 403);
    }

    @Test
    void getPhase_UnknownError() throws Exception {
        getPhase_WithException(new RuntimeException(""), 500);
    }

    @Test
    void getPhase_Successful() throws Exception {
        TrackPhase fakeTrackPhase = TrackPhase.BIDDING;
        String expectedJSON = objectMapper.writeValueAsString(fakeTrackPhase);

        Long requesterID = 1L;
        Long conferenceID = 2L;
        Long trackID = 3L;

        // Make sure correct exception is thrown when the respective call to service is called
        doReturn(fakeTrackPhase).when(trackInformationService).getTrackPhase(requesterID, conferenceID, trackID);

        // Send the request to respective endpoint
        mockMvc.perform(
                MockMvcRequestBuilders.get("/conferences/{conferenceID}/tracks/{trackID}/phase",
                                conferenceID.toString(), trackID.toString())
                        .param("requesterID", requesterID.toString())
                ).andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.content().json(expectedJSON));

        // Make sure the required call to the service was made
        verify(trackInformationService).getTrackPhase(requesterID, conferenceID, trackID);
    }

    /**
     * Simulates an exception inside getBiddingDeadline function and checks if
     * correct status code was returned.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    private void getBiddingDeadline_WithException(Exception exception, int expected) throws Exception {
        Long requesterID = 1L;
        Long conferenceID = 2L;
        Long trackID = 3L;

        // Make sure correct exception is thrown when the respective call to service is called
        doThrow(exception).when(trackDeadlineService).getBiddingDeadline(requesterID, conferenceID, trackID);

        // Send the request to respective endpoint
        mockMvc.perform(
                MockMvcRequestBuilders.get("/conferences/{conferenceID}/tracks/{trackID}/bidding-deadline",
                                conferenceID.toString(), trackID.toString())
                        .param("requesterID", requesterID.toString())
        ).andExpect(MockMvcResultMatchers.status().is(expected));

        // Make sure the required call to the service was made
        verify(trackDeadlineService).getBiddingDeadline(requesterID, conferenceID, trackID);
    }

    @Test
    void getBiddingDeadline_NoSuchPaper() throws Exception {
        getBiddingDeadline_WithException(new NotFoundException(""), 404);
    }

    @Test
    void getBiddingDeadline_IllegalAccess() throws Exception {
        getBiddingDeadline_WithException(new IllegalAccessException(""), 403);
    }

    @Test
    void getBiddingDeadline_UnknownError() throws Exception {
        getBiddingDeadline_WithException(new RuntimeException(""), 500);
    }

    @Test
    void getBiddingDeadline_Successful() throws Exception {
        Date fakeDate = Date.valueOf(LocalDate.of(2012, 10, 2));
        String expectedJSON = objectMapper.writeValueAsString(fakeDate);

        Long requesterID = 1L;
        Long conferenceID = 2L;
        Long trackID = 3L;

        // Make sure correct exception is thrown when the respective call to service is called
        doReturn(fakeDate).when(trackDeadlineService).getBiddingDeadline(requesterID, conferenceID, trackID);

        // Send the request to respective endpoint
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/conferences/{conferenceID}/tracks/{trackID}/bidding-deadline",
                                        conferenceID.toString(), trackID.toString())
                                .param("requesterID", requesterID.toString())
                ).andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.content().json(expectedJSON));

        // Make sure the required call to the service was made
        verify(trackDeadlineService).getBiddingDeadline(requesterID, conferenceID, trackID);
    }

    /**
     * Simulates an exception inside setBiddingDeadline function and checks if
     * correct status code was returned.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    private void setBiddingDeadline_WithException(Exception exception, int expected) throws Exception {
        Long requesterID = 1L;
        Long conferenceID = 2L;
        Long trackID = 3L;

        // Create the fake date
        Date date = Date.valueOf(LocalDate.of(2012, 10, 2));
        String dateJSON = objectMapper.writeValueAsString(date);

        // Make sure correct exception is thrown when the respective call to service is called
        doThrow(exception).when(trackDeadlineService).setBiddingDeadline(requesterID, conferenceID, trackID, date);

        // Send the request to respective endpoint
        mockMvc.perform(
                MockMvcRequestBuilders.put("/conferences/{conferenceID}/tracks/{trackID}/bidding-deadline",
                                conferenceID.toString(), trackID.toString())
                        .param("requesterID", requesterID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dateJSON)
        ).andExpect(MockMvcResultMatchers.status().is(expected));

        // Make sure the required call to the service was made
        verify(trackDeadlineService).setBiddingDeadline(requesterID, conferenceID, trackID, date);
    }

    @Test
    void setBiddingDeadline_NoSuchPaper() throws Exception {
        setBiddingDeadline_WithException(new NotFoundException(""), 404);
    }

    @Test
    void setBiddingDeadline_IllegalAccess() throws Exception {
        setBiddingDeadline_WithException(new IllegalAccessException(""), 403);
    }

    @Test
    void setBiddingDeadline_UnknownError() throws Exception {
        setBiddingDeadline_WithException(new RuntimeException(""), 500);
    }

    @Test
    void setBiddingDeadline_Successful() throws Exception {
        Long requesterID = 1L;
        Long conferenceID = 2L;
        Long trackID = 3L;

        // Create the fake date
        Date date = Date.valueOf(LocalDate.of(2012, 10, 2));
        String dateJSON = objectMapper.writeValueAsString(date);

        // Make sure correct exception is thrown when the respective call to service is called
        doNothing().when(trackDeadlineService).setBiddingDeadline(requesterID, conferenceID, trackID, date);

        // Send the request to respective endpoint
        mockMvc.perform(
                MockMvcRequestBuilders.put("/conferences/{conferenceID}/tracks/{trackID}/bidding-deadline",
                                conferenceID.toString(), trackID.toString())
                        .param("requesterID", requesterID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dateJSON)
        ).andExpect(MockMvcResultMatchers.status().is(200))
        .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

        // Make sure the required call to the service was made
        verify(trackDeadlineService).setBiddingDeadline(requesterID, conferenceID, trackID, date);
    }

    @Test
    void getAnalyticsSuccess() throws Exception {
        Long requesterID = 1L;
        Long conferenceID = 2L;
        Long trackID = 3L;

        var trackAnalytics = new TrackAnalytics(3, 2, 1);
        when(trackAnalyticsService.getAnalytics(new TrackID(conferenceID, trackID), requesterID)).thenReturn(trackAnalytics);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/conferences/{conferenceID}/tracks/{trackID}/analytics",
                                        conferenceID.toString(), trackID.toString())
                                .param("requesterID", requesterID.toString())
                ).andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(trackAnalytics)));
    }

    @Test
    void getAnalyticsTrackNotFound() throws Exception {
        Long requesterID = 1L;
        Long conferenceID = 2L;
        Long trackID = 3L;

        when(trackAnalyticsService.getAnalytics(new TrackID(conferenceID, trackID), requesterID))
                .thenThrow(new NotFoundException(""));

        mockMvc.perform(
                MockMvcRequestBuilders.get("/conferences/{conferenceID}/tracks/{trackID}/analytics",
                                conferenceID.toString(), trackID.toString())
                        .param("requesterID", requesterID.toString())
        ).andExpect(MockMvcResultMatchers.status().is(404));
    }

    @Test
    void getAnalyticsForbiddenAccess() throws Exception {
        Long requesterID = 1L;
        Long conferenceID = 2L;
        Long trackID = 3L;

        when(trackAnalyticsService.getAnalytics(new TrackID(conferenceID, trackID), requesterID))
                .thenThrow(new ForbiddenAccessException());

        mockMvc.perform(
                MockMvcRequestBuilders.get("/conferences/{conferenceID}/tracks/{trackID}/analytics",
                                conferenceID.toString(), trackID.toString())
                        .param("requesterID", requesterID.toString())
        ).andExpect(MockMvcResultMatchers.status().is(403));
    }

    @Test
    void getPapersSuccess() throws Exception {
        var paper1 = new PaperSummaryWithID();
        paper1.setPaperID(1L);
        paper1.setTitle("abc");
        paper1.setAbstractSection("def");
        var paper2 = new PaperSummaryWithID();
        paper2.setPaperID(2L);
        paper2.setTitle("zyx");
        paper2.setAbstractSection("wvu");
        var papersSummaryWithIDS = new ArrayList<PaperSummaryWithID>();
        papersSummaryWithIDS.add(paper1);
        papersSummaryWithIDS.add(paper2);
        Long requesterID = 3L;
        Long conferenceID = 4L;
        Long trackID = 5L;
        when(trackInformationService.getPapers(requesterID, conferenceID, trackID)).thenReturn(papersSummaryWithIDS);
        mockMvc.perform(
                MockMvcRequestBuilders.get("/conferences/{conferenceID}/tracks/{trackID}/papers",
                        conferenceID, trackID)
                    .param("requesterID", requesterID.toString())
            ).andExpect(MockMvcResultMatchers.status().is(200))
            .andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(papersSummaryWithIDS)));
    }

    @Test
    void getPapersNotFound() throws Exception {
        Long requesterID = 3L;
        Long conferenceID = 4L;
        Long trackID = 5L;
        when(trackInformationService.getPapers(requesterID, conferenceID, trackID))
            .thenThrow(new NotFoundException(""));

        mockMvc.perform(
            MockMvcRequestBuilders.get("/conferences/{conferenceID}/tracks/{trackID}/papers",
                    conferenceID, trackID)
                .param("requesterID", requesterID.toString())
        ).andExpect(MockMvcResultMatchers.status().is(404));
    }

    @Test
    void getPapersForbiddenAccess() throws Exception {
        Long requesterID = 3L;
        Long conferenceID = 4L;
        Long trackID = 5L;
        when(trackInformationService.getPapers(requesterID, conferenceID, trackID))
            .thenThrow(new ForbiddenAccessException());

        mockMvc.perform(
            MockMvcRequestBuilders.get("/conferences/{conferenceID}/tracks/{trackID}/papers",
                    conferenceID, trackID)
                .param("requesterID", requesterID.toString())
        ).andExpect(MockMvcResultMatchers.status().is(403));
    }
}
