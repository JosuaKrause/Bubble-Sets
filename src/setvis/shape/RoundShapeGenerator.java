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
 * 
 */
public abstract class RoundShapeGenerator extends AbstractShapeCreator {

	/**
	 * Whether the result of the set outlines are interpreted in clockwise
	 * order.
	 */
	private final boolean clockwise;

	/**
	 * Creates an {@link RoundShapeGenerator} with a given set outline creator.
	 * 
	 * @param outline
	 *            The creator of the set outlines.
	 * @param clockwise
	 *            Whether the result of the set outlines are interpreted in
	 *            clockwise order.
	 */
	public RoundShapeGenerator(final SetOutline outline, final boolean clockwise) {
		super(outline);
		this.clockwise = clockwise;
	}

	/**
	 * @param index
	 *            The index.
	 * @param len
	 *            The maximum length of the array.
	 * @param next
	 *            Whether to return the next or previous index.
	 * @return The next or previous index for an array with the given length.
	 */
	protected int getOtherIndex(final int index, final int len,
			final boolean next) {
		return ((next ^ clockwise) ? (index + len - 1) : (index + 1)) % len;
	}
}
