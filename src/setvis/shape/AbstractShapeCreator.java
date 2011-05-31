/**
 * 
 */
package setvis.shape;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

import setvis.SetOutline;

/**
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public abstract class AbstractShapeCreator {

	private final SetOutline setOutline;

	public AbstractShapeCreator(final SetOutline setOutline) {
		this.setOutline = setOutline;
	}

	private double radius = 10.0;

	/**
	 * @param radius
	 *            the radius to set
	 */
	public void setRadius(final double radius) {
		this.radius = radius;
	}

	/**
	 * @return the radius
	 */
	public double getRadius() {
		return radius;
	}

	public Shape[] createShapesForLists(final List<List<Rectangle2D>> items) {
		final List<Rectangle2D[]> list = new LinkedList<Rectangle2D[]>();
		for (final List<Rectangle2D> group : items) {
			list.add(group.toArray(new Rectangle2D[group.size()]));
		}
		return createShapesFor(list);
	}

	public Shape[] createShapesFor(final List<Rectangle2D[]> items) {
		final Shape[] res = new Shape[items.size()];
		int i = 0;
		for (final Rectangle2D[] group : items) {
			res[i++] = createShapeFor(group, getNonMembers(items, i));
		}
		return res;
	}

	/**
	 * Finds all items not belonging to the given group.
	 * 
	 * @param groupID
	 *            The group.
	 * @return All items not belonging to the group.
	 */
	private Rectangle2D[] getNonMembers(final List<Rectangle2D[]> items,
			final int groupID) {
		final List<Rectangle2D> res = new LinkedList<Rectangle2D>();
		int g = 0;
		for (final Rectangle2D[] group : items) {
			if (g++ == groupID) {
				continue;
			}
			for (final Rectangle2D r : group) {
				res.add(r);
			}
		}
		return res.toArray(new Rectangle2D[res.size()]);
	}

	public Shape createShapeFor(final Rectangle2D[] members,
			final Rectangle2D[] nonMembers) {
		final Rectangle2D[] m = mapRects(members);
		final Rectangle2D[] n = mapRects(nonMembers);
		final Point2D[] res = setOutline.createOutline(m, n);
		return convertToShape(res);
	}

	protected Rectangle2D[] mapRects(final Rectangle2D[] rects) {
		int i = rects.length;
		final Rectangle2D[] res = new Rectangle2D[i];
		while (--i >= 0) {
			res[i] = mapRect(rects[i]);
		}
		return res;
	}

	protected Rectangle2D mapRect(final Rectangle2D r) {
		return new Rectangle2D.Double(r.getMinX(), r.getMinY(), r.getWidth(), r
				.getHeight());
	}

	protected abstract Shape convertToShape(Point2D[] points);

}
