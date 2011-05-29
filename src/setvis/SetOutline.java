/**
 * 
 */
package setvis;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * An interface for generating an outline for a set.
 * 
 * @author Joschi
 * 
 */
public interface SetOutline {

	/**
	 * Creates an outline for the set of rectangles given by {@code members}
	 * avoiding the rectangles of {@code nonMembers}.
	 * 
	 * @param members
	 *            The rectangles to include.
	 * @param nonMembers
	 *            The rectangles to avoid.
	 * @return The vertices of the outline.
	 */
	Point2D[] createOutline(Rectangle2D[] members, Rectangle2D[] nonMembers);

}
