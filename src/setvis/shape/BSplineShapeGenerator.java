package setvis.shape;

import java.awt.Shape;
import java.awt.geom.CubicCurve2D;
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
		super(outline);
	}

	@Override
	public Shape convertToShape(final Point2D[] points, final boolean closed) {
		new CubicCurve2D.Double(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2)
		// TODO Auto-generated method stub
		return null;
	}
}
