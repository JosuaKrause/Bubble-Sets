package setvis.shape;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import setvis.SetOutline;

/**
 * Generates a {@link Shape} with a b-spline generated outline. The generated
 * shapes may be a bit exactly defined, causing performance issues. Setting the
 * granularity can work against this but can also lead to not smooth results.
 * When performance is an issue and the outlines do not have to be very smooth
 * {@link BezierShapeGenerator} are a better alternative.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public class BSplineShapeGenerator extends RoundShapeGenerator {

    // since the basic function is fixed this value should not be changed
    private static final int ORDER = 3;

    private static final int START_INDEX = ORDER - 1;

    private static final int REL_END = 1;

    private static final int REL_START = REL_END - ORDER;

    private int granularity = 6;

    /**
     * Creates a new {@link BSplineShapeGenerator} with the given set outline
     * creator.
     * 
     * @param outline
     *            The set outline generator.
     */
    public BSplineShapeGenerator(final SetOutline outline) {
        super(outline, true);
    }

    /**
     * Sets the granularity.
     * 
     * @param granularity
     *            The granularity is the number of line segments per base point.
     */
    public void setGranularity(final int granularity) {
        this.granularity = granularity;
    }

    /**
     * @return The granularity is the number of line segments per base point.
     */
    public int getGranularity() {
        return granularity;
    }

    @Override
    public Shape convertToShape(final Point2D[] points, final boolean closed) {
        final GeneralPath res = new GeneralPath();
        // covering special cases
        switch (points.length) {
        case 0:
            return res;
        case 1: {
            final Point2D p0 = points[0];
            res.moveTo(p0.getX(), p0.getY());
            res.lineTo(p0.getX(), p0.getY());
            return res;
        }
        case 2: {
            if (closed) {
                break;
            }
            final Point2D p0 = points[0];
            final Point2D p1 = points[1];
            res.moveTo(p0.getX(), p0.getY());
            res.lineTo(p1.getX(), p1.getY());
            return res;
        }
        default:
            break;
        }
        // actual b-spline calculation
        final int count = points.length + ORDER - 1;
        final double g = granularity;
        final Point2D start = calcPoint(points, START_INDEX - (closed ? 0 : 2),
                0, closed);
        res.moveTo(start.getX(), start.getY());
        for (int i = START_INDEX - (closed ? 0 : 2); i < count
                + (closed ? 0 : 2); ++i) {
            for (int j = 1; j <= granularity; ++j) {
                final Point2D cur = calcPoint(points, i, j / g, closed);
                res.lineTo(cur.getX(), cur.getY());
            }
        }
        return res;
    }

    private static double basicFunction(final int i, final double t) {
        // the basis function for a cubic B spline
        switch (i) {
        case -2:
            return (((-t + 3) * t - 3) * t + 1) / 6;
        case -1:
            return (((3 * t - 6) * t) * t + 4) / 6;
        case 0:
            return (((-3 * t + 3) * t + 3) * t + 1) / 6;
        case 1:
            return (t * t * t) / 6;
        default:
            throw new InternalError();
        }
    }

    // evaluates a point on the B spline
    private Point2D calcPoint(final Point2D[] points, final int i,
            final double t, final boolean closed) {
        double px = 0;
        double py = 0;
        for (int j = REL_START; j <= REL_END; j++) {
            final Point2D p = points[closed ? getRelativeIndex(i, j,
                    points.length) : Math.max(0,
                    Math.min(points.length - 1, i + j))];
            final double bf = basicFunction(j, t);
            px += bf * p.getX();
            py += bf * p.getY();
        }
        return new Point2D.Double(px, py);
    }

}
