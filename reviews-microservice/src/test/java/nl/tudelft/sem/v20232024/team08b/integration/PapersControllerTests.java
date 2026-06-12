package nl.tudelft.sem.v20232024.team08b.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.PapersService;
import nl.tudelft.sem.v20232024.team08b.controllers.PapersController;
import nl.tudelft.sem.v20232024.team08b.dtos.review.Paper;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperStatus;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperSummary;
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

import java.util.List;

import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PapersControllerTests {
    MockMvc mockMvc;
    private final PapersService paperService = Mockito.mock(PapersService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Paper fakePaper;
    private PaperSummary fakeTitleAndAbstract;

    private PaperStatus fakeStatus;
    private Long requesterID;
    private Long paperID;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(
                new PapersController(paperService)
        ).build();
        fakePaper = new Paper();
        fakePaper.setTitle("Title");
        fakePaper.setAbstractSection("Abstract");
        fakePaper.setKeywords(List.of("Keyword"));
        fakePaper.setMainText("Text");
        fakePaper.setReplicationPackageLink("https://localhost/paper");

        fakeTitleAndAbstract = new PaperSummary();
        fakeTitleAndAbstract.setTitle("Title");
        fakeTitleAndAbstract.setAbstractSection("Abstract");

        fakeStatus = PaperStatus.NOT_DECIDED;

        requesterID = 1L;
        paperID = 2L;
    }

    @Test
    public void getPaperSuccessfully() throws Exception {

        when(paperService.getPaper(requesterID, paperID)).thenReturn(fakePaper);

        String expectedJSON = objectMapper.writeValueAsString(fakePaper);

        mockMvc.perform(
                MockMvcRequestBuilders.get("/papers/" + paperID.toString())
                        .param("requesterID", requesterID.toString())
                        .param("paperID", paperID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.content().json(expectedJSON));

        verify(paperService).getPaper(requesterID, paperID);
    }

    /**
     * Simulates an exception inside getTitleAndAbstract function
     * and checks for correct status code.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    public void getPaperWithException(Exception exception, int expected) throws Exception {

        doThrow(exception).when(paperService).getPaper(requesterID, paperID);

        mockMvc.perform(
                MockMvcRequestBuilders.get("/papers/" + paperID.toString())
                        .param("requesterID", requesterID.toString())
                        .param("paperID", paperID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().is(expected));

        verify(paperService).getPaper(requesterID, paperID);
    }

    @Test
    void getPaper_NoSuchRequester() throws Exception {
        getPaperWithException(new IllegalCallerException(""), 404);
    }

    @Test
    void getPaper_NoSuchPaper() throws Exception {
        getPaperWithException(new NotFoundException(""), 404);
    }

    @Test
    void getPaper_IllegalAccess() throws Exception {
        getPaperWithException(new IllegalAccessException(""), 403);
    }

    @Test
    void getPaper_InternalError() throws Exception {
        getPaperWithException(new RuntimeException(""), 500);
    }

    @Test
    public void getTitleAndAbstractSuccessfully() throws Exception {

        when(paperService.getTitleAndAbstract(requesterID, paperID)).thenReturn(fakeTitleAndAbstract);

        String expectedJSON = objectMapper.writeValueAsString(fakeTitleAndAbstract);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/papers/" + paperID.toString() + "/title-and-abstract")
                                .param("requesterID", requesterID.toString())
                                .param("paperID", paperID.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.content().json(expectedJSON));

        verify(paperService).getTitleAndAbstract(requesterID, paperID);
    }

    /**
     * Simulates an exception inside getTitleAndAbstract function
     * and checks for correct status code.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    public void getTitleAndAbstractWithException(Exception exception, int expected) throws Exception {

        doThrow(exception).when(paperService).getTitleAndAbstract(requesterID, paperID);

        mockMvc.perform(
                MockMvcRequestBuilders.get("/papers/" + paperID.toString() + "/title-and-abstract")
                        .param("requesterID", requesterID.toString())
                        .param("paperID", paperID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().is(expected));

        verify(paperService).getTitleAndAbstract(requesterID, paperID);
    }

    @Test
    void getTitleAndAbstract_NoSuchRequester() throws Exception {
        getTitleAndAbstractWithException(new IllegalCallerException(""), 404);
    }

    @Test
    void getTitleAndAbstract_NoSuchPaper() throws Exception {
        getTitleAndAbstractWithException(new NotFoundException(""), 404);
    }

    @Test
    void getTitleAndAbstract_IllegalAccess() throws Exception {
        getTitleAndAbstractWithException(new IllegalAccessException(""), 403);
    }

    @Test
    void getTitleAndAbstract_InternalError() throws Exception {
        getTitleAndAbstractWithException(new RuntimeException(""), 500);
    }

    @Test
    public void getStateSuccessfully() throws Exception {

        when(paperService.getState(requesterID, paperID)).thenReturn(fakeStatus);

        String expectedJSON = objectMapper.writeValueAsString(fakeStatus);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/papers/" + paperID.toString() + "/status")
                                .param("requesterID", requesterID.toString())
                                .param("paperID", paperID.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.content().json(expectedJSON));

        verify(paperService).getState(requesterID, paperID);
    }

    /**
     * Simulates an exception inside getState function
     * and checks for correct status code.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    public void getStateWithException(Exception exception, int expected) throws Exception {

        doThrow(exception).when(paperService).getState(requesterID, paperID);

        mockMvc.perform(
                MockMvcRequestBuilders.get("/papers/" + paperID.toString() + "/status")
                        .param("requesterID", requesterID.toString())
                        .param("paperID", paperID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().is(expected));

        verify(paperService).getState(requesterID, paperID);
    }

    @Test
    void getState_NoSuchPaper() throws Exception {
        getStateWithException(new NotFoundException(""), 404);
    }

    @Test
    void getState_IllegalAccess() throws Exception {
        getStateWithException(new IllegalAccessException(""), 403);
    }

    @Test
    void getState_InternalError() throws Exception {
        getStateWithException(new RuntimeException(""), 500);
    }
}
