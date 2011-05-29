/**
 * 
 */
package setvis;

import setvis.ch.ConvexHull;
import setvis.gui.MainWindow;

/**
 * Starts the main application.
 * 
 * @author Joschi
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
		new MainWindow(setOutline).setVisible(true);
	}
}
