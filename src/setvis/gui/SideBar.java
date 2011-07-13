/**
 * 
 */
package setvis.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import setvis.OutlineType;
import setvis.SetOutline;
import setvis.ShapeType;
import setvis.shape.AbstractShapeCreator;

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
	private final Canvas canvas;

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

	/** The canvas list model. */
	private final CanvasListModel listModel;

	/** The groups list. */
	private final JList list;

	/** The text field for the rectangle width. */
	private final JTextField width;

	/** The text field for the rectangle height. */
	private final JTextField height;

	/** The box for choosing the outline generator. */
	private final JComboBox outlineBox;

	/** The box for choosing the shape generator. */
	private final JComboBox shapeBox;

	/** The constraints for the layout. */
	private final GridBagConstraints constraint;

	/**
	 * Creates a side bar for the given {@link Canvas}.
	 * 
	 * @param cc
	 *            The {@link Canvas}.
	 */
	public SideBar(final Canvas cc) {
		canvas = cc;
		setLayout(new GridBagLayout());
		constraint = new GridBagConstraints();
		constraint.gridx = 0;
		constraint.fill = GridBagConstraints.BOTH;
		// determine the current status
		final AbstractShapeCreator asc = cc.getShapeCreator();
		final SetOutline so = asc.getSetOutline();
		// the combo-box for outlines
		outlineBox = new JComboBox(OutlineType.values());
		outlineBox.setSelectedItem(OutlineType.getFor(so));
		addHor(new JLabel("Outline:"), outlineBox);
		// the combo-box for shape creators
		shapeBox = new JComboBox(ShapeType.values());
		shapeBox.setSelectedItem(ShapeType.getFor(asc));
		addHor(new JLabel("Shape:"), shapeBox);
		// interaction for the shape and outline combo-boxes
		final ActionListener shapeOutlineListener = new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				cc.setShapeAndOutline((OutlineType) outlineBox
						.getSelectedItem(), (ShapeType) shapeBox
						.getSelectedItem());
			}

		};
		outlineBox.addActionListener(shapeOutlineListener);
		shapeBox.addActionListener(shapeOutlineListener);
		// the groups list
		listModel = new CanvasListModel();
		list = new JList(listModel);
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
		add(new JScrollPane(list), constraint);
		// adding and removing groups
		final JButton addGroup = new JButton(new AbstractAction("+") {

			// serial version uid
			private static final long serialVersionUID = -7674517069323059813L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				canvas.addGroup();
				// select the newly created group
				list.setSelectedIndex(canvas.getGroupCount() - 1);
			}

		});
		final JButton remGroup = new JButton(new AbstractAction("-") {

			// serial version uid
			private static final long serialVersionUID = 45084574338099392L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				canvas.removeSelectedGroup();
			}

		});
		constraint.fill = GridBagConstraints.VERTICAL;
		addHor(addGroup, remGroup);
		constraint.fill = GridBagConstraints.BOTH;
		// add empty filling space
		final JPanel empty = new JPanel();
		constraint.weighty = 1.0;
		// empty.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		add(empty, constraint);
		constraint.weighty = 0.0;
		// the rectangle width and height input fields
		width = new JTextField(4);
		height = new JTextField(4);
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
		add(hor, constraint);
	}

	/**
	 * Is called when something on the outside has changed.
	 * 
	 * @param changes
	 *            The type of changes as defined in
	 *            {@link CanvasListener#canvasChanged(int)}.
	 */
	public void somethingChanged(final int changes) {
		if ((changes & CanvasListener.GROUPS) != 0) {
			list.setSelectedIndex(canvas.getCurrentGroup());
			listModel.invalidate();
		}
		if ((changes & CanvasListener.GENERATORS) != 0) {
			final AbstractShapeCreator asc = canvas.getShapeCreator();
			final SetOutline so = asc.getSetOutline();
			outlineBox.setSelectedItem(OutlineType.getFor(so));
			shapeBox.setSelectedItem(ShapeType.getFor(asc));
		}
		if ((changes & CanvasListener.RECT_SIZE) != 0) {
			final int cw = canvas.getCurrentItemWidth();
			final String tw = "" + cw;
			try {
				final int w = Integer.parseInt(width.getText());
				if (cw != w) {
					width.setText(tw);
				}
			} catch (final NumberFormatException e) {
				width.setText(tw);
			}
			final int ch = canvas.getCurrentItemHeight();
			final String th = "" + ch;
			try {
				final int h = Integer.parseInt(height.getText());
				if (ch != h) {
					height.setText(th);
				}
			} catch (final NumberFormatException e) {
				height.setText(th);
			}
		}
	}

}
