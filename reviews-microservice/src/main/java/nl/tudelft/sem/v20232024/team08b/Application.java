package nl.tudelft.sem.v20232024.team08b;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Reviews microservice, which manages the peer-review
 * lifecycle of conference papers: bidding, reviewer assignment, review
 * submission, discussion and the final acceptance decision.
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
