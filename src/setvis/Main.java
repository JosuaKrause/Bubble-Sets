/**
 * 
 */
package setvis;

import setvis.bubbleset.BubbleSet;
import setvis.gui.MainWindow;
import setvis.shape.AbstractShapeCreator;
import setvis.shape.BezierShapeGenerator;
import setvis.shape.PolygonShapeCreator;

/**
 * Starts the main application.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public final class Main {

	/**
	 * Whether to use a bezier shape renderer or one with direct lines.
	 */
	public static boolean useBezierShape = true;

	private Main() {
		// no constructor
	}

	/**
	 * Starts the main application.
	 * 
	 * @param args
	 *            Arguments are ignored.
	 */
	public static void main(final String[] args) {
		// using a simple convex hull
		final SetOutline setOutline = new BubbleSet();
		final AbstractShapeCreator shaper = useBezierShape ? new BezierShapeGenerator(
				setOutline) : new PolygonShapeCreator(setOutline);
		new MainWindow(shaper).setVisible(true);
	}
}
