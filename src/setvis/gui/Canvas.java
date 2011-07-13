package setvis.gui;

import java.util.List;

import setvis.OutlineType;
import setvis.ShapeType;
import setvis.gui.CanvasComponent.Position;
import setvis.shape.AbstractShapeCreator;

/**
 * Canvas is a direct interface to the visible canvas. The complete interaction
 * can be done here.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public interface Canvas {

	/**
	 * Sets the shape and outline generator by its type.
	 * 
	 * @param outlineType
	 *            The outline generator type.
	 * @param shapeType
	 *            The shape generator type.
	 */
	void setShapeAndOutline(final OutlineType outlineType,
			final ShapeType shapeType);

	/**
	 * @param shaper
	 *            Directly sets the shape generator.
	 */
	void setShapeCreator(final AbstractShapeCreator shaper);

	/**
	 * @return The current shape generator.
	 */
	AbstractShapeCreator getShapeCreator();

	/**
	 * Translates the whole scene.
	 * 
	 * @param dx
	 *            The translation on the x axis.
	 * @param dy
	 *            The translation on the y axis.
	 */
	void translateScene(final double dx, final double dy);

	/**
	 * Adds a new empty group.
	 */
	void addGroup();

	/**
	 * Removes the most recently added group.
	 */
	void removeLastGroup();

	/**
	 * Removes the group denoted by {@link #getCurrentGroup()}.
	 */
	void removeSelectedGroup();

	/**
	 * Sets the group to which newly created items will be added.
	 * 
	 * @param curItemGroup
	 *            The new group id.
	 */
	void setCurrentGroup(final int curItemGroup);

	/**
	 * @return The current group id. It determines the group of newly created
	 *         items.
	 */
	int getCurrentGroup();

	/**
	 * @return The number of distinct groups.
	 */
	int getGroupCount();

	/**
	 * Sets the width newly created items will get.
	 * 
	 * @param curItemWidth
	 *            The new width.
	 */
	void setCurrentItemWidth(final int curItemWidth);

	/**
	 * @return The width newly created items will get.
	 */
	int getCurrentItemWidth();

	/**
	 * Sets the height newly created items will get.
	 * 
	 * @param curItemHeight
	 *            The new height.
	 */
	void setCurrentItemHeight(final int curItemHeight);

	/**
	 * @return The height newly created items will get.
	 */
	int getCurrentItemHeight();

	/**
	 * Adds a new item to the canvas.
	 * 
	 * @param groupID
	 *            The group id.
	 * @param tx
	 *            The x position in component coordinates.
	 * @param ty
	 *            The y position in component coordinates.
	 * @param width
	 *            The width.
	 * @param height
	 *            The height.
	 */
	void addItem(final int groupID, final double tx, final double ty,
			final double width, final double height);

	/**
	 * Generates a list of all items at the position {@code (tx, ty)}.
	 * 
	 * @param tx
	 *            The x value in component coordinates.
	 * @param ty
	 *            The y value in component coordinates.
	 * @return A list of {@link Position}s.
	 */
	List<Position> getItemsAt(final double tx, final double ty);

	/**
	 * Removes all items at the given position.
	 * 
	 * @param x
	 *            The x value in component coordinates.
	 * @param y
	 *            The y value in component coordinates.
	 */
	void removeItem(final double x, final double y);

	/**
	 * Moves the item given by {@code pos}.
	 * 
	 * @param pos
	 *            The identifier for the item.
	 * @param dx
	 *            The translation on the x axis.
	 * @param dy
	 *            The translation on the y axis.
	 */
	void moveItem(final Position pos, final double dx, final double dy);

}