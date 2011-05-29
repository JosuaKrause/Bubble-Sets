/**
 * 
 */
package setvis.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;

import setvis.SetOutline;

/**
 * The main window of the application.
 * 
 * @author Joschi
 * 
 */
public class MainWindow extends JFrame {

	// serial version uid
	private static final long serialVersionUID = -7857037941409543268L;

	/**
	 * Creates the main window.
	 * 
	 * @param set
	 *            The outline generator.
	 */
	public MainWindow(final SetOutline set) {
		super("Set visualisation");
		final CanvasComponent canvas = new CanvasComponent(set);
		final JPanel pane = new JPanel(new BorderLayout());
		pane.add(canvas, BorderLayout.CENTER);
		pane.add(new SideBar(canvas), BorderLayout.EAST);
		add(pane);
		setPreferredSize(new Dimension(640, 480));
		pack();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
	}

}
