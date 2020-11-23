/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package uk.co.bithatch.snake.ui;

import static javafx.scene.control.SelectionMode.SINGLE;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import uk.co.bithatch.snake.ui.util.BasicList;

/**
 * Largely copied from <code>MultipleSelectionModelBase</code> that is package
 * protected (grr).
 *
 * @param <I> type of item
 */
public class ListMultipleSelectionModel<I> extends MultipleSelectionModel<I> {

	abstract class SelectedItemsReadOnlyObservableList<E> extends ObservableListBase<E> {

		// This is the actual observable list of selected indices used in the selection
		// model
		private final ObservableList<Integer> selectedIndices;
		private final Supplier<Integer> modelSizeSupplier;
		private final List<WeakReference<E>> itemsRefList;

		public SelectedItemsReadOnlyObservableList(ObservableList<Integer> selectedIndices,
				Supplier<Integer> modelSizeSupplier) {
			this.modelSizeSupplier = modelSizeSupplier;
			this.selectedIndices = selectedIndices;
			this.itemsRefList = new ArrayList<>();

			selectedIndices.addListener((ListChangeListener<Integer>) c -> {
				beginChange();

				while (c.next()) {
					if (c.wasReplaced()) {
						List<E> removed = getRemovedElements(c);
						List<E> added = getAddedElements(c);
						if (!removed.equals(added)) {
							nextReplace(c.getFrom(), c.getTo(), removed);
						}
					} else if (c.wasAdded()) {
						nextAdd(c.getFrom(), c.getTo());
					} else if (c.wasRemoved()) {
						int removedSize = c.getRemovedSize();
						if (removedSize == 1) {
							nextRemove(c.getFrom(), getRemovedModelItem(c.getFrom()));
						} else {
							nextRemove(c.getFrom(), getRemovedElements(c));
						}
					} else if (c.wasPermutated()) {
						int[] permutation = new int[size()];
						for (int i = 0; i < size(); i++) {
							permutation[i] = c.getPermutation(i);
						}
						nextPermutation(c.getFrom(), c.getTo(), permutation);
					} else if (c.wasUpdated()) {
						for (int i = c.getFrom(); i < c.getTo(); i++) {
							nextUpdate(i);
						}
					}
				}

				// regardless of the change, we recreate the itemsRefList to reflect the current
				// items list.
				// This is important for cases where items are removed (and so must their
				// selection, but we lose
				// access to the item before we can fire the event).
				// FIXME we could make this more efficient by only making the reported changes
				// to the list
				itemsRefList.clear();
				for (int selectedIndex : selectedIndices) {
					itemsRefList.add(new WeakReference<>(getModelItem(selectedIndex)));
				}

				endChange();
			});
		}

		protected abstract E getModelItem(int index);

		@Override
		public E get(int index) {
			int pos = selectedIndices.get(index);
			return getModelItem(pos);
		}

		@Override
		public int size() {
			return selectedIndices.size();
		}

		private E _getModelItem(int index) {
			if (index >= modelSizeSupplier.get()) {
				// attempt to return from the itemsRefList instead
				return getRemovedModelItem(index);
			} else {
				return getModelItem(index);
			}
		}

		private E getRemovedModelItem(int index) {
			// attempt to return from the itemsRefList instead
			return index < 0 || index >= itemsRefList.size() ? null : itemsRefList.get(index).get();
		}

		private List<E> getRemovedElements(ListChangeListener.Change<? extends Integer> c) {
			List<E> removed = new ArrayList<>(c.getRemovedSize());
			final int startPos = c.getFrom();
			for (int i = startPos, max = startPos + c.getRemovedSize(); i < max; i++) {
				removed.add(getRemovedModelItem(i));
			}
			return removed;
		}

		private List<E> getAddedElements(ListChangeListener.Change<? extends Integer> c) {
			List<E> added = new ArrayList<>(c.getAddedSize());
			for (int index : c.getAddedSubList()) {
				added.add(_getModelItem(index));
			}
			return added;
		}
	}

	private ObservableList<Integer> indices = new BasicList<Integer>();
	private ObservableList<I> model;
	private boolean atomic;
	private int focused = -1;
	private ListMultipleSelectionModel<I>.SelectedItemsReadOnlyObservableList<I> selectedItems;

	public ListMultipleSelectionModel(ObservableList<I> model) {
		super();
		this.model = model;
		selectedItems = new SelectedItemsReadOnlyObservableList<I>(indices, () -> getItemCount()) {
			@Override
			protected I getModelItem(int index) {
				return ListMultipleSelectionModel.this.getModelItem(index);
			}
		};
	}

