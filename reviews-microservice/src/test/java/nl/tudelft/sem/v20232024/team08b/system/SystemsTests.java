package nl.tudelft.sem.v20232024.team08b.system;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.tudelft.sem.v20232024.team08b.domain.Bid;
import nl.tudelft.sem.v20232024.team08b.domain.RecommendationScore;
import nl.tudelft.sem.v20232024.team08b.domain.Review;
import nl.tudelft.sem.v20232024.team08b.domain.ReviewID;
import nl.tudelft.sem.v20232024.team08b.dtos.review.*;
import nl.tudelft.sem.v20232024.team08b.dtos.submissions.Submission;
import nl.tudelft.sem.v20232024.team08b.dtos.users.Event;
import nl.tudelft.sem.v20232024.team08b.dtos.users.Track;
import nl.tudelft.sem.v20232024.team08b.dtos.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.TestAbortedException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class SystemsTests {
    private final TestRestTemplate testRestTemplate = new TestRestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final String usersURL = "http://localhost:8082";
    private final String reviewsURL = "http://localhost:8080";

    private Long submitter1ID;

    private Long reviewer1ID;

    private Long track1ID;
    private Long event1ID;
    private Long chair1ID;

    private Long submission1ID;
    private Long submission2ID;
    private Long submission3ID;

    @BeforeEach
    void setup() throws InterruptedException {
        // Verify that the other microservices are running
        String submissionsURL = "http://localhost:8081";
        try {
            sendRequest(RequestType.DELETE, null, Object.class, usersURL, "debug");
            System.out.println("[Systems Testing Log] Users microservice is running.");
            sendRequest(RequestType.GET, null, Object.class, submissionsURL, "submission",
                    "1000000");
            System.out.println("[Systems Testing Log] Submissions microservice is running.");
        } catch (Exception e) {
            if (e.getCause() != null && e.getCause().toString().contains("java.net" +
                    ".ConnectException")) {
                throw new TestAbortedException();
            }
        }

        // Make some test data
        Random rng = new Random();
        var user = new User();
        user.name("John");
        user.surname("Doe");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelft.nl");
        var submitter = (User) sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        submitter1ID = submitter.getId();

        user.name("John");
        user.surname("Doe");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        final Long submitter2ID = submitter.getId();

        user.name("John");
        user.surname("Doe");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        Long submitter3ID = submitter.getId();

        System.out.println(submitter1ID + " " + submitter2ID + " " + submitter3ID);

        user.name("John");
        user.surname("Doe");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        reviewer1ID = submitter.getId();

        System.out.println(reviewer1ID);

        user.name("John");
        user.surname("Doe");
        user.website("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        final var chair1 = submitter;

        var event1 = new Event();
        event1.name("TestEvent1");
        event1.generalChairs(List.of(chair1));
        event1.description("This is a test event.");
        event1 = sendRequest(RequestType.POST, event1, Event.class, usersURL, "event");
        System.out.println(event1);

        var track1 = new Track();
        track1.name("TestTrack1");
        track1.maxLength(10000);
        track1.description("This is a test track.");
        track1.setDeadline(System.currentTimeMillis() + 1000);
        track1 = sendRequest(RequestType.POST, track1, Track.class, usersURL, "track",
                event1.getId().toString());
        System.out.println(track1);

        // make chair1 pc chair of track1
        sendRequest(RequestType.PUT, null, null, usersURL, "event", event1.getId().toString(),
                track1.getId().toString(), "role",
                chair1.getId().toString() + "?Assignee=" + chair1.getId().toString()
                        + "&roleType=PCchair");

        sendRequest(RequestType.PUT, null, null, usersURL, "event", event1.getId().toString(),
                track1.getId().toString(), "role",
                chair1.getId().toString() + "?Assignee=" + chair1.getId().toString()
                        + "&roleType=PCchair");

        var track2 = new Track();
        track2.setId((long) rng.nextInt());
        track2.name("Test Track 2");
        track2.maxLength(10000);
        track2.description("This is a test track.");
        track2.setDeadline(System.currentTimeMillis() + 1000);
        track2 = sendRequest(RequestType.POST, track2, Track.class, usersURL, "track",
                event1.getId().toString());
        System.out.println(track2);

        // make chair1 pc chair of track2

        sendRequest(RequestType.PUT, null, null, usersURL, "event", event1.getId().toString(),
                track2.getId().toString(), "role",
                chair1.getId().toString() + "?Assignee=" + chair1.getId().toString()
                        + "&roleType=PCchair");

        sendRequest(RequestType.PUT, null, null, usersURL, "event", event1.getId().toString(),
                track1.getId().toString(), "role",
                submitter1ID + "?Assignee=" + chair1.getId().toString()
                        + "&roleType=Author");

        Submission fakeSubmission = new Submission();
        fakeSubmission.setTitle("Title 1");
        fakeSubmission.setKeywords(List.of("Keywords"));
        fakeSubmission.setAbstract("Abstract 1");
        fakeSubmission.setPaper("Content".getBytes());
        fakeSubmission.setEventId(event1.getId());
        fakeSubmission.setTrackId(track1.getId());
        fakeSubmission.setLinkToReplicationPackage("https://github.com");
        var submitter1 = new nl.tudelft.sem.v20232024.team08b.dtos.submissions.User();
        User submitter1User = sendRequest(RequestType.GET, null, User.class, usersURL, "user",
                submitter1ID.toString());
        submitter1.setUserId(submitter1ID);
        submitter1.setEmail(submitter1User.getEmail());
        submitter1.setName(submitter1User.getName());
        submitter1.setSurname(submitter1.getSurname());

        var reviewer1 = new nl.tudelft.sem.v20232024.team08b.dtos.submissions.User();
        User reviewer1User = sendRequest(RequestType.GET, null, User.class, usersURL, "user",
            reviewer1ID.toString());
        reviewer1.setUserId(reviewer1User.getId());
        reviewer1.setEmail(reviewer1User.getEmail());
        reviewer1.setName(reviewer1User.getName());
        reviewer1.setSurname(reviewer1User.getSurname());

        fakeSubmission.setAuthors(List.of(submitter1));
        fakeSubmission.setConflictsOfInterest(List.of());
        fakeSubmission = sendRequest(RequestType.POST, fakeSubmission, Submission.class,
                submissionsURL, "submission", submitter1ID.toString());
        submission1ID = fakeSubmission.getSubmissionId();
        System.out.println(fakeSubmission);

        sendRequest(RequestType.PUT, null, null, usersURL, "event", event1.getId().toString(),
                track1.getId().toString(), "role",
                submitter2ID + "?Assignee=" + chair1.getId().toString()
                        + "&roleType=Author");

        fakeSubmission = new Submission();
        fakeSubmission.setTitle("Title 2");
        fakeSubmission.setKeywords(List.of("Keywords"));
        fakeSubmission.setAbstract("Abstract 2");
        fakeSubmission.setPaper("Content".getBytes());
        fakeSubmission.setEventId(event1.getId());
        fakeSubmission.setTrackId(track1.getId());
        fakeSubmission.setLinkToReplicationPackage("https://github.com");
        var submitter2 = new nl.tudelft.sem.v20232024.team08b.dtos.submissions.User();
        User submitter2User = sendRequest(RequestType.GET, null, User.class, usersURL, "user",
                submitter1ID.toString());
        submitter2.setUserId(submitter1ID);
        submitter2.setEmail(submitter2User.getEmail());
        submitter2.setName(submitter2User.getName());
        submitter2.setSurname(submitter2.getSurname());
        fakeSubmission.setAuthors(List.of(submitter2));
        fakeSubmission.setConflictsOfInterest(List.of());
        fakeSubmission = sendRequest(RequestType.POST, fakeSubmission, Submission.class,
                submissionsURL, "submission", submitter1ID.toString());
        submission2ID = fakeSubmission.getSubmissionId();
        System.out.println(fakeSubmission);

        sendRequest(RequestType.PUT, null, null, usersURL, "event", event1.getId().toString(),
                track2.getId().toString(), "role",
                submitter3ID + "?Assignee=" + chair1.getId().toString()
                        + "&roleType=Author");

        fakeSubmission = new Submission();
        fakeSubmission.setTitle("Title 3");
        fakeSubmission.setKeywords(List.of("Keywords"));
        fakeSubmission.setAbstract("Abstract 3");
        fakeSubmission.setPaper("Content".getBytes());
        fakeSubmission.setEventId(event1.getId());
        fakeSubmission.setTrackId(track2.getId());
        fakeSubmission.setLinkToReplicationPackage("https://github.com");
        var submitter3 = new nl.tudelft.sem.v20232024.team08b.dtos.submissions.User();
        User submitter3User = sendRequest(RequestType.GET, null, User.class, usersURL, "user",
                submitter1ID.toString());
        submitter3.setUserId(submitter1ID);
        submitter3.setEmail(submitter3User.getEmail());
        submitter3.setName(submitter3User.getName());
        submitter3.setSurname(submitter3.getSurname());
        fakeSubmission.setAuthors(List.of(submitter3));
        fakeSubmission.setConflictsOfInterest(List.of(reviewer1));
        fakeSubmission = sendRequest(RequestType.POST, fakeSubmission, Submission.class,
                submissionsURL, "submission", submitter3ID.toString());
        submission3ID = fakeSubmission.getSubmissionId();
        System.out.println(fakeSubmission);

        track1ID = track1.getId();
        event1ID = event1.getId();
        chair1ID = chair1.getId();

        sendRequest(RequestType.PUT, null, null, usersURL, "event", event1.getId().toString(),
                track1.getId().toString(), "role",
                reviewer1ID + "?Assignee=" + chair1.getId().toString()
                        + "&roleType=PCmember");

        Thread.sleep(1000);
    }


    /**
     * Tests Must-have Requirement #3: Reviewers can see all of the papers in their tracks.
     * Using endpoint: GET /conferences/{conferenceID}/tracks/{trackID}/papers
     */
    @Test
    void reviewersCanSeeSubmittedPapersInATrack() {
        var paper1 = new PaperSummaryWithID();
        paper1.setTitle("Title 1");
        paper1.setAbstractSection("Abstract 1");
        paper1.setPaperID(submission1ID);
        var paper2 = new PaperSummaryWithID();
        paper2.setTitle("Title 2");
        paper2.setAbstractSection("Abstract 2");
        paper2.setPaperID(submission2ID);
        var papersSummaryWithIDS = new ArrayList<PaperSummaryWithID>();
        papersSummaryWithIDS.add(paper1);
        papersSummaryWithIDS.add(paper2);
        var response = testRestTemplate.getForEntity(reviewsURL + "/conferences/" + event1ID +
                "/tracks/" + track1ID + "/papers?requesterID=" + reviewer1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(papersSummaryWithIDS, response.getBody());
    }

    /**
     * Tests Must-have Requirement #2: Reviewers can read the titles and abstracts of the submitted papers.
     * Using endpoint: GET /papers/{paperID}/title-and-abstract
     */
    @Test
    void reviewersCanSeeTitlesAndAbstractsOfPapers() {
        ResponseEntity<PaperSummary> response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
                "/title-and-abstract?requesterID=" + reviewer1ID, PaperSummary.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        PaperSummary paperSummary = response.getBody();
        assert paperSummary != null;
        assertEquals("Title 1", paperSummary.getTitle());
        assertEquals("Abstract 1", paperSummary.getAbstractSection());
    }

    /**
     * Tests Could-have Requirement #2: Chairs can read the titles and abstracts of the submitted
     * papers, but not the authors of the papers.
     * Using endpoint: GET /papers/{paperID}/title-and-abstract
     */
    @Test
    void chairsCanReadTitlesAndAbstractsOfPapersInTheirTrackInBiddingPhase() {
        var paperSummaryWithoutID = new PaperSummary();
        paperSummaryWithoutID.setTitle("Title 1");
        paperSummaryWithoutID.setAbstractSection("Abstract 1");


        var response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
            "/title-and-abstract?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(paperSummaryWithoutID, response.getBody());
    }

    /**
     * Tests Should-have Requirement #5: Reviewers can read the papers that they are assigned to.
     * Must-have Requirement #4: Reviewers should not see the author of a paper.
     * Using endpoint: GET /papers/{paperID}
     */
    @Test
    void reviewersCanReadPapersTheyAreAssignedTo() {
        PaperSummaryWithID paper1 = new PaperSummaryWithID();
        paper1.setTitle("Title 1");
        paper1.setAbstractSection("Abstract 1");
        paper1.setPaperID(submission1ID);

        ResponseEntity<Object> response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
                "?requesterID=" + reviewer1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(paper1, response.getBody());
    }

    /**
     * Test Could-have Requirement #5: Chairs can read the papers that are a part of their track(s).
     * Using endpoint: GET /papers/{paperID}
     */
    @Test
    void chairsCanReadPapersInTheirTrackInAssignmentPhase() {
        var paper1 = new PaperSummaryWithID();
        paper1.setTitle("Title 1");
        paper1.setAbstractSection("Abstract 1");
        paper1.setPaperID(submission1ID);

        var response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
            "?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(paper1, response.getBody());

    }

    /**
     * Tests Must-have Requirement #5: Reviewers can decide to review/not review/stay neutral towards a paper.
     * Must-have Requirement #1: The bidding phase automatically starts when the submission deadline passes.
     * Must-have Requirement #6: The bidding phase must end after a certain deadline (default is a few days).
     * Using endpoints: PUT /papers/{paperID}/bid
     *                  GET /papers/{paperID}/bids/by-reviewer/{reviewerID}
     */
    @Test
    void reviewersCanBidOnPapers() {
        Bid bid = new Bid(submission1ID, reviewer1ID, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.CAN_REVIEW);
        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID +
            "/bid?requesterID=" + reviewer1ID, nl.tudelft.sem.v20232024.team08b.dtos.review.Bid.CAN_REVIEW);
        var response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
            "/by-reviewer/" + reviewer1ID + "?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(bid, response.getBody());
    }

    /**
     * Tests Must-have Requirement #7: Chairs can initiate an automated process that assigns papers
     * based on the bidding information for papers in their track(s).
     * Must-have Requirement #8: Chairs can view the current assignments of the reviews of papers in their track(s).
     * Must-have Requirement #9: Each submitted paper must be reviewed by three reviewers.
     * Could-have Requirement #3: Reviewers should be assigned to similar amounts of papers
     * by the automatic assignment.
     * Using endpoints: PUT /conferences/{conferenceID}/tracks/{trackID}/automatic
     * GET /papers/{paperID}/assignees
     */
    @Test
    void chairsCanAssignPapersAutomatically() {
        List<Long> userIDs = List.of(1L, 2L, 3L);

        testRestTemplate.put(reviewsURL + "/conferences/" + event1ID + "/tracks/" + track1ID + "/automatic" +
                "?requesterID=" + reviewer1ID, userIDs);
        var response2 = testRestTemplate.getForEntity(reviewsURL + "/papers" + submission1ID +
                "/assignees?requesterID=" + reviewer1ID, List.class);
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(userIDs, response2.getBody());
    }

    /**
     * Tests Must-have Requirement #10: Chairs can finalize the assignments, so they are no longer editable.
     * Must-have Requirement #9: Each submitted paper must be reviewed by three reviewers.
     * Using endpoints: POST /conferences/{conferenceID}/tracks/{trackID}/finalization
     * GET /conferences/{conferenceID}/tracks/{trackID}/phase
     */
    @Test
    void chairsCanFinalizeAssignments() {
        var response1 = testRestTemplate.postForEntity(reviewsURL + "/conferences/" + event1ID +
            "/tracks/" + track1ID + "finalization?requesterID=" + chair1ID, null, Object.class);
        assertEquals(HttpStatus.CREATED, response1.getStatusCode());
        var response2 = testRestTemplate.getForEntity(reviewsURL + "/conferences/" + event1ID +
            "/tracks/" + track1ID + "phase?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(TrackPhase.REVIEWING, response2.getBody());

    }

    /**
     * Tests Must-have Requirement #11: Reviewers can submit their review for each submission they are assigned to.
     * Must-have Requirement #12: Reviewers can resubmit their reviews
     * as long as the Review phase of the track is still ongoing.
     * Using endpoints: PUT /papers/{paperID}/reviews
     * GET /papers/{paperID}/reviews/by-reviewer/{reviewerID}
     */
    @Test
    void reviewersCanSubmitAndResubmitReviews() {
        // first time submitting a review
        Review review = new Review(new ReviewID(submission1ID, reviewer1ID), null, "Comment version 1", null, null, null);
        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID + "/reviews?requesterID=" + reviewer1ID, review);
        var response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID
                + "/reviews/by-reviewer/" + reviewer1ID + "?requesterID=" + reviewer1ID, Review.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review, response.getBody());
        assertEquals("Comment version 1", response.getBody().getCommentForAuthor());
        // updating a review
        review = new Review(new ReviewID(submission1ID, reviewer1ID), null, "Comment version 2", null, null, null);
        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID + "/reviews?requesterID=" + reviewer1ID, review);
        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID + "/reviews/by-reviewer/"
                + reviewer1ID + "?requesterID=" + reviewer1ID, Review.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review, response.getBody());
        assertEquals("Comment version 2", response.getBody().getCommentForAuthor());
    }

    /**
     * Tests Must-have Requirement #14: Once all reviews for a paper are submitted, the Review phase
     * for this paper automatically ends, and the Discussion phase for the paper begins.
     * Using endpoint: PUT /papers/{paperID}/reviews
     * GET /papers/{paperID}/reviews/phase
     */
    @Test
    void discussionPhaseBeginsSuccessfully() {
        Random rng = new Random();
        var user = new User();
        user.name("A");
        user.surname("A");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        User submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        final var reviewer2ID = submitter.getId();

        user.name("B");
        user.surname("B");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        var reviewer3ID = submitter.getId();

        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
            "/assignees/" + reviewer1ID + "?requesterID=" + chair1ID, null, Object.class);
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
            "/assignees/" + reviewer2ID + "?requesterID=" + chair1ID, null, Object.class);
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
            "/assignees/" + reviewer3ID + "?requesterID=" + chair1ID, null, Object.class);

        Review review1 = new Review(
            new ReviewID(submission1ID, reviewer1ID), null, null,
            null, null, null);
        Review review2 = new Review(
            new ReviewID(submission1ID, reviewer2ID), null, null,
            null, null, null);
        Review review3 = new Review(
            new ReviewID(submission1ID, reviewer3ID), null, null,
            null, null, null);

        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID +
            "/reviews?requesterID=" + reviewer1ID, review1);
        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID +
            "/reviews?requesterID=" + reviewer2ID, review2);
        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID +
            "/reviews?requesterID=" + reviewer3ID, review3);

        var response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
            "/reviews/phase?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(PaperPhase.IN_DISCUSSION, response.getBody());

    }

    /**
     * Tests Must-have Requirement #15: Reviewers can read the reviews of other reviewers
     * if they are assigned to the same paper.
     * Using endpoint: GET /papers/{paperID}/reviews/by-reviewer/{reviewerID}
     * PUT /papers/{paperID}/reviews
     * GET /papers/{paperID}/reviews/phase
     */
    @Test
    void reviewersCanReadOtherReviewsDuringDiscussionPhase() {
        discussionPhaseBeginsSuccessfully();
        Random rng = new Random();
        var user = new User();
        user.name("John");
        user.surname("Doe");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        User submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        var reviewer2ID = submitter.getId();

        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
                "/assignees/" + reviewer1ID + "?requesterID=" + chair1ID, null, Object.class);
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
                "/assignees/" + reviewer2ID + "?requesterID=" + chair1ID, null, Object.class);

        Review review1 = new Review(
                new ReviewID(submission1ID, reviewer1ID), null, null,
                null, null, null);
        Review review2 = new Review(
                new ReviewID(submission1ID, reviewer2ID), null, null,
                null, null, null);

        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID +
                "/reviews?requesterID=" + reviewer1ID, review1);
        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID +
                "/reviews?requesterID=" + reviewer2ID, review2);

        ResponseEntity<Review> response = testRestTemplate.getForEntity(reviewsURL
                + "/papers/" + submission1ID + "/reviews/by-reviewer/" +
                reviewer2ID + "?requesterID=" + reviewer1ID, Review.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review2, response.getBody());
    }

    /**
     * Tests Must-have Requirement #16: Chairs can read the reviews of the papers in their track(s).
     * Using endpoint: GET /papers/{paperID}/reviewers
     * GET /papers/{paperID}/reviews/by-reviewer/{reviewerID}
     */
    @Test
    void chairsCanReadReviewsDuringDiscussionPhase() {
        discussionPhaseBeginsSuccessfully();
        Random rng = new Random();
        var user = new User();
        user.name("A");
        user.surname("A");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        User submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        final var reviewer2ID = submitter.getId();

        user.name("B");
        user.surname("B");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        var reviewer3ID = submitter.getId();

        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
            "/assignees/" + reviewer1ID + "?requesterID=" + chair1ID, null, Object.class);
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
            "/assignees/" + reviewer2ID + "?requesterID=" + chair1ID, null, Object.class);
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
            "/assignees/" + reviewer3ID + "?requesterID=" + chair1ID, null, Object.class);

        final Review review1 = new Review(
            new ReviewID(submission1ID, reviewer1ID), null, null,
            null, null, null);
        final Review review2 = new Review(
            new ReviewID(submission1ID, reviewer2ID), null, null,
            null, null, null);
        final Review review3 = new Review(
            new ReviewID(submission1ID, reviewer3ID), null, null,
            null, null, null);


        var reviewers = List.of(reviewer1ID, reviewer2ID, reviewer3ID);
        var response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
            "/reviewers?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(reviewers, response.getBody());

        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
                "reviews/by-reviewer/" + reviewer1ID + "?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review1, response.getBody());

        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
                "reviews/by-reviewer/" + reviewer2ID + "?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review2, response.getBody());

        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
                "reviews/by-reviewer/" + reviewer3ID + "?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review3, response.getBody());


    }

    /**
     * Tests Must-have Requirement #17: Reviewers can edit their own reviews, by submitting them again.
     * Using endpoint: PUT /papers/{paperID}/reviews
     */
    @Test
    void reviewersCanEditTheirOwnReviewsDuringDiscussionPhase() {
        // first time submitting a review
        Review review = new Review(new ReviewID(submission1ID, reviewer1ID), null, "Comment version 1", null, null, null);
        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID + "/reviews?requesterID=" + reviewer1ID, review);
        var response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID
                + "/reviews/by-reviewer/" + reviewer1ID + "?requesterID=" + reviewer1ID, Review.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review, response.getBody());
        assertEquals("Comment version 1", response.getBody().getCommentForAuthor());
        discussionPhaseBeginsSuccessfully();
        // editing a review
        review = new Review(new ReviewID(submission1ID, reviewer1ID), null, "Comment version 2", null, null, null);
        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID + "/reviews?requesterID=" + reviewer1ID, review);
        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID + "/reviews/by-reviewer/"
                + reviewer1ID + "?requesterID=" + reviewer1ID, Review.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review, response.getBody());
        assertEquals("Comment version 2", response.getBody().getCommentForAuthor());
    }

    /**
     * Tests Must-have Requirement #18: Once a Chair has approved the reviews for a paper,
     * the Discussion phase for this paper ends and the Final phase begins.
     * Must-have Requirement #19: Chairs can approve (finalize) the reviews for a paper
     * in their track(s) and only if the reviews for the paper are all positive or all negative.
     * Using endpoints: POST /papers/{paperID}/reviews/finalization
     * GET /papers/{paperID}/reviews/phase
     */
    @Test
    void chairsCanFinalizeReviewsDuringDiscussionPhase() {
        discussionPhaseBeginsSuccessfully();
        //write a bunch of reviews
        Random rng = new Random();
        var user = new User();
        user.name("John");
        user.surname("Doe");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        User submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        final var reviewer2ID = submitter.getId();

        user.name("John");
        user.surname("Doe");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        final var reviewer3ID = submitter.getId();

        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
                "/assignees/" + reviewer1ID + "?requesterID=" + chair1ID, null, Object.class);
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
                "/assignees/" + reviewer2ID + "?requesterID=" + chair1ID, null, Object.class);
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
                "/assignees/" + reviewer3ID + "?requesterID=" + chair1ID, null, Object.class);

        Review review1 = new Review(
                new ReviewID(submission1ID, reviewer1ID), null, null,
                RecommendationScore.STRONG_ACCEPT, null, null);
        Review review2 = new Review(
                new ReviewID(submission1ID, reviewer2ID), null, null,
                RecommendationScore.STRONG_ACCEPT, null, null);
        Review review3 = new Review(
                new ReviewID(submission1ID, reviewer3ID), null, null,
                RecommendationScore.STRONG_ACCEPT, null, null);

        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID +
                "/reviews?requesterID=" + reviewer1ID, review1);
        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID +
                "/reviews?requesterID=" + reviewer2ID, review2);
        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID +
                "/reviews?requesterID=" + reviewer3ID, review3);
        //finalize them
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID + "/reviews/finalization?requesterID="
                + chair1ID, null, Object.class);
        //check if it has been finalized
        var response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID
                + "/reviews/phase?requesterID=" + chair1ID, PaperPhase.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(PaperPhase.REVIEWED, response.getBody());
    }

    /**
     * Tests Must-have Requirement #21: Chairs and Reviewers can see the finalized reviews
     * for the appropriate papers.
     * Could-have Requirement #8: Chairs view the finalized reviews of all papers in their track(s).
     * Using endpoint: GET /papers/{paperID}/reviews
     */
    @Test
    void chairsAndReviewersCanSeeFinalizedReviews() {
        chairsCanFinalizeReviewsDuringDiscussionPhase();
        discussionPhaseBeginsSuccessfully();
        Random rng = new Random();
        var user = new User();
        user.name("A");
        user.surname("A");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        User submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        final var reviewer2ID = submitter.getId();

        user.name("B");
        user.surname("B");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        var reviewer3ID = submitter.getId();

        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
            "/assignees/" + reviewer1ID + "?requesterID=" + chair1ID, null, Object.class);
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
            "/assignees/" + reviewer2ID + "?requesterID=" + chair1ID, null, Object.class);
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
            "/assignees/" + reviewer3ID + "?requesterID=" + chair1ID, null, Object.class);

        final Review review1 = new Review(
            new ReviewID(submission1ID, reviewer1ID), null, null,
            null, null, null);
        final Review review2 = new Review(
            new ReviewID(submission1ID, reviewer2ID), null, null,
            null, null, null);
        final Review review3 = new Review(
            new ReviewID(submission1ID, reviewer3ID), null, null,
            null, null, null);


        var reviewers = List.of(reviewer1ID, reviewer2ID, reviewer3ID);
        var response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
            "/reviewers?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(reviewers, response.getBody());

        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
                "reviews/by-reviewer/" + reviewer1ID + "?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review1, response.getBody());

        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
                "reviews/by-reviewer/" + reviewer2ID + "?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review2, response.getBody());

        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
                "reviews/by-reviewer/" + reviewer3ID + "?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review3, response.getBody());


        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
                "reviews/by-reviewer/" + reviewer1ID + "?requesterID=" + reviewer1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review1, response.getBody());

        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
                "reviews/by-reviewer/" + reviewer2ID + "?requesterID=" + reviewer2ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review2, response.getBody());

        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
                "reviews/by-reviewer/" + reviewer3ID + "?requesterID=" + reviewer3ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(review3, response.getBody());
    }

    /**
     * Tests Must-have Requirement #22: Authors can check if their paper was accepted.
     * Must-have Requirement #20: If all scores for a paper are positive, the paper is accepted.
     * Using endpoint: GET /papers/{paperID}/status
     */
    @Test
    void authorsCanCheckTheStatusOfTheirPaper() {
        chairsCanFinalizeReviewsDuringDiscussionPhase();
        //write a bunch of reviews
        Random rng = new Random();
        var user = new User();
        user.name("John");
        user.surname("Doe");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        User submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        final var reviewer2ID = submitter.getId();

        user.name("John");
        user.surname("Doe");
        user.setWebsite("www.tudelft.nl");
        user.email(rng.nextInt() + "@tudelt.nl");
        submitter = sendRequest(RequestType.POST, user, User.class, usersURL, "user");
        var reviewer3ID = submitter.getId();

        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
                "/assignees/" + reviewer1ID + "?requesterID=" + chair1ID, null, Object.class);
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
                "/assignees/" + reviewer2ID + "?requesterID=" + chair1ID, null, Object.class);
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
                "/assignees/" + reviewer3ID + "?requesterID=" + chair1ID, null, Object.class);

        Review review1 = new Review(
                new ReviewID(submission1ID, reviewer1ID), null, null,
                RecommendationScore.STRONG_ACCEPT, null, null);
        Review review2 = new Review(
                new ReviewID(submission1ID, reviewer2ID), null, null,
                RecommendationScore.STRONG_ACCEPT, null, null);
        Review review3 = new Review(
                new ReviewID(submission1ID, reviewer3ID), null, null,
                RecommendationScore.STRONG_ACCEPT, null, null);

        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID +
                "/reviews?requesterID=" + reviewer1ID, review1);
        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID +
                "/reviews?requesterID=" + reviewer2ID, review2);
        testRestTemplate.put(reviewsURL + "/papers/" + submission1ID +
                "/reviews?requesterID=" + reviewer3ID, review3);
        //finalize them
        testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID + "/reviews/finalization?requesterID="
                + chair1ID, null, Object.class);
        //get paper status and check if author could read it
        ResponseEntity<PaperStatus> response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID
                + "status?requesterID=" + submitter1ID, PaperStatus.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * Tests Should-have Requirement #1: Reviewers cannot be assigned papers, for review,
     * if conflicts of interest (COIs) have been indicated.
     * Using endpoint: PUT /papers/{paperID}/bid
     */
    @Test
    void reviewersCannotBeAssignedToPapersIfCOI() {
        assertThrows(RestClientException.class, () ->
            testRestTemplate.put(reviewsURL + "/papers/" + submission3ID + "/bid?requesterID=" + chair1ID,
                    List.class));
    }

    /**
     * Tests Should-have Requirement #2: Chairs can manually assign papers in their track(s)
     * to each reviewer based on the bidding information.
     * Should-have Requirement #3: Chairs can edit any of the assignments before the finalization.
     * Should-have Requirement #4: Chairs can delete any of the assignments before the finalization.
     * Using endpoints: POST /papers/{paperID}/assignees/{reviewerID}
     * DELETE /papers/{paperID}/assignees/{reviewerID}
     * GET /papers/{paperID}/assignees
     */
    @Test
    void chairsCanManuallyAssignPapers() {
        final var ids = List.of(reviewer1ID);
        var response = testRestTemplate.postForEntity(reviewsURL + "/papers/" + submission1ID +
            "/assignees/" + reviewer1ID + "?requesterID=" + chair1ID, null, Object.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
            "assignees?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ids, response.getBody());
        testRestTemplate.delete(reviewsURL + "/papers/" + submission1ID +
            "/assignees/" + reviewer1ID + "?requesterID=" + chair1ID, Object.class);
        response = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
            "assignees?requesterID=" + chair1ID, Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotEquals(ids, response.getBody());

    }

    /**
     * Tests Should-have Requirement #6: Reviewers can write discussion comments on each other's reviews
     * Could-have Requirement #6: Chairs can also make discussion comments on the reviews.
     * Using endpoints: GET /papers/{paperID}/reviews/by-reviewer/{reviewerID}/discussion-comments
     * POST /papers/{paperID}/reviews/by-reviewer/{reviewerID}/discussion-comments
     */
    @Test
    void reviewersAndChairsCanWriteDiscussionCommentsDuringDiscussionPhase() {
        discussionPhaseBeginsSuccessfully();
        DiscussionComment comment = new DiscussionComment(reviewer1ID, "Comment From Reviewer1");
        testRestTemplate.postForEntity("/papers/" + submission1ID + "/reviews/by-reviewer/"
                + reviewer1ID + "/discussion-comments?requesterID=" + reviewer1ID, comment, DiscussionComment.class);
        ResponseEntity<DiscussionComment> response = testRestTemplate.getForEntity("/papers/"
                + submission1ID + "/reviews/by-reviewer/" + reviewer1ID + "/discussion-comments?requesterID="
                + reviewer1ID, DiscussionComment.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(comment, response.getBody());

        comment = new DiscussionComment(chair1ID, "Comment From Chair1");
        testRestTemplate.postForEntity("/papers/" + submission1ID + "/reviews/by-reviewer/"
                + chair1ID + "/discussion-comments?requesterID=" + chair1ID, comment, DiscussionComment.class);
        response = testRestTemplate.getForEntity("/papers/" + submission1ID + "/reviews/by-reviewer/"
                + chair1ID + "/discussion-comments?requesterID=" + chair1ID, DiscussionComment.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(comment, response.getBody());
    }

    /**
     * Tests Should-have Requirement #7: Authors can read the reviews for their papers
     * Must-have Requirement #13: Authors should never be shown the confidential comments,
     * review changes, and discussion comments under any circumstance.
     * Using endpoints: GET /papers/{paperID}/reviewers
     * GET /papers/{paperID}/reviews/by-reviewer/{reviewerID}
     */
    @Test
    void authorsCanReadReviews() {
        chairsCanFinalizeReviewsDuringDiscussionPhase();
    }

    /**
     * Tests Could-have Requirement #1: Before the bidding has started, Chairs can decide on the deadline
     * for the bidding phase for their track(s), i.e., the deadline for bidding for reviews
     * after the submission of papers.
     * Using endpoints: PUT /conferences/{conferenceID}/tracks/{trackID}/bidding-deadline
     * GET /conferences/{conferenceID}/tracks/{trackID}/bidding-deadline
     */
    @Test
    void chairsCanSetBiddingDeadline() {
        long deadline = System.currentTimeMillis() + 1000;
        testRestTemplate.put(reviewsURL + "/conferences/" + event1ID + "/tracks/" + track1ID +
                "/bidding-deadline?requesterID=" + chair1ID, deadline);
        var response = testRestTemplate.getForEntity(reviewsURL + "/conferences/" + event1ID
                        + "/tracks/" + track1ID + "/bidding-deadline?requesterID=" + chair1ID,
                Long.class).getBody();
        assertEquals(deadline, response);
    }

    /**
     * Tests Could-have Requirement #4: Chairs can view analytics for their track(s):
     * the number of papers that will be accepted, rejected, and papers with no final verdict.
     * Using endpoint: GET /conferences/{conferenceID}/tracks/{trackID}/analytics
     */
    @Test
    void chairsCanViewAnalytics() {
        TrackAnalytics analytics = testRestTemplate.getForEntity(reviewsURL + "/conferences/" + event1ID +
                "/tracks/" + track1ID + "/analytics?requesterID=" + chair1ID, TrackAnalytics.class).getBody();
        assertEquals(0, analytics.getAccepted());
        assertEquals(0, analytics.getRejected());
        assertEquals(2, analytics.getUnknown());
    }

    /**
     * Tests Could-have Requirement #7: Reviewers can check the final decision
     * of the papers they were assigned to.
     * Using endpoint: GET /papers/{paperID}/status
     */
    @Test
    void reviewersCanCheckStatusOfPaper() {
        var status = testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID +
                "/status?requesterID=" + reviewer1ID, PaperStatus.class).getBody();
        assertEquals(PaperStatus.NOT_DECIDED, status);
    }

    /**
     * Tests Requirements:
     * Must-have #23: We verify that the users are properly authenticated and have the correct
     * privileges for what they are trying to do.
     * Using endpoints:
     * GET /papers/{paperID}/bids
     */
    @Test
    void verification() {
        assertThrows(RestClientException.class, () ->
            testRestTemplate.getForEntity(reviewsURL + "/papers/" + submission1ID + "/bids?requesterID=" + submitter1ID,
                    List.class));
    }

    /**
     * Sends an http request.
     *
     * @param requestType           GET, POST, PUT, or DELETE
     * @param body                  The body of the request
     * @param expectedResponseClass The class of the expected response
     * @param url                   The url of the request
     * @param <T>                   The type of the request body
     * @param <U>                   The type of the response body
     * @return The response body converted to the expected class
     */
    <T, U> U sendRequest(RequestType requestType, T body, Class<T> expectedResponseClass,
                         String... url) {
        HttpRequest request;
        HttpResponse<String> response;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(String.join("/", url)))
                    .header("Content-Type", "application/json")
                    .method(requestType.toString(), body == null ?
                            HttpRequest.BodyPublishers.noBody() :
                            HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))
                    ).build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            System.out.println("[Systems Test Error] Failed to send request.");
            throw new RuntimeException(e);
        }

        switch (HttpStatus.valueOf(response.statusCode())) {
            case OK, CREATED -> {
                try {
                    return expectedResponseClass == null ? null :
                            (U) objectMapper.readValue(response.body(), expectedResponseClass);
                } catch (JsonProcessingException e) {
                    System.out.println("[Systems Test Error] Failed to parse response body.");
                    throw new RuntimeException(e);
                }
            }
            default -> {
                System.out.println("[Systems Testing Error] Request was: " + request.toString());
                throw new RuntimeException("Unexpected response status: " + response.statusCode());
            }
        }
    }

    /**
     * Enum for the type of request.
     */
    enum RequestType {
        GET, POST, PUT, DELETE
    }
}
