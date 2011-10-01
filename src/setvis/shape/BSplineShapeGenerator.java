package setvis.shape;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import setvis.SetOutline;

/**
 * Generates a {@link Shape} with a b-spline generated outline.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public class BSplineShapeGenerator extends RoundShapeGenerator {

	/**
	 * Creates a new {@link BSplineShapeGenerator} with the given set outline
	 * creator.
	 * 
	 * @param outline
	 *            The set outline generator.
	 */
	public BSplineShapeGenerator(final SetOutline outline) {
		super(outline, true);
	}

	@Override
	public Shape convertToShape(final Point2D[] points, final boolean closed) {
		final GeneralPath res = new GeneralPath();
		// final Point2D last = null;
		// for (int i = 0; i < points.length; ++i) {
		//
		// }

		// TODO Auto-generated method stub
		return res;
	}

	// the basis function for a cubic B spline
	float b(final int i, final float t) {
		switch (i) {
		case -2:
			return (((-t + 3) * t - 3) * t + 1) / 6;
		case -1:
			return (((3 * t - 6) * t) * t + 4) / 6;
		case 0:
			return (((-3 * t + 3) * t + 3) * t + 1) / 6;
		case 1:
			return (t * t * t) / 6;
		}
		return 0; // we only get here if an invalid i is specified
	}

	// evaluate a point on the B spline
	Point p(final int i, final float t) {
		float px = 0;
		float py = 0;
		for (int j = -2; j <= 1; j++) {
			px += b(j, t) * pts.xpoints[i + j];
			py += b(j, t) * pts.ypoints[i + j];
		}
		return new Point(Math.round(px), Math.round(py));
	}

	final int STEPS = 12;

	public void paint(final Graphics g) {
		final FontMetrics fm = g.getFontMetrics(f);
		g.setFont(f);
		final int h = fm.getAscent() / 2;

		for (int i = 0; i < pts.npoints; i++) {
			final String s = Integer.toString(i);
			final int w = fm.stringWidth(s) / 2;
			g.drawString(Integer.toString(i), pts.xpoints[i] - w,
					pts.ypoints[i] + h);
		}
		final Polygon pol = new Polygon();
		Point q = p(2, 0);
		pol.addPoint(q.x, q.y);
		for (int i = 2; i < pts.npoints - 1; i++) {
			for (int j = 1; j <= STEPS; j++) {
				q = p(i, j / (float) STEPS);
				pol.addPoint(q.x, q.y);
			}
		}
		g.drawPolyline(pol.xpoints, pol.ypoints, pol.npoints);
	}

	protected Polygon pts = new Polygon();
	protected int selection = -1;

	static Font f = new Font("Courier", Font.PLAIN, 12);

	static final int EPSILON = 36; /* square of distance for picking */

	/* return index of control point near to (x,y) or -1 if nothing near */
	public int selectPoint(final int x, final int y) {
		int mind = Integer.MAX_VALUE;
		selection = -1;
		for (int i = 0; i < pts.npoints; i++) {
			final int d = sqr(pts.xpoints[i] - x) + sqr(pts.ypoints[i] - y);
			if (d < mind && d < EPSILON) {
				mind = d;
				selection = i;
			}
		}
		return selection;
	}

	// square of an int
	static int sqr(final int x) {
		return x * x;
	}

	/* add a control point, return index of new control point */
	public int addPoint(final int x, final int y) {
		pts.addPoint(x, y);
		return selection = pts.npoints - 1;
	}

	/* set selected control point */
	public void setPoint(final int x, final int y) {
		if (selection >= 0) {
			pts.xpoints[selection] = x;
			pts.ypoints[selection] = y;
		}
	}

	/** remove selected control point */
	public void removePoint() {
		if (selection >= 0) {
			pts.npoints--;
			for (int i = selection; i < pts.npoints; i++) {
				pts.xpoints[i] = pts.xpoints[i + 1];
				pts.ypoints[i] = pts.ypoints[i + 1];
			}
		}
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (int i = 0; i < pts.npoints; i++) {
			result.append(" " + pts.xpoints[i] + " " + pts.ypoints[i]);
		}
		return result.toString();
	}
}