	@Override
	public ObservableList<Integer> getSelectedIndices() {
		return indices;
	}

	@Override
	public ObservableList<I> getSelectedItems() {
		return selectedItems;
	}

	@Override
	public void selectIndices(int row, int... rows) {
		if (rows == null || rows.length == 0) {
			select(row);
			return;
		}

		/*
		 * Performance optimisation - if multiple selection is disabled, only process
		 * the end-most row index.
		 */

		int rowCount = getItemCount();

		if (getSelectionMode() == SINGLE) {
			quietClearSelection();

			for (int i = rows.length - 1; i >= 0; i--) {
				int index = rows[i];
				if (index >= 0 && index < rowCount) {
					indices.add(index);
					select(index);
					break;
				}
			}

			if (indices.isEmpty()) {
				if (row > 0 && row < rowCount) {
					indices.add(row);
					select(row);
				}
			}
		} else {
			indices.add(row);
			for (int i : rows)
				indices.add(i);

			IntStream.concat(IntStream.of(row), IntStream.of(rows)).filter(index -> index >= 0 && index < rowCount)
					.reduce((first, second) -> second).ifPresent(lastIndex -> {
						setSelectedIndex(lastIndex);
						focus(lastIndex);
						setSelectedItem(getModelItem(lastIndex));
					});
		}
	}

	public void selectAll() {
		if (getSelectionMode() == SINGLE)
			return;

		if (getItemCount() <= 0)
			return;

		final int rowCount = getItemCount();
		final int focusedIndex = getFocusedIndex();

		// set all selected indices to true
		clearSelection();
		List<Integer> l = new ArrayList<>();
		for (int i = 0; i < rowCount; i++)
			l.add(i);
		indices.addAll(l);

		if (focusedIndex == -1) {
			setSelectedIndex(rowCount - 1);
			focus(rowCount - 1);
		} else {
			setSelectedIndex(focusedIndex);
			focus(focusedIndex);
		}
	}

	@Override
	public void selectFirst() {
		if (getSelectionMode() == SINGLE) {
			quietClearSelection();
		}

		if (getItemCount() > 0) {
			select(0);
		}
	}

	@Override
	public void selectLast() {
		if (getSelectionMode() == SINGLE) {
			quietClearSelection();
		}

		int numItems = getItemCount();
		if (numItems > 0 && getSelectedIndex() < numItems - 1) {
			select(numItems - 1);
		}
	}

	@Override
	public void clearAndSelect(int row) {
		if (row < 0 || row >= getItemCount()) {
			clearSelection();
			return;
		}

		final boolean wasSelected = isSelected(row);

		// RT-33558 if this method has been called with a given row, and that
		// row is the only selected row currently, then this method becomes a no-op.
		if (wasSelected && getSelectedIndices().size() == 1) {
			// before we return, we double-check that the selected item
			// is equal to the item in the given index
			if (getSelectedItem() == getModelItem(row)) {
				return;
			}
		}

		// firstly we make a copy of the selection, so that we can send out
		// the correct details in the selection change event.
		// We remove the new selection from the list seeing as it is not removed.
//		BitSet selectedIndicesCopy = new BitSet();
//		selectedIndicesCopy.or(selectedIndices.bitset);
//		selectedIndicesCopy.clear(row);
//		List<Integer> previousSelectedIndices = new SelectedIndicesList(selectedIndicesCopy);

		ObservableList<Integer> previousSelectedIndices = new BasicList<Integer>();
		previousSelectedIndices.addAll(indices);

		// RT-32411 We used to call quietClearSelection() here, but this
		// resulted in the selectedItems and selectedIndices lists never
		// reporting that they were empty.
		// makeAtomic toggle added to resolve RT-32618

		// then clear the current selection

		// TODO: SNAKE ... had to move the clear selection out of the weak
		// implementation of 'startAtomic'.
		// or selection is not cleared properly. The choice is to copy yet MORE package
		// protected and private code (will even require some more classes) from the
		// private javafx API to completely duplicate the multipl section model. 
		// or find a different way. Why is this so hard :(
		clearSelection();

		// and select the new row
		startAtomic();
//		clearSelection();
		select(row);
		stopAtomic();

		// fire off a single add/remove/replace notification (rather than
		// individual remove and add notifications) - see RT-33324
//		ListChangeListener.Change<Integer> change;

		/*
		 * getFrom() documentation: If wasAdded is true, the interval contains all the
		 * values that were added. If wasPermutated is true, the interval marks the
		 * values that were permutated. If wasRemoved is true and wasAdded is false,
		 * getFrom() and getTo() should return the same number - the place where the
		 * removed elements were positioned in the list.
		 */
//		if (wasSelected) {
//			change = ControlUtils.buildClearAndSelectChange(selectedIndices, previousSelectedIndices, row);
//		} else {
//			int changeIndex = Math.max(0, selectedIndices.indexOf(row));
//			change = new NonIterableChange.GenericAddRemoveChange<>(changeIndex, changeIndex + 1,
//					previousSelectedIndices, selectedIndices);
//		}
//
//		selectedIndices.callObservers(change);
	}

