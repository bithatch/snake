package uk.co.bithatch.snake.ui.util;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ModifiableObservableListBase;

public class BasicList<J> extends ModifiableObservableListBase<J> {
	private List<J> list;

	public BasicList() {
		this(new ArrayList<>());
	}

	public BasicList(List<J> list) {
		this.list = list;
	}

	public List<J> getList() {
		return list;
	}

	public void setList(List<J> list) {
		this.list = list;
	}

	@Override
	public J get(int index) {
		return list.get(index);
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	protected void doAdd(int index, J element) {
		list.add(index, element);
	}

	@Override
	protected J doSet(int index, J element) {
		return list.set(index, element);
	}

	@Override
	protected J doRemove(int index) {
		return list.remove(index);
	}

}