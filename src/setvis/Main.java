/**
 * 
 */
package setvis;

import setvis.bubbleset.BubbleSet;
import setvis.gui.Canvas;
import setvis.gui.MainWindow;
import setvis.shape.AbstractShapeCreator;
import setvis.shape.BezierShapeGenerator;

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
		final SetOutline setOutline = new BubbleSet();
		final AbstractShapeCreator shaper = new BezierShapeGenerator(setOutline);
		final MainWindow mw = new MainWindow(shaper);
		final Canvas canvas = mw.getCanvas();
		final double w = canvas.getCurrentItemWidth();
		final double h = canvas.getCurrentItemHeight();
		canvas.addItem(0, 163.0, 141.0, w, h);
		canvas.addItem(0, 130.0, 306.0, w, h);
		canvas.addItem(0, 279.0, 256.0, w, h);
		canvas.addGroup();
		canvas.addItem(1, 162.0, 219.0, w, h);
		canvas.addItem(1, 373.0, 194.0, w, h);
		canvas.addItem(1, 288.0, 328.0, w, h);
		canvas.setCurrentGroup(0);
		mw.setVisible(true);
	}
}
