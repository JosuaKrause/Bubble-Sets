/**
 * 
 */
package setvis.shape;

import static setvis.VecUtil.addVec;
import static setvis.VecUtil.getOrthoLeft;
import static setvis.VecUtil.getOrthoRight;
import static setvis.VecUtil.mulVec;
import static setvis.VecUtil.normVec;
import static setvis.VecUtil.subVec;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import setvis.SetOutline;

/**
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public class BezierShapeGenerator extends RoundShapeGenerator {

	public BezierShapeGenerator(final SetOutline outline,
			final boolean clockwise) {
		super(outline, clockwise);
	}

	@Override
	protected Shape convertToShape(final Point2D[] points) {
		final GeneralPath res = new GeneralPath();
		final int len = points.length;
		Point2D first = null;
		for (int i = 0; i < len; ++i) {
			final Point2D[] vertices = getOrthoBezier(points, i, getRadius());
			final Point2D p = vertices[0];
			if (first == null) {
				first = p;
				res.moveTo(p.getX(), p.getY());
			} else {
				res.lineTo(p.getX(), p.getY());
			}
			final Point2D s = vertices[1];
			final Point2D e = vertices[2];
			if (s == null) {
				res.lineTo(e.getX(), e.getY());
				continue;
			}
			res.curveTo(s.getX(), s.getY(), s.getX(), s.getY(), e.getX(), e
					.getY());
		}
		if (first != null) {
			res.lineTo(first.getX(), first.getY());
		}
		return res;
	}

	public Point2D[] getOrthoBezier(final Point2D[] points, final int index,
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
}
