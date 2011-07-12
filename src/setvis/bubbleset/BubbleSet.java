package setvis.bubbleset;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;

import setvis.SetOutline;
import sun.misc.Sort;

public class BubbleSet implements SetOutline {

	private enum Direction {
		N, S, E, W
	};

	private Direction direction = Direction.S;

	/**
	 * Merge consecutive edges resulting from edge routing if there is no
	 * obstacle blocking merge.
	 */
	public boolean doMerge = true;

	/**
	 * Whether to consider blocking items when discovering nearest neighbours
	 * for edge routing.
	 */
	public boolean considerBlockingItems = true;

	private static final String THRESHOLD = "aggregate_threshold";
	private static final String NEIF = "aggreagate_negativeEdgeInfluenceFactor";
	private static final String NIF = "aggregate_nodeInfluenceFactor";
	private static final String NNIF = "aggreagate_negativeNodeInfluenceFactor";
	private static final String EIF = "aggregate_edgeInfluenceFactor";

	private static final float MAX_LABEL_SIZE = 30f;
	private static final float MIN_LABEL_SIZE = 14f;

	// data column for storing the surface points
	private static final String SURFACE = "SURFACE";

	public static long time = 0;
	public static int renderings = 0;

	class MemberItem {
		Rectangle2D member;
		float[] centroidDistance;
		Deque<Line2D> virtualEdges;

		public double getX() {
			return member.getX();
		}

		public double getY() {
			return member.getY();
		}

		public Rectangle2D getBounds2D() {
			return member.getBounds2D();
		}
	}

	/** The maximum number of passes through all nodes to attempt edge rerouting */
	private static final int MAX_ROUTING_ITERATIONS = 100;

	/**
	 * The maximum number of passes of marching squares while trying to ensure
	 * connectedness
	 */
	private static final int MAX_MARCHING_ITERATIONS = 20;

	/**
	 * The number of smoothing iterations to calculate after initializing the
	 * surface.
	 */
	private int smoothingIterations = 0;

	// skip every N points in the marching squares when making the contour
	private int skip = 10;
	// look at super-pixel groupings of this size
	private int pixelGroup = 6; // 3

	/** the energy threshold for marching squares */
	private double threshold = 1;
	/**
	 * the radius for the contour around a single edge -- the point at which the
	 * energy is 1
	 */
	private final double edgeR0 = 30;
	/**
	 * the radius at which potential reaches zero -- the extent of energy
	 * contribution by the edge
	 */
	private final double edgeR1 = 60; // 100
	// (determined by quadratic equation to give the same maximum at distance 0
	// as the node relationship (15/50)
	// that is: 100^2/(100-30)^2 = 50^2/(50-15)^2

	/**
	 * the radius for the contour around a single node -- the point at which the
	 * energy is 1
	 */
	private final double nodeR0 = 20;// 15;
	/**
	 * the radius at which potential reaches zero -- the extent of energy
	 * contribution by the node
	 */
	private final double nodeR1 = 40;// 50

	/** the distance to morph energy around a node */
	private double morphBuffer = nodeR0;
	/** whether to perform edge routing around intersections */
	private boolean doMorph = true;
	/** whether to connect neighbours left to right or to centroid */
	private final boolean centroidSort = true;

	private double nodeInfluenceFactor = 1;
	private double edgeInfluenceFactor = 1;
	private double negativeNodeInfluenceFactor = -0.8;
	private double negativeEdgeInfluenceFactor = 0;

	/** Hull used to store coordinates during smoothing */
	private Point2D[] tempHull = new Point2D[0];

	/**
	 * Set the superpixel size (resolution) used to calculate set boundaries.
	 * Larger is faster but less accurate.
	 * 
	 * @param pixelGroup
	 */
	public void setPixelGroup(final int pixelGroup) {
		this.pixelGroup = pixelGroup;
	}

	/**
	 * Set the spacing between control points on the drawn bubble set boundary.
	 * 
	 * @param skip
	 */
	public void setSkip(final int skip) {
		this.skip = skip;
	}

	public void setMorphBuffer(final double morphBuffer) {
		this.morphBuffer = morphBuffer;
	}

	/**
	 * Set the number of smoothing iterations per frame.
	 */
	public void setSmoothing(final int smoothingIterations) {
		this.smoothingIterations = smoothingIterations;
	}

	/**
	 * Toggle whether to morph sets around intervening non-set items.
	 * 
	 * @param doMorph
	 */
	public void setDoMorph(final boolean doMorph) {
		this.doMorph = doMorph;
	}

	/**
	 * Calculate the Bubble Set using energy and marching squares with edge
	 * routing.
	 */
	@Override
	public Point2D[] createOutline(final Rectangle2D[] members,
			final Rectangle2D[] nonMembers) {
		final long localtime = System.currentTimeMillis();

		Rectangle2D bounds = null;

		final MemberItem[] memberItems = new MemberItem[members.length];
		for (int i = 0; i < members.length; i++) {
			memberItems[i].member = members[i];
		}

		// calculate and store virtual edges
		calculateVirtualEdges(memberItems, nonMembers);

		// cycle through members of aggregate adding to bounds of influence
		for (int memberIndex = 0; memberIndex < members.length; memberIndex++) {
			if (bounds == null) {
				// clone because we don't want to change bounds of items, but we
				// need to start with item bounds (not empty bounds because 0,0
				// may not be
				// in area of influence
				bounds = (Rectangle2D) memberItems[memberIndex].getBounds2D()
						.clone();
			} else {
				bounds.add(memberItems[memberIndex].getBounds2D());
			}

			// add the bounds of the virtual edges to the active area
			if (memberItems[memberIndex].virtualEdges != null) {
				final Deque<Line2D> virtualMemberEdges = memberItems[memberIndex].virtualEdges;
				final Iterator<Line2D> lines = virtualMemberEdges.iterator();
				while (lines.hasNext()) {
					bounds.add(lines.next().getBounds2D());
				}
			}
		}

		// bounds contains a rectangle with all the nodes in the aggregate
		// within it (convex hull)
		// expand bounds by the maximum radius on all sides
		bounds.setRect(bounds.getX() - Math.max(edgeR1, nodeR1) - morphBuffer,
				bounds.getY() - Math.max(edgeR1, nodeR1) - morphBuffer,
				bounds.getWidth() + 2 * Math.max(edgeR1, nodeR1) + 2
						* morphBuffer,
				bounds.getHeight() + 2 * Math.max(edgeR1, nodeR1) + 2
						* morphBuffer);
		final double[][] potentialArea = new double[(int) (Math.ceil(bounds
				.getWidth() / pixelGroup))][(int) (Math.ceil(bounds.getHeight()
				/ pixelGroup))];

		// estimate length of contour to be the perimeter of the rectangular
		// aggregate bounds (tested, it's a good approx)
		final ArrayList<Point2D> surface = new ArrayList<Point2D>(
				(int) bounds.getWidth() * 2 + (int) bounds.getHeight() * 2);

		// store defaults and adjust globals so that changes are visible to
		// calculateSurface method
		final double tempThreshold = threshold;
		final double tempNegativeNodeInfluenceFactor = negativeNodeInfluenceFactor;
		final double tempNegativeEdgeInfluenceFactor = negativeEdgeInfluenceFactor;
		final double tempNodeInfluenceFactor = nodeInfluenceFactor;
		final double tempEdgeInfluenceFactor = edgeInfluenceFactor;

		int iterations = 0;

		// add the aggregate and all it's members and virtual edges
		fillPotentialArea(members, nonMembers, potentialArea);

		// try to march, check if surface contains all items
		while ((!calculateSurface(surface, bounds, members, nonMembers,
				potentialArea)) && (iterations < MAX_MARCHING_ITERATIONS)) {
			surface.clear();
			iterations++;

			// reduce negative influences first; this will allow the surface to
			// pass without making it fatter all around (which raising the
			// threshold does)
			if (iterations <= MAX_MARCHING_ITERATIONS / 2) {
				threshold *= 0.95f;
				nodeInfluenceFactor *= 1.2;
				edgeInfluenceFactor *= 1.2;
				fillPotentialArea(bounds, members, nonMembers, potentialArea);
			}

			// after half the iterations, start increasing positive energy and
			// lowering the threshold

			if (iterations > MAX_MARCHING_ITERATIONS / 2) {
				if ((negativeEdgeInfluenceFactor != 0)
						|| (negativeNodeInfluenceFactor != 0)) {
					threshold *= 0.95f;
					negativeNodeInfluenceFactor *= 0.8;
					negativeEdgeInfluenceFactor *= 0.8;
					fillPotentialArea(bounds, members, nonMembers,
							potentialArea);
				}
			}
		}

		threshold = tempThreshold;
		negativeEdgeInfluenceFactor = tempNegativeEdgeInfluenceFactor;
		negativeNodeInfluenceFactor = tempNegativeNodeInfluenceFactor;
		nodeInfluenceFactor = tempNodeInfluenceFactor;
		edgeInfluenceFactor = tempEdgeInfluenceFactor;

		// finalize the surface by adding bounds to positions and set into the
		// aitem

		// start with global SKIP value, but decrease skip amount if there
		// aren't enough points in the surface
		int thisSkip = skip;
		// prepare viz attribute array
		int size = surface.size();

		if (thisSkip > 1) {
			size = surface.size() / thisSkip;
			// if we reduced too much (fewer than three points in reduced
			// surface) reduce skip and try again
			while ((size < 3) && (thisSkip > 1)) {
				thisSkip--;
				size = surface.size() / thisSkip;
			}
		}

		final float xcorner = (float) bounds.getX();
		final float ycorner = (float) bounds.getY();

		Point2D[] fhull = new Point2D[size];

		// copy hull values
		for (int i = 0, j = 0; j < size - 1; j++, i += thisSkip) {
			fhull[j] = new Point2D.Float((float) surface.get(i).getX()
					+ xcorner, (float) surface.get(i).getY() + ycorner);
		}

		fhull = adjacentPointsSmooth(fhull, size, smoothingIterations);

		return fhull;
	}

	/**
	 * Perform adjacent point smoothing on the curve. Geometric drop off.
	 * 
	 * http://en.wikipedia.org/wiki/Gaussian_smoothing
	 * 
	 * @param hull
	 *            a list of x,y values to be smoothed - x1, y1, x2, y2 etc.
	 * @param iterations
	 * @return smoothed coordinated
	 */
	private Point2D[] adjacentPointsSmooth(final Point2D[] hull,
			final int size, final int iterations) {
		if (tempHull.length < size) {
			tempHull = new Point2D[size];
		}
		System.arraycopy(hull, 0, tempHull, 0, size);

		for (int j = 0; j < iterations; j++) {

			// NOTE smoothing first and last points resulted in these points
			// crossing each other
			// first point
			// tempHull[0] = (hull[size-2]/2 + hull[0] + hull[2]/2) / 2f;
			// tempHull[1] = (hull[size-1]/2 + hull[1] + hull[3]/2) / 2f;

			for (int i = 1; i < (size - 1); i++) {
				tempHull[i].setLocation(hull[i - 1].getX() / 2 + hull[i].getX()
						+ hull[i + 1].getX() / 2 / 2f, hull[i - 1].getY() / 2
						+ hull[i].getY() + hull[i + 1].getY() / 2 / 2f);
			}

			// last point
			// tempHull[size-4] = (hull[size-4]/2 + hull[size-2] + hull[0]/2) /
			// 2f;
			// tempHull[size-3] = (hull[size-3]/2 + hull[size-1] + hull[1]/2) /
			// 2f;

			System.arraycopy(tempHull, 0, hull, 0, size);
		}
		return hull;
	}

