/**
 * 
 */
package setvis.shape;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import setvis.SetOutline;

/**
 * Generates a precise {@link Shape} with round edges for the vertices generated
 * by {@link SetOutline#createOutline(Rectangle2D[], Rectangle2D[])}.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 */
public abstract class RoundShapeGenerator extends AbstractShapeGenerator {

  /**
   * Whether the result of the set outlines are interpreted in clockwise order.
   */
  private final boolean clockwise;

  /**
   * Creates an {@link RoundShapeGenerator} with a given set outline creator.
   * 
   * @param outline The set outline.
   * @param clockwise Whether the result of the set outlines are interpreted in
   *          clockwise order.
   */
  public RoundShapeGenerator(final SetOutline outline,
      final boolean clockwise) {
    super(outline);
    this.clockwise = clockwise;
  }

  /**
   * Getter.
   * 
   * @param index The index.
   * @param len The maximum length of the array.
   * @param next Whether to return the next or previous index.
   * @return The next or previous index for an array with the given length.
   */
  protected final int getOtherIndex(final int index, final int len,
      final boolean next) {
    return bound((next ^ clockwise) ? (index + len - 1) : (index + 1), len);
  }

  /**
   * Getter.
   * 
   * @param index The index.
   * @param relIndex The difference.
   * @param len The length of the array.
   * @return The relative index.
   */
  protected final int getRelativeIndex(final int index, final int relIndex,
      final int len) {
    return bound(index + (clockwise ? relIndex : -relIndex), len);
  }

  /**
   * Ensures that an index lies within in the array.
   * 
   * @param index The index.
   * @param len The length of the array.
   * @return The index that lies in the array.
   */
  protected static final int bound(final int index, final int len) {
    int i = index;
    i %= len;
    if(i < 0) {
      i += len;
    }
    return i;
  }

  /**
   * Getter.
   * 
   * @return Whether the points are in clockwise order.
   */
  public boolean isClockwise() {
    return clockwise;
  }

}
