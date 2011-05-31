/**
 * 
 */
package setvis.shape;

import setvis.SetOutline;

/**
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public abstract class RoundShapeGenerator extends AbstractShapeCreator {

	private final boolean clockwise;

	public RoundShapeGenerator(final SetOutline outline, final boolean clockwise) {
		super(outline);
		this.clockwise = clockwise;
	}

	protected int getOtherIndex(final int index, final int len,
			final boolean next) {
		return ((next ^ clockwise) ? (index + len - 1) : (index + 1)) % len;
	}
}
