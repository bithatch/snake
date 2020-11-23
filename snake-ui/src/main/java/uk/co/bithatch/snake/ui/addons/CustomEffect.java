package uk.co.bithatch.snake.ui.addons;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import uk.co.bithatch.snake.lib.Interpolation;
import uk.co.bithatch.snake.lib.KeyFrame;
import uk.co.bithatch.snake.lib.Sequence;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.effects.CustomEffectHandler;

public class CustomEffect extends AbstractJsonAddOn {

	final static System.Logger LOG = System.getLogger(CustomEffect.class.getName());

	private Sequence sequence;
	private List<CustomEffectHandler> handlers = new ArrayList<>();

	CustomEffect(Path path, App context) throws IOException {
		super(path, context);
	}

	CustomEffect(JsonObject object, App context) throws IOException {
		super(object, context);
	}

	public List<CustomEffectHandler> getHandlers() {
		return handlers;
	}

	public CustomEffect(String id) {
		super(id);
	}

	public Sequence getSequence() {
		return sequence;
	}

	public void setSequence(Sequence sequence) {
		this.sequence = sequence;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public URL getScreenshot() {
		return null;
	}

	@Override
	public void close() throws Exception {
		for (CustomEffectHandler h : handlers) {
			if (h.getDevice() != null)
				context.getEffectManager().remove(h);
		}
	}

	@Override
	protected void construct(JsonObject addOnJson) {

		/* Sequence */
		JsonObject sequenceJson = addOnJson.get("sequence").getAsJsonObject();
		sequence = new Sequence();
		sequence.setFps(sequenceJson.has("fps") ? sequenceJson.get("fps").getAsInt() : 25);
		sequence.setSpeed(sequenceJson.has("speed") ? sequenceJson.get("speed").getAsFloat() : 1);
		sequence.setRepeat(sequenceJson.has("repeat") ? sequenceJson.get("repeat").getAsBoolean() : true);
		sequence.setInterpolation(
				sequenceJson.has("interpolation") ? Interpolation.get(sequenceJson.get("interpolation").getAsString())
						: Interpolation.linear);
		JsonArray frames = sequenceJson.get("frames").getAsJsonArray();
		for (JsonElement frame : frames) {

			JsonObject framesObject = frame.getAsJsonObject();

			/* Frame */
			KeyFrame keyFrame = new KeyFrame();
			keyFrame.setInterpolation(framesObject.has("interpolation")
					? Interpolation.get(framesObject.get("interpolation").getAsString())
					: Interpolation.sequence);
			keyFrame.setHoldFor(framesObject.has("holdFor") ? framesObject.get("holdFor").getAsLong() : 0);

			/* Rows */
			JsonArray rows = framesObject.get("rows").getAsJsonArray();
			int[][][] rowsArray = new int[rows.size()][][];
			int rowIndex = 0;
			for (JsonElement row : rows) {
				List<int[]> colsArray = new ArrayList<>();
				JsonArray cols = row.getAsJsonArray();
				for (JsonElement col : cols) {
					JsonArray rgb = col.getAsJsonArray();
					colsArray.add(new int[] { rgb.get(0).getAsInt(), rgb.get(1).getAsInt(), rgb.get(2).getAsInt() });
				}
				rowsArray[rowIndex++] = colsArray.toArray(new int[0][]);
				rowIndex++;
			}
			keyFrame.setFrame(rowsArray);
			sequence.add(keyFrame);
		}
	}

	@Override
	protected void store(JsonObject addOnJson) {

		JsonObject sequenceInfo = new JsonObject();
		sequenceInfo.addProperty("repeat", sequence.isRepeat());
		sequenceInfo.addProperty("fps", sequence.getFps());
		sequenceInfo.addProperty("speed", sequence.getSpeed());
		sequenceInfo.addProperty("interpolation", sequence.getInterpolation().getName());

		JsonArray frameInfo = new JsonArray();
		for (KeyFrame kf : sequence) {
			JsonObject keyFrameInfo = new JsonObject();
			keyFrameInfo.addProperty("interpolation", kf.getInterpolation().getName());
			keyFrameInfo.addProperty("holdFor", kf.getHoldFor());

			JsonArray rowInfo = new JsonArray();

			for (int[][] row : kf.getFrame()) {
				JsonArray colInfo = new JsonArray();
				for (int[] col : row) {
					JsonArray rgb = new JsonArray();
					rgb.add(col[0]);
					rgb.add(col[1]);
					rgb.add(col[2]);
					colInfo.add(rgb);
				}
				rowInfo.add(colInfo);
			}

			keyFrameInfo.add("rows", rowInfo);

			frameInfo.add(keyFrameInfo);
		}
		sequenceInfo.add("frames", frameInfo);

		addOnJson.add("sequence", sequenceInfo);
	}

}
