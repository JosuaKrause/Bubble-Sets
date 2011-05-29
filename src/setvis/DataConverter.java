/**
 * 
 */
package setvis;

import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Converts data types and values.
 * 
 * @author Joschi
 * 
 */
public final class DataConverter {

	private DataConverter() {
		// not constructible
	}

	/**
	 * Converts a list of points into a polygon. Note that the precision is
	 * reduced due to Polygon has only integer precision.
	 * 
	 * @param points
	 *            The points to convert.
	 * @return The resulting polygon.
	 */
	public static Polygon convertToPolygon(final Point2D[] points) {
		final int size = points.length;
		final int[] x = new int[size];
		final int[] y = new int[size];
		int i = size;
		while (--i >= 0) {
			x[i] = (int) points[i].getX();
			y[i] = (int) points[i].getY();
		}
		return new Polygon(x, y, size);
	}

	/**
	 * Converts a list of rectangles into an array by calling
	 * {@link #convertRect(Rectangle2D)} on each item.
	 * 
	 * @param list
	 *            The list of rectangles.
	 * @return The resulting array.
	 */
	public static Rectangle2D[] convertToRect(final List<Rectangle2D> list) {
		int i = list.size();
		final Rectangle2D[] res = new Rectangle2D[i];
		while (--i >= 0) {
			res[i] = convertRect(list.get(i));
		}
		return res;
	}

	/**
	 * The default radius of {@link #convertRect(Rectangle2D, double)}.
	 */
	private static final double DEFAULT_RADIUS = 10.0;

	/**
	 * Calls {@link #convertRect(Rectangle2D, double)} with the
	 * {@link #DEFAULT_RADIUS}.
	 * 
	 * @param r
	 *            The rectangle to convert.
	 * @return The resulting rectangle.
	 */
	public static Rectangle2D convertRect(final Rectangle2D r) {
		return convertRect(r, DEFAULT_RADIUS);
	}

	/**
	 * Converts a rectangle in a larger one.
	 * 
	 * @param r
	 *            The rectangle.
	 * @param addRadius
	 *            The radius to add.
	 * @return The larger rectangle.
	 */
	public static Rectangle2D convertRect(final Rectangle2D r,
			final double addRadius) {
		final double dblRad = addRadius * 2.0;
		return new Rectangle2D.Double(r.getMinX() - addRadius, r.getMinY()
				- addRadius, r.getWidth() + dblRad, r.getHeight() + dblRad);
	}
}
