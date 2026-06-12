package nl.tudelft.sem.v20232024.team08b.exceptions;

/**
 * Thrown when a requested resource (paper, review, track, user, bid, ...)
 * does not exist.
 */
public class NotFoundException extends Exception {
    public NotFoundException(String message) {
        super(message);
    }
}
