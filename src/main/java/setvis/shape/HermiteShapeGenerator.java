package setvis.shape;

import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

import setvis.SetOutline;

/**
 * A generic hermite shape generator. The tangent calculation must be done by a
 * subclass.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 */
public abstract class HermiteShapeGenerator extends RoundShapeDecorator {

  private final AbstractShapeGenerator parent;

  private int granularity = 6;

  /**
   * Creates a hermite shape generator for a set outline.
   * 
   * @param outline The outline.
   */
  public HermiteShapeGenerator(final SetOutline outline) {
    this(new PolygonShapeGenerator(outline));
  }

  /**
   * Creates an hermite shape generator.
   * 
   * @param parent The parent.
   */
  public HermiteShapeGenerator(final AbstractShapeGenerator parent) {
    super(parent, true);
    this.parent = parent;
  }

  /**
   * Sets the granularity.
   * 
   * @param granularity The granularity is the number of line segments per base
   *          point.
   */
  public void setGranularity(final int granularity) {
    this.granularity = granularity;
  }

  /**
   * Getter.
   * 
   * @return The granularity is the number of line segments per base point.
   */
  public int getGranularity() {
    return granularity;
  }

  @Override
  public Point2D[] convert(final Point2D[] points, final boolean closed) {
    // covering special cases
    if(points.length < 3) return points;
    // actual hermite-curve calculation
    final List<Point2D> list = new LinkedList<Point2D>();
    final int count = points.length - (closed ? 0 : 1);
    final double g = granularity;
    for(int i = 0; i < count; ++i) {
      final int j = getPointIndex(points, closed, i, 1);
      final Point2D p0 = points[i];
      final Point2D p1 = points[j];
      final Point2D t0 = getTangentFor(points, closed, i, false);
      final Point2D t1 = getTangentFor(points, closed, j, true);
      if(!closed && i == 0) {
        list.add(calcPoint(p0, p1, t0, t1, 0));
      }
      for(int s = 1; s <= granularity; ++s) {
        list.add(calcPoint(p0, p1, t0, t1, s / g));
      }
    }
    return list.toArray(new Point2D[list.size()]);
  }

  /**
   * Gets a point at a relative position to the current point.
   * 
   * @param points The array of points.
   * @param closed Whether the spline is closed.
   * @param i The current index.
   * @param j The relative index.
   * @return The relative point.
   */
  protected Point2D getPoint(final Point2D[] points, final boolean closed,
      final int i, final int j) {
    return points[getPointIndex(points, closed, i, j)];
  }

  /**
   * Gets the index of a point.
   * 
   * @param points The points array.
   * @param closed Whether the curve should be closed, i.e. index 0 and
   *          {@code points.length} are the same.
   * @param i The index.
   * @param j The relative position.
   * @return The index of the point.
   */
  protected int getPointIndex(final Point2D[] points, final boolean closed,
      final int i, final int j) {
    return closed ? getRelativeIndex(i, j, points.length) : Math.max(0,
        Math.min(points.length - 1, i + j));
  }

  // evaluates a point on the hermite curve
  private static Point2D calcPoint(final Point2D p0, final Point2D p1,
      final Point2D t0, final Point2D t1, final double s) {
    final double s2 = s * s;
    final double s3 = s * s2;
    final double h1 = 2 * s3 - 3 * s2 + 1;
    final double h2 = -2 * s3 + 3 * s2;
    final double h3 = s3 - 2 * s2 + s;
    final double h4 = s3 - s2;
    final double x = h1 * p0.getX() + h2 * p1.getX() + h3 * t0.getX() + h4
        * t1.getX();
    final double y = h1 * p0.getY() + h2 * p1.getY() + h3 * t0.getY() + h4
        * t1.getY();
    return new Point2D.Double(x, y);
  }

  /**
   * Calculates the tanget for the given point.
   * 
   * @param points The point array.
   * @param closed Whether the shape is closed.
   * @param i The index of the current point.
   * @param incoming Whether it is an incoming or outgoing tangent.
   * @return The tangent.
   */
  protected abstract Point2D getTangentFor(Point2D[] points, boolean closed,
      int i, boolean incoming);

  /**
   * Getter.
   * 
   * @return The parent.
   */
  @Override
  public AbstractShapeGenerator getParent() {
    return parent;
  }

}
