package setvis.shape;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

import setvis.SetOutline;
import setvis.VecUtil;

public class ShapeSimplifier extends AbstractShapeGenerator {

	private final AbstractShapeGenerator parent;

	private double tolerance;

	public ShapeSimplifier(final AbstractShapeGenerator parent) {
		this(parent, 0.0);
	}

	public ShapeSimplifier(final AbstractShapeGenerator parent,
			final double tolerance) {
		super(parent.getSetOutline());
		this.parent = parent;
		// proper initialization of the tolerance
		setTolerance(tolerance);
	}

	@Override
	public void setRadius(final double radius) {
		if (parent != null) {
			parent.setRadius(radius);
		}
	}

	@Override
	public double getRadius() {
		return parent.getRadius();
	}

	private double tsqr;

	public void setTolerance(final double tolerance) {
		this.tolerance = tolerance;
		tsqr = tolerance * tolerance;
	}

	public double getTolerance() {
		return tolerance;
	}

	@Override
	public SetOutline getSetOutline() {
		return parent.getSetOutline();
	}

	private class State {

		private final boolean closed;

		private final Point2D[] list;

		private final int start;

		private int end;

		public State(final Point2D[] list, final boolean closed, final int start) {
			this.list = list;
			this.closed = closed;
			this.start = start;
			end = start + 1;
		}

		public void advanceEnd() {
			++end;
		}

		public void decreaseEnd() {
			--end;
		}

		public int getEndIndex() {
			return end;
		}

		public boolean validEnd() {
			return closed ? end < list.length : end < list.length - 1;
		}

		public Point2D getEnd() {
			return list[end % list.length];
		}

		public Point2D getStart() {
			return list[start];
		}

		private double lineDst(final int i) {
			return VecUtil.distPointLineSqr(getStart(), getEnd(), list[i]);
		}

		public boolean canTakeNext() {
			if (!validEnd()) {
				return false;
			}
			boolean ok = true;
			advanceEnd();
			for (int i = start + 1; i < end; ++i) {
				if (lineDst(i) > tsqr) {
					ok = false;
					break;
				}
			}
			decreaseEnd();
			return ok;
		}

	}

	private static Point2D[] createArrayFrom(final List<State> states) {
		final Point2D[] res = new Point2D[states.size()];
		int p = 0;
		for (final State s : states) {
			res[p++] = s.getStart();
		}
		return res;
	}

	@Override
	public Shape convertToShape(final Point2D[] points, final boolean closed) {
		if (points.length < 3) {
			return parent.convertToShape(points, closed);
		}
		final List<State> states = new LinkedList<State>();
		int start = 0;
		while (start < points.length) {
			final State s = new State(points, closed, start);
			while (s.canTakeNext()) {
				s.advanceEnd();
			}
			start = s.getEndIndex();
			states.add(s);
		}
		return parent.convertToShape(createArrayFrom(states), closed);
	}

}
