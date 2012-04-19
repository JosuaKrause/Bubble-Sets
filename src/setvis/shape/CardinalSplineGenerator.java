package setvis.shape;

import java.awt.geom.Point2D;

import setvis.SetOutline;
import setvis.VecUtil;

/**
 * A cardinal spline. Without an alternate value for {@link #a} the spline is a
 * catmull-rom spline.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 */
public class CardinalSplineGenerator extends HermiteShapeGenerator {

  private double a = 0.5; // defaults to catmull-rom spline

  /**
   * Creates a cardinal spline.
   * 
   * @param outline The set outline.
   */
  public CardinalSplineGenerator(final SetOutline outline) {
    super(outline);
  }

  /**
   * Creates a cardinal spline.
   * 
   * @param parent The parent generator.
   */
  public CardinalSplineGenerator(final AbstractShapeGenerator parent) {
    super(parent);
  }

  /**
   * Setter.
   * 
   * @param a Sets the coefficient of the spline. A value of {@code 0.5} is a
   *          catmull-rom spline. Reasonable values range from {@code 0} to
   *          {@code 1}.
   */
  public void setA(final double a) {
    this.a = a;
  }

  /**
   * Getter.
   * 
   * @return The coefficient.
   */
  public double getA() {
    return a;
  }

  @Override
  protected Point2D getTangentFor(final Point2D[] points,
      final boolean closed, final int i, final boolean incoming) {
    final Point2D m1 = getPoint(points, closed, i, -1);
    final Point2D p1 = getPoint(points, closed, i, 1);
    return VecUtil.mulVec(VecUtil.subVec(p1, m1), a);
  }

}