	/**
	 * Fill the surface using marching squares, return true if and only if all
	 * items in the given aggregate are contained inside rectangle specified by
	 * the extents of the surface. This does not guarantee the surface will
	 * contain all items, but it is a fast approximation.
	 * 
	 * @param surface
	 *            the surface to fill
	 * @param bounds
	 *            the bounds of the space being calculated, in screen
	 *            coordinates
	 * @param aitem
	 *            the aggregate item for which a surface is being calculated
	 * @param potentialArea
	 *            the energy field corresponding to the given aggregate and
	 *            bounds
	 * @return true if and only if marching squares successfully found a surface
	 *         containing all elements in the aggregate
	 */
	public boolean calculateSurface(final ArrayList<Point2D> surface,
			final Rectangle2D bounds, final AggregateItem aitem,
			final double[][] potentialArea) {

		// find a first point on the contour
		boolean marched = false;
		// set starting direction for conditional states (6 & 9)
		direction = Direction.S;
		for (int x = 0; x < potentialArea.length && !marched; x++) {
			for (int y = 0; y < potentialArea[x].length && !marched; y++) {
				if (test(potentialArea[x][y])) {
					// check invalid state condition
					if (getState(potentialArea, x, y) != 15) {
						marched = march(surface, potentialArea, x, y);
					}
				}
			}
		}

		// if no surface could be found stop
		if (!marched) {
			return false;
		}

		final boolean[] containment = testContainment(surface, bounds, aitem);

		return containment[0];
	}

	/**
	 * Test containment of items in the bubble set.
	 * 
	 * @param surface
	 *            the points on the surface
	 * @param bounds
	 *            the bounds of influence used to calculate the surface
	 * @param aitem
	 *            the aggregate item to test
	 * @return an array where the first element indicates if the set contains
	 *         all required items and the second element indicates if the set
	 *         contains extra items
	 */
	public boolean[] testContainment(final ArrayList<Point2D> surface,
			final Rectangle2D bounds, final AggregateItem aitem) {
		// precise bounds checking
		// copy hull values
		final Path2D g = new Path2D.Double();
		// start with global SKIP value, but decrease skip amount if there
		// aren't enough points in the surface
		int thisSkip = skip;
		// prepare viz attribute array
		int size = surface.size();
		if (thisSkip > 1) {
			size = surface.size() / thisSkip;
			// if we reduced too much (fewer than three points in reduced
			// surface) reduce skip and try again
			while ((size < 3) && (thisSkip > 1)) {
				thisSkip--;
				size = surface.size() / thisSkip;
			}
		}

		final float xcorner = (float) bounds.getX();
		final float ycorner = (float) bounds.getY();

		// simulate the surface we will eventually draw, using straight segments
		// (approximate, but fast)
		for (int i = 0; i < size - 1; i++) {
			if (i == 0) {
				g.moveTo((float) surface.get(i * thisSkip).getX() + xcorner,
						(float) surface.get(i * thisSkip).getY() + ycorner);
			} else {
				g.lineTo((float) surface.get(i * thisSkip).getX() + xcorner,
						(float) surface.get(i * thisSkip).getY() + ycorner);
			}
		}

		g.closePath();

		boolean containsAll = true;
		boolean containsExtra = false;
		final Iterator items = m_vis.items(visibleNodePredicate);
		while (items.hasNext()) {
			final VisualItem item = (VisualItem) items.next();
			if (aitem.isHover()) {
				item.setHighlighted(false);
			}
			if (aitem.containsItem(item)) {
				// check rough bounds
				containsAll = (containsAll)
						&& (g.getBounds().contains(item.getBounds()
								.getCenterX(), item.getBounds().getCenterY()));
				// check precise bounds if rough passes
				containsAll = (containsAll)
						&& (g.contains(item.getBounds().getCenterX(), item
								.getBounds().getCenterY()));

			} else {
				// check rough bounds
				if (g.getBounds().contains(item.getBounds().getCenterX(),
						item.getBounds().getCenterY())) {
					// check precise bounds if rough passes
					if (g.contains(item.getBounds().getCenterX(), item
							.getBounds().getCenterY())) {
						if (aitem.isHover()) {
							item.setHighlighted(true);
						}
						containsExtra = true;
					}
				}
			}
		}
		return new boolean[] { containsAll, containsExtra };
	}

	/**
	 * Fill the given area with energy, with values modulated by the preset
	 * energy function parameters (radial extent, postive and negative
	 * influences for included and excluded nodes and edges).
	 * 
	 * @param activeArea
	 *            the screen coordinates of the region to fill
	 * @param aitem
	 *            the aggregate item to calculate energy for
	 * @param potentialArea
	 *            the energy field to fill in
	 */
	public void fillPotentialArea(final Rectangle2D activeArea,
			final AggregateItem aitem, final double[][] potentialArea) {
		double influenceFactor = 0;

		// add all positive energy (included items) first, as negative energy
		// morphing
		// requires all positives to be already set

		if (nodeInfluenceFactor != 0) {
			final Iterator aNodeItems = aitem.items(visibleNodePredicate);
			while (aNodeItems.hasNext()) {
				final VisualItem item = (VisualItem) aNodeItems.next();

				// add node energy
				influenceFactor = nodeInfluenceFactor;
				double a = 1 / (Math.pow(nodeR0 - nodeR1, 2));
				calculateRectangleInfluence(potentialArea, a * influenceFactor,
						nodeR1, new Rectangle2D.Double(item.getBounds().getX()
								- activeArea.getX(), item.getBounds().getY()
								- activeArea.getY(), item.getBounds()
								.getWidth(), item.getBounds().getHeight()));

				// add the influence of all the virtual edges
				final Deque<Line2D> scannedLines = (Deque<Line2D>) item
						.get(VIRTUAL_EDGES);
				influenceFactor = edgeInfluenceFactor;
				a = 1 / ((edgeR0 - edgeR1) * (edgeR0 - edgeR1));

				// only count distance from point on surface to nearest segment,
				// not all segments
				if (scannedLines.size() > 0) {
					calculateLinesInfluence(potentialArea, a * influenceFactor,
							edgeR1, scannedLines, activeArea);
				}
			} // end processing node items of this aggregate
		} // end processing positive node energy

		// calculate positive edge energy
		final Iterator aEdgeItems = aitem.items(VISIBLE_EDGE_PREDICATE);

		while (aEdgeItems.hasNext()) {
			final EdgeItem edge = (EdgeItem) aEdgeItems.next();
			if (edgeInfluenceFactor != 0) {
				influenceFactor = edgeInfluenceFactor;
				final double a = 1 / ((edgeR0 - edgeR1) * (edgeR0 - edgeR1));
				calculateLineInfluence(
						potentialArea,
						a * influenceFactor,
						edgeR1,
						new Line2D.Double(edge.getSourceItem().getX()
								- activeArea.getX(), edge.getSourceItem()
								.getY() - activeArea.getY(), edge
								.getTargetItem().getX() - activeArea.getX(),
								edge.getTargetItem().getY() - activeArea.getY()));
			}
		}

		// calculate negative energy contribution for all other visible items
		// within bounds
		if ((negativeNodeInfluenceFactor != 0)
				|| (negativeEdgeInfluenceFactor != 0)) {
			final Iterator sourceItems = m_vis.visibleItems(sourceGroup);
			while (sourceItems.hasNext()) {
				final VisualItem item = (VisualItem) sourceItems.next();
				// check for items in the aggregate
				if (aitem.containsItem(item)) {
					continue;
				} else {
					// if item is within influence bounds, add potential
					if (activeArea.intersects(item.getBounds())) {
						if ((item.isInGroup(nodeGroup))
								&& (negativeNodeInfluenceFactor != 0)) {
							// subtract influence
							influenceFactor = negativeNodeInfluenceFactor;
							final double a = 1 / Math.pow(nodeR0 - nodeR1, 2);
							calculateRectangleInfluence(
									potentialArea,
									a * influenceFactor,
									nodeR1,
									new Rectangle2D.Double(item.getBounds()
											.getX() - activeArea.getX(), item
											.getBounds().getY()
											- activeArea.getY(), item
											.getBounds().getWidth(), item
											.getBounds().getHeight()));
						}
						// subtract edges normally
						if ((item instanceof EdgeItem)
								&& (negativeEdgeInfluenceFactor != 0)) {
							influenceFactor = negativeEdgeInfluenceFactor;
							final EdgeItem edge = (EdgeItem) item;
							final double a = 1 / Math.pow(edgeR0 - edgeR1, 2);
							calculateLineInfluence(potentialArea, a
									* influenceFactor, edgeR1,
									new Line2D.Double(edge.getSourceItem()
											.getX() - activeArea.getX(), edge
											.getSourceItem().getY()
											- activeArea.getY(), edge
											.getTargetItem().getX()
											- activeArea.getX(), edge
											.getTargetItem().getY()
											- activeArea.getY()));
						}
					}
				}
			}
		}
	}

	/**
	 * Fill the given area with energy, with values modulated by the preset
	 * energy function parameters (radial extent, positive and negative
	 * influences for included and excluded nodes and edges).
	 * 
	 * @param activeArea
	 *            the screen coordinates of the region to fill
	 * @param aggregateDecorator
	 *            the aggregate decorator (e.g. label) to calculate energy for
	 * @param potentialArea
	 *            the energy field to fill in
	 */
	public void fillPotentialArea(final Rectangle2D activeArea,
			final DecoratorItem aggregateDecorator,
			final double[][] potentialArea) {
		double influenceFactor = 0;

		// add all positive energy (included items) first, as negative energy
		// morphing
		// requires all positives to be already set

		if (nodeInfluenceFactor != 0) {
			// add decorator energy
			influenceFactor = nodeInfluenceFactor;
			final double a = 1 / (Math.pow(nodeR0 - nodeR1, 2));

			final Area labelArea = new Area(
					((AbstractShapeRenderer) aggregateDecorator.getRenderer())
							.getShape(aggregateDecorator));
			calculateAreaInfluence(potentialArea, a * influenceFactor, nodeR1,
					labelArea, activeArea);

		} // end processing positive node energy
	}

