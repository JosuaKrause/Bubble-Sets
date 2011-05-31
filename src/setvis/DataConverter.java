/**
 * 
 */
package setvis;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import setvis.ch.RoundBorder;

/**
 * Converts data types and values.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public final class DataConverter {

	/**
	 * The method of Shape creation.
	 * 
	 * @author Joschi <josua.krause@googlemail.com>
	 * 
	 */
	public static enum ShapeMethod {
		POLY, QUAD, BEZIER,
	}

	/**
	 * The default method to create shapes.
	 */
	private static final ShapeMethod DEFAULT_METHOD = ShapeMethod.POLY;

	/**
	 * The default radius of {@link #convertRect(Rectangle2D, double)}, which is
	 * applied before outline creation.
	 */
	private static final double DEFAULT_RADIUS = 10.0;

	/**
	 * The default border size, which is applied after outline creation.
	 */
	private static final double DEFAULT_BORDER = 10.0;

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
	 * Generates a path with quadratic interpolated lines.
	 * 
	 * @param points
	 *            The vertices.
	 * @return The path.
	 */
	public static GeneralPath convertToQuadPath(final Point2D[] points) {
		final GeneralPath res = new GeneralPath();
		final RoundBorder rb = new RoundBorder(points, false);
		final int len = rb.getSize();
		Point2D first = null;
		final Point2D[] buff = new Point2D[2];
		for (int i = 0; i < len; ++i) {
			for (final Point2D p : rb.getOrthos(i, DEFAULT_BORDER)) {
				if (first == null) {
					first = p;
					res.moveTo(p.getX(), p.getY());
					continue;
				}
				feedPoint(res, buff, p);
			}
		}
		feedPoint(res, buff, first);
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
	private static void feedPoint(final GeneralPath path, final Point2D[] buff,
			final Point2D p) {
		if (buff[0] == null) {
			buff[0] = p;
			return;
		}
		buff[1] = p;
		if (buff[1] == null) {
			return;
		}
		path.quadTo(buff[0].getX(), buff[0].getY(), buff[1].getX(),
				buff[1].getY());
		buff[0] = null;
		buff[1] = null;
	}

	/**
	 * Converts the vertices to a bezier interpolated path.
	 * 
	 * @param points
	 *            The vertices.
	 * @return The path.
	 */
	public static GeneralPath convertToBezierPath(final Point2D[] points) {
		final GeneralPath res = new GeneralPath();
		final RoundBorder rb = new RoundBorder(points, false);
		final int len = rb.getSize();
		Point2D first = null;
		for (int i = 0; i < len; ++i) {
			final Point2D[] vertices = rb.getOrthoBezier(i, DEFAULT_BORDER);
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
			res.curveTo(s.getX(), s.getY(), s.getX(), s.getY(), e.getX(),
					e.getY());
		}
		if (first != null) {
			res.lineTo(first.getX(), first.getY());
		}
		return res;
	}

	/**
	 * Converts vertices to a shape with the method given by
	 * {@link #DEFAULT_METHOD}.
	 * 
	 * @param points
	 *            The vertices.
	 * @return The Shape.
	 */
	public static Shape convertToShape(final Point2D[] points) {
		switch (DEFAULT_METHOD) {
		case POLY:
			return convertToPolygon(points);
		case QUAD:
			return convertToQuadPath(points);
		case BEZIER:
			return convertToBezierPath(points);
		default:
			throw new InternalError();
		}
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
