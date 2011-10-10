package setvis.bubbleset;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class EdgeCreator {

    private static class Edge {

        private final Set<Integer> ids = new HashSet<Integer>();

    }

    public static Collection<Line2D> createLines(final Rectangle2D[] boxes,
            final Collection<Line2D> external, final Collection<Line2D> calc) {
        return null;
    }

    public static Collection<Line2D> extendFull(final Rectangle2D[] boxes,
            final Collection<Line2D> external) {
        return null;
    }

}
