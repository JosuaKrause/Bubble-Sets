/**
 * 
 */
package setvis.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * The side bar for controlling the input to the {@link CanvasComponent}.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public class SideBar extends JPanel {

	// serial version uid
	private static final long serialVersionUID = 6967000092103706967L;

	/**
	 * The color to indicate an invalid number.
	 */
	private static final Color ERROR = new Color(Color.HSBtoRGB(0f, 0.6f, 1f));

	/**
	 * The normal input background.
	 */
	private static final Color NORMAL = Color.WHITE;

	/**
	 * The minimal rectangle size.
	 */
	private static final int MIN_SIZE = 10;

	/**
	 * The underlying canvas.
	 */
	private final CanvasComponent canvas;

	/**
	 * A simple list model for maintaining groups of the {@link CanvasComponent}
	 * .
	 * 
	 * @author Joschi <josua.krause@googlemail.com>
	 * 
	 */
	private class CanvasListModel extends AbstractListModel {

		// serial version uid
		private static final long serialVersionUID = 3431270899264849193L;

		@Override
		public Object getElementAt(final int index) {
			return "Group " + index;
		}

		@Override
		public int getSize() {
			return canvas.getGroupCount();
		}

		/**
		 * Propagates that something in the list has changed.
		 */
		public void invalidate() {
			fireContentsChanged(this, 0, canvas.getGroupCount());
		}

	}

	/**
	 * Creates a side bar for the given {@link CanvasComponent}.
	 * 
	 * @param cc
	 *            The {@link CanvasComponent}.
	 */
	public SideBar(final CanvasComponent cc) {
		canvas = cc;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		final CanvasListModel listModel = new CanvasListModel();
		// the groups list
		final JList list = new JList(listModel);
		list.setSelectedIndex(cc.getCurrentGroup());
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setLayoutOrientation(JList.VERTICAL);
		list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(final ListSelectionEvent e) {
				// selection of groups
				cc.setCurrentGroup(list.getSelectedIndex());
			}

		});
		add(new JScrollPane(list));
		// adding and removing groups
		final JButton addGroup = new JButton(new AbstractAction("+") {

			// serial version uid
			private static final long serialVersionUID = -7674517069323059813L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				canvas.addGroup();
				// select the newly created group
				list.setSelectedIndex(canvas.getGroupCount() - 1);
				listModel.invalidate();
			}

		});
		final JButton remGroup = new JButton(new AbstractAction("-") {

			// serial version uid
			private static final long serialVersionUID = 45084574338099392L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				canvas.removeSelectedGroup();
				// the current group may have changed
				list.setSelectedIndex(canvas.getCurrentGroup());
				listModel.invalidate();
			}

		});
		addHor(addGroup, remGroup);
		// the rectangle width and height input fields
		final JTextField width = new JTextField(4);
		final JTextField height = new JTextField(4);
		width.setMaximumSize(width.getPreferredSize());
		height.setMaximumSize(height.getPreferredSize());
		width.setText("" + canvas.getCurrentItemWidth());
		height.setText("" + canvas.getCurrentItemHeight());
		final ActionListener bounds = new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent ae) {
				int w = 0;
				try {
					width.setBackground(NORMAL);
					w = Integer.parseInt(width.getText());
				} catch (final NumberFormatException e) {
					// nothing to do
				}
				int h = 0;
				try {
					height.setBackground(NORMAL);
					h = Integer.parseInt(height.getText());
				} catch (final NumberFormatException e) {
					// nothing to do
				}
				if (w < MIN_SIZE) {
					width.setBackground(ERROR);
				} else {
					canvas.setCurrentItemWidth(w);
				}
				if (h < MIN_SIZE) {
					height.setBackground(ERROR);
				} else {
					canvas.setCurrentItemHeight(h);
				}
			}
		};
		width.addActionListener(bounds);
		height.addActionListener(bounds);
		add(Box.createVerticalGlue());
		addHor(new JLabel("width:"), width);
		addHor(new JLabel("height:"), height);
	}

	/**
	 * Adds a series of components in a horizontal manner.
	 * 
	 * @param comps
	 *            The components.
	 */
	private void addHor(final JComponent... comps) {
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
}
