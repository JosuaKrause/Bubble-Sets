package setvis.gui;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import setvis.SetOutline;

/**
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public abstract class AbstractOutlineConfiguration extends JPanel {

	private static final long serialVersionUID = -5356999538568674877L;

	private final OutlineType type;

	protected final Canvas canvas;

	private SetOutline outline;

	public AbstractOutlineConfiguration(final Canvas canvas,
			final OutlineType type) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.canvas = canvas;
		this.type = type;
	}

	public abstract void fillContent();

	/**
	 * Adds a series of components in a horizontal manner. This method may not
	 * be called outside the constructor.
	 * 
	 * @param comps
	 *            The components.
	 */
	protected void addHor(final JComponent... comps) {
		final JPanel hor = new JPanel();
		hor.setLayout(new BoxLayout(hor, BoxLayout.X_AXIS));
		boolean first = true;
		for (final JComponent c : comps) {
			if (first) {
				first = false;
			} else {
				hor.add(Box.createRigidArea(new Dimension(5, 5)));
			}
			hor.add(c);
		}
		add(hor);
	}

	public void changed() {
		canvas.fireCanvasChange(CanvasListener.GENERATORS);
	}

	public void setOutline(final SetOutline outline) {
		this.outline = outline;
	}

	public SetOutline getOutline() {
		return outline;
	}

	public OutlineType getType() {
		return type;
	}

	public abstract void somethingChanged();

}
