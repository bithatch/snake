package uk.co.bithatch.snake.ui.util;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ModifiableObservableListBase;

public abstract class ListWrapper<I, O> extends ModifiableObservableListBase<O> {
	private List<I> list;

	public ListWrapper() {
		this(new ArrayList<>());
	}

	public ListWrapper(List<I> list) {
		this.list = list;
	}

	public List<I> getList() {
		return list;
	}

	public void setList(List<I> list) {
		this.list = list;
	}

	@Override
	public O get(int index) {
		return convertToWrapped(list.get(index));
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	protected void doAdd(int index, O element) {
		list.add(index, convertToNative(element));
	}

	@Override
	protected O doSet(int index, O element) {
		return convertToWrapped(list.set(index, convertToNative(element)));
	}

	@Override
	protected O doRemove(int index) {
		return convertToWrapped(list.remove(index));
	}

	O convertToWrapped(I in) {
		return in == null ? null : doConvertToWrapped(in);
	}

	I convertToNative(O in) {
		return in == null ? null : doConvertToNative(in);
	}

	protected abstract O doConvertToWrapped(I in);

	protected I doConvertToNative(O in) {
		throw new UnsupportedOperationException();
	}
}