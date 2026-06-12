package nl.tudelft.sem.v20232024.team08b.application.strategies;

import java.util.List;
import nl.tudelft.sem.v20232024.team08b.exceptions.NotFoundException;
import nl.tudelft.sem.v20232024.team08b.domain.Paper;
import nl.tudelft.sem.v20232024.team08b.domain.TrackID;

public interface AutomaticAssignmentStrategy {
    void automaticAssignment(TrackID trackID, List<Paper> papers) throws NotFoundException;

}

