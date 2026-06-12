package nl.tudelft.sem.v20232024.team08b.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.application.DiscussionService;
import nl.tudelft.sem.v20232024.team08b.application.PapersService;
import nl.tudelft.sem.v20232024.team08b.application.ReviewsService;
import nl.tudelft.sem.v20232024.team08b.controllers.ReviewsController;
import nl.tudelft.sem.v20232024.team08b.domain.ConfidenceScore;
import nl.tudelft.sem.v20232024.team08b.domain.RecommendationScore;
import nl.tudelft.sem.v20232024.team08b.dtos.review.DiscussionComment;
import nl.tudelft.sem.v20232024.team08b.dtos.review.PaperPhase;
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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ReviewsControllerTests {
    MockMvc mockMvc;

    private final ReviewsService reviewsService = Mockito.mock(ReviewsService.class);
    private final PapersService papersService = Mockito.mock(PapersService.class);

    private final DiscussionService discussionService = Mockito.mock(DiscussionService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private nl.tudelft.sem.v20232024.team08b.dtos.review.Review fakeReviewDTO;

    private Long requesterID;
    private Long reviewerID;
    private Long paperID;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(
                new ReviewsController(reviewsService, papersService, discussionService)
        ).build();
        fakeReviewDTO = new nl.tudelft.sem.v20232024.team08b.dtos.review.Review(
                ConfidenceScore.BASIC,
                "Comment for author",
                "Confidential comment",
                RecommendationScore.STRONG_REJECT
        );
        requesterID = 1L;
        reviewerID = 2L;
        paperID = 3L;
    }

    /**
     * Simulates an exception inside finalizeDiscussion function and checks if
     * correct status code was returned.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    public void finalizeDiscussionWithException(Exception exception, int expected) throws Exception {
        Long requesterID = 1L;
        Long paperID = 2L;

        doThrow(exception).when(discussionService).finalizeDiscussionPhase(requesterID, paperID);

        // Send the request to respective endpoint
        mockMvc.perform(
                MockMvcRequestBuilders.post("/papers/{paperID}/reviews/finalization", paperID.toString())
                        .param("requesterID", requesterID.toString())
        ).andExpect(MockMvcResultMatchers.status().is(expected));

        // Make sure the required call to the service was made
        verify(discussionService, times(1)).finalizeDiscussionPhase(requesterID, paperID);
    }

    @Test
    public void finalizeDiscussion_PaperNotFound() throws Exception {
        finalizeDiscussionWithException(new NotFoundException("No such paper found"), 404);
    }

    @Test
    public void finalizeDiscussion_UnknownException() throws Exception {
        finalizeDiscussionWithException(new RuntimeException(), 500);
    }

    @Test
    public void finalizeDiscussion_InvalidRequester() throws Exception {
        finalizeDiscussionWithException(new IllegalAccessException(), 403);
    }

    @Test
    public void finalizeDiscussion_InvalidPaperPhase() throws Exception {
        finalizeDiscussionWithException(new IllegalStateException(), 409);
    }

    @Test
    public void finalizeDiscussion_InvalidReviews() throws Exception {
        finalizeDiscussionWithException(new IllegalStateException(), 409);
    }

    @Test
    public void finalizeDiscussionSuccessful() throws Exception {
        Long requesterID = 1L;
        Long paperID = 2L;
        doNothing().when(discussionService).finalizeDiscussionPhase(requesterID, paperID);

        mockMvc.perform(
                MockMvcRequestBuilders.post("/papers/{paperID}/reviews/finalization", paperID.toString())
                        .param("requesterID", requesterID.toString())
                ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
        verify(discussionService, times(1)).finalizeDiscussionPhase(requesterID, paperID);
    }

    @Test
    public void submitReviewSuccessful() throws Exception {
        // Convert the object into JSON to be passed as body
        String fakeReviewDTOJson = objectMapper.writeValueAsString(fakeReviewDTO);

        // Make sure nothing is done when the respective call to service is called
        doNothing().when(reviewsService).submitReview(fakeReviewDTO, requesterID, paperID);

        // Send the request to respective endpoint
        mockMvc.perform(
                MockMvcRequestBuilders.put("/papers/{paperID}/reviews", paperID.toString())
                        .param("requesterID", requesterID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fakeReviewDTOJson)
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

        // Make sure the required call to the service was made
        verify(reviewsService).submitReview(fakeReviewDTO, requesterID, paperID);
    }

    /**
     * Simulates an exception inside submitReview function and checks if
     * correct status code was returned.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    public void submitReviewWithException(Exception exception, int expected) throws Exception {
        // Convert the object into JSON to be passed as body
        String fakeReviewDTOJson = objectMapper.writeValueAsString(fakeReviewDTO);

        // Make sure nothing is done when the respective call to service is called
        doThrow(exception).when(reviewsService).submitReview(fakeReviewDTO, requesterID, paperID);

        // Send the request to respective endpoint
        mockMvc.perform(
                MockMvcRequestBuilders.put("/papers/{paperID}/reviews", paperID.toString())
                        .param("requesterID", requesterID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fakeReviewDTOJson)
        ).andExpect(MockMvcResultMatchers.status().is(expected));

        // Make sure the required call to the service was made
        verify(reviewsService).submitReview(fakeReviewDTO, requesterID, paperID);
    }

    @Test
    void testSubmit_ReviewNoSuchUser() throws Exception {
        submitReviewWithException(new IllegalCallerException(""), 404);
    }

    @Test
    void testSubmit_ReviewNoSuchPaper() throws Exception {
        submitReviewWithException(new NotFoundException(""), 404);
    }

    @Test
    void testSubmit_ReviewUserNotReviewer() throws Exception {
        submitReviewWithException(new IllegalAccessException(""), 403);
    }

    @Test
    void testSubmit_ReviewInternalError() throws Exception {
        submitReviewWithException(new RuntimeException(""), 500);
    }

    /**
     * Simulates an exception inside getReview function and checks if
     * correct status code was returned.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    public void getReviewWithException(Exception exception, int expected) throws Exception {
        // Make sure nothing is done when the respective call to service is called
        doThrow(exception).when(reviewsService).getReview(requesterID, reviewerID, paperID);

        // Send the request to respective endpoint
        mockMvc.perform(
                MockMvcRequestBuilders.get("/papers/{paperID}/reviews/by-reviewer/{reviewerID}",
                                paperID.toString(), reviewerID.toString())
                        .param("requesterID", requesterID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().is(expected));

        // Make sure the required call to the service was made
        verify(reviewsService).getReview(requesterID, reviewerID, paperID);
    }

    @Test
    void getReview_NoSuchRequester() throws Exception {
        getReviewWithException(new IllegalAccessException(""), 403);
    }

    @Test
    void getReview_NoSuchReview() throws Exception {
        getReviewWithException(new NotFoundException(""), 404);
    }

    @Test
    void getReview_IllegalAccess() throws Exception {
        getReviewWithException(new IllegalAccessException(""), 403);
    }

    @Test
    void getReview_OtherProblems() throws Exception {
        getReviewWithException(new RuntimeException(""), 500);
    }

    @Test
    public void getReview_Successful() throws Exception {
        // Make sure some fake review is returned from the service
        when(reviewsService.getReview(requesterID, reviewerID, paperID)).thenReturn(fakeReviewDTO);

        // Convert that fake review to json
        String expectedJSON = objectMapper.writeValueAsString(fakeReviewDTO);

        // Send the request to respective endpoint
        mockMvc.perform(
                MockMvcRequestBuilders.get("/papers/{paperID}/reviews/by-reviewer/{reviewerID}",
                                paperID.toString(), reviewerID.toString())
                        .param("requesterID", requesterID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().is(200))
        .andExpect(MockMvcResultMatchers.content().json(expectedJSON));

        // Make sure the required call to the service was made
        verify(reviewsService).getReview(requesterID, reviewerID, paperID);
    }

    /**
     * Simulates an exception inside getPaperPhase function and checks if
     * correct status code was returned.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    private void getPhase_WithException(Exception exception, int expected) throws Exception {
        // Make sure correct exception is thrown when the respective call to service is called
        doThrow(exception).when(papersService).getPaperPhase(requesterID, paperID);

        // Send the request to respective endpoint
        mockMvc.perform(
                MockMvcRequestBuilders.get("/papers/{paperID}/reviews/phase", paperID.toString())
                        .param("requesterID", requesterID.toString())
        ).andExpect(MockMvcResultMatchers.status().is(expected));

        // Make sure the required call to the service was made
        verify(papersService).getPaperPhase(requesterID, paperID);
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
        PaperPhase fakePaperPhase = PaperPhase.REVIEWED;

        Long requesterID = 1L;
        Long paperID = 2L;

        // Convert the object into JSON to be passed as body
        String fakePaperPhaseJson = objectMapper.writeValueAsString(fakePaperPhase);

        // Make sure correct exception is thrown when the respective call to service is called
        doReturn(fakePaperPhase).when(papersService).getPaperPhase(requesterID, paperID);

        // Send the request to respective endpoint
        mockMvc.perform(
                MockMvcRequestBuilders.get("/papers/{paperID}/reviews/phase", paperID.toString())
                        .param("requesterID", requesterID.toString())
        ).andExpect(MockMvcResultMatchers.status().is(200))
        .andExpect(MockMvcResultMatchers.content().json(fakePaperPhaseJson));

        // Make sure the required call to the service was made
        verify(papersService).getPaperPhase(requesterID, paperID);
    }

    @Test
    void getReviewers_Successful() throws Exception {
        List<Long> fakeReviewersIDs = List.of(1L, 2L, 3L);

        String fakeReviewersIDsJSON = objectMapper.writeValueAsString(fakeReviewersIDs);

        doReturn(fakeReviewersIDs).when(reviewsService).getReviewersFromPaper(requesterID, paperID);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/papers/{paperID}/reviewers", Long.toString(paperID))
                                .param("requesterID", Long.toString(requesterID))
                ).andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.content().json(fakeReviewersIDsJSON));

        verify(reviewsService).getReviewersFromPaper(requesterID, paperID);
    }

    /**
     * Simulates an exception inside getReviewers function and checks if
     * correct status code was returned.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    public void getReviewers_WithException(Exception exception, int expected) throws Exception {
        doThrow(exception).when(reviewsService).getReviewersFromPaper(requesterID, paperID);

        mockMvc.perform(
                MockMvcRequestBuilders.get("/papers/{paperID}/reviewers", Long.toString(paperID))
                        .param("requesterID", Long.toString(requesterID))
        ).andExpect(MockMvcResultMatchers.status().is(expected));

        verify(reviewsService).getReviewersFromPaper(requesterID, paperID);
    }

    @Test
    void getReviewers_NoSuchPaper() throws Exception {
        getReviewers_WithException(new NotFoundException(""), 404);
    }

    @Test
    void getReviewers_IllegalAccess() throws Exception {
        getReviewers_WithException(new IllegalAccessException(""), 403);
    }

    @Test
    void getReviewers_UnknownError() throws Exception {
        getReviewers_WithException(new RuntimeException(""), 500);
    }

    @Test
    public void submitConfidentialCommentSuccessfully() throws Exception {

        String text = "text";

        doNothing().when(discussionService).submitDiscussionComment(requesterID, reviewerID, paperID, text);

        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/papers/{paperID}/reviews/by-reviewer/{reviewerID}/discussion-comments", paperID, reviewerID)
                        .param("requesterID", requesterID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(text)
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

        verify(discussionService).submitDiscussionComment(requesterID, reviewerID, paperID, "text");
    }

    /**
     * Simulates an exception inside getReviewers function and checks if
     * correct status code was returned.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    public void submitConfidentialCommentWithException(Exception exception, int expected) throws Exception {

        String text = "text";

        doThrow(exception).when(discussionService).submitDiscussionComment(requesterID, reviewerID, paperID, text);

        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/papers/{paperID}/reviews/by-reviewer/{reviewerID}/discussion-comments", paperID, reviewerID)
                        .param("requesterID", requesterID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(text)
                )
                .andExpect(MockMvcResultMatchers.status().is(expected));

        verify(discussionService).submitDiscussionComment(requesterID, reviewerID, paperID, "text");
    }

    @Test
    void submitConfidentialComment_NoSuchPaper() throws Exception {
        submitConfidentialCommentWithException(new NotFoundException(""), 404);
    }

    @Test
    void submitConfidentialComment_IllegalAccess() throws Exception {
        submitConfidentialCommentWithException(new IllegalAccessException(""), 403);
    }

    @Test
    void submitConfidentialComment_InternalError() throws Exception {
        submitConfidentialCommentWithException(new RuntimeException(""), 500);
    }

    @Test
    public void getDiscussionCommentsSuccessfully() throws Exception {

        List<DiscussionComment> comments = new ArrayList<>();
        comments.add(new DiscussionComment(2L, "comment"));
        comments.add(new DiscussionComment(3L, "comment"));

        when(discussionService.getDiscussionComments(requesterID, reviewerID, paperID)).thenReturn(comments);

        String expectedJSON = objectMapper.writeValueAsString(comments);

        mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/papers/{paperID}/reviews/by-reviewer/{reviewerID}/discussion-comments", paperID, reviewerID)
                        .param("requesterID", requesterID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andExpect(MockMvcResultMatchers.content().json(expectedJSON));

        verify(discussionService).getDiscussionComments(requesterID, reviewerID, paperID);
    }

    /**
     * Simulates an exception inside getDiscussionComments function
     * and checks for correct status code.
     *
     * @param exception the exception to be thrown
     * @param expected the expected status code
     * @throws Exception method can throw exception
     */
    public void getDiscussionCommentsWithException(Exception exception, int expected) throws Exception {

        doThrow(exception).when(discussionService).getDiscussionComments(requesterID, reviewerID, paperID);

        mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/papers/{paperID}/reviews/by-reviewer/{reviewerID}/discussion-comments", paperID, reviewerID)
                        .param("requesterID", requesterID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().is(expected));

        verify(discussionService).getDiscussionComments(requesterID, reviewerID, paperID);
    }

    @Test
    void getDiscussionComments_NoSuchPaper() throws Exception {
        getDiscussionCommentsWithException(new NotFoundException(""), 404);
    }

    @Test
    void getDiscussionComments_IllegalAccess() throws Exception {
        getDiscussionCommentsWithException(new IllegalAccessException(""), 403);
    }

    @Test
    void getDiscussionComments_InternalError() throws Exception {
        getDiscussionCommentsWithException(new RuntimeException(""), 500);
    }
}
