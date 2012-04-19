package setvis.shape;

import java.awt.Shape;
import java.awt.geom.Point2D;

/**
 * Decorates another {@link AbstractShapeGenerator}.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 */
public abstract class ShapeGeneratorDecorator extends AbstractShapeGenerator {

  private final AbstractShapeGenerator parent;

  /**
   * Creates a new shape generator decorator.
   * 
   * @param parent The parent to decorate.
   */
  public ShapeGeneratorDecorator(final AbstractShapeGenerator parent) {
    super(parent.getSetOutline());
    this.parent = parent;
  }

  @Override
  public Shape convertToShape(final Point2D[] points, final boolean closed) {
    return parent.convertToShape(convert(points, closed), closed);
  }

  /**
   * Manipulates the original outline by modifying the vertices.
   * 
   * @param points The original points.
   * @param closed Whether the shape should be closed.
   * @return The new points.
   */
  protected abstract Point2D[] convert(Point2D[] points, boolean closed);

  /**
   * Getter.
   * 
   * @return The parent.
   */
  public AbstractShapeGenerator getParent() {
    return parent;
  }

}
