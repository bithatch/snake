package uk.co.bithatch.snake.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.SizeConverter;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class KeyboardLayout extends Pane {

	private static final String COL_CONSTRAINT = "col";
	private static final String ROW_CONSTRAINT = "row";

	static void setConstraint(Node node, Object key, Object value) {
		if (value == null) {
			node.getProperties().remove(key);
		} else {
			node.getProperties().put(key, value);
		}
		if (node.getParent() != null) {
			node.getParent().requestLayout();
		}
	}

	static Object getConstraint(Node node, Object key) {
		if (node.hasProperties()) {
			Object value = node.getProperties().get(key);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	public static void setRow(Node child, int value) {
		setConstraint(child, ROW_CONSTRAINT, value == -1 ? null : value);
	}

	public static int getRow(Node child) {
		return (Integer) getConstraint(child, ROW_CONSTRAINT);
	}

	public static void setCol(Node child, int value) {
		setConstraint(child, COL_CONSTRAINT, value == -1 ? null : value);
	}

	public static int getCol(Node child) {
		return (Integer) getConstraint(child, COL_CONSTRAINT);
	}

	public static void clearConstraints(Node child) {
		setCol(child, -1);
		setRow(child, -1);
	}

	public KeyboardLayout() {
		super();
	}

	public KeyboardLayout(double spacing) {
		this();
		setSpacing(spacing);
	}

	public KeyboardLayout(Node... children) {
		super();
		getChildren().addAll(children);
	}

	public KeyboardLayout(double spacing, Node... children) {
		this();
		setSpacing(spacing);
		getChildren().addAll(children);
	}

	public final ObjectProperty<Pos> alignmentProperty() {
		if (alignment == null) {
			alignment = new StyleableObjectProperty<Pos>(Pos.TOP_LEFT) {
				@Override
				public void invalidated() {
					requestLayout();
				}

				@Override
				public CssMetaData<KeyboardLayout, Pos> getCssMetaData() {
					return StyleableProperties.ALIGNMENT;
				}

				@Override
				public Object getBean() {
					return KeyboardLayout.this;
				}

				@Override
				public String getName() {
					return "alignment";
				}
			};
		}
		return alignment;
	}

	private ObjectProperty<Pos> alignment;
	private ObjectProperty<Number> xProperty;
	private ObjectProperty<Number> yProperty;

	public final void setAlignment(Pos value) {
		alignmentProperty().set(value);
	}

	public final Pos getAlignment() {
		return alignment == null ? Pos.CENTER : alignment.get();
	}

	private Pos getAlignmentInternal() {
		Pos localPos = getAlignment();
		return localPos == null ? Pos.CENTER : localPos;
	}

	public final DoubleProperty spacingProperty() {
		if (spacing == null) {
			spacing = new StyleableDoubleProperty() {
				@Override
				public void invalidated() {
					requestLayout();
				}

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public CssMetaData getCssMetaData() {
					return StyleableProperties.SPACING;
				}

				@Override
				public Object getBean() {
					return KeyboardLayout.this;
				}

				@Override
				public String getName() {
					return "spacing";
				}
			};
			spacing.set(7);
		}
		return spacing;
	}

	public final ObjectProperty<Number> xProperty() {
		if (xProperty == null) {
			xProperty = new ObjectPropertyBase<>() {
				protected void invalidated() {
					requestLayout();
				}

				@Override
				public Object getBean() {
					return KeyboardLayout.this;
				}

				@Override
				public String getName() {
					return "x";
				}
			};
		}
		return xProperty;
	}

	public final ObjectProperty<Number> yProperty() {
		if (yProperty == null) {
			yProperty = new ObjectPropertyBase<>() {
				protected void invalidated() {
					requestLayout();
				}

				@Override
				public Object getBean() {
					return KeyboardLayout.this;
				}

				@Override
				public String getName() {
					return "y";
				}
			};
		}
		return yProperty;
	}

	public final DoubleProperty keyWidthProperty() {
		if (keyWidth == null) {
			keyWidth = new StyleableDoubleProperty() {
				@Override
				public void invalidated() {
					requestLayout();
				}

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public CssMetaData getCssMetaData() {
					return StyleableProperties.KEY_WIDTH;
				}

				@Override
				public Object getBean() {
					return KeyboardLayout.this;
				}

				@Override
				public String getName() {
					return "key-width";
				}
			};
			keyWidth.set(36);
		}
		return keyWidth;
	}

	public final DoubleProperty keyHeightProperty() {
		if (keyHeight == null) {
			keyHeight = new StyleableDoubleProperty() {
				@Override
				public void invalidated() {
					requestLayout();
				}

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public CssMetaData getCssMetaData() {
					return StyleableProperties.KEY_HEIGHT;
				}

				@Override
				public Object getBean() {
					return KeyboardLayout.this;
				}

				@Override
				public String getName() {
					return "key-height";
				}
			};
			keyHeight.set(36);
		}
		return keyHeight;
	}

	public final DoubleProperty keyFactorProperty() {
		if (keyFactor == null) {
			keyFactor = new StyleableDoubleProperty() {
				@Override
				public void invalidated() {
					requestLayout();
				}

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public CssMetaData getCssMetaData() {
					return StyleableProperties.KEY_FACTOR;
				}

				@Override
				public Object getBean() {
					return KeyboardLayout.this;
				}

				@Override
				public String getName() {
					return "key-factor";
				}
			};
		}
		return keyFactor;
	}

	private DoubleProperty spacing;

	public final void setSpacing(double value) {
		spacingProperty().set(value);
	}

	public final double getSpacing() {
		return spacing == null ? 0 : spacing.get();
	}

	private DoubleProperty keyWidth;

	public final void setKeyWidth(double value) {
		keyWidthProperty().set(value);
	}

	public final double getKeyWidth() {
		return keyWidth == null ? 0 : keyWidth.get();
	}

	private DoubleProperty keyHeight;

	public final void setKeyHeight(double value) {
		keyHeightProperty().set(value);
	}

	public final double getKeyHeight() {
		return keyHeight == null ? 0 : keyHeight.get();
	}

	private DoubleProperty keyFactor;

	public final void setKeyFactor(double value) {
		keyFactorProperty().set(value);
	}

	public final double getKeyFactor() {
		return keyFactor == null ? 0 : keyFactor.get();
	}

	@Override
	protected double computeMinWidth(double height) {
		Insets insets = getInsets();
		double w = snapSpaceX(insets.getLeft()) + computeContentWidth(getManagedChildren(), height, true)
				+ snapSpaceX(insets.getRight());
		return w;
	}

	@Override
	protected double computeMinHeight(double width) {
		Insets insets = getInsets();
		double h = snapSpaceY(insets.getTop()) + computeContentHeight(getManagedChildren(), width, true)
				+ snapSpaceY(insets.getBottom());
		return h;
	}

	@Override
	protected double computePrefWidth(double height) {
		return computeMinWidth(height);
	}

	@Override
	protected double computePrefHeight(double width) {
		return computeMinHeight(width);
	}

	private double computeContentWidth(List<Node> managedChildren, double height, boolean minimum) {
		double x = xProperty().get().doubleValue();
		return x * keyWidthProperty().get() + (x - 1) * snapSpaceX(getSpacing());
	}

	private double computeContentHeight(List<Node> managedChildren, double height, boolean minimum) {
		double y = yProperty().get().doubleValue();
		return y * keyHeightProperty().get() + (y - 1) * snapSpaceY(getSpacing());
	}

	@Override
	public void requestLayout() {
		super.requestLayout();
	}

	@Override
	protected void layoutChildren() {
		List<Node> managed = getManagedChildren();

		Insets insets = getInsets();
		double width = getWidth();
		double height = getHeight();
		double top = snapSpaceY(insets.getTop());
		double left = snapSpaceX(insets.getLeft());
		double right = snapSpaceX(insets.getRight());
		double bottom = snapSpaceX(insets.getBottom());
		double space = snapSpaceX(spacingProperty().get());
		double kwidth = keyWidthProperty().get();
		double kheight = keyHeightProperty().get();
		Pos align = getAlignmentInternal();
		HPos alignHpos = align.getHpos();
		VPos alignVpos = align.getVpos();

		double xo = left + computeXOffset(width - left - right, prefWidth(height), align.getHpos());
		double yo = top + computeYOffset(height - top - bottom, prefHeight(width), align.getVpos());
		double x, y;

		for (Node cell : managed) {
			int j = getCol(cell);
			int i = getRow(cell);
			double pw = kwidth;

			x = xo + (((j * kwidth) + (j * space)) - (pw / 2) + (kwidth / 2));
			y = yo + ((i * kheight) + (i * space));

			layoutInArea(cell, x, y, pw, kheight, 0, null, false, false, alignHpos, alignVpos);
		}

	}

	static double computeXOffset(double width, double contentWidth, HPos hpos) {
		switch (hpos) {
		case LEFT:
			return 0;
		case CENTER:
			return (width - contentWidth) / 2;
		case RIGHT:
			return width - contentWidth;
		default:
			throw new AssertionError("Unhandled hPos");
		}
	}

	static double computeYOffset(double height, double contentHeight, VPos vpos) {
		switch (vpos) {
		case BASELINE:
		case TOP:
			return 0;
		case CENTER:
			return (height - contentHeight) / 2;
		case BOTTOM:
			return height - contentHeight;
		default:
			throw new AssertionError("Unhandled vPos");
		}
	}

	private static class StyleableProperties {

		private static final CssMetaData<KeyboardLayout, Pos> ALIGNMENT = new CssMetaData<KeyboardLayout, Pos>(
				"-fx-alignment", new EnumConverter<Pos>(Pos.class), Pos.TOP_LEFT) {

			@Override
			public boolean isSettable(KeyboardLayout node) {
				return node.alignment == null || !node.alignment.isBound();
			}

			@SuppressWarnings("unchecked")
			@Override
			public StyleableProperty<Pos> getStyleableProperty(KeyboardLayout node) {
				return (StyleableProperty<Pos>) node.alignmentProperty();
			}

		};

		private static final CssMetaData<KeyboardLayout, Number> SPACING = new CssMetaData<KeyboardLayout, Number>(
				"-fx-spacing", SizeConverter.getInstance(), 4.0) {

			@Override
			public boolean isSettable(KeyboardLayout node) {
				return node.spacing == null || !node.spacing.isBound();
			}

			@SuppressWarnings("unchecked")
			@Override
			public StyleableProperty<Number> getStyleableProperty(KeyboardLayout node) {
				return (StyleableProperty<Number>) node.spacingProperty();
			}

		};
		private static final CssMetaData<KeyboardLayout, Number> KEY_WIDTH = new CssMetaData<KeyboardLayout, Number>(
				"-fx-key-width", SizeConverter.getInstance(), 36.0) {

			@Override
			public boolean isSettable(KeyboardLayout node) {
				return node.spacing == null || !node.keyWidth.isBound();
			}

			@SuppressWarnings("unchecked")
			@Override
			public StyleableProperty<Number> getStyleableProperty(KeyboardLayout node) {
				return (StyleableProperty<Number>) node.keyWidthProperty();
			}

		};
		private static final CssMetaData<KeyboardLayout, Number> KEY_HEIGHT = new CssMetaData<KeyboardLayout, Number>(
				"-fx-key-height", SizeConverter.getInstance(), 36.0) {

			@Override
			public boolean isSettable(KeyboardLayout node) {
				return node.spacing == null || !node.keyHeightProperty().isBound();
			}

			@SuppressWarnings("unchecked")
			@Override
			public StyleableProperty<Number> getStyleableProperty(KeyboardLayout node) {
				return (StyleableProperty<Number>) node.keyHeightProperty();
			}

		};
		private static final CssMetaData<KeyboardLayout, Number> KEY_FACTOR = new CssMetaData<KeyboardLayout, Number>(
				"-fx-key-factor", SizeConverter.getInstance(), 0.4) {

			@Override
			public boolean isSettable(KeyboardLayout node) {
				return node.keyFactor == null || !node.keyFactor.isBound();
			}

			@SuppressWarnings("unchecked")
			@Override
			public StyleableProperty<Number> getStyleableProperty(KeyboardLayout node) {
				return (StyleableProperty<Number>) node.keyFactorProperty();
			}

		};

		private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
		static {
			final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<CssMetaData<? extends Styleable, ?>>(
					Pane.getClassCssMetaData());
			styleables.add(SPACING);
			styleables.add(KEY_WIDTH);
			styleables.add(KEY_HEIGHT);
			styleables.add(KEY_FACTOR);
			styleables.add(ALIGNMENT);
			STYLEABLES = Collections.unmodifiableList(styleables);
		}
	}

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return StyleableProperties.STYLEABLES;
	}

	@Override
	public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
		return getClassCssMetaData();
	}
}
