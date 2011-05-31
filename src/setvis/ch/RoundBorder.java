package setvis.ch;

import static setvis.VecUtil.addVec;
import static setvis.VecUtil.getOrthoLeft;
import static setvis.VecUtil.getOrthoRight;
import static setvis.VecUtil.invVec;
import static setvis.VecUtil.middleVec;
import static setvis.VecUtil.mulVec;
import static setvis.VecUtil.normVec;
import static setvis.VecUtil.subVec;

import java.awt.geom.Point2D;

/**
 * TODO
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public class RoundBorder {

	private final Point2D[] points;

	private boolean clockwise;

	private final int len;

	public RoundBorder(final Point2D[] points, final boolean clockwise) {
		this.points = points;
		len = points.length;
	}

	private int getOtherIndex(final int index, final boolean next) {
		return ((next ^ clockwise) ? (index + len - 1) : (index + 1)) % len;
	}

	public Point2D[] getOrthoBezier(final int index, final double distance) {
		final Point2D point = points[index];
		final Point2D left = points[getOtherIndex(index, false)];
		final Point2D right = points[getOtherIndex(index, true)];
		final Point2D lp = subVec(left, point);
		final Point2D pr = subVec(right, point);
		final Point2D ol = getOrthoRight(lp);
		final Point2D or = getOrthoLeft(pr);
		final Point2D nol = mulVec(normVec(ol), distance);
		final Point2D nor = mulVec(normVec(or), distance);
		final Point2D first = addVec(nor, point);
		final Point2D second = addVec(nol, point);
		return new Point2D[] { first, getIntersection(first, pr, second, lp),
				second };
	}

	private Point2D getIntersection(final Point2D a, final Point2D v,
			final Point2D b, final Point2D w) {
		final double vx = v.getX();
		final double wx = w.getX();
		if (wx != 0.0 && Math.abs(vx) < Math.abs(wx)) {
			return getIntersection(b, w, a, v);
		}
		// TODO: more parallel checks
		if (vx == 0.0) {
			// throw new IllegalArgumentException("the lines are parallel: a " +
			// a
			// + " v " + v + " b " + b + " w " + w);
			return null;
		}
		final double dy = a.getY() - b.getY();
		final double dx = b.getX() - a.getX();
		final double quot = v.getY() / vx;
		final double dw = w.getY() - wx * quot;
		final double mu = (dy + dx * quot) / dw;
		return addVec(b, mulVec(w, mu));
	}

	public Point2D[] getOrthos(final int index, final double distance) {
		switch (len) {
		case 1:
			return singleOrthos(points[index], distance);
		case 2:
			return doubleOrthos(points[index],
					points[getOtherIndex(index, false)], distance);
		default:
			return defaultOrthos(index, distance);
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

	private Point2D[] defaultOrthos(final int index, final double distance) {
		final Point2D point = points[index];
		final Point2D left = points[getOtherIndex(index, false)];
		final Point2D right = points[getOtherIndex(index, true)];
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

	public int getSize() {
		return len;
	}

}
