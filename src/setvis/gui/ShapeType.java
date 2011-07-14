package setvis.gui;

import java.util.HashMap;
import java.util.Map;

import setvis.shape.AbstractShapeCreator;
import setvis.shape.BezierShapeGenerator;
import setvis.shape.PolygonShapeCreator;

/**
 * Enumerates the types of shape generators.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public enum ShapeType {

	/** Direct line shape generator. */
	LINES(PolygonShapeCreator.class),

	/** Bezier line shape generator. */
	BEZIER(BezierShapeGenerator.class),

	;

	/** The associated class. */
	private final Class<? extends AbstractShapeCreator> assocClass;

	/**
	 * Creates an outline type.
	 * 
	 * @param assocClass
	 *            The associated class. It is used to reverse lookup types from
	 *            {@link AbstractShapeCreator} objects.
	 */
	private ShapeType(final Class<? extends AbstractShapeCreator> assocClass) {
		this.assocClass = assocClass;
	}

	/** The lookup map. */
	private static final Map<Class<?>, ShapeType> MAP = new HashMap<Class<?>, ShapeType>();

	/** Initializing the map. */
	static {
		for (final ShapeType type : values()) {
			MAP.put(type.assocClass, type);
		}
	}

	/**
	 * Finds the type of the given object.
	 * 
	 * @param shaper
	 *            The shape generator object to find the type for.
	 * @return The type of the given object.
	 */
	public static ShapeType getFor(final AbstractShapeCreator shaper) {
		return MAP.get(shaper.getClass());
	}

}
