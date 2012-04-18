package setvis.shape;

import java.awt.geom.Point2D;

import setvis.SetOutline;
import setvis.VecUtil;

public class KochanekBartelsSplineGenerator extends HermiteShapeGenerator {

  private double t = -0.5;

  private double c = 0;

  private double b = -0.5;

  public KochanekBartelsSplineGenerator(final SetOutline out) {
    super(out);
  }

  public KochanekBartelsSplineGenerator(final AbstractShapeGenerator parent) {
    super(parent);
  }

  public void setTension(final double t) {
    this.t = t;
  }

  public double getTension() {
    return t;
  }

  public void setContinuity(final double c) {
    this.c = c;
  }

  public double getContinuity() {
    return c;
  }

  public void setBias(final double b) {
    this.b = b;
  }

  public double getBias() {
    return b;
  }

  @Override
  protected Point2D getTangentFor(final Point2D[] points,
      final boolean closed, final int i, final boolean incoming) {
    final Point2D p0 = points[i];
    final Point2D p1 = getPoint(points, closed, i, 1);
    final Point2D m1 = getPoint(points, closed, i, -1);
    final Point2D a0 = VecUtil.subVec(p0, m1);
    final Point2D a1 = VecUtil.subVec(p1, p0);
    double mul0;
    double mul1;
    if(incoming) {
      mul0 = (1 - t) * (1 - c) * (1 + b);
      mul1 = (1 - t) * (1 + c) * (1 - b);
    } else {
      mul0 = (1 - t) * (1 + c) * (1 + b);
      mul1 = (1 - t) * (1 - c) * (1 - b);
    }
    return VecUtil.addVec(VecUtil.mulVec(a0, mul0 * .5),
        VecUtil.mulVec(a1, mul1 * .5));
  }

}
