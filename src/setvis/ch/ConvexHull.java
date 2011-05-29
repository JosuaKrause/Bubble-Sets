/**
 * 
 */
package setvis.ch;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

import setvis.SetOutline;

/**
 * Calculates a convex hull outline, ignoring the non members. The convex hull
 * is computed with the Graham's Scan algorithm.
 * 
 * @author Joschi
 * 
 */
public class ConvexHull implements SetOutline {

	/**
	 * Calculates the relative orientation of two vectors.
	 * 
	 * @param from
	 *            The starting point of the vector.
	 * @param to
	 *            The vector the orientation is calculated for.
	 * @param rel
	 *            The vector relative to the other.
	 * @return {@code > 0} if {@code rel} is left of {@code from -> to} and
	 *         {@code < 0} if {@code rel} is right of {@code from -> to}.
	 */
	public static int relTo(final Point2D from, final Point2D to,
			final Point2D rel) {
		return (int) Math.signum((to.getX() - from.getX())
				* (from.getY() - rel.getY()) - (rel.getX() - from.getX())
				* (from.getY() - to.getY()));
	}

	/**
	 * @param from
	 *            The starting point.
	 * @param to
	 *            The end point.
	 * @return Whether the vector {@code from -> to} is the null vector.
	 */
	public static boolean isNull(final Point2D from, final Point2D to) {
		return from.getX() == to.getX() && from.getY() == to.getY();
	}

	@Override
	public Point2D[] createOutline(final Rectangle2D[] members,
			final Rectangle2D[] nonMembers) {
		if (members.length == 0) {
			return new Point2D[0];
		}
		final Point2D[] all = new Point2D[members.length * 4];
		int pos = 0;
		Point2D ref = null;
		for (final Rectangle2D r : members) {
			final Point2D p = new Point2D.Double(r.getMinX(), r.getMaxY());
			if (ref == null || p.getY() > ref.getY()
					|| (p.getY() == ref.getY() && p.getX() < ref.getX())) {
				ref = p;
			}
			all[pos++] = p;
			all[pos++] = new Point2D.Double(r.getMaxX(), r.getMinY());
			all[pos++] = new Point2D.Double(r.getMaxX(), r.getMaxY());
			all[pos++] = new Point2D.Double(r.getMinX(), r.getMinY());
		}
		sortPolar(all, ref);
		return grahamScan(all);
	}

	/**
	 * Sorts the array by polar coordinates in reference to {@code ref}.
	 * 
	 * @param all
	 *            The array to sort.
	 * @param ref
	 *            The reference point for the polar coordinates.
	 */
	private void sortPolar(final Point2D[] all, final Point2D ref) {
		final Comparator<Point2D> cmp = new Comparator<Point2D>() {
			@Override
			public int compare(final Point2D o1, final Point2D o2) {
				if (isNull(ref, o1)) {
					return -1;
				}
				if (isNull(ref, o2)) {
					return 1;
				}
				return -relTo(ref, o1, o2);
			}
		};
		Arrays.sort(all, cmp);
	}

	/**
	 * Performs the graham scan on a sorted array.
	 * 
	 * @param all
	 *            The sorted array.
	 * @return The convex hull vertices.
	 */
	private Point2D[] grahamScan(final Point2D[] all) {
		final LinkedList<Point2D> res = new LinkedList<Point2D>();
		res.addFirst(all[0]);
		res.addFirst(all[1]);
		int i = 2;
		final int n = all.length;
		while (i < n) {
			final Point2D p1 = res.get(0);
			final Point2D p2 = res.get(1);
			final Point2D si = all[i];
			if (isNull(p2, p1) || relTo(p2, p1, si) < 0) {
				res.removeFirst();
				continue;
			}
			res.addFirst(si);
			++i;
		}
		return res.toArray(new Point2D[res.size()]);
	}

}