	private Iterator getSortedNodeIterator(final AggregateItem aitem,
			final boolean centroidSort) {
		Sort positionSort = null;

		if (centroidSort) {
			// find the centroid
			double totalx = 0;
			double totaly = 0;
			double nodeCount = 0;

			Iterator aNodeItems = aitem.items(visibleNodePredicate);
			while (aNodeItems.hasNext()) {
				final VisualItem item = (VisualItem) aNodeItems.next();
				totalx += item.getX();
				totaly += item.getY();
				nodeCount++;
			}

			totalx /= nodeCount;
			totaly /= nodeCount;

			aNodeItems = aitem.items(visibleNodePredicate);

			// select centroid or ordered sort
			while (aNodeItems.hasNext()) {
				final VisualItem item = (VisualItem) aNodeItems.next();
				item.setDouble(
						CENTROID_DISTANCE,
						Math.sqrt(Math.pow(totalx - item.getX(), 2)
								+ Math.pow(totaly - item.getY(), 2)));
			}
			positionSort = Sort.parse(CENTROID_DISTANCE + " ASC");
		} else {
			positionSort = Sort.parse("_x ASC, _y ASC");
		}

		final TupleSet set = new DefaultTupleSet();
		final Iterator aItems = aitem.items();
		while (aItems.hasNext()) {
			set.addTuple((Tuple) aItems.next());
		}

		return set.tuples(visibleNodePredicate, positionSort);
	}

	/**
	 * Visit all items in the aggregate, connecting them to their nearest
	 * explicitly connected neighbour, or the best neighbour which is also in
	 * the set (see connectItem(aitem, item, visited) for detail on selection of
	 * best neighbour. Stores the connections in the VIRTUAL_EDGES deque for
	 * each item in the aggregate.
	 * 
	 * getAdjacentItem
	 * 
	 * @param aitem
	 */
	private void calculateVirtualEdges(final MemberItem[] memberItems,
			final Rectangle2D[] nonMembers) {
		final Deque<MemberItem> visited = new ArrayDeque<MemberItem>();

		final Iterator memberIterator = getSortedItemIterator(memberItems,
				centroidSort);

		while (memberIterator.hasNext()) {
			final MemberItem item = (MemberItem) memberIterator.next();
			final boolean itemConnected = false;

			/*
			 * // check for visible edge structure connecting this node to
			 * others if ((useStructuralEdges) && (item instanceof NodeItem)) {
			 * final NodeItem nodeItem = (NodeItem) item; final Iterator
			 * edgeIterator = aitem .items(VISIBLE_EDGE_PREDICATE); while
			 * (edgeIterator.hasNext()) { final EdgeItem edge = (EdgeItem)
			 * edgeIterator.next(); // check to see if this edge connects the
			 * current node if ((edge.getTargetItem() == item) ||
			 * (edge.getSourceItem() == nodeItem)) { if
			 * (aitem.containsItem(edge.getAdjacentItem(nodeItem))) { // edge
			 * connects this item to another item in // aggregate itemConnected
			 * = true; } } } }
			 */
			if (!itemConnected) {
				item.virtualEdges = connectItem(memberItems, nonMembers, item,
						visited);
			} else {
				item.virtualEdges = new ArrayDeque<Line2D>();
			}
			visited.add(item);
		}
	}

	/**
	 * Find the sequence of virtual edges which connect a given item to it's
	 * best unvisited neighbour within the set. Considerations for selection of
	 * best neighbour include distance and number of intervening non-set items
	 * on the straight line between the item and the candidate neighbour.
	 * 
	 * @param aitem
	 *            the set to search
	 * @param item
	 *            the item to find the best neighbour for
	 * @param visited
	 *            the already connected items within the set
	 * @return a deque containing the virtual edges which connect the item to
	 *         its best neighbour
	 */
	private Deque<Line2D> connectItem(final MemberItem[] memberItems,
			final Rectangle2D[] nonMembers, final MemberItem item,
			final Collection<MemberItem> visited) {
		MemberItem closestNeighbour = null;
		Deque<Line2D> scannedLines = new ArrayDeque<Line2D>();
		final Deque<Line2D> linesToCheck = new ArrayDeque<Line2D>();

		// if item is not connected within the aggregate by a visible edge
		// find the closest visited node neighbour in same aggregate
		closestNeighbour = null;

		final Iterator neighbourIterator = visited.iterator();
		double minLength = Double.MAX_VALUE;
		while (neighbourIterator.hasNext()) {
			double numberInterferenceItems = 0;
			final MemberItem neighbourItem = (MemberItem) neighbourIterator
					.next();
			final double distance = Point2D.distance(item.getX(), item.getY(),
					neighbourItem.getX(), neighbourItem.getY());

			// move virtual edges around nodes, not other edges (routing around
			// edges would be too difficult)

			// discover the nearest neighbour

			if (considerBlockingItems) {
				// augment distance by number of interfering items
				final Line2D completeLine = new Line2D.Double(item.getX(),
						item.getY(), neighbourItem.getX(), neighbourItem.getY());
				numberInterferenceItems = countInterferenceItems(nonMembers,
						memberItems, completeLine);
				// add all non interference edges
				/*
				 * if (numberInterferenceItems == 0) {
				 * scannedLines.push(completeLine); }
				 */
			}
			// TODO is there a better function to consider interference in
			// nearest-neighbour checking? This is hacky
			if ((distance * (numberInterferenceItems + 1) < minLength)) {
				closestNeighbour = neighbourItem;
				minLength = distance * (numberInterferenceItems + 1);
			}
		}

		// if there is a visited closest neighbour, add straight line between
		// them to the positive energy to
		// ensure connected clusters
		if ((closestNeighbour != null) && (edgeInfluenceFactor != 0)) {
			final Line2D completeLine = new Line2D.Double(item.getX(),
					item.getY(), closestNeighbour.getX(),
					closestNeighbour.getY());

			// route the edge around intersecting nodes not in set
			if (doMorph) {
				linesToCheck.push(completeLine);

				boolean hasIntersection = true;
				int iterations = 0;
				final Intersection[] intersections = new Intersection[4];
				int numIntersections = 0;
				while (hasIntersection && iterations < MAX_ROUTING_ITERATIONS) {
					hasIntersection = false;
					while (!hasIntersection && !linesToCheck.isEmpty()) {
						final Line2D line = linesToCheck.pop();
						// move virtual edges around nodes, not other edges
						// (routing around edges would be too difficult)

						// resolve intersections in order along edge
						final Rectangle2D closestItem = getCenterItem(
								nonMembers, memberItems, line);

						if (closestItem != null) {
							numIntersections = testIntersection(line,
									closestItem.getBounds(), intersections);

							if (numIntersections == 2) {
								double tempMorphBuffer = morphBuffer;

								Point2D movePoint = rerouteLine(line,
										closestItem.getBounds(),
										tempMorphBuffer, intersections, true);
								// test the movePoint already exists

								boolean foundFirst = (pointExists(movePoint,
										linesToCheck.iterator()) || pointExists(
										movePoint, scannedLines.iterator()));
								boolean pointInside = isPointInsideNode(
										movePoint,
										m_vis.getVisualGroup(sourceGroup)
												.tuples(visibleNodePredicate),
										aitem);

								// prefer first corner, even if buffer becomes
								// very small
								while ((!foundFirst) && (pointInside)
										&& (tempMorphBuffer >= 1)) {
									// try a smaller buffer
									tempMorphBuffer /= 1.5;
									movePoint = rerouteLine(line,
											closestItem.getBounds(),
											tempMorphBuffer, intersections,
											true);
									foundFirst = (pointExists(movePoint,
											linesToCheck.iterator()) || pointExists(
											movePoint, scannedLines.iterator()));
									pointInside = isPointInsideNode(
											movePoint,
											m_vis.getVisualGroup(sourceGroup)
													.tuples(visibleNodePredicate),
											aitem);
								}

								if ((movePoint != null) && (!foundFirst)
										&& (!pointInside)) {
									// add 2 rerouted lines to check
									linesToCheck.push(new Line2D.Double(line
											.getP1(), movePoint));
									linesToCheck.push(new Line2D.Double(
											movePoint, line.getP2()));
									// indicate intersection found
									hasIntersection = true;
								}

								// if we didn't find a valid point around the
								// first corner, try the second
								if (!hasIntersection) {
									tempMorphBuffer = morphBuffer;

									movePoint = rerouteLine(line,
											closestItem.getBounds(),
											tempMorphBuffer, intersections,
											false);
									boolean foundSecond = (pointExists(
											movePoint, linesToCheck.iterator()) || pointExists(
											movePoint, scannedLines.iterator()));
									pointInside = isPointInsideNode(
											movePoint,
											m_vis.getVisualGroup(sourceGroup)
													.tuples(visibleNodePredicate),
											aitem);

									// if both corners have been used, stop;
									// otherwise gradually reduce buffer and try
									// second corner
									while ((!foundSecond) && (pointInside)
											&& (tempMorphBuffer >= 1)) {
										// try a smaller buffer
										tempMorphBuffer /= 1.5;
										movePoint = rerouteLine(line,
												closestItem.getBounds(),
												tempMorphBuffer, intersections,
												false);
										foundSecond = (pointExists(movePoint,
												linesToCheck.iterator()) || pointExists(
												movePoint,
												scannedLines.iterator()));
										pointInside = isPointInsideNode(
												movePoint,
												m_vis.getVisualGroup(
														sourceGroup).tuples(
														visibleNodePredicate),
												aitem);
									}

									if ((movePoint != null) && (!foundSecond)) {
										// add 2 rerouted lines to check
										linesToCheck.push(new Line2D.Double(
												line.getP1(), movePoint));
										linesToCheck.push(new Line2D.Double(
												movePoint, line.getP2()));
										// indicate intersection found
										hasIntersection = true;
									}
								}
							}
						} // end check of closest item

						// no intersection found, mark this line as completed
						if (!hasIntersection) {
							scannedLines.push(line);
						}

						iterations++;
					} // end inner loop - out of lines or found an intersection
				} // end outer loop - no more intersections or out of iterations

				if (iterations >= MAX_ROUTING_ITERATIONS) {
					LOGGER.warning("Warning: exceeding routing iterations.");
				}

				// finalize any that were not rerouted (due to running out of
				// iterations) or if we aren't morphing
				while (!linesToCheck.isEmpty()) {
					scannedLines.push(linesToCheck.pop());
				}

				// try to merge consecutive lines if possible (see reviewer #3
				// comments regarding 'sideways 7' in video)
				if (doMerge) {
					while (!scannedLines.isEmpty()) {
						final Line2D line1 = scannedLines.pop();

						if (!scannedLines.isEmpty()) {
							final Line2D line2 = scannedLines.pop();

							final Line2D mergeLine = new Line2D.Double(
									line1.getP1(), line2.getP2());

							// move virtual edges around nodes, not other edges
							// (routing around edges would be too difficult)
							final Iterator interferenceItems = m_vis
									.getVisualGroup(sourceGroup).tuples(
											visibleNodePredicate);

							// resolve intersections in order along edge
							final VisualItem closestItem = getCenterItem(
									interferenceItems, aitem, mergeLine);

							// merge most recent line and previous line
							if (closestItem == null) {
								scannedLines.push(mergeLine);
							} else {
								linesToCheck.push(line1);
								scannedLines.push(line2);
							}
						} else {
							linesToCheck.push(line1);
						}
					}
					scannedLines = linesToCheck;
				}
			} else {
				scannedLines.push(completeLine);
			}
		}
		return scannedLines;
	}

