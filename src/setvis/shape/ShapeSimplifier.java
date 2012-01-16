package setvis.shape;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import setvis.SetOutline;
import setvis.VecUtil;

public class ShapeSimplifier extends AbstractShapeGenerator {

    private final AbstractShapeGenerator parent;

    private double tolerance;

    public ShapeSimplifier(final AbstractShapeGenerator parent) {
        this(parent, 0.0);
    }

    public ShapeSimplifier(final AbstractShapeGenerator parent,
            final double tolerance) {
        super(parent.getSetOutline());
        this.parent = parent;
        // proper initialization of the tolerance
        setTolerance(tolerance);
    }

    @Override
    public void setRadius(final double radius) {
        if (parent != null) {
            parent.setRadius(radius);
        }
    }

    @Override
    public double getRadius() {
        return parent.getRadius();
    }

    private double tsqr;

    public void setTolerance(final double tolerance) {
        this.tolerance = tolerance;
        tsqr = tolerance * tolerance;
    }

    public double getTolerance() {
        return tolerance;
    }

    @Override
    public SetOutline getSetOutline() {
        return parent.getSetOutline();
    }

    @Override
    public Shape convertToShape(final Point2D[] points, final boolean closed) {
        if (points.length < 3) {
            return parent.convertToShape(points, closed);
        }
        final List<Point2D> list = new ArrayList<Point2D>(points.length);
        final int maxLen = closed ? points.length : points.length - 1;
        list.add(points[0]);
        Point2D cur = points[0];
        double distSum = 0;
        for (int i = 1; i < maxLen; ++i) {
            final Point2D p = points[i];
            distSum += VecUtil.distPointLineSqr(cur, points[(i + 1)
                    % points.length], p);
            if (distSum > tsqr) {
                // no removal
                list.add(p);
                cur = p;
                distSum = 0;
            }
        }
        if (!closed) {
            list.add(points[points.length - 1]);
        }
        return parent.convertToShape(list.toArray(new Point2D[list.size()]),
                closed);
    }

}
