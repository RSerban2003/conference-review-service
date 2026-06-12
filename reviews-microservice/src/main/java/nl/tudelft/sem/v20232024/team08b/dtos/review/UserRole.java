package nl.tudelft.sem.v20232024.team08b.dtos.review;

public enum UserRole {
    CHAIR,
    REVIEWER,
    AUTHOR;

    /**
     * Parses a string given from other microservices into an enum
     * that can be used internally.
     *
     * @param userString the string describing a role.
     * @return a parsed enum
     */
    public static UserRole parse(String userString) {
        // The Users microservice is not consistent in how it spells role names,
        // so both known spellings of each role are accepted here.
        switch (userString) {
            case "PC Chair", "PCchair":
            case "General Chair", "GeneralChair":
                return CHAIR;
            case "Author", "author":
                return AUTHOR;
            case "PC Member", "PCMember":
            case "Sub-reviewer", "Subreviewer":
                return REVIEWER;
            default:
                throw new RuntimeException("Failed to parse user role");
        }
    }
}
