package setvis.bubbleset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FastList<E> extends ArrayList<E> {

	private static final long serialVersionUID = 2901108923922468511L;

	private final Set<E> set = new HashSet<E>();

	public FastList() {
		super();
	}

	public FastList(final int size) {
		super(size);
	}

	public FastList(final Collection<? extends E> c) {
		super(c);
		set.addAll(c);
	}

	@Override
	public boolean add(final E e) {
		set.add(e);
		return super.add(e);
	}

	@Override
	public void add(final int index, final E element) {
		set.add(element);
		super.add(index, element);
	}

	@Override
	public boolean addAll(final Collection<? extends E> c) {
		set.addAll(c);
		return super.addAll(c);
	}

	@Override
	public boolean addAll(final int index, final Collection<? extends E> c) {
		set.addAll(c);
		return super.addAll(index, c);
	}

	@Override
	public boolean contains(final Object o) {
		return set.contains(o);
	}

	@Override
	public void clear() {
		set.clear();
		super.clear();
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E remove(final int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(final Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E set(final int index, final E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void removeRange(final int fromIndex, final int toIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException();
	}

}
