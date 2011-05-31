/**
 * 
 */
package setvis;

import setvis.ch.ConvexHull;
import setvis.gui.MainWindow;
import setvis.shape.AbstractShapeCreator;
import setvis.shape.PolygonShapeCreator;

/**
 * Starts the main application.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public final class Main {

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
		final SetOutline setOutline = new ConvexHull();
		final AbstractShapeCreator shaper = new PolygonShapeCreator(setOutline);
		new MainWindow(shaper).setVisible(true);
	}
}
