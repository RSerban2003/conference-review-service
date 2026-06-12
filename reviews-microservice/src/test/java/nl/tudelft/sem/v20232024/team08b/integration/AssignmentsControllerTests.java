package nl.tudelft.sem.v20232024.team08b.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.AssignmentsService;
import nl.tudelft.sem.v20232024.team08b.controllers.AssignmentsController;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperSummaryWithID;
import nl.tudelft.sem.v20232024.team08b.exceptions.ConflictOfInterestException;
import nl.tudelft.sem.v20232024.team08b.exceptions.ForbiddenAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AssignmentsControllerTests {


    private final AssignmentsService assignmentsService = Mockito.mock(AssignmentsService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();


    private MockMvc mockMvc;
    private final Long requesterID = 1L;
    private final Long reviewerID = 2L;
    private final Long paperID = 3L;
    private final Long conferenceID = 4L;
    private final Long trackID = 5L;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentsController(assignmentsService)).build();
    }

    @Test
    void assignManualReturnsOk() throws Exception {

        doNothing().when(assignmentsService).assignManually(requesterID, reviewerID, paperID);

        mockMvc.perform(post("/papers/{paperID}/assignees/{reviewerID}", paperID, reviewerID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

        verify(assignmentsService).assignManually(requesterID, reviewerID, paperID);
    }

    @Test
    void assignManualReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Not found")).when(assignmentsService).assignManually(requesterID,
            reviewerID, paperID);

        mockMvc.perform(post("/papers/{paperID}/assignees/{reviewerID}", paperID, reviewerID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(assignmentsService).assignManually(requesterID, reviewerID, paperID);
    }

    @Test
    void assignManualReturnsForbidden() throws Exception {

        doThrow(new IllegalAccessException("Forbidden")).when(assignmentsService).assignManually(requesterID,
            reviewerID, paperID);

        mockMvc.perform(post("/papers/{paperID}/assignees/{reviewerID}", paperID, reviewerID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());

        verify(assignmentsService).assignManually(requesterID, reviewerID, paperID);
    }

    @Test
    void assignManualReturnsConflict() throws Exception {

        doThrow(new ConflictOfInterestException("COI")).when(assignmentsService).assignManually(requesterID,
            reviewerID, paperID);

        mockMvc.perform(post("/papers/{paperID}/assignees/{reviewerID}", paperID, reviewerID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict());

        verify(assignmentsService).assignManually(requesterID, reviewerID, paperID);
    }

    @Test
    void assignManualReturnsInternalServerError() throws Exception {

        doThrow(new RuntimeException("Internal server error")).when(assignmentsService).assignManually(requesterID,
            reviewerID, paperID);


        mockMvc.perform(post("/papers/{paperID}/assignees/{reviewerID}", paperID, reviewerID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());

        verify(assignmentsService).assignManually(requesterID, reviewerID, paperID);
    }

    @Test
    void assignmentsReturnsOk() throws Exception {

        List<Long> expectedAssignments = Arrays.asList(3L, 4L, 5L);
        when(assignmentsService.assignments(requesterID, paperID)).thenReturn(expectedAssignments);

        mockMvc.perform(get("/papers/{paperID}/assignees", paperID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json("[3, 4, 5]"));

        verify(assignmentsService).assignments(requesterID, paperID);
    }

    @Test
    void assignmentsReturnsForbidden() throws Exception {

        when(assignmentsService.assignments(requesterID, paperID)).thenThrow(new IllegalAccessException("Forbidden"));

        mockMvc.perform(get("/papers/{paperID}/assignees", paperID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());

        verify(assignmentsService).assignments(requesterID, paperID);
    }

    @Test
    void assignmentsReturnsNotFound() throws Exception {

        when(assignmentsService.assignments(requesterID, paperID)).thenThrow(new IllegalCallerException("Not Found"));

        mockMvc.perform(get("/papers/{paperID}/assignees", paperID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(assignmentsService).assignments(requesterID, paperID);
    }

    @Test
    void assignmentsReturnsInternalServerError() throws Exception {

        when(assignmentsService.assignments(requesterID, paperID)).thenThrow(new RuntimeException("Internal Server Error"));

        mockMvc.perform(get("/papers/{paperID}/assignees", paperID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());

        verify(assignmentsService).assignments(requesterID, paperID);
    }

    @Test
    void assignmentsReturnsConflict() throws Exception {

        when(assignmentsService.assignments(requesterID, paperID)).thenThrow(new IllegalArgumentException());

        mockMvc.perform(get("/papers/{paperID}/assignees", paperID)
                        .param("requesterID", String.valueOf(requesterID))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());

        verify(assignmentsService).assignments(requesterID, paperID);
    }

    @Test
    void assignAutoReturnsOk() throws Exception {

        doNothing().when(assignmentsService).assignAuto(requesterID, conferenceID, trackID);

        mockMvc.perform(put("/conferences/{conferenceID}/tracks/{trackID}/automatic", conferenceID, trackID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(assignmentsService).assignAuto(requesterID, conferenceID, trackID);
    }

    @Test
    void assignAutoReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Not found")).when(assignmentsService).assignAuto(requesterID,
            conferenceID, trackID);

        mockMvc.perform(put("/conferences/{conferenceID}/tracks/{trackID}/automatic", conferenceID, trackID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(assignmentsService).assignAuto(requesterID, conferenceID, trackID);
    }

    @Test
    void assignAutoReturnsForbidden() throws Exception {

        doThrow(new IllegalAccessException("Forbidden")).when(assignmentsService).assignAuto(requesterID,
            conferenceID, trackID);

        mockMvc.perform(put("/conferences/{conferenceID}/tracks/{trackID}/automatic", conferenceID, trackID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());

        verify(assignmentsService).assignAuto(requesterID, conferenceID, trackID);
    }

    @Test
    void assignAutoReturnsConflict() throws Exception {

        doThrow(new IllegalArgumentException("CONFLICT")).when(assignmentsService).assignAuto(requesterID,
            conferenceID, trackID);

        mockMvc.perform(put("/conferences/{conferenceID}/tracks/{trackID}/automatic", conferenceID, trackID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict());

        verify(assignmentsService).assignAuto(requesterID, conferenceID, trackID);
    }

    @Test
    void assignAutoReturnsInternalServerError() throws Exception {

        doThrow(new RuntimeException("Internal server error")).when(assignmentsService).assignAuto(requesterID,
            conferenceID, trackID);


        mockMvc.perform(put("/conferences/{conferenceID}/tracks/{trackID}/automatic", conferenceID, trackID)
            .param("requesterID", String.valueOf(requesterID))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());

        verify(assignmentsService).assignAuto(requesterID, conferenceID, trackID);
    }

    @Test
    void getAssignedPapers_Successful() throws Exception {
        PaperSummaryWithID summaryWithID = new PaperSummaryWithID();
        summaryWithID.setAbstractSection("a");
        summaryWithID.setPaperID(paperID);
        summaryWithID.setTitle("t");

        List<PaperSummaryWithID> list = List.of(summaryWithID);
        String paperSummaryJSON = objectMapper.writeValueAsString(list);
        doReturn(list).when(assignmentsService).getAssignedPapers(requesterID);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/papers/by-reviewer")
                                .param("requesterID", Long.toString(requesterID))
                ).andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.content().json(paperSummaryJSON));
        verify(assignmentsService).getAssignedPapers(requesterID);
    }

    /**
     * Simulates an exception inside getAssignedPapers function and checks if
     * correct status code was returned.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    public void getAssignedPapers_Exceptions(Exception exception, int expected) throws Exception {
        long requesterID = 1L;

        doThrow(exception).when(assignmentsService).getAssignedPapers(requesterID);

        mockMvc.perform(
                MockMvcRequestBuilders.get("/papers/by-reviewer")
                        .param("requesterID", Long.toString(requesterID))
        ).andExpect(MockMvcResultMatchers.status().is(expected));

        verify(assignmentsService).getAssignedPapers(requesterID);
    }

    @Test
    public void getAssignedPaper_NotFoundException() throws Exception {
        getAssignedPapers_Exceptions(new NotFoundException("User does not exist!"), 404);
    }

    @Test
    public void getAssignedPaper_OtherExceptions() throws Exception {
        getAssignedPapers_Exceptions(new RuntimeException(), 500);
    }

    @Test
    public void testFinalizationSuccess() throws Exception {
        doNothing().when(assignmentsService).finalization(anyLong(), anyLong(), anyLong());

        mockMvc.perform(post("/conferences/{conferenceID}/tracks/{trackID}/finalization", 1L, 2L)
                        .param("requesterID", "3"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(assignmentsService, times(1)).finalization(eq(3L),
                eq(1L), eq(2L));
    }

    @Test
    public void testFinalizationIllegalStateException() throws Exception {
        doThrow(new IllegalStateException()).when(assignmentsService).finalization(anyLong(), anyLong(), anyLong());

        mockMvc.perform(post("/conferences/{conferenceID}/tracks/{trackID}/finalization", 1L, 2L)
                        .param("requesterID", "3"))
                .andExpect(status().isConflict());

        verify(assignmentsService, times(1)).finalization(3L,
                1L, 2L);
    }

    @Test
    public void testFinalizationNotFoundException() throws Exception {
        doThrow(new NotFoundException("")).when(assignmentsService).finalization(anyLong(), anyLong(), anyLong());

        mockMvc.perform(post("/conferences/{conferenceID}/tracks/{trackID}/finalization", 1L, 2L)
                        .param("requesterID", "3"))
                .andExpect(status().isNotFound());

        verify(assignmentsService, times(1)).finalization(3L, 1L, 2L);
    }

    @Test
    public void testFinalizationForbiddenAccessException() throws Exception {
        doThrow(new ForbiddenAccessException()).when(assignmentsService).finalization(anyLong(), anyLong(), anyLong());

        mockMvc.perform(post("/conferences/{conferenceID}/tracks/{trackID}/finalization", 1L, 2L)
                        .param("requesterID", "3"))
                .andExpect(status().isForbidden());

        verify(assignmentsService, times(1)).finalization(3L,
                1L, 2L);
    }

    @Test
    void testRemoveSuccessful() throws Exception {

        doNothing().when(assignmentsService).remove(requesterID, paperID, reviewerID);

        mockMvc.perform(delete("/papers/{paperID}/assignees/{reviewerID}", paperID, reviewerID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is(200))
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));


        verify(assignmentsService, times(1)).remove(requesterID, paperID, reviewerID);
    }

    @Test
    void testRemoveNotFound() throws Exception {


        doThrow(new NotFoundException("")).when(assignmentsService).remove(requesterID, paperID, reviewerID);

        mockMvc.perform(delete("/papers/{paperID}/assignees/{reviewerID}", paperID, reviewerID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(assignmentsService, times(1)).remove(requesterID, paperID, reviewerID);
    }

    @Test
    void testRemoveForbidden() throws Exception {

        doThrow(new IllegalAccessException()).when(assignmentsService).remove(requesterID, paperID, reviewerID);

        mockMvc.perform(delete("/papers/{paperID}/assignees/{reviewerID}", paperID, reviewerID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());

        verify(assignmentsService, times(1)).remove(requesterID, paperID, reviewerID);
    }

    @Test
    void testRemoveInternalServerError() throws Exception {

        doThrow(new RuntimeException()).when(assignmentsService).remove(requesterID, paperID, reviewerID);

        mockMvc.perform(delete("/papers/{paperID}/assignees/{reviewerID}", paperID, reviewerID)
                .param("requesterID", String.valueOf(requesterID))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());

        verify(assignmentsService, times(1)).remove(requesterID, paperID, reviewerID);
    }
}
