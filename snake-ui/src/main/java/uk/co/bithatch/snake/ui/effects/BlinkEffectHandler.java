package uk.co.bithatch.snake.ui.effects;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.Sequence;
import uk.co.bithatch.snake.lib.effects.Matrix;
import uk.co.bithatch.snake.lib.layouts.Cell;
import uk.co.bithatch.snake.ui.CustomOptions;

public class BlinkEffectHandler extends AbstractEffectHandler<Sequence, CustomOptions> {

	private ScheduledFuture<?> task;
	private boolean on;
	private Matrix effect;
	private List<Cell> highlights = new ArrayList<>();

	public BlinkEffectHandler() {
	}

	@Override
	public void removed(Device device) {
	}

	@Override
	public void deactivate(Lit component) {
		synchronized (highlights) {
			task.cancel(false);
			on = false;
			frame(component);
		}
	}

	@Override
	public void update(Lit component) {
		getContext().getScheduler().execute(() -> component.updateEffect(effect));
	}

	public Matrix getEffect() {
		return effect;
	}

	@Override
	protected void onActivate(Lit component) {
		effect = component.createEffect(Matrix.class);
		int[] dw = Lit.getDevice(component).getMatrixSize();
		effect.setCells(new int[dw[0]][dw[1]][]);
		on = true;
		reset(component);
	}

	protected synchronized void reset(Lit component) {
		if (task != null)
			task.cancel(false);
		task = getContext().getScheduler().scheduleAtFixedRate(() -> {
			frame(component);
			on = !on;
		}, 0, 250, TimeUnit.MILLISECONDS);
	}

	@Override
	public Class<CustomOptions> getOptionsController() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSupported(Lit component) {
		return true;
	}

	@Override
	public URL getEffectImage(int size) {
		return null;
	}

	@Override
	public String getDisplayName() {
		return getClass().getSimpleName();
	}

	@Override
	protected void onStore(Lit component, CustomOptions controller) throws Exception {
	}

	void frame(Lit component) {
		synchronized (highlights) {
			for (Cell h : highlights) {
				effect.getCells()[h.getY()][h.getX()] = on ? Colors.COLOR_BLACK : new int[] { 0xff, 0xff, 0xff };
			}
		}
		update(component);
	}

	public void clear(Lit component) {

		synchronized (highlights) {
			highlights.clear();
			effect.clear();
			reset(component);
		}
	}

	public void highlight(Lit component, int matrixX, int matrixY) {
		highlight(component, new Cell(matrixX, matrixY));
	}

	public void highlight(Lit component, Cell... cells) {
		synchronized (highlights) {
			if (cells.length == highlights.size()) {
				for (Cell c : cells) {
					if (!highlights.contains(c)) {
						doHighlight(component, cells);
						return;
					}
				}
			} else
				doHighlight(component, cells);
		}
	}

	protected void doHighlight(Lit component, Cell... cells) {
		highlights.clear();
		effect.clear();
		highlights.addAll(Arrays.asList(cells));
		reset(component);
	}
}
