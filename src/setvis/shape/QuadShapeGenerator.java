/**
 * 
 */
package setvis.shape;

import static setvis.VecUtil.addVec;
import static setvis.VecUtil.getOrthoLeft;
import static setvis.VecUtil.getOrthoRight;
import static setvis.VecUtil.invVec;
import static setvis.VecUtil.middleVec;
import static setvis.VecUtil.mulVec;
import static setvis.VecUtil.normVec;
import static setvis.VecUtil.subVec;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import setvis.SetOutline;

/**
 * Generates a quadratic interpolated {@link Shape} for the vertices generated
 * by {@link SetOutline#createOutline(Rectangle2D[], Rectangle2D[])}.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public class QuadShapeGenerator extends RoundShapeGenerator {

	/**
	 * Creates an {@link QuadShapeGenerator} with a given set outline creator.
	 * 
	 * @param outline
	 *            The creator of the set outlines.
	 * @param clockwise
	 *            Whether the result of the set outlines are interpreted in
	 *            clockwise order.
	 */
	public QuadShapeGenerator(final SetOutline outline, final boolean clockwise) {
		super(outline, clockwise);
	}

	@Override
	protected Shape convertToShape(final Point2D[] points) {
		final GeneralPath res = new GeneralPath();
		final int len = points.length;
		Point2D first = null;
		final Point2D[] buff = new Point2D[2];
		for (int i = 0; i < len; ++i) {
			for (final Point2D p : getOrthos(points, i, getRadius())) {
				if (first == null) {
					first = p;
					res.moveTo(p.getX(), p.getY());
					continue;
				}
				feedPoint(res, buff, p);
			}
		}
		feedPoint(res, buff, first);
		// close the line, if the buffer is not empty
		if (buff[0] != null) {
			feedPoint(res, buff, buff[0]);
		}
		return res;
	}

	/**
	 * Adds a point to the buffer and flushes the buffer when it is full, by
	 * drawing a quadratic interpolated curve.
	 * 
	 * @param path
	 *            The path drawer.
	 * @param buff
	 *            The buffer of size 2.
	 * @param p
	 *            The new point.
	 */
	private void feedPoint(final GeneralPath path, final Point2D[] buff,
			final Point2D p) {
		if (buff[0] == null) {
			buff[0] = p;
			return;
		}
		buff[1] = p;
		if (buff[1] == null) {
			return;
		}
		path.quadTo(buff[0].getX(), buff[0].getY(), buff[1].getX(), buff[1]
				.getY());
		buff[0] = null;
		buff[1] = null;
	}

	private Point2D[] getOrthos(final Point2D[] points, final int index,
			final double distance) {
		final int len = points.length;
		switch (len) {
		case 1:
			return singleOrthos(points[index], distance);
		case 2:
			return doubleOrthos(points[index], points[getOtherIndex(index, len,
					false)], distance);
		default:
			return defaultOrthos(points, index, distance);
		}
	}

	private Point2D[] singleOrthos(final Point2D p, final double d) {
		final Point2D x = new Point2D.Double(d, 0.0);
		final Point2D y = new Point2D.Double(0.0, d);
		final Point2D r = addVec(p, x);
		final Point2D t = addVec(p, y);
		final Point2D l = addVec(p, invVec(x));
		final Point2D b = addVec(p, invVec(y));
		return new Point2D[] { l, middleVec(p, l, t, d), t,
				middleVec(p, t, r, d), r, middleVec(p, r, b, d), b,
				middleVec(p, b, l, d) };
	}

	private Point2D[] doubleOrthos(final Point2D p, final Point2D other,
			final double d) {
		final Point2D same = mulVec(normVec(subVec(p, other)), d);
		final Point2D left = getOrthoLeft(same);
		final Point2D right = getOrthoRight(same);
		final Point2D l = addVec(p, left);
		final Point2D m = addVec(p, same);
		final Point2D r = addVec(p, right);
		return new Point2D[] { l, middleVec(p, l, m, d), m,
				middleVec(p, m, r, d), r };
	}

	private Point2D[] defaultOrthos(final Point2D[] points, final int index,
			final double distance) {
		final int len = points.length;
		final Point2D point = points[index];
		final Point2D left = points[getOtherIndex(index, len, false)];
		final Point2D right = points[getOtherIndex(index, len, true)];
		final Point2D lp = subVec(left, point);
		final Point2D pr = subVec(right, point);
		final Point2D ol = getOrthoRight(lp);
		final Point2D or = getOrthoLeft(pr);
		final Point2D nol = mulVec(normVec(ol), distance);
		final Point2D nor = mulVec(normVec(or), distance);
		final Point2D first = addVec(nor, point);
		final Point2D second = addVec(nol, point);
		final Point2D mid = middleVec(point, first, second, distance);
		return new Point2D[] { first, mid, second };
	}

}