	@Override
	public void select(int row) {
		if (row == -1) {
			clearSelection();
			return;
		}
		if (row < 0 || row >= getItemCount()) {
			return;
		}

		int selectedIndex = getSelectedIndex();
		boolean isSameRow = row == selectedIndex;
		I currentItem = getSelectedItem();
		I newItem = getModelItem(row);
		boolean isSameItem = newItem != null && newItem.equals(currentItem);
		boolean fireUpdatedItemEvent = isSameRow && !isSameItem;

		// focus must come first so that we have the anchors set appropriately
		focus(row);
//
		if (!indices.contains(row)) {
			if (getSelectionMode() == SINGLE) {
				startAtomic();
				quietClearSelection();
				stopAtomic();
			}
			indices.add(row);
		}

		setSelectedIndex(row);

		if (fireUpdatedItemEvent) {
			setSelectedItem(newItem);
		}
	}

	protected void focus(int index) {
		this.focused = index;
	}

	protected int getFocusedIndex() {
		return focused;
	}

	protected I getModelItem(int row) {
		return model.get(row);
	}

	protected int getItemCount() {
		return model.size();
	}

	@Override
	public void select(I obj) {

		if (obj == null && getSelectionMode() == SelectionMode.SINGLE) {
			clearSelection();
			return;
		}

		// We have no option but to iterate through the model and select the
		// first occurrence of the given object. Once we find the first one, we
		// don't proceed to select any others.
		Object rowObj = null;
		for (int i = 0, max = getItemCount(); i < max; i++) {
			rowObj = getModelItem(i);
			if (rowObj == null)
				continue;

			if (rowObj.equals(obj)) {
				if (isSelected(i)) {
					return;
				}

				if (getSelectionMode() == SINGLE) {
					quietClearSelection();
				}

				select(i);
				return;
			}
		}

		// if we are here, we did not find the item in the entire data model.
		// Even still, we allow for this item to be set to the give object.
		// We expect that in concrete subclasses of this class we observe the
		// data model such that we check to see if the given item exists in it,
		// whilst SelectedIndex == -1 && SelectedItem != null.
		setSelectedIndex(-1);
		setSelectedItem(obj);
	}

	@Override
	public void clearSelection(int index) {
		if (index < 0)
			return;

		// TODO shouldn't directly access like this
		// TODO might need to update focus and / or selected index/item
		boolean wasEmpty = indices.isEmpty();
		indices.remove((Integer) index);

		if (!wasEmpty && indices.isEmpty()) {
			clearSelection();
		}
	}

	@Override
	public void clearSelection() {
		quietClearSelection();
		if (!isAtomic()) {
			setSelectedIndex(-1);
			focus(-1);
		}
	}

	@Override
	public boolean isSelected(int index) {
		if (index >= 0 && index < model.size()) {
			return indices.contains(index);
		}

		return false;
	}

	@Override
	public boolean isEmpty() {
		return indices.isEmpty();
	}

	@Override
	public void selectPrevious() {
		int focusIndex = getFocusedIndex();

		if (getSelectionMode() == SINGLE) {
			quietClearSelection();
		}

		if (focusIndex == -1) {
			select(getItemCount() - 1);
		} else if (focusIndex > 0) {
			select(focusIndex - 1);
		}
	}

	@Override
	public void selectNext() {
		int focusIndex = getFocusedIndex();

		if (getSelectionMode() == SINGLE) {
			quietClearSelection();
		}

		if (focusIndex == -1) {
			select(0);
		} else if (focusIndex != getItemCount() - 1) {
			select(focusIndex + 1);
		}
	}

	void startAtomic() {
		atomic = true;
	}

	void stopAtomic() {
		atomic = false;
	}

	boolean isAtomic() {
		return atomic;
	}

	private void quietClearSelection() {
		indices.clear();
	}

}
