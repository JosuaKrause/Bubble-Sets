/**
 * 
 */
package setvis.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

import setvis.OutlineType;
import setvis.SetOutline;
import setvis.ShapeType;
import setvis.bubbleset.BubbleSet;
import setvis.ch.ConvexHull;
import setvis.shape.AbstractShapeCreator;
import setvis.shape.BezierShapeGenerator;
import setvis.shape.PolygonShapeCreator;

/**
 * The component for maintaining and displaying the rectangles.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public class CanvasComponent extends JComponent implements Canvas {

	/**
	 * A class to identify a given rectangle.
	 * 
	 * @author Joschi <josua.krause@googlemail.com>
	 * 
	 */
	public class Position {
		/**
		 * The group in which the rectangle is.
		 */
		public final int groupID;

		/**
		 * The rectangle. This is the actual reference, so that modifying this
		 * rectangle results in modifying the original rectangle.
		 */
		public final Rectangle2D rect;

		/**
		 * Generates a Position object.
		 * 
		 * @param groupID
		 *            The group id.
		 * @param rect
		 *            The reference to the rectangle.
		 */
		private Position(final int groupID, final Rectangle2D rect) {
			this.groupID = groupID;
			this.rect = rect;
		}
	}

	// serial version uid
	private static final long serialVersionUID = -310139729093190621L;

	/**
	 * The parent of this component.
	 */
	private final MainWindow parent;

	/**
	 * A list of all groups containing lists of the group members.
	 */
	private final List<List<Rectangle2D>> items;

	/**
	 * The mouse and mouse motion listener for the interaction.
	 */
	private final MouseAdapter mouse = new MouseAdapter() {

		@Override
		public void mouseClicked(final MouseEvent e) {
			final double x = e.getX();
			final double y = e.getY();
			switch (e.getButton()) {
			case MouseEvent.BUTTON1:
				// left click -> shaper item
				if (getItemsAt(x, y).isEmpty()) {
					addItem(curItemGroup, x, y, curItemWidth, curItemHeight);
				}
				break;
			case MouseEvent.BUTTON3:
				// right click -> remove items
				removeItem(x, y);
				break;
			}
			invalidateOutlines();
		}

		// the last mouse position
		private Point p = null;

		// the items to move or an empty list if the background is moved
		private List<Position> items = null;

		@Override
		public void mousePressed(final MouseEvent e) {
			// only left click dragging counts
			p = e.getButton() == MouseEvent.BUTTON1 ? e.getPoint() : null;
			if (p == null) {
				return;
			}
			items = getItemsAt(p.x, p.y);
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			if (p == null) {
				return;
			}
			final Point n = e.getPoint();
			move(p, n);
			p = n;
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			final Point n = e.getPoint();
			if (p == null || p.equals(n)) {
				return;
			}
			move(p, n);
			p = null;
		}

		/**
		 * Moves {@link #items} by the difference of the given positions.
		 * 
		 * @param from
		 *            The previous position.
		 * @param to
		 *            The position.
		 */
		private void move(final Point from, final Point to) {
			final double dx = to.x - from.x;
			final double dy = to.y - from.y;
			if (items.isEmpty()) {
				translateScene(dx, dy);
			} else {
				for (final Position p : items) {
					moveItem(p, dx, dy);
				}
				invalidateOutlines();
			}
			repaint();
		}

	};

	/**
	 * The generator of the shapes of the sets.
	 */
	private AbstractShapeCreator shaper;

	/**
	 * The cached shapes of the outlines.
	 */
	private Shape[] groupShapes;

	/**
	 * The current group new rectangles will be added to.
	 */
	private int curItemGroup;

	/**
	 * The width new rectangles will get.
	 */
	private int curItemWidth;

	/**
	 * The height new rectangles will get.
	 */
	private int curItemHeight;

	/**
	 * The scene translation on the x axis.
	 */
	private double dx;

	/**
	 * The scene translation on the y axis.
	 */
	private double dy;

	/**
	 * Creates a canvas component.
	 * 
	 * @param parent
	 *            The parent of this component.
	 * 
	 * @param shaper
	 *            The shape generator for the outlines.
	 */
	public CanvasComponent(final MainWindow parent,
			final AbstractShapeCreator shaper) {
		this.parent = parent;
		this.shaper = shaper;
		items = new ArrayList<List<Rectangle2D>>();
		addGroup();
		dx = 0.0;
		dy = 0.0;
		curItemGroup = 0;
		curItemWidth = 50;
		curItemHeight = 30;
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	@Override
	public void setShapeAndOutline(final OutlineType outlineType,
			final ShapeType shapeType) {
		final SetOutline oldOut = shaper.getSetOutline();
		final OutlineType oldType = OutlineType.getFor(oldOut);
		SetOutline outline;
		if (oldType == outlineType) {
			final ShapeType oldShapeType = ShapeType.getFor(shaper);
			if (oldShapeType == shapeType) {
				return;
			}
			outline = oldOut;
		} else {
			switch (outlineType) {
			case BUBBLE_SETS:
				// TODO: configurable
				outline = new BubbleSet();
				break;
			case CONVEX_HULL:
				outline = new ConvexHull();
				break;
			default:
				throw new InternalError("" + outlineType);
			}
		}
		AbstractShapeCreator asc;
		switch (shapeType) {
		case LINES:
			asc = new PolygonShapeCreator(outline);
			break;
		case BEZIER:
			asc = new BezierShapeGenerator(outline);
			break;
		default:
			throw new InternalError("" + shapeType);
		}
		setShapeCreator(asc);
	}

	@Override
	public void setShapeCreator(final AbstractShapeCreator shaper) {
		this.shaper = shaper;
		invalidateOutlines();
	}

	@Override
	public AbstractShapeCreator getShapeCreator() {
		return shaper;
	}

	@Override
	public void translateScene(final double dx, final double dy) {
		this.dx += dx;
		this.dy += dy;
	}

	/**
	 * Signalizes that something has changed. This results in clearing the
	 * outline cache, notifying the parent and a call to {@link #repaint()}.
	 */
	protected void invalidateOutlines() {
		groupShapes = null;
		parent.canvasChanged();
		repaint();
	}

	@Override
	public void addGroup() {
		curItemGroup = items.size();
		items.add(new LinkedList<Rectangle2D>());
		invalidateOutlines();
	}

	@Override
	public void removeLastGroup() {
		final int last = items.size() - 1;
		if (last == 0) {
			return;
		}
		items.remove(last);
		if (curItemGroup == last) {
			curItemGroup = 0;
		}
		invalidateOutlines();
	}

	@Override
	public void removeSelectedGroup() {
		if (items.size() <= 1) {
			return;
		}
		items.remove(curItemGroup);
		--curItemGroup;
		if (curItemGroup < 0) {
			curItemGroup = 0;
		}
		invalidateOutlines();
	}

	@Override
	public void setCurrentGroup(final int curItemGroup) {
		this.curItemGroup = curItemGroup;
	}

	@Override
	public int getCurrentGroup() {
		return curItemGroup;
	}

	@Override
	public int getGroupCount() {
		return items.size();
	}

	@Override
	public void setCurrentItemWidth(final int curItemWidth) {
		this.curItemWidth = curItemWidth;
	}

	@Override
	public int getCurrentItemWidth() {
		return curItemWidth;
	}

	@Override
	public void setCurrentItemHeight(final int curItemHeight) {
		this.curItemHeight = curItemHeight;
	}

	@Override
	public int getCurrentItemHeight() {
		return curItemHeight;
	}

	@Override
	public void addItem(final int groupID, final double tx, final double ty,
			final double width, final double height) {
		final double x = tx - dx;
		final double y = ty - dy;
		final List<Rectangle2D> group = items.get(groupID);
		group.add(new Rectangle2D.Double(x - width * 0.5, y - height * 0.5,
				width, height));
	}

	@Override
	public List<Position> getItemsAt(final double tx, final double ty) {
		final double x = tx - dx;
		final double y = ty - dy;
		final List<Position> res = new LinkedList<Position>();
		int groupID = 0;
		for (final List<Rectangle2D> group : items) {
			for (final Rectangle2D r : group.toArray(new Rectangle2D[group
					.size()])) {
				if (r.contains(x, y)) {
					res.add(new Position(groupID, r));
				}
			}
			++groupID;
		}
		return res;
	}

	@Override
	public void removeItem(final double x, final double y) {
		final List<Position> pos = getItemsAt(x, y);
		for (final Position p : pos) {
			final List<Rectangle2D> group = items.get(p.groupID);
			group.remove(p.rect);
		}
	}

	@Override
	public void moveItem(final Position pos, final double dx, final double dy) {
		final Rectangle2D r = pos.rect;
		r.setRect(r.getMinX() + dx, r.getMinY() + dy, r.getWidth(),
				r.getHeight());
	}

	@Override
	public void paint(final Graphics gfx) {
		final Graphics2D g2d = (Graphics2D) gfx;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		final int count = getGroupCount();
		if (groupShapes == null) {
			// the cache needs to be recreated
			groupShapes = shaper.createShapesForLists(items);
		}
		// draw background
		g2d.setColor(Color.WHITE);
		final Rectangle2D r = getBounds();
		g2d.fillRect(0, 0, (int) r.getWidth() - 1, (int) r.getHeight() - 1);
		// translate the scene
		g2d.translate(dx, dy);
		final float step = 1f / count;
		float hue = 0f;
		int pos = 0;
		// draw the outlines
		for (int i = 0; i < items.size(); ++i) {
			final Color c = new Color(Color.HSBtoRGB(hue, 0.7f, 1f));
			final Color t = new Color(~0x80000000 & c.getRGB(), true);
			final Shape gs = groupShapes[pos];
			if (gs != null) {
				g2d.setColor(t);
				g2d.fill(gs);
				g2d.setColor(c);
				g2d.draw(gs);
			}
			hue += step;
			++pos;
		}
		hue = 0f;
		pos = 0;
		// draw the items
		for (final List<Rectangle2D> group : items) {
			final Color c = new Color(Color.HSBtoRGB(hue, 0.7f, 1f));
			g2d.setColor(c);
			for (final Rectangle2D item : group) {
				final Graphics2D g = (Graphics2D) g2d.create();
				final int w = (int) item.getWidth();
				final int h = (int) item.getHeight();
				g.translate(item.getMinX(), item.getMinY());
				g.fillRect(0, 0, w, h);
				g.setColor(Color.BLACK);
				g.drawRect(0, 0, w, h);
				g.dispose();
			}
			hue += step;
			++pos;
		}
	}

}
