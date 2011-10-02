package setvis.gui;

import java.util.HashMap;
import java.util.Map;

import setvis.shape.AbstractShapeGenerator;
import setvis.shape.BSplineShapeGenerator;
import setvis.shape.BezierShapeGenerator;
import setvis.shape.PolygonShapeGenerator;

/**
 * Enumerates the types of shape generators.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public enum ShapeType {

	/** Direct line shape generator. */
	LINES("Direct Lines", PolygonShapeGenerator.class),

	/** Bezier line shape generator. */
	BEZIER("Bezier Curves", BezierShapeGenerator.class),

	/** B-Splines shape generator. */
	BSPLINE("B-Splines", BSplineShapeGenerator.class),

	;

	/** The associated class. */
	private final Class<? extends AbstractShapeGenerator> assocClass;

	/** A readable name. */
	private final String name;

	/**
	 * Creates an outline type.
	 * 
	 * @param name
	 *            A readable name.
	 * @param assocClass
	 *            The associated class. It is used to reverse lookup types from
	 *            {@link AbstractShapeGenerator} objects.
	 */
	private ShapeType(final String name,
			final Class<? extends AbstractShapeGenerator> assocClass) {
		this.name = name;
		this.assocClass = assocClass;
	}

	@Override
	public String toString() {
		return name;
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
	public static ShapeType getFor(final AbstractShapeGenerator shaper) {
		return MAP.get(shaper.getClass());
	}

}
