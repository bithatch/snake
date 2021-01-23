/* Copyright 2018 Jesper Öqvist
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package uk.co.bithatch.snake.widgets;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.image.PixelFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * HSV color palette for a simple JavaFX color picker.
 *
 * <p>The color palette shows a Hue gradient for picking the Hue value,
 * and a 2D HSV-gradient displaying a slice of the HSV color cube which
 * can be clicked to select the Saturation and Value components.
 * The color palette also has some swatches displaying neighbour colors
 * and previously selected colors. There is a large color swatch showing
 * the current color.
 *
 * <p>A cancel button is at the bottom right of the color palette, and the
 * current HTML color code is displayed in a text field at the bottom of the
 * palette.
 */
public class LuxColorPalette extends Region implements Initializable {
  private static final WritablePixelFormat<IntBuffer> PIXEL_FORMAT =
      PixelFormat.getIntArgbInstance();

  /** Cached hue gradient image. */
  private static final Image gradientImage;
  static {
    List<double[]> gradient = new ArrayList<>();
    gradient.add(new double[] { 1, 0, 0, 0.00 });
    gradient.add(new double[] { 1, 1, 0, 1 / 6.0 });
    gradient.add(new double[] { 0, 1, 0, 2 / 6.0 });
    gradient.add(new double[] { 0, 1, 1, 3 / 6.0 });
    gradient.add(new double[] { 0, 0, 1, 4 / 6.0 });
    gradient.add(new double[] { 1, 0, 1, 5 / 6.0 });
    gradient.add(new double[] { 1, 0, 0, 1.00 });
    gradientImage = drawGradient(522, 75, gradient);
  } 

  private final ColorChooser colorPicker;
  private Random random = new Random();

  private @FXML VBox palette;
  private @FXML ImageView huePicker;
  private @FXML Canvas colorSample;
  private @FXML TextField webColorCode;
  private @FXML Button saveBtn;
  private @FXML Button cancelBtn;
  private @FXML StackPane satValueRect;
  private @FXML Pane huePickerOverlay;
  private @FXML Region sample0;
  private @FXML Region sample1;
  private @FXML Region sample2;
  private @FXML Region sample3;
  private @FXML Region sample4;
  private @FXML Region history0;
  private @FXML Region history1;
  private @FXML Region history2;
  private @FXML Region history3;
  private @FXML Region history4;

  private final DoubleProperty hue = new SimpleDoubleProperty();
  private final DoubleProperty saturation = new SimpleDoubleProperty();
  private final DoubleProperty value = new SimpleDoubleProperty();

  private final Circle satValIndicator = new Circle(9);
  private final Rectangle hueIndicator = new Rectangle(20, 69);

  private Region[] sample;
  private Region[] history;

  /** Updates the current color based on changes to the web color code. */
  private ChangeListener<String> webColorListener = (observable, oldValue, newValue) -> {
    try {
      editingWebColorCode = true;
      Color color = Color.web(newValue);
      hue.set(color.getHue() / 360);
      saturation.set(color.getSaturation());
      value.set(color.getBrightness());
    } catch (IllegalArgumentException e) {
      // Harmless exception - ignored.
    } finally {
      editingWebColorCode = false;
    }
  };

  /**
   * Set to true while the web color code listener is modifying the selected color.
   *
   * <p>When this is set to true the updating of the HTML color code is disabled to
   * avoid update loops.
   */
  private boolean editingWebColorCode = false;

