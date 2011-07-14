package setvis.gui;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import setvis.SetOutline;
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
	BUBBLE_SETS("Bubble Sets", BubbleSet.class),

	/** A convex hull generator. */
	CONVEX_HULL("Convex Hull", ConvexHull.class),

	;

	/** A readable name. */
	private String name;

	/** The associated class. */
	private final Class<? extends SetOutline> assocClass;

	/**
	 * Creates an outline type.
	 * 
	 * @param name
	 *            A readable name.
	 * @param assocClass
	 *            The associated class. It is used to reverse lookup types from
	 *            {@link SetOutline} objects.
	 */
	private OutlineType(final String name,
			final Class<? extends SetOutline> assocClass) {
		this.name = name;
		this.assocClass = assocClass;
	}

	@Override
	public String toString() {
		return name;
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

	/**
	 * Creates an outline configuration panel for the given canvas.
	 * 
	 * @param canvas
	 *            The canvas.
	 * @return The panel or <code>null</code> if no panel is needed.
	 */
	public AbstractOutlineConfiguration createOutlineConfiguration(
			final Canvas canvas) {
		switch (this) {
		case BUBBLE_SETS:
			return createBubbleSetConfiguration(canvas);
		default:
			break;
		}
		return null;
	}

	/**
	 * Creates a bubble set configuration panel for the given canvas.
	 * 
	 * @param canvas
	 *            The canvas.
	 * @return The bubble set configuration panel.
	 */
	private AbstractOutlineConfiguration createBubbleSetConfiguration(
			final Canvas canvas) {
		return new AbstractOutlineConfiguration(canvas, this) {

			private static final long serialVersionUID = -4099593260786691472L;

			private JSlider pixelGroup;

			private JSlider skip;

			private JLabel pixelGroupLabel;

			private JLabel skipLabel;

			@Override
			protected void fillContent() {
				pixelGroup = new JSlider(1, 10);
				skip = new JSlider(1, 30);
				pixelGroupLabel = new JLabel();
				skipLabel = new JLabel();
				final ChangeListener change = new ChangeListener() {

					@Override
					public void stateChanged(final ChangeEvent e) {
						final BubbleSet bubble = (BubbleSet) getOutline();
						bubble.setSkip(skip.getValue());
						bubble.setPixelGroup(pixelGroup.getValue());
						changed();
					}
				};
				skip.addChangeListener(change);
				pixelGroup.addChangeListener(change);
				addHor(new JLabel("Grid size:"), pixelGroup, pixelGroupLabel);
				addHor(new JLabel("Skip points:"), skip, skipLabel);
			}

			@Override
			public void somethingChanged() {
				final BubbleSet bubble = (BubbleSet) getOutline();
				final int s = bubble.getSkip();
				skip.setValue(s);
				skipLabel.setText("" + s);
				final int pg = bubble.getPixelGroup();
				pixelGroup.setValue(pg);
				pixelGroupLabel.setText("" + pg);
			}
		};
	}
}
