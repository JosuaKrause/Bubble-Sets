package setvis;

import java.util.HashMap;
import java.util.Map;

import setvis.bubbleset.BubbleSet;
import setvis.ch.ConvexHull;

/**
 * Enumerates the types of outline generators.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 * 
 */
public enum OutlineType {

	/** The bubble set generator. */
	BUBBLE_SETS(BubbleSet.class),

	/** A convex hull generator. */
	CONVEX_HULL(ConvexHull.class),

	;

	/** The associated class. */
	private final Class<? extends SetOutline> assocClass;

	/**
	 * Creates an outline type.
	 * 
	 * @param assocClass
	 *            The associated class. It is used to reverse lookup types from
	 *            {@link SetOutline} objects.
	 */
	private OutlineType(final Class<? extends SetOutline> assocClass) {
		this.assocClass = assocClass;
	}

	/** The lookup map. */
	private static final Map<Class<?>, OutlineType> MAP = new HashMap<Class<?>, OutlineType>();

	/** Initializing the map. */
	static {
		for (final OutlineType type : values()) {
			MAP.put(type.assocClass, type);
		}
	}

	/**
	 * Finds the type of the given object.
	 * 
	 * @param set
	 *            The outline object to find the type for.
	 * @return The type of the given object.
	 */
	public static OutlineType getFor(final SetOutline set) {
		return MAP.get(set.getClass());
	}

}