  public LuxColorPalette() {
	  this(new ColorChooser() {

			private ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.CRIMSON);
		@Override
		public void updateHistory() {
		}
		
		@Override
		public void revertToOriginalColor() {
		}
		
		@Override
		public void hidePopup() {
		}
		
		@Override
		public Color getColor() {
			return color.get();
		}
		
		@Override
		public ObjectProperty<Color> colorProperty() {
			return color;
		}
	});
  }
  
  public LuxColorPalette(ColorChooser colorPicker) {
    this.colorPicker = colorPicker;
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("LuxColorPalette.fxml"));
      loader.setController(this);
      getChildren().add(loader.load());
      addEventFilter(KeyEvent.KEY_PRESSED, e -> {
        if (e.getCode() == KeyCode.ESCAPE) {
          e.consume();
          colorPicker.revertToOriginalColor();
          colorPicker.hidePopup();
        }
      });
    } catch (IOException e) {
      throw new Error(e);
    }
  }


  @Override public void initialize(URL location, ResourceBundle resources) {
    sample = new Region[] { sample0, sample1, sample2, sample3, sample4 };
    history = new Region[] { history0, history1, history2, history3, history4 };

    // Handle color selection on click.
    colorSample.setOnMouseClicked(event -> {
      colorPicker.updateHistory();
      colorPicker.hidePopup();
    });

    webColorCode.textProperty().addListener(webColorListener);

    saveBtn.setOnAction(event -> {
      colorPicker.updateHistory();
      colorPicker.hidePopup();
    });

    saveBtn.setDefaultButton(true);

    cancelBtn.setOnAction(event -> {
      colorPicker.revertToOriginalColor();
      colorPicker.hidePopup();
    });

    satValueRect.setBackground(
        new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
    satValueRect.backgroundProperty().bind(new ObjectBinding<Background>() {
      {
        bind(hue);
      }

      @Override protected Background computeValue() {
        return new Background(
            new BackgroundFill(Color.hsb(hue.get() * 360, 1.0, 1.0), CornerRadii.EMPTY,
                Insets.EMPTY));
      }
    });

    Pane saturationOverlay = new Pane();
    saturationOverlay.setBackground(new Background(new BackgroundFill(
        new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 255, 255, 1.0)), new Stop(1, Color.rgb(255, 255, 255, 0.0))),
        CornerRadii.EMPTY, Insets.EMPTY)));

    Pane valueOverlay = new Pane();
    valueOverlay.setBackground(new Background(new BackgroundFill(
        new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(0, 0, 0, 1.0)), new Stop(1, Color.rgb(0, 0, 0, 0.0))),
        CornerRadii.EMPTY, Insets.EMPTY)));

    satValIndicator.layoutXProperty().bind(saturation.multiply(256));
    satValIndicator.layoutYProperty().bind(Bindings.subtract(1, value).multiply(256));
    satValIndicator.setStroke(Color.WHITE);
    satValIndicator.fillProperty().bind(new ObjectBinding<Paint>() {
      {
        bind(hue);
        bind(saturation);
        bind(value);
      }

      @Override protected Paint computeValue() {
        return Color.hsb(hue.get() * 360, saturation.get(), value.get());
      }
    });
    satValIndicator.setStrokeWidth(2);
    satValIndicator.setMouseTransparent(true);
    satValIndicator.setEffect(new DropShadow(5, Color.BLACK));

    hueIndicator.setMouseTransparent(true);
    hueIndicator.setTranslateX(-10);
    hueIndicator.setTranslateY(3);
    hueIndicator.layoutXProperty().bind(hue.multiply(huePicker.fitWidthProperty()));
    hueIndicator.fillProperty().bind(new ObjectBinding<Paint>() {
      {
        bind(hue);
      }

      @Override protected Paint computeValue() {
        return Color.hsb(hue.get() * 360, 1.0, 1.0);
      }
    });
    hueIndicator.setStroke(Color.WHITE);
    hueIndicator.setStrokeWidth(2);
    hueIndicator.setEffect(new DropShadow(5, Color.BLACK));

    huePickerOverlay.getChildren().add(hueIndicator);
    huePickerOverlay.setClip(new Rectangle(522, 75));

    valueOverlay.getChildren().add(satValIndicator);
    valueOverlay.setClip(new Rectangle(256, 256)); // Clip the indicator circle.

    satValueRect.getChildren().addAll(saturationOverlay, valueOverlay);

    setStyle(
        "-fx-background-color: -fx-background;"
            + "-fx-background-insets: 0;"
            + "-fx-background-radius: 4px");

    DropShadow dropShadow = new DropShadow();
    dropShadow.setColor(Color.color(0, 0, 0, 0.8));
    dropShadow.setWidth(18);
    dropShadow.setHeight(18);
    setEffect(dropShadow);

    setHueGradient();

    EventHandler<MouseEvent> hueMouseHandler =
        event -> hue.set(clamp(event.getX() / huePicker.getFitWidth()));

    huePickerOverlay.setOnMouseDragged(hueMouseHandler);
    huePickerOverlay.setOnMousePressed(hueMouseHandler);
    huePickerOverlay.setOnMouseReleased(event -> updateRandomColorSamples());

    EventHandler<MouseEvent> mouseHandler = event -> {
      saturation.set(clamp(event.getX() / satValueRect.getWidth()));
      value.set(clamp(1 - event.getY() / satValueRect.getHeight()));
    };

    valueOverlay.setOnMousePressed(mouseHandler);
    valueOverlay.setOnMouseDragged(mouseHandler);
    valueOverlay.setOnMouseReleased(event -> updateRandomColorSamples());

    hue.addListener((observable, oldValue, newValue) -> updateCurrentColor(newValue.doubleValue(),
        saturation.get(), value.get()));
    saturation.addListener(
        (observable, oldValue, newValue) -> updateCurrentColor(hue.get(), newValue.doubleValue(),
            value.get()));
    value.addListener(
        (observable, oldValue, newValue) -> updateCurrentColor(hue.get(), saturation.get(),
            newValue.doubleValue()));

    EventHandler<MouseEvent> swatchClickHandler = event -> {
      if (event.getSource() instanceof Region) {
        Region swatch = (Region) event.getSource();
        if (!swatch.getBackground().getFills().isEmpty()) {
          Color color = (Color) swatch.getBackground().getFills().get(0).getFill();
          hue.set(color.getHue() / 360);
          saturation.set(color.getSaturation());
          value.set(color.getBrightness());
        }
      }
    };

    for (Region region : history) {
      region.setOnMouseClicked(swatchClickHandler);
    }

    for (Region region : sample) {
      region.setOnMouseClicked(swatchClickHandler);
    }
    // Initialize history with random colors.
    for (Region swatch : history) {
      swatch.setBackground(new Background(
          new BackgroundFill(getRandomNearColor(colorPicker.getColor()), CornerRadii.EMPTY,
              Insets.EMPTY)));
    }
  }

  /**
   * Updates the nearby random color samples.
   */
  void updateRandomColorSamples() {
    Color color = colorPicker.getColor();
    for (Region swatch : sample) {
      swatch.setBackground(new Background(
          new BackgroundFill(getRandomNearColor(color), CornerRadii.EMPTY, Insets.EMPTY)));
    }
  }

  protected void setColor(Color color) {
    hue.set(color.getHue() / 360);
    saturation.set(color.getSaturation());
    value.set(color.getBrightness());
  }

  protected void addToHistory(Color color) {
    for (int i = history.length - 1; i >= 1; i -= 1) {
      history[i].setBackground(history[i - 1].getBackground());
    }
    history[0].setBackground(new Background(
        new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
  }

  private void setHueGradient() {
    huePicker.setImage(gradientImage);
  }

  private static double clamp(double value) {
    return (value < 0) ? 0 : (value > 1 ? 1 : value);
  }

  /**
   * Change the currently selected color and update UI state to match.
   */
  private void updateCurrentColor(double hue, double saturation, double value) {
    Color newColor = Color.hsb(hue * 360, saturation, value);

    colorPicker.colorProperty().set(newColor);

    GraphicsContext gc = colorSample.getGraphicsContext2D();
    gc.setFill(newColor);
    gc.fillRect(0, 0, colorSample.getWidth(), colorSample.getHeight());

    if (!editingWebColorCode) {
      // Suspending the web color listener to avoid it tweaking the current color.
      webColorCode.textProperty().removeListener(webColorListener);
      webColorCode.setText(String.format("#%02X%02X%02X",
          (int) (newColor.getRed() * 255 + 0.5),
          (int) (newColor.getGreen() * 255 + 0.5),
          (int) (newColor.getBlue() * 255 + 0.5)));
      webColorCode.textProperty().addListener(webColorListener);
    }
  }

  private Color getRandomNearColor(Color color) {
    double hueMod = random.nextDouble() * .45;
    double satMod = random.nextDouble() * .75;
    double valMod = random.nextDouble() * .4;
    hueMod = 2 * (random.nextDouble() - .5) * 360 * hueMod * hueMod;
    satMod = 2 * (random.nextDouble() - .5) * satMod * satMod;
    valMod = 2 * (random.nextDouble() - .5) * valMod * valMod;
    double hue = color.getHue() + hueMod;
    double sat = Math.max(0, Math.min(1, color.getSaturation() + satMod));
    double val = Math.max(0, Math.min(1, color.getBrightness() + valMod));
    if (hue > 360) {
      hue -= 360;
    } else if (hue < 0) {
      hue += 360;
    }
    return Color.hsb(hue, sat, val);
  }

  protected static Image drawGradient(int width, int height, List<double[]> gradient) {
    int[] pixels = new int[width * height];
    if (width <= 0 || height <= 0 || gradient.size() < 2) {
      throw new IllegalArgumentException();
    }
    int x = 0;
    // Fill the first row.
    for (int i = 0; i < width; ++i) {
      double weight = i / (double) width;
      double[] c0 = gradient.get(x);
      double[] c1 = gradient.get(x + 1);
      double xx = (weight - c0[3]) / (c1[3] - c0[3]);
      while (x + 2 < gradient.size() && xx > 1) {
        x += 1;
        c0 = gradient.get(x);
        c1 = gradient.get(x + 1);
        xx = (weight - c0[3]) / (c1[3] - c0[3]);
      }
      xx = 0.5 * (Math.sin(Math.PI * xx - Math.PI / 2) + 1);
      double a = 1 - xx;
      double b = xx;
      int argb = getArgb(a * c0[0] + b * c1[0], a * c0[1] + b * c1[1], a * c0[2] + b * c1[2]);
      pixels[i] = argb;
    }
    // Copy the first row to the rest of the image.
    for (int j = 1; j < height; ++j) {
      System.arraycopy(pixels, 0, pixels, j * width, width);
    }
    WritableImage image = new WritableImage(width, height);
    image.getPixelWriter().setPixels(0, 0, width, height, PIXEL_FORMAT, pixels, 0, width);
    return image;
  }

  /**
   * @return int ARGB values corresponding to the given color
   */
  private static int getArgb(double r, double g, double b) {
    return 0xFF000000 |
        ((int) (255 * r + .5) << 16) |
        ((int) (255 * g + .5) << 8) |
        (int) (255 * b + .5);
  }
}
