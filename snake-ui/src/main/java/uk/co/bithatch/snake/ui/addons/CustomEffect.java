package uk.co.bithatch.snake.ui.addons;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import uk.co.bithatch.snake.lib.animation.AudioParameters;
import uk.co.bithatch.snake.lib.animation.Interpolation;
import uk.co.bithatch.snake.lib.animation.KeyFrame;
import uk.co.bithatch.snake.lib.animation.KeyFrameCell;
import uk.co.bithatch.snake.lib.animation.Sequence;
import uk.co.bithatch.snake.lib.animation.KeyFrame.KeyFrameCellSource;
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
		if (sequenceJson.has("audio")) {
			sequence.setAudioParameters(createAudioParametersFromJson(sequenceJson.get("audio").getAsJsonObject()));
		}
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
			KeyFrameCell[][] rowsArray = new KeyFrameCell[rows.size()][];
			int rowIndex = 0;
			for (JsonElement row : rows) {
				List<KeyFrameCell> colsArray = new ArrayList<>();
				JsonArray cols = row.getAsJsonArray();
				for (JsonElement col : cols) {
					if (col.isJsonArray()) {
						/* Used versions 1.0-SNAPSHOT-24 and earlier, every cell was a colour */
						JsonArray rgb = col.getAsJsonArray();
						KeyFrameCell cell = new KeyFrameCell(
								new int[] { rgb.get(0).getAsInt(), rgb.get(1).getAsInt(), rgb.get(2).getAsInt() });
						colsArray.add(cell);
					} else {
						/* Newer builds, each cell is an object */
						JsonObject job = col.getAsJsonObject();
						JsonArray arr = job.get("value").getAsJsonArray();
						JsonArray sources = job.get("sources").getAsJsonArray();
						var srcs = new ArrayList<>();
						for (JsonElement e : sources) {
							srcs.add(KeyFrameCellSource.valueOf(e.getAsString()));
						}
						KeyFrameCell cell = new KeyFrameCell(
								new int[] { arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt() },
								srcs.toArray(new KeyFrameCellSource[0]));
						cell.setInterpolation(job.has("interpolation")
								? Interpolation.fromName(job.get("interpolation").getAsString())
								: Interpolation.keyframe);
						colsArray.add(cell);
					}
				}
				rowsArray[rowIndex++] = colsArray.toArray(new KeyFrameCell[0]);
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
		if (sequence.getAudioParameters() != null)
			sequenceInfo.add("audio", getAudioParametersJson(sequence.getAudioParameters()));

		JsonArray frameInfo = new JsonArray();
		for (KeyFrame kf : sequence) {
			JsonObject keyFrameInfo = new JsonObject();
			keyFrameInfo.addProperty("interpolation", kf.getInterpolation().getName());
			keyFrameInfo.addProperty("holdFor", kf.getHoldFor());

			JsonArray rowInfo = new JsonArray();

			for (KeyFrameCell[] row : kf.getFrame()) {
				JsonArray colInfo = new JsonArray();
				for (KeyFrameCell col : row) {

					JsonObject keycellJson = new JsonObject();
					JsonArray rgb = new JsonArray();
					rgb.add(col.getValues()[0]);
					rgb.add(col.getValues()[1]);
					rgb.add(col.getValues()[2]);
					JsonArray srcs = new JsonArray();
					srcs.add(col.getSources()[0].name());
					srcs.add(col.getSources()[1].name());
					srcs.add(col.getSources()[2].name());
					keycellJson.add("sources", srcs);
					keycellJson.addProperty("interpolation", col.getInterpolation().getName());
					keycellJson.add("value", rgb);
					colInfo.add(keycellJson);
				}
				rowInfo.add(colInfo);
			}

			keyFrameInfo.add("rows", rowInfo);

			frameInfo.add(keyFrameInfo);
		}
		sequenceInfo.add("frames", frameInfo);

		addOnJson.add("sequence", sequenceInfo);
	}

	private AudioParameters createAudioParametersFromJson(JsonObject json) {
		AudioParameters p = new AudioParameters();
		p.setLow(json.has("low") ? json.get("low").getAsInt() : 0);
		p.setHigh(json.has("high") ? json.get("high").getAsInt() : 255);
		p.setGain(json.has("gain") ? json.get("gain").getAsFloat() : 1.0f);
		return p;
	}

	private JsonElement getAudioParametersJson(AudioParameters audioParameters) {
		JsonObject obj = new JsonObject();
		obj.addProperty("low", audioParameters.getLow());
		obj.addProperty("high", audioParameters.getHigh());
		obj.addProperty("gain", audioParameters.getGain());
		return obj;
	}

}
