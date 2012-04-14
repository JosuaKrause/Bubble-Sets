/**
 * 
 */
package setvis.gui;

import setvis.SetOutline;
import setvis.shape.AbstractShapeGenerator;

/**
 * A canvas listener gets notified each time something changes on the canvas.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 */
public interface CanvasListener {

  /**
   * Indicates that one of the generators ({@link AbstractShapeGenerator} or
   * {@link SetOutline}) has changed.
   */
  int GENERATORS = 0x01;

  /**
   * Indicates that the translation of the scene representing the canvas has
   * changed.
   */
  int TRANSLATION = 0x02;

  /**
   * Indicates that the number or selection of the groups has changed.
   */
  int GROUPS = 0x04;

  /**
   * Indicates that an item has changed. Either it was created, deleted or
   * moved.
   */
  int ITEMS = 0x08;

  /**
   * Indicates that the current rectangle size has changed.
   */
  int RECT_SIZE = 0x10;

  /**
   * Indicates that the scene has to be redrawn.
   */
  int SCREEN = 0x20;

  /**
   * Indicates that information texts has been changed.
   */
  int TEXT = 0x40;

  /**
   * Indicates that all things may have changed.
   */
  int ALL = -1;

  /**
   * Is automatically called when the canvas changes. This method may be called
   * multiple times for the same event, so be sure to re-check if something
   * actually has been changed.
   * 
   * @param changed What parameter has been changed. This is an bitwise-or
   *          combination of the bit-masks defined in {@link CanvasListener} .
   */
  void canvasChanged(int changed);

}
