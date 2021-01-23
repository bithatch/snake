package uk.co.bithatch.snake.ui.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class CompoundIterator<I> implements Iterator<I> {

	private List<Iterator<? extends I>> iterators = new LinkedList<>();
	private Iterator<Iterator<? extends I>> iteratorsIterator;
	private Iterator<? extends I> iterator;
	private I element;
	private I current;

	public CompoundIterator() {
	}

	@SuppressWarnings("unchecked")
	public CompoundIterator(Iterator<? extends I>... iterators) {
		this.iterators.addAll(Arrays.asList(iterators));
	}

	public void addIterator(Iterator<? extends I> iterator) {
		if (iteratorsIterator != null)
			throw new IllegalStateException("Cannot add iterators after started iterating.");
		iterators.add(iterator);
	}

	@Override
	public void remove() {
		if(current == null)
			throw new IllegalStateException();
		iterator.remove();
	}

	@Override
	public boolean hasNext() {
		checkNext();
		return iterator != null;
	}

	private void checkNext() {
		if (element == null) {
			if (iteratorsIterator == null)
				iteratorsIterator = iterators.iterator();

			while (true) {
				if (iterator == null)
					if (iteratorsIterator.hasNext())
						iterator = iteratorsIterator.next();
					else
						break;

				if (iterator.hasNext()) {
					element = iterator.next();
					break;
				} else
					iterator = null;
			}
		}
	}

	@Override
	public I next() {
		checkNext();
		if (element == null)
			throw new NoSuchElementException();
		try {
			return element;
		} finally {
			current = element;
			element = null;
		}
	}
}