	/**
	 * Check if a point is inside the rectangular bounds of any of the given
	 * tuples. Ignores members of the given aggregate item if one is specified.
	 * 
	 * @param movePoint
	 *            the point to check, in screen coordinates
	 * @param tuples
	 *            the tuples to scan
	 * @param exceptionItem
	 *            the aggregate whose items should be ignored
	 * @return true if this point is within the rectangular bounding box of at
	 *         least one tuple; false otherwise
	 */
	private boolean isPointInsideNode(final Point2D movePoint,
			final Iterator tuples, final AggregateItem exceptionItem) {
		while (tuples.hasNext()) {
			final VisualItem item = (VisualItem) tuples.next();
			if ((exceptionItem != null) && (exceptionItem.containsItem(item))) {
				continue;
			}
			if (item.getBounds().contains(movePoint)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether a given point is already an endpoint of any of the given
	 * lines.
	 * 
	 * @see MathLib#doublePointsEqual(Point2D, Point2D)
	 * 
	 * @param pointToCheck
	 *            the point to check
	 * @param linesIterator
	 *            the lines to scan the endpoints of
	 * @return true if the given point is the endpoint of at least one line;
	 *         false otherwise
	 */
	public boolean pointExists(final Point2D pointToCheck,
			final Iterator<Line2D> linesIterator) {
		boolean found = false;

		while ((linesIterator.hasNext()) && (!found)) {
			final Line2D checkEndPointsLine = (Line2D) linesIterator.next();
			if (MathLib.doublePointsEqual(checkEndPointsLine.getP1(),
					pointToCheck)) {
				found = true;
			}
			if (MathLib.doublePointsEqual(checkEndPointsLine.getP2(),
					pointToCheck)) {
				found = true;
			}
		}
		return found;
	}

	/**
	 * Add radial (circular) contribution of a point source to all points in a
	 * given area.
	 * 
	 * @param potentialArea
	 *            the area to fill with influence values
	 * @param factor
	 *            the influence factor of this point source
	 * @param pointx
	 *            the x-coordinate of the point source
	 * @param pointy
	 *            the y-coordinate of the point source
	 */
	public void calculatePointInfluence(final double[][] potentialArea,
			final double factor, final double r1, final double pointx,
			final double pointy) {
		double tempX = 0, tempY = 0, distance = 0;

		// for every point in potentialArea, calculate distance to center of
		// item and add influence
		for (int x = 0; x < potentialArea.length; x++) {
			for (int y = 0; y < potentialArea[x].length; y++) {
				tempX = x * pixelGroup;
				tempY = y * pixelGroup;
				distance = Point2D.distance(tempX, tempY, pointx, pointy);
				// only influence if less than r1
				if (distance < r1) {
					potentialArea[x][y] += factor * Math.pow(distance - r1, 2);
				}
			}
		}
	}

	/**
	 * Add a contribution of a line source to all points in a given area. For
	 * every point in the given area, the distance to the closest point on the
	 * line is calculated and this distance is input into the gradient influence
	 * function, then added to the potentialArea.
	 * 
	 * @param potentialArea
	 *            the area to fill with influence values
	 * @param influenceFactor
	 *            the influence factor of the line in the area
	 * @param line
	 *            the line source
	 */
	public void calculateLineInfluence(final double[][] potentialArea,
			final double influenceFactor, final double r1, final Line2D line) {
		double tempX, tempY, distance = 0;

		final Rectangle2D r = line.getBounds2D();
		final int startX = Math.min(
				Math.max(0, (int) ((r.getX() - r1) / pixelGroup)),
				potentialArea.length - 1);
		final int startY = Math.min(
				Math.max(0, (int) ((r.getY() - r1) / pixelGroup)),
				potentialArea[startX].length - 1);
		final int endX = Math.min(potentialArea.length - 1, Math.max(0,
				(int) ((r.getX() + r.getWidth() + r1) / pixelGroup)));
		final int endY = Math.min(potentialArea[startX].length, Math.max(0,
				(int) ((r.getY() + r.getHeight() + r1) / pixelGroup)));

		// for every point in potentialArea, calculate distance to nearest point
		// on line and add influence
		for (int x = startX; x < endX; x++) {
			for (int y = startY; y < endY; y++) {
				tempX = x * pixelGroup;
				tempY = y * pixelGroup;

				distance = line.ptSegDist(tempX, tempY);

				// only influence if less than r1
				if (distance <= r1) {
					potentialArea[x][y] += influenceFactor
							* Math.pow(distance - r1, 2);
				}
			}
		}
	}

	/**
	 * Finds the item in the iterator whose rectangular bounds intersect the
	 * line closest to the P1 endpoint and the item is not in the given
	 * aggregate. Note that despite the shape of the rendered VisualItem, the
	 * rectangular bounds are used as for this check.
	 * 
	 * @param interferenceItems
	 * @param currentAggregate
	 * @param testLine
	 * @return the closest item or null if there are no intersections.
	 */
	public VisualItem getClosestItem(final Iterator interferenceItems,
			final AggregateItem currentAggregate, final Line2D testLine) {
		double minDistance = Double.MAX_VALUE;
		VisualItem closestItem = null;

		while (interferenceItems.hasNext()) {
			final VisualItem interferenceItem = (VisualItem) interferenceItems
					.next();
			if (!currentAggregate.containsItem(interferenceItem)) {

				// only test if overlap is possible (QUES not sure if this is
				// faster b/c it adds some tests to every item)
				if ((interferenceItem.getBounds().getMinX() <= (Math.max(
						testLine.getX1(), testLine.getX2()))
						&& (interferenceItem.getBounds().getMinY() <= Math.max(
								testLine.getY1(), testLine.getY2())) && ((interferenceItem
						.getBounds().getMaxX() >= (Math.min(testLine.getX1(),
						testLine.getX2())) && (interferenceItem.getBounds()
						.getMaxY() >= Math.min(testLine.getY1(),
						testLine.getY2())))))) {

					final double distance = fractionToLineEnd(
							interferenceItem.getBounds(), testLine);
					// find closest intersection
					if ((distance != -1) && (distance < minDistance)) {
						closestItem = interferenceItem;
						minDistance = distance;
					}
				}
			}
		}
		return closestItem;
	}

	/**
	 * Finds the item in the iterator whose rectangular bounds intersect the
	 * line closest to the center and the item is not in the given aggregate.
	 * Note that despite the shape of the rendered VisualItem, the rectangular
	 * bounds are used as for this check.
	 * 
	 * @param interferenceItems
	 * @param currentAggregate
	 * @param testLine
	 * @return the closest item or null if there are no intersections.
	 */
	public Rectangle2D getCenterItem(final Rectangle2D[] nonMembers,
			final MemberItem[] memberItems, final Line2D testLine) {
		double minDistance = Double.MAX_VALUE;
		Rectangle2D closestItem = null;

		for (final Rectangle2D interferenceItem : nonMembers) {
			// only test if overlap is possible (QUES not sure if this is
			// faster b/c it adds some tests to every item)
			if ((interferenceItem.getBounds().getMinX() <= (Math.max(
					testLine.getX1(), testLine.getX2()))
					&& (interferenceItem.getBounds().getMinY() <= Math.max(
							testLine.getY1(), testLine.getY2())) && ((interferenceItem
					.getBounds().getMaxX() >= (Math.min(testLine.getX1(),
					testLine.getX2())) && (interferenceItem.getBounds()
					.getMaxY() >= Math.min(testLine.getY1(), testLine.getY2())))))) {
				final double distance = fractionToLineCenter(
						interferenceItem.getBounds(), testLine);
				// find closest intersection
				if ((distance != -1) && (distance < minDistance)) {
					closestItem = interferenceItem;
					minDistance = distance;
				}
			}
		}
		return closestItem;
	}

	public int countInterferenceItems(final Rectangle2D[] interferenceItems,
			final MemberItem[] memberItems, final Line2D testLine) {
		int count = 0;
		for (final Rectangle2D interferenceItem : interferenceItems) {
			// only test if overlap is possible (QUES not sure if this is
			// faster b/c it adds some tests to every item)
			if ((interferenceItem.getBounds().getMinX() <= (Math.max(
					testLine.getX1(), testLine.getX2()))
					&& (interferenceItem.getBounds().getMinY() <= Math.max(
							testLine.getY1(), testLine.getY2())) && ((interferenceItem
					.getBounds().getMaxX() >= (Math.min(testLine.getX1(),
					testLine.getX2())) && (interferenceItem.getBounds()
					.getMaxY() >= Math.min(testLine.getY1(), testLine.getY2())))))) {
				if (fractionToLineCenter(interferenceItem.getBounds(), testLine) != -1) {
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Add a contribution of a line source to all points in a given area. For
	 * every point in the given area, the distance to the closest point on the
	 * line is calculated and this distance is input into the gradient influence
	 * function, then added to the potentialArea.
	 * 
	 * @param potentialArea
	 *            the area to fill with influence values
	 * @param influenceFactor
	 *            the influence factor of the line in the area
	 * @param line
	 *            the line source
	 */
	public void calculateLinesInfluence(final double[][] potentialArea,
			final double influenceFactor, final double r1,
			final Deque<Line2D> lines, final Rectangle2D bounds) {
		double tempX, tempY, distance = 0;
		double minDistance = Double.MAX_VALUE;

		Rectangle2D r = null;

		// calculate active region
		for (final Line2D line : lines) {
			if (r == null) {
				r = (Rectangle2D) line.getBounds2D().clone();
			} else {
				r.add(line.getBounds2D());
			}
		}
		r.setFrame(r.getX() - bounds.getX(), r.getY() - bounds.getY(),
				r.getWidth(), r.getHeight());

		final int startX = Math.min(
				Math.max(0, (int) ((r.getX() - r1) / pixelGroup)),
				potentialArea.length - 1);
		final int startY = Math.min(
				Math.max(0, (int) ((r.getY() - r1) / pixelGroup)),
				potentialArea[startX].length - 1);
		final int endX = Math.min(potentialArea.length - 1, Math.max(0,
				(int) ((r.getX() + r.getWidth() + r1) / pixelGroup)));
		final int endY = Math.min(potentialArea[startX].length, Math.max(0,
				(int) ((r.getY() + r.getHeight() + r1) / pixelGroup)));

		// for every point in potentialArea, calculate distance to nearest point
		// on line and add influence
		for (int x = startX; x < endX; x++) {
			for (int y = startY; y < endY; y++) {

				// if we are adding negative energy, skip if not already
				// positive
				// positives have already been added first, and adding negative
				// to <=0 will have no affect on surface
				if ((influenceFactor < 0) && (potentialArea[x][y] <= 0)) {
					continue;
				}

				tempX = x * pixelGroup + bounds.getX();
				tempY = y * pixelGroup + bounds.getY();

				minDistance = Double.MAX_VALUE;
				for (final Line2D line : lines) {
					distance = line.ptSegDist(tempX, tempY);
					if (distance < minDistance) {
						minDistance = distance;
					}
				}

				// only influence if less than r1
				if (minDistance <= r1) {
					potentialArea[x][y] += influenceFactor
							* Math.pow(minDistance - r1, 2);
				}
			}
		}
	}

	/**
	 * Add a contribution of a rectangle source to all points in a given area.
	 * For every point in the given area, the distance to the closest point on
	 * the rectangle is calculated and this distance is input into the gradient
	 * influence function, then added to the potentialArea.
	 * 
	 * @param potentialArea
	 *            the area to fill with influence values
	 * @param influenceFactor
	 *            the influence factor of the line in the area
	 * @param line
	 *            the line source
	 */
	public void calculateRectangleInfluence(final double[][] potentialArea,
			final double influenceFactor, final double r1, final Rectangle2D r) {
		double tempX, tempY, distance = 0;
		Line2D line;

		final int startX = Math.min(
				Math.max(0, (int) ((r.getX() - r1) / pixelGroup)),
				potentialArea.length - 1);
		final int startY = Math.min(
				Math.max(0, (int) ((r.getY() - r1) / pixelGroup)),
				potentialArea[startX].length - 1);
		final int endX = Math.min(potentialArea.length - 1, Math.max(0,
				(int) ((r.getX() + r.getWidth() + r1) / pixelGroup)));
		final int endY = Math.min(potentialArea[startX].length, Math.max(0,
				(int) ((r.getY() + r.getHeight() + r1) / pixelGroup)));

		// for every point in potentialArea, calculate distance to nearest point
		// on rectangle
		// and add influence
		for (int x = startX; x < endX; x++) {
			for (int y = startY; y < endY; y++) {
				// if we are adding negative energy, skip if not already
				// positive
				// positives have already been added first, and adding negative
				// to <=0 will have no affect on surface

				if ((influenceFactor < 0) && (potentialArea[x][y] <= 0)) {
					continue;
				}

				tempX = x * pixelGroup;
				tempY = y * pixelGroup;

				// inside
				if (r.contains(tempX, tempY)) {
					distance = 0;
				} else {
					final int outcode = r.outcode(tempX, tempY);
					// top
					if ((outcode & Rectangle2D.OUT_TOP) == Rectangle2D.OUT_TOP) {
						// and left
						if ((outcode & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT) {
							// linear distance from upper left corner
							distance = Point2D.distance(tempX, tempY,
									r.getMinX(), r.getMinY());
						} else {
							// and right
							if ((outcode & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT) {
								// linear distance from upper right corner
								distance = Point2D.distance(tempX, tempY,
										r.getMaxX(), r.getMinY());
							} else {
								// distance from top line segment
								line = new Line2D.Double(r.getMinX(),
										r.getMinY(), r.getMaxX(), r.getMinY());
								distance = line.ptSegDist(tempX, tempY);
							}
						}
					} else {
						// bottom
						if ((outcode & Rectangle2D.OUT_BOTTOM) == Rectangle2D.OUT_BOTTOM) {
							// and left
							if ((outcode & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT) {
								// linear distance from lower left corner
								distance = Point2D.distance(tempX, tempY,
										r.getMinX(), r.getMaxY());
							} else {
								// and right
								if ((outcode & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT) {
									// linear distance from lower right corner
									distance = Point2D.distance(tempX, tempY,
											r.getMaxX(), r.getMaxY());
								} else {
									// distance from bottom line segment
									line = new Line2D.Double(r.getMinX(),
											r.getMaxY(), r.getMaxX(),
											r.getMaxY());
									distance = line.ptSegDist(tempX, tempY);
								}
							}
						} else {
							// left only
							if ((outcode & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT) {
								// linear distance from left edge
								line = new Line2D.Double(r.getMinX(),
										r.getMinY(), r.getMinX(), r.getMaxY());
								distance = line.ptSegDist(tempX, tempY);
							} else {
								// right only
								if ((outcode & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT) {
									// linear distance from right edge
									line = new Line2D.Double(r.getMaxX(),
											r.getMinY(), r.getMaxX(),
											r.getMaxY());
									distance = line.ptSegDist(tempX, tempY);
								}
							}
						}
					}
				}
				// only influence if less than r1
				if (distance <= r1) {
					potentialArea[x][y] += influenceFactor
							* Math.pow(distance - r1, 2);
				}
			}
		}
	}

	/**
	 * Add a contribution of an arbitrary area made of straight line segments to
	 * all points in a given area. For every point in the given area, the
	 * distance to the closest point on the area boundary is calculated and this
	 * distance is input into the gradient influence function, then added to the
	 * potentialArea.
	 * 
	 * @param potentialArea
	 *            the area to fill with influence values
	 * @param influenceFactor
	 *            the influence factor of the line in the area
	 * @param r1
	 *            the radius at which energy drops to zero
	 * @param a
	 *            the area to add positive influence (in world coordinates)
	 * @param activeArea
	 *            the bounds of the calculation region
	 */
	public void calculateAreaInfluence(final double[][] potentialArea,
			final double influenceFactor, final double r1, final Area a,
			final Rectangle2D activeArea) {
		double tempX, tempY, distance = 0;

		// create a deque of the lines
		final Deque<Line2D> lines = new ArrayDeque<Line2D>();
		final PathIterator pathIterator = a.getPathIterator(null);

		final double[] current = new double[6];
		final double[] previous = new double[6];
		double[] start = null;

		int segmentType;

		while (!pathIterator.isDone()) {
			segmentType = pathIterator.currentSegment(current);
			if (segmentType == PathIterator.SEG_LINETO) {
				if (previous != null) {
					lines.add(new Line2D.Double(previous[0], previous[1],
							current[0], current[1]));
				}
			}
			if (segmentType == PathIterator.SEG_CLOSE) {
				if (previous != null) {
					lines.add(new Line2D.Double(previous[0], previous[1],
							start[0], start[1]));
				}
			}
			System.arraycopy(current, 0, previous, 0, current.length);

			if (start == null) {
				start = new double[6];
				System.arraycopy(current, 0, start, 0, current.length);
			}

			pathIterator.next();
		}

		// go around edges
		calculateLinesInfluence(potentialArea, influenceFactor, r1, lines,
				activeArea);

		final int startX = Math.min(
				Math.max(0, (int) ((activeArea.getX() - r1) / pixelGroup)),
				potentialArea.length - 1);
		final int startY = Math.min(
				Math.max(0, (int) ((activeArea.getY() - r1) / pixelGroup)),
				potentialArea[startX].length - 1);
		final int endX = Math
				.min(potentialArea.length - 1,
						Math.max(
								0,
								(int) ((activeArea.getX()
										+ activeArea.getWidth() + r1) / pixelGroup)));
		final int endY = Math
				.min(potentialArea[startX].length,
						Math.max(
								0,
								(int) ((activeArea.getY()
										+ activeArea.getHeight() + r1) / pixelGroup)));

		// for every point in potentialArea, calculate distance to nearest point
		// on rectangle
		// and add influence
		for (int x = startX; x < endX; x++) {
			for (int y = startY; y < endY; y++) {
				// if we are adding negative energy, skip if not already
				// positive
				// positives have already been added first, and adding negative
				// to <=0 will have no affect on surface

				if ((influenceFactor < 0) && (potentialArea[x][y] <= 0)) {
					continue;
				}

				tempX = x * pixelGroup + activeArea.getX();
				tempY = y * pixelGroup + activeArea.getY();

				// inside
				if (a.contains(tempX, tempY)) {
					distance = 0;
				} else {
					distance = Double.MAX_VALUE;
				}
				// only influence if less than r1
				if (distance <= r1) {
					potentialArea[x][y] += influenceFactor
							* Math.pow(distance - r1, 2);
				}
			}
		}
	}

	/**
	 * 2-D Marching squares algorithm. March around a given area to find an
	 * iso-energy surface.
	 * 
	 * @param surface
	 *            the surface to fill with iso-energy points
	 * @param potentialArea
	 *            the area, filled with potential values
	 * @param x
	 *            the current x-position in the area
	 * @param y
	 *            the current y-position in the area
	 */
	public boolean march(final ArrayList<Point2D> surface,
			final double[][] potentialArea, final int x, final int y) {
		final Point2D p = new Point2D.Float(x * pixelGroup, y * pixelGroup);

		// check if we're back where we started
		if (surface.contains(p)) {
			if (!surface.get(0).equals(p)) {
				// encountered a loop but haven't returned to start; will change
				// direction using conditionals and continue
			} else {
				// back to start
				return true;
			}
		} else {
			surface.add(p);
		}

		final int state = getState(potentialArea, x, y);
		// x, y are upper left of 2X2 marching square

		switch (state) {
		case -1:
			LOGGER.warning("Marched out of bounds");
			return false; // marched out of bounds (shouldn't happen)
		case 0:
		case 3:
		case 2:
		case 7:
			direction = Direction.E;
			break;
		case 12:
		case 14:
		case 4:
			direction = Direction.W;
			break;
		case 6:
			direction = (direction == Direction.N) ? Direction.W : Direction.E;
			break;
		case 1:
		case 13:
		case 5:
			direction = Direction.N;
			break;
		case 9:
			direction = (direction == Direction.E) ? Direction.N : Direction.S;
			break;
		case 10:
		case 8:
		case 11:
			direction = Direction.S;
			break;
		default:
			throw new IllegalStateException("Marching squares invalid state: "
					+ state);
		}

		switch (direction) {
		case N:
			return march(surface, potentialArea, x, y - 1); // up
		case S:
			return march(surface, potentialArea, x, y + 1); // down
		case W:
			return march(surface, potentialArea, x - 1, y); // left
		case E:
			return march(surface, potentialArea, x + 1, y); // right
		default:
			throw new IllegalStateException("Marching squares invalid state: "
					+ state);
		}
	}

	public void setThreshold(final double threshold) {
		this.threshold = threshold;
	}

	public void setNodeInfluence(final double positive, final double negative) {
		nodeInfluenceFactor = positive;
		negativeNodeInfluenceFactor = negative;
	}

	public void setEdgeInfluenceFactor(final double positive,
			final double negative) {
		edgeInfluenceFactor = positive;
		negativeEdgeInfluenceFactor = negative;
	}

	/**
	 * Tests whether a given value meets the threshold specified for marching
	 * squares.
	 * 
	 * @param test
	 *            the value to test
	 * @return whether the test value passes
	 */
	public boolean test(final double test) {
		return (test > threshold);
	}

	/**
	 * 2-D Marching Squares algorithm. Given a position and an area of potential
	 * energy values, calculate the current marching squares state by testing
	 * neighbouring squares.
	 * 
	 * @param potentialArea
	 *            the area filled with potential energy values
	 * @param x
	 *            the current x-position in the area
	 * @param y
	 *            the current y-position in the area
	 * @return an int value representing a marching squares state
	 */
	public int getState(final double[][] potentialArea, final int x, final int y) {
		int dir = 0;
		try {
			dir += (test(potentialArea[x][y]) ? 1 << 0 : 0);
			dir += (test(potentialArea[x + 1][y]) ? 1 << 1 : 0);
			dir += (test(potentialArea[x][y + 1]) ? 1 << 2 : 0);
			dir += (test(potentialArea[x + 1][y + 1]) ? 1 << 3 : 0);
		} catch (final ArrayIndexOutOfBoundsException e) {
			LOGGER.severe("Marched out of bounds: " + x + " " + y + " bounds: "
					+ potentialArea.length + " " + potentialArea[0].length);
			return -1;
		}
		return dir;
	}

	/**
	 * Calculate the energy function of any hovered aggregates and visualize it
	 * in the given graphics context.
	 * 
	 * @param g2d
	 *            the graphics context into which to visualize the energy
	 *            function
	 */
	public void paintPotentialArea(final Graphics2D g2d) {
		final AggregateTable aggr = (AggregateTable) m_vis.getGroup(m_group);
		if (aggr == null) {
			return;
		}

		// do we have any to process?
		final int num = aggr.getTupleCount();
		if (num == 0) {
			return;
		}

		if (aggregateLabelLayout != null) {
			// add their energy to aggregates
			final TupleSet aggregateLabels = m_vis
					.getGroup(aggregateLabelLayout.getGroup());
			final Iterator labelIterator = aggregateLabels.tuples();

			while (labelIterator.hasNext()) {
				final DecoratorItem di = (DecoratorItem) labelIterator.next();
				final AggregateItem aitem = (AggregateItem) di
						.getDecoratedItem();
				if (di.isVisible()) {
					if ((aitem.isHover()) && (aitem.getAggregateSize() > 0)) {
						paintPotential(aitem, di, g2d);
					}
				} else {
					if (aitem.isVisible()) {
						if ((aitem.isHover()) && (aitem.getAggregateSize() > 0)) {
							paintPotential(aitem, null, g2d);
						}
					}
				}
			}
		} else {
			// compute and assign convex hull for each aggregate
			final Iterator aggrs = m_vis.visibleItems(m_group);
			while (aggrs.hasNext()) {
				final AggregateItem aitem = (AggregateItem) aggrs.next();
				if ((aitem.isHover()) && (aitem.getAggregateSize() > 0)) {
					paintPotential(aitem, null, g2d);
				}
			}
		}
	}

	public void paintPotential(final AggregateItem aitem,
			final DecoratorItem di, final Graphics2D g2d) {
		// calculate bounds of influence
		Rectangle2D bounds = null;
		bounds = aitem.getBounds();

		// expand bounds by the maximum radius on all sides
		bounds.setRect(bounds.getX() - Math.max(edgeR1, nodeR1), bounds.getY()
				- Math.max(edgeR1, nodeR1),
				bounds.getWidth() + 2 * Math.max(edgeR1, nodeR1),
				bounds.getHeight() + 2 * Math.max(edgeR1, nodeR1));

		// calculates potential for all points within bounds
		final double[][] potentialArea = new double[(int) Math.ceil(bounds
				.getWidth() / pixelGroup)][(int) Math.ceil(bounds.getHeight()
				/ pixelGroup)];

		// save old energy parameters
		final double tempThreshold = threshold;
		final double tempNegativeNodeInfluenceFactor = negativeNodeInfluenceFactor;
		final double tempNegativeEdgeInfluenceFactor = negativeEdgeInfluenceFactor;
		final double tempNodeInfluenceFactor = nodeInfluenceFactor;
		final double tempEdgeInfluenceFactor = edgeInfluenceFactor;

		// set energy parameters from node cache
		threshold = aitem.getDouble(THRESHOLD);
		negativeNodeInfluenceFactor = aitem.getDouble(NNIF);
		negativeEdgeInfluenceFactor = aitem.getDouble(NEIF);
		nodeInfluenceFactor = aitem.getDouble(NIF);
		edgeInfluenceFactor = aitem.getDouble(EIF);

		LOGGER.info("Aggregate info: threshold: " + threshold + " NNIF: "
				+ negativeNodeInfluenceFactor + " NEIF: "
				+ negativeEdgeInfluenceFactor + " NIF: " + nodeInfluenceFactor
				+ " EIF: " + edgeInfluenceFactor);

		fillPotentialArea(bounds, aitem, potentialArea);
		if (di != null) {
			fillPotentialArea(bounds, di, potentialArea);
		}

		// draw energy field
		int tempX, tempY;
		for (int x = 0; x < potentialArea.length - 1; x++) {
			for (int y = 0; y < potentialArea[x].length - 1; y++) {
				tempX = x * pixelGroup + (int) bounds.getX();
				tempY = y * pixelGroup + (int) bounds.getY();

				if (potentialArea[x][y] < 0) {
					g2d.setColor(ColorLib.getColor(ColorLib.rgba(
							20,
							20,
							150,
							(int) Math.min(255,
									Math.abs((potentialArea[x][y] * 40))))));
				} else {
					g2d.setColor(ColorLib.getColor(ColorLib.rgba(
							150,
							20,
							20,
							(int) Math.min(255,
									Math.abs((potentialArea[x][y] * 40))))));
				}
				if (potentialArea[x][y] == threshold) {
					g2d.setColor(ColorLib.getColor(ColorLib.gray(0, 120)));
				}
				g2d.fillRect(tempX, tempY, pixelGroup, pixelGroup);
			}
		}

		// reset parameters
		threshold = tempThreshold;
		negativeEdgeInfluenceFactor = tempNegativeEdgeInfluenceFactor;
		negativeNodeInfluenceFactor = tempNegativeNodeInfluenceFactor;
		nodeInfluenceFactor = tempNodeInfluenceFactor;
		edgeInfluenceFactor = tempEdgeInfluenceFactor;
	}

	/**
	 * Calculate the intersection of two line segments.
	 * 
	 * @param a
	 * @param b
	 * @return an Intersection item storing the type of intersection and the
	 *         exact point if any was found
	 */
	public Intersection intersectLineLine(final Line2D a, final Line2D b) {
		Intersection result;

		final double ua_t = (b.getX2() - b.getX1()) * (a.getY1() - b.getY1())
				- (b.getY2() - b.getY1()) * (a.getX1() - b.getX1());
		final double ub_t = (a.getX2() - a.getX1()) * (a.getY1() - b.getY1())
				- (a.getY2() - a.getY1()) * (a.getX1() - b.getX1());
		final double u_b = (b.getY2() - b.getY1()) * (a.getX2() - a.getX1())
				- (b.getX2() - b.getX1()) * (a.getY2() - a.getY1());

		if (u_b != 0) {
			final double ua = ua_t / u_b;
			final double ub = ub_t / u_b;

			if (0 <= ua && ua <= 1 && 0 <= ub && ub <= 1) {
				result = new Intersection(a.getX1() + ua
						* (a.getX2() - a.getX1()), a.getY1() + ua
						* (a.getY2() - a.getY1()));
			} else {
				result = new Intersection(State.None);
			}
		} else {
			if (ua_t == 0 || ub_t == 0) {
				result = new Intersection(State.Coincident);
			} else {
				result = new Intersection(State.Parallel);
			}
		}

		return result;
	};

	/**
	 * Find the fraction along the line a that line b intersects, closest to P1
	 * on line a. This is slightly faster than determining the actual
	 * intersection coordinates.
	 * 
	 * @param bounds
	 * @param line
	 * @return the smallest fraction along the line that indicates an
	 *         intersection point
	 */
	public double fractionAlongLineA(final Line2D a, final Line2D b) {
		final double ua_t = (b.getX2() - b.getX1()) * (a.getY1() - b.getY1())
				- (b.getY2() - b.getY1()) * (a.getX1() - b.getX1());
		final double ub_t = (a.getX2() - a.getX1()) * (a.getY1() - b.getY1())
				- (a.getY2() - a.getY1()) * (a.getX1() - b.getX1());
		final double u_b = (b.getY2() - b.getY1()) * (a.getX2() - a.getX1())
				- (b.getX2() - b.getX1()) * (a.getY2() - a.getY1());

		if (u_b != 0) {
			final double ua = ua_t / u_b;
			final double ub = ub_t / u_b;

			if (0 <= ua && ua <= 1 && 0 <= ub && ub <= 1) {
				return ua;
			}
		}
		return Double.MAX_VALUE;
	};

	/**
	 * Find the fraction along the given line that the rectangle intersects,
	 * closest to the center of the line. This is slightly faster than
	 * determining the actual intersection coordinates.
	 * 
	 * @param bounds
	 * @param line
	 * @return the smallest fraction along the line that indicates an
	 *         intersection point
	 */
	public double fractionToLineCenter(final Rectangle2D bounds,
			final Line2D line) {
		double minDistance = Double.MAX_VALUE;
		double testDistance = 0;
		int countIntersections = 0;

		// top
		testDistance = fractionAlongLineA(
				line,
				new Line2D.Double(bounds.getMinX(), bounds.getMinY(), bounds
						.getMaxX(), bounds.getMinY()));
		testDistance -= 0.5;
		testDistance = Math.abs(testDistance);
		if ((testDistance >= 0) && (testDistance <= 1)) {
			countIntersections++;
			if (testDistance < minDistance) {
				minDistance = testDistance;
			}
		}

		// left
		testDistance = fractionAlongLineA(
				line,
				new Line2D.Double(bounds.getMinX(), bounds.getMinY(), bounds
						.getMinX(), bounds.getMaxY()));
		testDistance -= 0.5;
		testDistance = Math.abs(testDistance);
		if ((testDistance >= 0) && (testDistance <= 1)) {
			countIntersections++;
			if (testDistance < minDistance) {
				minDistance = testDistance;
			}
		}
		if (countIntersections == 2) {
			return minDistance; // max 2 intersections
		}

		// bottom
		testDistance = fractionAlongLineA(
				line,
				new Line2D.Double(bounds.getMinX(), bounds.getMaxY(), bounds
						.getMaxX(), bounds.getMaxY()));
		testDistance -= 0.5;
		testDistance = Math.abs(testDistance);
		if ((testDistance >= 0) && (testDistance <= 1)) {
			countIntersections++;
			if (testDistance < minDistance) {
				minDistance = testDistance;
			}
		}
		if (countIntersections == 2) {
			return minDistance; // max 2 intersections
		}

		// right
		testDistance = fractionAlongLineA(
				line,
				new Line2D.Double(bounds.getMaxX(), bounds.getMinY(), bounds
						.getMaxX(), bounds.getMaxY()));
		testDistance -= 0.5;
		testDistance = Math.abs(testDistance);
		if ((testDistance >= 0) && (testDistance <= 1)) {
			countIntersections++;
			if (testDistance < minDistance) {
				minDistance = testDistance;
			}
		}

		// if no intersection, return -1
		if (countIntersections == 0) {
			return -1;
		}

		return minDistance;
	}

	/**
	 * Find the fraction along the given line that the rectangle intersects,
	 * closest to P1 on the line. This is slightly faster than determining the
	 * actual intersection coordinates.
	 * 
	 * @param bounds
	 * @param line
	 * @return the smallest fraction along the line that indicates an
	 *         intersection point
	 */
	public double fractionToLineEnd(final Rectangle2D bounds, final Line2D line) {
		double minDistance = Double.MAX_VALUE;
		double testDistance = 0;
		int countIntersections = 0;

		// top
		testDistance = fractionAlongLineA(
				line,
				new Line2D.Double(bounds.getMinX(), bounds.getMinY(), bounds
						.getMaxX(), bounds.getMinY()));
		if ((testDistance >= 0) && (testDistance <= 1)) {
			countIntersections++;
			if (testDistance < minDistance) {
				minDistance = testDistance;
			}
		}

		// left
		testDistance = fractionAlongLineA(
				line,
				new Line2D.Double(bounds.getMinX(), bounds.getMinY(), bounds
						.getMinX(), bounds.getMaxY()));
		if ((testDistance >= 0) && (testDistance <= 1)) {
			countIntersections++;
			if (testDistance < minDistance) {
				minDistance = testDistance;
			}
		}
		if (countIntersections == 2) {
			return minDistance; // max 2 intersections
		}

		// bottom
		testDistance = fractionAlongLineA(
				line,
				new Line2D.Double(bounds.getMinX(), bounds.getMaxY(), bounds
						.getMaxX(), bounds.getMaxY()));
		if ((testDistance >= 0) && (testDistance <= 1)) {
			countIntersections++;
			if (testDistance < minDistance) {
				minDistance = testDistance;
			}
		}
		if (countIntersections == 2) {
			return minDistance; // max 2 intersections
		}

		// right
		testDistance = fractionAlongLineA(
				line,
				new Line2D.Double(bounds.getMaxX(), bounds.getMinY(), bounds
						.getMaxX(), bounds.getMaxY()));
		if ((testDistance >= 0) && (testDistance <= 1)) {
			countIntersections++;
			if (testDistance < minDistance) {
				minDistance = testDistance;
			}
		}

		// if no intersection, return -1
		if (countIntersections == 0) {
			return -1;
		}

		return minDistance;
	}

	/**
	 * Tests intersection of the given line segment with all sides of the given
	 * rectangle.
	 * 
	 * @param line
	 *            the line to test
	 * @param rectangle
	 *            the rectangular bounds to test each side of
	 * @param intersections
	 *            an array of at least 4 intersections where the intersections
	 *            will be stored as top, left, bottom, right
	 * @return the number of intersection points found (doesn't count
	 *         coincidental lines)
	 */
	public int testIntersection(final Line2D line, final Rectangle2D bounds,
			final Intersection[] intersections) {

		int countIntersections = 0;

		// top
		intersections[0] = intersectLineLine(
				line,
				new Line2D.Double(bounds.getMinX(), bounds.getMinY(), bounds
						.getMaxX(), bounds.getMinY()));
		if (intersections[0].state == State.Point) {
			countIntersections++;
		}

		// left
		intersections[1] = intersectLineLine(
				line,
				new Line2D.Double(bounds.getMinX(), bounds.getMinY(), bounds
						.getMinX(), bounds.getMaxY()));
		if (intersections[1].state == State.Point) {
			countIntersections++;
		}

		// CAN'T STOP HERE: NEED ALL INTERSECTIONS TO BE FILLED IN
		// if (countIntersections == 2) return countIntersections; // max 2
		// intersections

		// bottom
		intersections[2] = intersectLineLine(
				line,
				new Line2D.Double(bounds.getMinX(), bounds.getMaxY(), bounds
						.getMaxX(), bounds.getMaxY()));
		if (intersections[2].state == State.Point) {
			countIntersections++;
		}

		// right
		intersections[3] = intersectLineLine(
				line,
				new Line2D.Double(bounds.getMaxX(), bounds.getMinY(), bounds
						.getMaxX(), bounds.getMaxY()));
		if (intersections[3].state == State.Point) {
			countIntersections++;
		}

		return countIntersections;
	}

	/**
	 * Move an endpoint of a line to outside the given bounds if it is within
	 * them. Moves it to the nearest corner if wrapNormal is true, otherwise to
	 * the corner opposite the nearest.
	 * 
	 * @param line
	 *            the line to test
	 * @param rectangle
	 *            the rectangular bounds to move the line out of
	 * @param rerouteBuffer
	 *            the buffer to place between the rectangle corner and the new
	 *            point position
	 * @param wrapNormal
	 *            whether to wrap around the closest corner to the endpoint (if
	 *            true) or the opposite corner (if false)
	 * @return an array of two points: the original point and the new location
	 */
	public Point2D moveEndPoint(final Point2D oldPoint,
			final Rectangle2D rectangle, final double rerouteBuffer,
			final boolean wrapNormal) {
		final Rectangle2D bounds = rectangle.getBounds2D();

		// only add 1, if we add morph buffer could get thrashing by routing the
		// endpoint back into the item it came from

		Point2D newPoint;

		if (wrapNormal) {
			// reroute around the opposite corner of the bounds from where the
			// endpoint currently sits in the bounds
			// top
			if (oldPoint.getY() < bounds.getCenterY()) {
				// left
				if (oldPoint.getX() < bounds.getCenterX()) {
					// bottom right
					newPoint = new Point2D.Double(bounds.getMaxX()
							+ rerouteBuffer, bounds.getMaxY() + rerouteBuffer);
				} else {
					// bottom left
					newPoint = new Point2D.Double(bounds.getMinX()
							- rerouteBuffer, bounds.getMaxY() + rerouteBuffer);
				}
			} else {
				// bottom
				if (oldPoint.getX() < bounds.getCenterX()) {
					// top right
					newPoint = new Point2D.Double(bounds.getMaxX()
							+ rerouteBuffer, bounds.getMinY() - rerouteBuffer);
				} else {
					// top left
					newPoint = new Point2D.Double(bounds.getMinX()
							- rerouteBuffer, bounds.getMinY() - rerouteBuffer);
				}
			}
		} else {
			// reroute around the closest corner of the bounds from where the
			// endpoint currently sits in the bounds
			// top
			if (oldPoint.getY() < bounds.getCenterY()) {
				// left
				if (oldPoint.getX() < bounds.getCenterX()) {
					// top left
					newPoint = new Point2D.Double(bounds.getMinX()
							- rerouteBuffer, bounds.getMinY() - rerouteBuffer);
				} else {
					// top right
					newPoint = new Point2D.Double(bounds.getMaxX()
							+ rerouteBuffer, bounds.getMinY() - rerouteBuffer);
				}
			} else {
				// bottom
				if (oldPoint.getX() < bounds.getCenterX()) {
					// bottom left
					newPoint = new Point2D.Double(bounds.getMinX()
							- rerouteBuffer, bounds.getMaxY() + rerouteBuffer);
				} else {
					// bottom right
					newPoint = new Point2D.Double(bounds.getMaxX()
							+ rerouteBuffer, bounds.getMaxY() + rerouteBuffer);
				}
			}
		}

		return newPoint;
	}

	/**
	 * Find an appropriate split point in the line to wrap the line around the
	 * given rectangle.
	 * 
	 * @param line
	 *            the line to split
	 * @param rectangle
	 *            the rectangle which intersects the line exactly twice
	 * @param rerouteBuffer
	 *            the buffer to place between the selected reroute corner and
	 *            the new point
	 * @param intersections
	 *            the intersections of the line with each of the rectangle edges
	 * @param wrapNormal
	 *            whether to wrap around the closest corner (if true) or the
	 *            opposite corner (if false)
	 * @return the position of the new endpoint
	 */
	public Point2D rerouteLine(final Line2D line, final Rectangle2D rectangle,
			final double rerouteBuffer, final Intersection[] intersections,
			final boolean wrapNormal) {
		final Rectangle2D bounds = rectangle.getBounds2D();

		final Intersection topIntersect = intersections[0];
		final Intersection leftIntersect = intersections[1];
		final Intersection bottomIntersect = intersections[2];
		final Intersection rightIntersect = intersections[3];

		// wrap around the most efficient way
		if (wrapNormal) {
			if (leftIntersect.state == State.Point) {
				if (topIntersect.state == State.Point) {
					// triangle, must go around top left
					return new Point2D.Double(rectangle.getMinX()
							- rerouteBuffer, rectangle.getMinY()
							- rerouteBuffer);
				}
				if (bottomIntersect.state == State.Point) {
					// triangle, must go around bottom left
					return new Point2D.Double(rectangle.getMinX()
							- rerouteBuffer, rectangle.getMaxY()
							+ rerouteBuffer);
				}
				// else through left to right, calculate areas
				final double totalArea = bounds.getHeight() * bounds.getWidth();
				// top area
				final double topArea = bounds.getWidth()
						* (((leftIntersect.getY() - bounds.getY()) + (rightIntersect
								.getY() - bounds.getY())) / 2);
				if (topArea < totalArea / 2) {
					// go around top (the side which would make a greater
					// movement)
					if (leftIntersect.getY() > rightIntersect.getY()) {
						// top left
						return new Point2D.Double(rectangle.getMinX()
								- rerouteBuffer, rectangle.getMinY()
								- rerouteBuffer);
					} else {
						// top right
						return new Point2D.Double(rectangle.getMaxX()
								+ rerouteBuffer, rectangle.getMinY()
								- rerouteBuffer);
					}
				} else {
					// go around bottom
					if (leftIntersect.getY() < rightIntersect.getY()) {
						// bottom left
						return new Point2D.Double(rectangle.getMinX()
								- rerouteBuffer, rectangle.getMaxY()
								+ rerouteBuffer);
					} else {
						// bottom right
						return new Point2D.Double(rectangle.getMaxX()
								+ rerouteBuffer, rectangle.getMaxY()
								+ rerouteBuffer);
					}
				}
			} else {
				if (rightIntersect.state == State.Point) {
					if (topIntersect.state == State.Point) {
						// triangle, must go around top right
						return new Point2D.Double(rectangle.getMaxX()
								+ rerouteBuffer, rectangle.getMinY()
								- rerouteBuffer);
					}
					if (bottomIntersect.state == State.Point) {
						// triangle, must go around bottom right
						return new Point2D.Double(rectangle.getMaxX()
								+ rerouteBuffer, rectangle.getMaxY()
								+ rerouteBuffer);
					}
				} else {
					// else through top to bottom, calculate areas
					final double totalArea = bounds.getHeight()
							* bounds.getWidth();
					// top area
					final double leftArea = bounds.getHeight()
							* (((topIntersect.getX() - bounds.getX()) + (rightIntersect
									.getX() - bounds.getX())) / 2);
					if (leftArea < totalArea / 2) {
						// go around left
						if (topIntersect.getX() > bottomIntersect.getX()) {
							// top left
							return new Point2D.Double(rectangle.getMinX()
									- rerouteBuffer, rectangle.getMinY()
									- rerouteBuffer);
						} else {
							// bottom left
							return new Point2D.Double(rectangle.getMinX()
									- rerouteBuffer, rectangle.getMaxY()
									+ rerouteBuffer);
						}
					} else {
						// go around right
						if (topIntersect.getX() < bottomIntersect.getX()) {
							// top right
							return new Point2D.Double(rectangle.getMaxX()
									+ rerouteBuffer, rectangle.getMinY()
									- rerouteBuffer);
						} else {
							// bottom right
							return new Point2D.Double(rectangle.getMaxX()
									+ rerouteBuffer, rectangle.getMaxY()
									+ rerouteBuffer);
						}

					}
				}
			}
		} else {
			// wrap around opposite (usually because the first move caused a
			// problem
			if (leftIntersect.state == State.Point) {
				if (topIntersect.state == State.Point) {
					// triangle, must go around bottom right
					return new Point2D.Double(rectangle.getMaxX()
							+ rerouteBuffer, rectangle.getMaxY()
							+ rerouteBuffer);
				}
				if (bottomIntersect.state == State.Point) {
					// triangle, must go around top right
					return new Point2D.Double(rectangle.getMaxX()
							+ rerouteBuffer, rectangle.getMinY()
							- rerouteBuffer);
				}
				// else through left to right, calculate areas
				final double totalArea = bounds.getHeight() * bounds.getWidth();
				// top area
				final double topArea = bounds.getWidth()
						* (((leftIntersect.getY() - bounds.getY()) + (rightIntersect
								.getY() - bounds.getY())) / 2);
				if (topArea < totalArea / 2) {
					// go around bottom (the side which would make a lesser
					// movement)
					if (leftIntersect.getY() > rightIntersect.getY()) {
						// bottom right
						return new Point2D.Double(rectangle.getMaxX()
								+ rerouteBuffer, rectangle.getMaxY()
								+ rerouteBuffer);
					} else {
						// bottom left
						return new Point2D.Double(rectangle.getMinX()
								- rerouteBuffer, rectangle.getMaxY()
								+ rerouteBuffer);
					}
				} else {
					// go around top
					if (leftIntersect.getY() < rightIntersect.getY()) {
						// top right
						return new Point2D.Double(rectangle.getMaxX()
								+ rerouteBuffer, rectangle.getMinY()
								- rerouteBuffer);
					} else {
						// top left
						return new Point2D.Double(rectangle.getMinX()
								- rerouteBuffer, rectangle.getMinY()
								- rerouteBuffer);
					}
				}
			} else {
				if (rightIntersect.state == State.Point) {
					if (topIntersect.state == State.Point) {
						// triangle, must go around bottom left
						return new Point2D.Double(rectangle.getMinX()
								- rerouteBuffer, rectangle.getMaxY()
								+ rerouteBuffer);
					}
					if (bottomIntersect.state == State.Point) {
						// triangle, must go around top left
						return new Point2D.Double(rectangle.getMinX()
								- rerouteBuffer, rectangle.getMinY()
								- rerouteBuffer);
					}
				} else {
					// else through top to bottom, calculate areas
					final double totalArea = bounds.getHeight()
							* bounds.getWidth();
					// left area
					final double leftArea = bounds.getHeight()
							* (((topIntersect.getX() - bounds.getX()) + (rightIntersect
									.getX() - bounds.getX())) / 2);
					if (leftArea < totalArea / 2) {
						// go around right
						if (topIntersect.getX() > bottomIntersect.getX()) {
							// bottom right
							return new Point2D.Double(rectangle.getMaxX()
									+ rerouteBuffer, rectangle.getMaxY()
									+ rerouteBuffer);
						} else {
							// top right
							return new Point2D.Double(rectangle.getMaxX()
									+ rerouteBuffer, rectangle.getMinY()
									- rerouteBuffer);
						}
					} else {
						// go around left
						if (topIntersect.getX() < bottomIntersect.getX()) {
							// bottom left
							return new Point2D.Double(rectangle.getMinX()
									- rerouteBuffer, rectangle.getMaxY()
									+ rerouteBuffer);
						} else {
							// top left
							return new Point2D.Double(rectangle.getMinX()
									- rerouteBuffer, rectangle.getMinY()
									- rerouteBuffer);
						}
					}
				}
			}
		}

		// will only get here if intersection was along edge (parallel) or at a
		// corner
		return null;
	}

	static class Intersection extends Point2D.Double {
		enum State {
			Point, Parallel, Coincident, None
		};

		State state = State.Point;

		public Intersection(final double x, final double y) {
			super(x, y);
			state = State.Point;
		}

		public Intersection(final State state) {
			this.state = state;
		}
	}

	public BubbleSetLabelLayout createLabelLayout(final String group,
			final String labelField) {
		aggregateLabelLayout = new BubbleSetLabelLayout(group, labelField);
		return aggregateLabelLayout;
	}

	public BubbleSetLabelLayout getLabelLayout() {
		return aggregateLabelLayout;
	}

	/**
	 * Positions aggregate labels (DecoratorItems) along the longest virtual
	 * edge in the aggregate. Rotates label up to +-45 degress to follow the
	 * edge. Label size is modulated to fit within the length of the edge.
	 * 
	 * @author Christopher
	 */
	public class BubbleSetLabelLayout extends ItemAction {

		private Font labelFont = new Font("Verdana", Font.BOLD, 10);
		private final String titleField;

		public BubbleSetLabelLayout(final String group, final String titleField) {
			super(group);
			this.titleField = titleField;
		}

		public void setFont(final Font labelFont) {
			this.labelFont = labelFont;
		}

		@Override
		public void process(final VisualItem visualItem, final double frac) {
			final DecoratorItem di = (DecoratorItem) visualItem;
			final AggregateItem aItem = (AggregateItem) di.getDecoratedItem();

			Iterator aNodeItems = aItem.items();
			Line2D longestLine = new Line2D.Double();
			double maxLength = -1 * Double.MAX_VALUE;

			// min label length
			labelFont = labelFont.deriveFont(MIN_LABEL_SIZE);
			FontMetrics fm = getFontMetrics(labelFont);

			// START WITH A LINE ACROSS THE TOP OF THE FIRST ITEM; keep this if
			// no virtual edge is longer than the top of the first item
			if (aNodeItems.hasNext()) {
				final VisualItem item = (VisualItem) aNodeItems.next();
				longestLine = new Line2D.Double(item.getBounds().getX(), item
						.getBounds().getY() - nodeR0 / 2, item.getBounds()
						.getMaxX(), item.getBounds().getY() - nodeR0 / 2);
				maxLength = longestLine.getP1().distance(longestLine.getP2());
			}

			double length = 0;
			double labelWidth = fm.stringWidth(di.getString(titleField));

			// increase to max font size that would fit across size across top
			while ((fm.stringWidth(di.getString(titleField)) < maxLength)
					&& (labelFont.getSize() < MAX_LABEL_SIZE)) {
				// enlarge as much as possible
				labelFont = labelFont.deriveFont(labelFont.getSize2D() + 0.5f);
				fm = getFontMetrics(labelFont);
				labelWidth = fm.stringWidth(di.getString(titleField));
			}

			// raise by descent of final font
			longestLine.setLine(longestLine.getP1().getX(), longestLine.getP1()
					.getY() - fm.getDescent(), longestLine.getP2().getX(),
					longestLine.getP2().getY() - fm.getDescent());

			// get the items in this aggregate
			aNodeItems = aItem.items();

			while (aNodeItems.hasNext()) {
				// for each item in the aggregate
				final VisualItem item = (VisualItem) aNodeItems.next();
				// get the virtual edges of this item
				final Deque<Line2D> edges = (Deque<Line2D>) item
						.get(VIRTUAL_EDGES);
				// for all virtual edges of this item
				for (final Line2D edge : edges) {
					// calculate length of edge
					length = edge.getP1().distance(edge.getP2());
					// if half this edge is > current max, test further
					if (length / 2 >= maxLength) {
						// check if starting point of label at current font size
						// would be within item bounds
						Point2D testPoint = interpolateLine2D(edge,
								0.5 - ((labelWidth / length) / 2.0));
						if (!item.getBounds().contains(testPoint)) {
							while (!item.getBounds().contains(testPoint)
									&& (labelFont.getSize() < MAX_LABEL_SIZE)) {
								// enlarge as much as possible
								labelFont = labelFont.deriveFont(labelFont
										.getSize2D() + 0.5f);
								fm = getFontMetrics(labelFont);
								labelWidth = fm.stringWidth(di
										.getString(titleField));
								testPoint = interpolateLine2D(edge,
										0.5 - ((labelWidth / length) / 2.0));
							}
							// step down by one from the largest size found
							labelFont = labelFont.deriveFont(labelFont
									.getSize2D() - 3f);
							fm = getFontMetrics(labelFont);
							labelWidth = fm.stringWidth(di
									.getString(titleField));
							maxLength = length / 2;
							longestLine = edge;
						}
					}
				}
			}

			// set decorator at middle
			di.setX((longestLine.getP1().getX() + longestLine.getP2().getX()) / 2);
			di.setY((longestLine.getP1().getY() + longestLine.getP2().getY()) / 2);
			// rotate along line
			final double rotation = Math.toDegrees(Math.atan2(
					longestLine.getY2() - longestLine.getY1(),
					longestLine.getX2() - longestLine.getX1()));

			di.setDouble("rotation", rotation);
			di.setFont(labelFont);
		}
	}

	/**
	 * Return the point that is frac along the segment from P1, where frac is
	 * [0,1]
	 */
	public static Point2D interpolateLine2D(final Line2D line, final double frac) {
		final double newX = line.getP1().getX() * (1 - frac)
				+ line.getP2().getX() * frac;
		final double newY = line.getP1().getY() * (1 - frac)
				+ line.getP2().getY() * frac;
		return new Point2D.Double(newX, newY);
	}

	private static HashMap<Font, FontMetrics> fontmetrics = new HashMap<Font, FontMetrics>();

	public static FontMetrics getFontMetrics(final Font font) {
		if (fontmetrics.containsKey(font)) {
			return fontmetrics.get(font);
		}
		final FontMetrics fm = createFontMetrics(font);
		fontmetrics.put(font, fm);
		return fm;
	}

	private static FontMetrics createFontMetrics(final Font font) {
		BufferedImage bi = new BufferedImage(1, 1,
				BufferedImage.TYPE_INT_ARGB_PRE);
		final Graphics g = bi.getGraphics();
		final FontMetrics fm = g.getFontMetrics(font);
		g.dispose();
		bi = null;
		return fm;
	}

}
