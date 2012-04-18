package setvis.shape;

import java.awt.geom.Point2D;

import setvis.SetOutline;
import setvis.VecUtil;

public class CardinalSplineGenerator extends HermiteShapeGenerator {

  private double a = 0.5; // defaults to catmull-rom spline

  public CardinalSplineGenerator(final SetOutline outline) {
    super(outline);
  }

  public CardinalSplineGenerator(final AbstractShapeGenerator parent) {
    super(parent);
  }

  public void setA(final double a) {
    this.a = a;
  }

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
