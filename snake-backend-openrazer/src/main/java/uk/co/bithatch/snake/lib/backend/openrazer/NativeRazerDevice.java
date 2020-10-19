package uk.co.bithatch.snake.lib.backend.openrazer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.System.Logger.Level;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.co.bithatch.snake.lib.BrandingImage;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.DeviceType;
import uk.co.bithatch.snake.lib.Key;
import uk.co.bithatch.snake.lib.Macro;
import uk.co.bithatch.snake.lib.MacroKey;
import uk.co.bithatch.snake.lib.MacroScript;
import uk.co.bithatch.snake.lib.MacroSequence;
import uk.co.bithatch.snake.lib.MacroURL;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.MacroKey.State;
import uk.co.bithatch.snake.lib.Region.Name;
import uk.co.bithatch.snake.lib.effects.Breath;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.lib.effects.Matrix;
import uk.co.bithatch.snake.lib.effects.On;
import uk.co.bithatch.snake.lib.effects.Off;
import uk.co.bithatch.snake.lib.effects.Pulsate;
import uk.co.bithatch.snake.lib.effects.Reactive;
import uk.co.bithatch.snake.lib.effects.Ripple;
import uk.co.bithatch.snake.lib.effects.Spectrum;
import uk.co.bithatch.snake.lib.effects.Starlight;
import uk.co.bithatch.snake.lib.effects.Static;
import uk.co.bithatch.snake.lib.effects.Wave;

public class NativeRazerDevice implements Device {

	final static System.Logger LOG = System.getLogger(NativeRazerDevice.class.getName());

	final static long MAX_CACHE_AGE = TimeUnit.DAYS.toMillis(7);

	abstract class AbstractRazerRegion<I extends DBusInterface> implements Region {
		I underlying;
		Effect effect = new Off();
		Class<I> clazz;
		short brightness = -1;
		Name name;
		Set<Capability> caps = new HashSet<>();
		DBusConnection conn;
		String effectPrefix;
		Set<Class<? extends Effect>> supportedEffects = new LinkedHashSet<>();
		Map<String, List<Class<?>>> methods = new HashMap<>();
		private Document document;

		AbstractRazerRegion(Class<I> clazz, Name name, DBusConnection conn, String effectPrefix) {
			this.effectPrefix = effectPrefix;
			this.clazz = clazz;
			this.name = name;
			this.conn = conn;
		}

		protected boolean hasMethod(String name, Class<?>... classes) {
			List<Class<?>> sig = methods.get(name);
			return sig != null && sig.equals(Arrays.asList(classes));
		}

		@Override
		public Effect createEffect(Class<? extends Effect> clazz) {
			Effect effectInstance;
			try {
				effectInstance = clazz.getConstructor().newInstance();
				effectInstance.load(regionPrefs());
				return effectInstance;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new IllegalStateException(String.format("Cannot create effect %s.", clazz));
			}
		}

		void assertCap(Capability cap) {
			if (!caps.contains(cap))
				throw new UnsupportedOperationException(
						String.format("The capability %s is not supported by region %s on device %s.", cap, name.name(),
								getDevice().getName()));
		}

		@Override
		public Set<Capability> getCapabilities() {
			return caps;
		}

		public Device getDevice() {
			return NativeRazerDevice.this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public final void load(String path) throws Exception {
			loadIntrospection(path);
			loadInterfaces(path);

			if (hasMethod("set" + effectPrefix + "Off"))
				supportedEffects.add(Off.class);
			if (hasMethod("set" + effectPrefix + "Static", byte.class, byte.class, byte.class))
				supportedEffects.add(Static.class);
			if (hasMethod("set" + effectPrefix + "BreathRandom"))
				supportedEffects.add(Breath.class);
			if (hasMethod("set" + effectPrefix + "Reactive", byte.class, byte.class, byte.class, byte.class))
				supportedEffects.add(Reactive.class);
			if (hasMethod("set" + effectPrefix + "Spectrum"))
				supportedEffects.add(Spectrum.class);
			if (hasMethod("set" + effectPrefix + "Wave", int.class))
				supportedEffects.add(Wave.class);

			onCaps(caps);
			if (supportedEffects.isEmpty()) {
				throw new UnsupportedOperationException(String.format("Region %s not supported.", name.name()));
			}
			caps.add(Capability.EFFECT_PER_REGION);
			caps.add(Capability.EFFECTS);

			try {
				brightness = doGetBrightness();
				caps.add(Capability.BRIGHTNESS_PER_REGION);

				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("Region %s on device %s supports separate brightness.",
							name.name(), NativeRazerDevice.class.getName()));
			} catch (Exception e) {

				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("Region %s on device %s does not support separate brightness.",
							name.name(), NativeRazerDevice.class.getName()));
			}

			Preferences regionPrefs = regionPrefs();
			String effect = regionPrefs.get("effect", "");
			if (!effect.equals("")) {
				try {
					setEffect(createEffect((Class<? extends Effect>) getClass().getClassLoader().loadClass(effect)));
				} catch (Exception e) {
					LOG.log(Level.ERROR, String.format("Failed to set configured effect %s.", effect), e);
				}
			}
		}

		protected void loadInterfaces(String path) throws DBusException {
			underlying = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path), clazz, true);
			loadMethods(clazz);
		}

		protected void loadIntrospection(String path)
				throws DBusException, ParserConfigurationException, SAXException, IOException {
			Introspectable intro = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path),
					Introspectable.class, true);
			InputSource source = new InputSource(new StringReader(intro.Introspect()));
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			dbf.setValidating(false);
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			document = db.parse(source);
		}

		protected boolean loadMethods(Class<?> clazz) {
			NodeList nl = document.getElementsByTagName("interface");
			boolean ok = false;
			for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				Node attr = n.getAttributes().getNamedItem("name");
				if (attr != null) {
					DBusInterfaceName dbusName = clazz.getAnnotation(DBusInterfaceName.class);
					if (dbusName.value().equals(attr.getNodeValue())) {
						NodeList ichildren = n.getChildNodes();
						for (int j = 0; j < ichildren.getLength(); j++) {
							Node in = ichildren.item(j);
							if (in.getNodeName().equalsIgnoreCase("method")) {
								Node nattr = in.getAttributes().getNamedItem("name");

								List<Class<?>> sig = new ArrayList<>();
								NodeList achildren = in.getChildNodes();
								for (int aj = 0; aj < achildren.getLength(); aj++) {
									Node ain = achildren.item(aj);
									if (ain.getNodeName().equalsIgnoreCase("arg")) {

										Node naattr = ain.getAttributes().getNamedItem("type");
										if (naattr != null) {
											String natype = naattr.getNodeValue();
											if (natype.equals("y"))
												sig.add(byte.class);
											else if (natype.equals("b"))
												sig.add(boolean.class);
											else if (natype.equals("n") || natype.equals("q"))
												sig.add(short.class);
											else if (natype.equals("i") || natype.equals("i"))
												sig.add(int.class);
											else if (natype.equals("x") || natype.equals("t"))
												sig.add(long.class);
											else if (natype.equals("d"))
												sig.add(double.class);
											else if (natype.equals("s"))
												sig.add(String.class);

											/*
											 * TODO: Other types we don't really care about at the moment, as the
											 * signatures are only used for capabilities, which only use the above
											 * primitive types
											 */
										}
									}
								}
								methods.put(nattr.getNodeValue(), sig);

							}
						}
						break;
					}
				}
			}
			return ok;
		}

		protected void onCaps(Set<Capability> caps) {
		}

		@Override
		public final Name getName() {
			return name;
		}

		@Override
		public final short getBrightness() {
			assertCap(Capability.BRIGHTNESS_PER_REGION);
			if (brightness == -1) {
				brightness = doGetBrightness();
			}
			return brightness;
		}

		@Override
		public final void setBrightness(short brightness) {
			assertCap(Capability.BRIGHTNESS_PER_REGION);
			if (brightness != this.brightness) {
				this.brightness = brightness;
				doSetBrightness(brightness);
				fireChange(this);
				fireChange(null);
			}
		}

		@Override
		public void setEffect(Effect effect) {
			if (!Objects.equals(effect, this.effect)) {

				doSetEffect(effect);
				fireChange(null);
			}
		}

		protected void doSetEffect(Effect effect) {

			this.effect = effect;
			brightness = -1;
			if (effect instanceof Breath) {
				Breath breath = (Breath) effect;
				switch (breath.getMode()) {
				case DUAL:
					doSetBreathDual(breath);
					break;
				case SINGLE:
					doSetBreathSingle(breath);
					break;
				default:
					doSetBreathRandom();
					break;
				}
			} else if (effect instanceof Off) {
				doSetOff();
			} else if (effect instanceof Spectrum) {
				doSetSpectrum();
			} else if (effect instanceof Reactive) {
				doSetReactive((Reactive) effect);
			} else if (effect instanceof Static) {
				doSetStatic((Static) effect);
			} else if (effect instanceof On) {
				doSetOn((On) effect);
			} else if (effect instanceof Wave) {
				doSetWave((Wave) effect);
			} else if (effect instanceof Matrix) {
				doSetMatrix((Matrix) effect);
			} else if (effect instanceof Pulsate) {
				doSetPulsate((Pulsate) effect);
			} else if (effect instanceof Starlight) {
				Starlight starlight = (Starlight) effect;
				switch (starlight.getMode()) {
				case DUAL:
					doSetStarlightDual(starlight);
					break;
				case SINGLE:
					doSetStarlightSingle(starlight);
					break;
				default:
					doSetStarlightRandom(starlight);
					break;
				}
			} else if (effect instanceof Ripple) {
				Ripple ripple = (Ripple) effect;
				switch (ripple.getMode()) {
				case SINGLE:
					doSetRipple(ripple);
					break;
				default:
					doSetRippleRandomColour(ripple);
					break;
				}
			} else
				throw new UnsupportedOperationException(
						String.format("Effect %s not supported by region %s", effect.getClass(), name));
			Preferences regionPrefs = regionPrefs();
			regionPrefs.put("effect", effect.getClass().getName());
			effect.save(regionPrefs);
			fireChange(this);
		}

		private Preferences regionPrefs() {
			Preferences regionPrefs = prefs.node(this.name.name());
			return regionPrefs;
		}

		@Override
		public Set<Class<? extends Effect>> getSupportedEffects() {
			return supportedEffects;
		}

		@Override
		public final Effect getEffect() {
			return effect;
		}

		protected void doSetBreathDual(Breath breath) {
			throw new UnsupportedOperationException();
		}

		protected void doSetBreathRandom() {
			throw new UnsupportedOperationException();
		}

		protected void doSetBreathSingle(Breath breath) {
			throw new UnsupportedOperationException();
		}

		protected void doSetBrightness(int brightness) {
			throw new UnsupportedOperationException();
		}

		protected void doSetOff() {
			throw new UnsupportedOperationException();
		}

		protected void doSetReactive(Reactive reactive) {
			throw new UnsupportedOperationException();
		}

		protected void doSetSpectrum() {
			throw new UnsupportedOperationException();
		}

		protected void doSetStatic(Static staticEffect) {
			throw new UnsupportedOperationException();
		}

		protected void doSetOn(On monoStaticEffect) {
			throw new UnsupportedOperationException();
		}

		protected void doSetWave(Wave wave) {
			throw new UnsupportedOperationException();
		}

		protected void doSetPulsate(Pulsate pulsate) {
			throw new UnsupportedOperationException();
		}

		protected void doSetMatrix(Matrix matrix) {
			throw new UnsupportedOperationException();
		}

		protected short doGetBrightness() {
			throw new UnsupportedOperationException();
		}

		protected void doSetRipple(Ripple ripple) {
			throw new UnsupportedOperationException();
		}

		protected void doSetRippleRandomColour(Ripple ripple) {
			throw new UnsupportedOperationException();
		}

		protected void doSetStarlightDual(Starlight starlight) {
			throw new UnsupportedOperationException();
		}

		protected void doSetStarlightRandom(Starlight starlight) {
			throw new UnsupportedOperationException();
		}

		protected void doSetStarlightSingle(Starlight starlight) {
			throw new UnsupportedOperationException();
		}
	}

	class NativeRazerRegionLeft extends AbstractRazerRegion<RazerRegionLeft> {

		public NativeRazerRegionLeft(DBusConnection connection) {
			super(RazerRegionLeft.class, Name.LEFT, connection, "Left");
		}

		@Override
		protected short doGetBrightness() {
			return (short) underlying.getLeftBrightness();
		}

		@Override
		protected void doSetBreathDual(Breath breath) {
			underlying.setLeftBreathDual((byte) breath.getColor1()[0], (byte) breath.getColor1()[1],
					(byte) breath.getColor1()[2], (byte) breath.getColor2()[0], (byte) breath.getColor2()[1],
					(byte) breath.getColor1()[2]);
		}

		@Override
		protected void doSetBreathRandom() {
			underlying.setLeftBreathRandom();
		}

		@Override
		protected void doSetBreathSingle(Breath breath) {
			underlying.setLeftBreathSingle((byte) breath.getColor()[0], (byte) breath.getColor()[1],
					(byte) breath.getColor()[2]);
		}

		@Override
		protected void doSetBrightness(int brightness) {
			underlying.setLeftBrightness(brightness);
		}

		@Override
		protected void doSetOff() {
			underlying.setLeftNone();
		}

		@Override
		protected void doSetReactive(Reactive reactive) {
			underlying.setLeftReactive((byte) reactive.getColor()[0], (byte) reactive.getColor()[1],
					(byte) reactive.getColor()[2], (byte) reactive.getSpeed());
		}

		@Override
		protected void doSetSpectrum() {
			underlying.setLeftSpectrum();
		}

		@Override
		protected void doSetStatic(Static staticEffect) {
			underlying.setLeftStatic((byte) staticEffect.getColor()[0], (byte) staticEffect.getColor()[1],
					(byte) staticEffect.getColor()[2]);
		}

		@Override
		protected void doSetWave(Wave wave) {
			underlying.setLeftWave(wave.getDirection().ordinal() + 1);
		}
	}

	class NativeRazerRegionChroma extends AbstractRazerRegion<RazerRegionChroma> {
		RazerBW2013 underlyingBw2013;
		RazerCustom underlyingCustom;

		public NativeRazerRegionChroma(DBusConnection connection) {
			super(RazerRegionChroma.class, Name.CHROMA, connection, "");
		}

		@Override
		protected void loadInterfaces(String path) throws DBusException {
			super.loadInterfaces(path);
			try {
				underlyingBw2013 = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path),
						RazerBW2013.class, true);
				loadMethods(RazerBW2013.class);
			} catch (Exception e) {
			}
			try {
				underlyingCustom = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path),
						RazerCustom.class, true);
				loadMethods(RazerCustom.class);
			} catch (Exception e) {
			}
		}

		@Override
		protected void doSetMatrix(Matrix matrix) {
			// https://github.com/openrazer/openrazer/wiki/Using-the-keyboard-driver
			int[][][] cells = matrix.getCells();
			int[] dim = getDevice().getMatrixSize();
			int y = dim[0];
			int x = dim[1];
			byte[] b = new byte[(y * 3) + (y * x * 3)];
			int q = 0;
			if (cells != null) {
				for (int yy = 0; yy < y; yy++) {
					b[q++] = (byte) yy;
					b[q++] = (byte) 0;
					b[q++] = (byte) (x - 1);
					if (cells[yy] != null)
						for (int xx = 0; xx < x; xx++) {
							if (cells[yy][xx] != null) {
								b[q++] = (byte) (cells[yy][xx][0] & 0xff);
								b[q++] = (byte) (cells[yy][xx][1] & 0xff);
								b[q++] = (byte) (cells[yy][xx][2] & 0xff);
							}
						}
				}
			}
			/* TODO only need to do this once when the matrix is initially selected */
			underlying.setCustom();

			underlying.setKeyRow(b);
		}

		@SuppressWarnings("resource")
		@Override
		protected void onCaps(Set<Capability> caps) {
			if (((NativeRazerDevice) getDevice()).device.hasMatrix()) {
				supportedEffects.add(Matrix.class);
			}
			if (hasMethod("setStarlightDual", byte.class, byte.class, byte.class, byte.class, byte.class, byte.class,
					byte.class)) {
				caps.add(Capability.STARLIGHT_DUAL);
				supportedEffects.add(Starlight.class);
			}
			if (hasMethod("setStarlightRandom", byte.class)) {
				caps.add(Capability.STARLIGHT_RANDOM);
				supportedEffects.add(Starlight.class);
			}
			if (hasMethod("setStarlightSingle", byte.class, byte.class, byte.class, byte.class)) {
				caps.add(Capability.STARLIGHT_SINGLE);
				supportedEffects.add(Starlight.class);
			}
			if (hasMethod("setPulsate"))
				supportedEffects.add(Pulsate.class);
			if (hasMethod("setStatic"))
				supportedEffects.add(On.class);
			if (hasMethod("setRipple", byte.class, byte.class, byte.class, double.class)) {
				caps.add(Capability.RIPPLE_SINGLE);
				supportedEffects.add(Ripple.class);
			}
			if (hasMethod("setRippleRandomColour", double.class)) {
				caps.add(Capability.RIPPLE_RANDOM);
				supportedEffects.add(Ripple.class);
			}
		}

		@Override
		protected short doGetBrightness() {
			return (short) underlying.getBrightness();
		}

		@Override
		protected void doSetBreathDual(Breath breath) {
			underlying.setBreathDual((byte) breath.getColor1()[0], (byte) breath.getColor1()[1],
					(byte) breath.getColor1()[2], (byte) breath.getColor2()[0], (byte) breath.getColor2()[1],
					(byte) breath.getColor2()[2]);
		}

		@Override
		protected void doSetStarlightRandom(Starlight starlight) {
			underlying.setStarlightRandom((byte) starlight.getSpeed());
		}

		@Override
		protected void doSetStarlightDual(Starlight starlight) {
			underlying.setStarlightDual((byte) starlight.getColor1()[0], (byte) starlight.getColor1()[1],
					(byte) starlight.getColor1()[2], (byte) starlight.getColor2()[0], (byte) starlight.getColor2()[1],
					(byte) starlight.getColor2()[2], (byte) starlight.getSpeed());
		}

		@Override
		protected void doSetStarlightSingle(Starlight starlight) {
			underlying.setStarlightSingle((byte) starlight.getColor()[0], (byte) starlight.getColor()[1],
					(byte) starlight.getColor()[2], (byte) starlight.getSpeed());
		}

		@Override
		protected void doSetBreathRandom() {
			underlying.setBreathRandom();
		}

		@Override
		protected void doSetBreathSingle(Breath breath) {
			underlying.setBreathSingle((byte) breath.getColor()[0], (byte) breath.getColor()[1],
					(byte) breath.getColor()[2]);
		}

		@Override
		protected void doSetBrightness(int brightness) {
			underlying.setBrightness(brightness);
		}

		@Override
		protected void doSetOff() {
			underlying.setNone();
		}

		@Override
		protected void doSetReactive(Reactive reactive) {
			underlying.setReactive((byte) reactive.getColor()[0], (byte) reactive.getColor()[1],
					(byte) reactive.getColor()[2], (byte) reactive.getSpeed());
		}

		@Override
		protected void doSetSpectrum() {
			underlying.setSpectrum();
		}

		@Override
		protected void doSetStatic(Static staticEffect) {
			underlying.setStatic((byte) staticEffect.getColor()[0], (byte) staticEffect.getColor()[1],
					(byte) staticEffect.getColor()[2]);
		}

		@Override
		protected void doSetWave(Wave wave) {
			underlying.setWave(wave.getDirection().ordinal() + 1);
		}

		@Override
		protected void doSetRipple(Ripple ripple) {
			underlyingCustom.setRipple((byte) ripple.getColor()[0], (byte) ripple.getColor()[1],
					(byte) ripple.getColor()[2], (double) ripple.getRefreshRate());
		}

		@Override
		protected void doSetRippleRandomColour(Ripple ripple) {
			underlyingCustom.setRippleRandomColour((double) ripple.getRefreshRate());
		}

		@Override
		protected void doSetPulsate(Pulsate pulsate) {
			underlyingBw2013.setPulsate();
		}

		@Override
		protected void doSetOn(On monoStaticEffect) {
			underlyingBw2013.setStatic();
		}
	}

	class NativeRazerRegionRight extends AbstractRazerRegion<RazerRegionRight> {

		public NativeRazerRegionRight(DBusConnection connection) {
			super(RazerRegionRight.class, Name.RIGHT, connection, "Right");
		}

		@Override
		protected short doGetBrightness() {
			return (short) underlying.getRightBrightness();
		}

		@Override
		protected void doSetBreathDual(Breath breath) {
			underlying.setRightBreathDual((byte) breath.getColor1()[0], (byte) breath.getColor1()[1],
					(byte) breath.getColor1()[2], (byte) breath.getColor2()[0], (byte) breath.getColor2()[1],
					(byte) breath.getColor1()[2]);
		}

		@Override
		protected void doSetBreathRandom() {
			underlying.setRightBreathRandom();
		}

		@Override
		protected void doSetBreathSingle(Breath breath) {
			underlying.setRightBreathSingle((byte) breath.getColor()[0], (byte) breath.getColor()[1],
					(byte) breath.getColor()[2]);
		}

		@Override
		protected void doSetBrightness(int brightness) {
			underlying.setRightBrightness(brightness);
		}

		@Override
		protected void doSetOff() {
			underlying.setRightNone();
		}

		@Override
		protected void doSetReactive(Reactive reactive) {
			underlying.setRightReactive((byte) reactive.getColor()[0], (byte) reactive.getColor()[1],
					(byte) reactive.getColor()[2], (byte) reactive.getSpeed());
		}

		@Override
		protected void doSetSpectrum() {
			underlying.setRightSpectrum();
		}

		@Override
		protected void doSetStatic(Static staticEffect) {
			underlying.setRightStatic((byte) staticEffect.getColor()[0], (byte) staticEffect.getColor()[1],
					(byte) staticEffect.getColor()[2]);
		}

		@Override
		protected void doSetWave(Wave wave) {
			underlying.setRightWave(wave.getDirection().ordinal() + 1);
		}
	}

	class NativeRazerRegionBacklight extends AbstractRazerRegion<RazerRegionBacklight> {

		public NativeRazerRegionBacklight(DBusConnection connection) {
			super(RazerRegionBacklight.class, Name.BACKLIGHT, connection, "Backlight");
		}

		@Override
		protected void doSetOff() {
			underlying.setBacklightActive(false);
		}

		@Override
		protected void doSetOn(On on) {
			underlying.setBacklightActive(true);
		}

		@Override
		protected void onCaps(Set<Capability> caps) {
			if (hasMethod("setBacklightActive", boolean.class)) {
				supportedEffects.add(On.class);
				supportedEffects.add(Off.class);
			}
		}
	}

	class NativeRazerRegionLogo extends AbstractRazerRegion<RazerRegionLogo> {

		public NativeRazerRegionLogo(DBusConnection connection) {
			super(RazerRegionLogo.class, Name.LOGO, connection, "Logo");
		}

		@Override
		protected short doGetBrightness() {
			return (short) underlying.getLogoBrightness();
		}

		@Override
		protected void doSetBreathDual(Breath breath) {
			underlying.setLogoBreathDual((byte) breath.getColor1()[0], (byte) breath.getColor1()[1],
					(byte) breath.getColor1()[2], (byte) breath.getColor2()[0], (byte) breath.getColor2()[1],
					(byte) breath.getColor1()[2]);
		}

		@Override
		protected void doSetBreathRandom() {
			underlying.setLogoBreathRandom();
		}

		@Override
		protected void doSetBreathSingle(Breath breath) {
			underlying.setLogoBreathSingle((byte) breath.getColor()[0], (byte) breath.getColor()[1],
					(byte) breath.getColor()[2]);
		}

		@Override
		protected void doSetBrightness(int brightness) {
			underlying.setLogoBrightness(brightness);
		}

		@Override
		protected void doSetOff() {
			if (hasMethod("setLogoActive", boolean.class)) {
				underlying.setLogoActive(false);
			} else
				underlying.setLogoNone();
		}

		@Override
		protected void doSetReactive(Reactive reactive) {
			underlying.setLogoReactive((byte) reactive.getColor()[0], (byte) reactive.getColor()[1],
					(byte) reactive.getColor()[2], (byte) reactive.getSpeed());
		}

		@Override
		protected void doSetSpectrum() {
			underlying.setLogoSpectrum();
		}

		@Override
		protected void doSetStatic(Static staticEffect) {
			underlying.setLogoStatic((byte) staticEffect.getColor()[0], (byte) staticEffect.getColor()[1],
					(byte) staticEffect.getColor()[2]);
		}

		@Override
		protected void doSetWave(Wave wave) {
			underlying.setLogoWave(wave.getDirection().ordinal() + 1);
		}

		@Override
		protected void doSetOn(On on) {
			underlying.setLogoActive(true);
		}

		@Override
		protected void onCaps(Set<Capability> caps) {
			if (hasMethod("setLogoActive", boolean.class)) {
				supportedEffects.add(On.class);
				supportedEffects.add(Off.class);
			}
		}
	}

	class NativeRazerRegionScroll extends AbstractRazerRegion<RazerRegionScroll> {

		public NativeRazerRegionScroll(DBusConnection connection) {
			super(RazerRegionScroll.class, Name.SCROLL, connection, "Scroll");
		}

		@Override
		protected short doGetBrightness() {
			return (short) underlying.getScrollBrightness();
		}

		@Override
		protected void doSetBreathDual(Breath breath) {
			underlying.setScrollBreathDual((byte) breath.getColor1()[0], (byte) breath.getColor1()[1],
					(byte) breath.getColor1()[2], (byte) breath.getColor2()[0], (byte) breath.getColor2()[1],
					(byte) breath.getColor1()[2]);
		}

		@Override
		protected void doSetBreathRandom() {
			underlying.setScrollBreathRandom();
		}

		@Override
		protected void doSetBreathSingle(Breath breath) {
			underlying.setScrollBreathSingle((byte) breath.getColor()[0], (byte) breath.getColor()[1],
					(byte) breath.getColor()[2]);
		}

		@Override
		protected void doSetBrightness(int brightness) {
			underlying.setScrollBrightness(brightness);
		}

		@Override
		protected void doSetOff() {
			if (hasMethod("setScrollActive", boolean.class)) {
				underlying.setScrollActive(false);
			} else
				underlying.setScrollNone();
		}

		@Override
		protected void doSetOn(On on) {
			underlying.setScrollActive(true);
		}

		@Override
		protected void doSetReactive(Reactive reactive) {
			underlying.setScrollReactive((byte) reactive.getColor()[0], (byte) reactive.getColor()[1],
					(byte) reactive.getColor()[2], (byte) reactive.getSpeed());
		}

		@Override
		protected void doSetSpectrum() {
			underlying.setScrollSpectrum();
		}

		@Override
		protected void doSetStatic(Static staticEffect) {
			underlying.setScrollStatic((byte) staticEffect.getColor()[0], (byte) staticEffect.getColor()[1],
					(byte) staticEffect.getColor()[2]);
		}

		@Override
		protected void doSetWave(Wave wave) {
			underlying.setScrollWave(wave.getDirection().ordinal() + 1);
		}

		@Override
		protected void onCaps(Set<Capability> caps) {
			if (hasMethod("setScrollActive", boolean.class)) {
				supportedEffects.add(On.class);
				supportedEffects.add(Off.class);
			}
		}
	}

	private RazerDevice device;
	private String path;
	private RazerDPI dpi;
	private List<Listener> listeners = new ArrayList<>();
	private List<Region> regionList;
	private Preferences prefs;
	private RazerBrightness brightness;
	private RazerBattery battery;
	private RazerMacro macros;
	private short lastBrightness = -1;
	private Set<Capability> caps = new HashSet<>();
	private DBusConnection conn;
	private RazerGameMode gameMode;
	private Set<Class<? extends Effect>> supportedEffects = new LinkedHashSet<>();
	private Map<BrandingImage, String> brandingImages = new HashMap<>();

	private int batteryLevel;
	private boolean wasCharging;
	private ScheduledFuture<?> batteryTask;
	private Effect effect;
	private int maxDpi;
	private String deviceName;
	private String driverVersion;
	private int pollRate;
	private String firmware;

	NativeRazerDevice(String path, DBusConnection conn, OpenRazerBackend backend) throws Exception {
		this.path = path;
		this.conn = conn;
		device = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path), RazerDevice.class);
		prefs = OpenRazerBackend.PREFS.node(getName());

		try {
			device.getPollRate();
			caps.add(Capability.POLL_RATE);

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("Device %s supports poll rate.", NativeRazerDevice.class.getName()));
		} catch (Exception e) {

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("Device %s does not support separate brightness.",
						NativeRazerDevice.class.getName()));
		}

		try {
			RazerDPI dpi = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path),
					RazerDPI.class);
			dpi.getDPI();
			this.dpi = dpi;

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has DPI control.", getName()));
			caps.add(Capability.DPI);
		} catch (Exception e) {
			//

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has no DPI control.", getName()));
		}

		try {
			RazerBrightness brightness = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path),
					RazerBrightness.class);
			brightness.getBrightness();
			this.brightness = brightness;

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has brightness control.", getName()));
			caps.add(Capability.BRIGHTNESS);
		} catch (Exception e) {
			//

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has no brightness control.", getName()));
		}

		try {
			RazerBattery battery = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path),
					RazerBattery.class);
			battery.getBattery();
			this.battery = battery;

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has battery control.", getName()));
			caps.add(Capability.BATTERY);
		} catch (Exception e) {
			//

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has no battery control.", getName()));
		}

		try {
			RazerGameMode gameMode = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path),
					RazerGameMode.class);
			gameMode.getGameMode();
			this.gameMode = gameMode;

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has game mode control.", getName()));
			caps.add(Capability.GAME_MODE);
		} catch (Exception e) {
			//

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has no game mode control.", getName()));
		}

		try {
			device.getKeyboardLayout();

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has keyboard layout.", getName()));
			caps.add(Capability.KEYBOARD_LAYOUT);
		} catch (Exception e) {
			//

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has no keyboard layout.", getName()));
		}

		try {
			RazerMacro macro = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path),
					RazerMacro.class);
			macro.getMacros();
			this.macros = macro;

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has macro control.", getName()));
			caps.add(Capability.MACROS);

			try {
				macro.getModeModifier();
				caps.add(Capability.MACRO_MODE_MODIFIER);
				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("%s has no macro mode modifier.", getName()));
			} catch (Exception e) {
			}
		} catch (Exception e) {
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has no macro control.", getName()));
		}

		if (device.hasDedicatedMacroKeys()) {
			caps.add(Capability.DEDICATED_MACRO_KEYS);
		}

		if (device.hasMatrix()) {
			caps.add(Capability.MATRIX);
			supportedEffects.add(Matrix.class);
		}

		getRegions();
		for (Region r : regionList) {

			for (Class<? extends Effect> ec : r.getSupportedEffects()) {
				if (!supportedEffects.contains(ec))
					supportedEffects.add(ec);
			}

			if (r.getCapabilities().contains(Capability.BRIGHTNESS_PER_REGION)) {
				caps.add(Capability.BRIGHTNESS);
			}
			if (r.getCapabilities().contains(Capability.EFFECT_PER_REGION)) {
				caps.add(Capability.EFFECTS);
			}

			caps.addAll(r.getCapabilities());
		}

		/*
		 * Because OpenRazer doesn't seem to provider getters for these values, we just
		 * have to store them locally, and reset them when any Snake app starts up
		 */
		if (getCapabilities().contains(Capability.BATTERY)) {
			battery.setLowBatteryThreshold(getLowBatteryThreshold());
			battery.setIdleTime(getIdleTime());

			/* Poll for battery changes, we don't get events from openrazer */
			/* TODO: Check, does it use standard battery events maybe? */
			batteryTask = backend.getBatteryPoll().scheduleAtFixedRate(() -> pollBattery(), 30, 30, TimeUnit.SECONDS);
		}

		JsonObject jsonObject = JsonParser.parseString(device.getRazerUrls()).getAsJsonObject();
		for (String key : jsonObject.keySet()) {
			try {
				String img = jsonObject.get(key).getAsString();
				BrandingImage bimg = BrandingImage.valueOf(key.substring(0, key.length() - 4).toUpperCase());
				brandingImages.put(bimg, img);
			} catch (Exception e) {
			}
		}
	}

	@Override
	public String getImage() {
		String image = device.getDeviceImage();
		if (!image.startsWith("http:") && !image.startsWith("https:"))
			return image;
		return getCachedImage(image);
	}

	private String getCachedImage(String image) {
		if (image == null)
			return null;
		String hash = genericHash(image);
		File cacheDir = new File(System.getProperty("user.home") + File.separator + ".cache" + File.separator + "snake"
				+ File.separator + "device-image-cache");
		if (!cacheDir.exists() && !cacheDir.mkdirs()) {
			throw new IllegalStateException(
					String.format("Failed to create device image cache directory %s.", cacheDir));
		}
		try {
			File cacheFile = new File(cacheDir, hash);
			if (!cacheFile.exists()) {
				URL url = new URL(image);
				try (InputStream in = url.openStream()) {
					try (OutputStream out = new FileOutputStream(cacheFile)) {
						in.transferTo(out);
					}
				}
			}
			return cacheFile.toURI().toURL().toExternalForm();
		} catch (MalformedURLException murle) {
			throw new IllegalStateException(String.format("Failed to construct image URL for %s.", image), murle);
		} catch (IOException ioe) {
			throw new IllegalStateException(String.format("Failed to cache device image %s.", image), ioe);
		}
	}

	@Override
	public DeviceType getType() {
		try {
			return DeviceType.valueOf(device.getDeviceType().toUpperCase());
		} catch (Exception e) {
			return DeviceType.UNRECOGNISED;
		}
	}

	@Override
	public String getMode() {
		return device.getDeviceMode();
	}

	@Override
	public String getName() {
		if (deviceName == null)
			deviceName = device.getDeviceName();
		return deviceName;
	}

	@Override
	public String getDriverVersion() {
		if (driverVersion == null)
			driverVersion = device.getDriverVersion();
		return driverVersion;
	}

	@Override
	public String getFirmware() {
		if (firmware == null)
			firmware = device.getFirmware();
		return firmware;
	}

	@Override
	public int getPollRate() {
		if (pollRate == -1)
			pollRate = device.getPollRate();
		return pollRate;
	}

	@Override
	public String getSerial() {
		return device.getSerial();
	}

	@Override
	public void setPollRate(int pollRate) {
		this.pollRate = pollRate;
		device.setPollRate((short) pollRate);
		fireChange(null);
	}

	@Override
	public void setSuspended(boolean suspended) {
		if (suspended)
			device.suspendDevice();
		else
			device.resumeDevice();
		fireChange(null);

	}

	@Override
	public Set<Capability> getCapabilities() {
		return caps;
	}

	@Override
	public short getBrightness() {
		assertCap(Capability.BRIGHTNESS);
		if (brightness == null)
			return Device.super.getBrightness();
		else {
			if (lastBrightness == -1) {
				lastBrightness = (short) brightness.getBrightness();
			}
			return lastBrightness;
		}
	}

	@Override
	public boolean isSuspended() {
		// TODO err
		return false;
	}

	@Override
	public String toString() {
		return "NativeRazerDevice [getImage()=" + getImage() + ", getType()=" + getType() + ", getMode()=" + getMode()
				+ ", getName()=" + getName() + ", getDriverVersion()=" + getDriverVersion() + ", getFirmware()="
				+ getFirmware() + ", getPollRate()=" + getPollRate() + ", getSerial()=" + getSerial()
				+ ", isSuspended()=" + isSuspended() + ", getCapabilties()=" + getCapabilities() + "]";
	}

	@Override
	public List<Region> getRegions() {
		if (regionList == null) {
			regionList = new ArrayList<Region>();
			for (Region r : Arrays.asList(new NativeRazerRegionChroma(conn), new NativeRazerRegionLeft(conn),
					new NativeRazerRegionRight(conn), new NativeRazerRegionLogo(conn),
					new NativeRazerRegionScroll(conn), new NativeRazerRegionBacklight(conn))) {
				try {
					r.load(path);
					regionList.add(r);
				} catch (Exception e) {
					LOG.log(Level.DEBUG, "Failed to load region.", e);
				}
			}
		}
		return regionList;
	}

	@Override
	public int[] getDPI() {
		assertCap(Capability.DPI);
		return dpi.getDPI();
	}

	@Override
	public int getMaxDPI() {
		assertCap(Capability.DPI);
		if (maxDpi == -1)
			maxDpi = dpi.maxDPI();
		return maxDpi;
	}

	@Override
	public void setDPI(short x, short y) {
		assertCap(Capability.DPI);
		dpi.setDPI(x, y);
		fireChange(null);
	}

	@Override
	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	@Override
	public Set<Class<? extends Effect>> getSupportedEffects() {
		return supportedEffects;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Effect getEffect() {
		if (effect == null) {
			String efClazzName = prefs.get("effect", "");
			Class<? extends Effect> efClazz = null;
			if (efClazzName.equals(""))
				efClazz = getSupportedEffects().isEmpty() ? null : getSupportedEffects().iterator().next();
			else {
				try {
					efClazz = (Class<? extends Effect>) getClass().getClassLoader().loadClass(efClazzName);
				} catch (Exception e) {
					LOG.log(Level.DEBUG,
							String.format("Could not load configured effect from preferences, %s.", efClazzName), e);
				}
			}
			if (efClazz != null) {
				effect = createEffect(efClazz);
			}
		}
		return effect;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void setEffect(Effect effect) {
		this.effect = effect;
		lastBrightness = -1;
		prefs.put("effect", effect.getClass().getName());
		effect.save(prefs);
		for (Region r : getRegions()) {
			if (r.isSupported(effect))
				((AbstractRazerRegion) r).doSetEffect(effect);
		}
		fireChange(null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void setBrightness(short brightness) {
		assertCap(Capability.BRIGHTNESS);
		if (this.brightness == null) {
			/* Calculated overall brightness */
			if (brightness != getBrightness()) {
				if (caps.contains(Capability.BRIGHTNESS_PER_REGION)) {
					for (Region r : getRegions()) {
						if (r.getCapabilities().contains(Capability.BRIGHTNESS_PER_REGION)) {
							/*
							 * NOTE: These are set directly so we don't fire too many events, just the
							 * device change
							 */
							((AbstractRazerRegion) r).brightness = brightness;
							((AbstractRazerRegion) r).doSetBrightness(brightness);
						}
					}
					fireChange(null);
				}
			}
		} else {
			/* Driver supplied overall brightness */
			if (brightness != lastBrightness) {
				lastBrightness = brightness;
				prefs.putInt("brightness", brightness);
				this.brightness.setBrightness(brightness);
				fireChange(null);
			}
		}
	}

	@Override
	public boolean isGameMode() {
		assertCap(Capability.GAME_MODE);
		return this.gameMode.getGameMode();
	}

	@Override
	public void setGameMode(boolean gameMode) {
		assertCap(Capability.GAME_MODE);
		this.gameMode.setGameMode(gameMode);
		fireChange(null);
	}

	protected void fireChange(Region region) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).changed(this, region);
	}

	void assertCap(Capability cap) {
		if (!caps.contains(cap))
			throw new UnsupportedOperationException(
					String.format("The capability %s is not supported on device %s.", cap, getName()));
	}

	@Override
	public int getBattery() {
		assertCap(Capability.BATTERY);
		return (int) battery.getBattery();
	}

	@Override
	public boolean isCharging() {
		assertCap(Capability.BATTERY);
		return battery.isCharging();
	}

	@Override
	public void setIdleTime(int idleTime) {
		assertCap(Capability.BATTERY);
		int old = prefs.getInt("idleTime", -1);
		if (old != idleTime) {
			prefs.putInt("idleTime", idleTime);
			battery.setIdleTime(idleTime);
			fireChange(null);
		}
	}

	@Override
	public void setLowBatteryThreshold(byte threshold) {
		assertCap(Capability.BATTERY);
		int old = prefs.getInt("lowBatteryThreshold", -1);
		if (old != threshold) {
			prefs.putInt("lowBatteryThreshold", Byte.toUnsignedInt(threshold));
			battery.setLowBatteryThreshold(threshold);
			fireChange(null);
		}
	}

	public static String genericHash(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash).replace("/", "").replace("=", "");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getIdleTime() {
		return prefs.getInt("idleTime", (int) TimeUnit.MINUTES.toSeconds(5));
	}

	@Override
	public byte getLowBatteryThreshold() {
		return (byte) prefs.getInt("lowBatteryThreshold", 5);
	}

	private void pollBattery() {
		boolean charging = battery.isCharging();
		int level = (int) battery.getBattery();
		if (charging != wasCharging || batteryLevel != level) {
			wasCharging = charging;
			batteryLevel = level;
			fireChange(null);
		}
	}

	@Override
	public void close() throws Exception {
		if (batteryTask != null)
			batteryTask.cancel(false);
	}

	@Override
	public int[] getMatrixSize() {
		assertCap(Capability.MATRIX);
		return device.getMatrixDimensions();
	}

	@Override
	public String getImageUrl(BrandingImage image) {
		return getCachedImage(brandingImages.get(image));
	}

	@Override
	public String getKeyboardLayout() {
		return device.getKeyboardLayout();
	}

	@Override
	public Effect createEffect(Class<? extends Effect> clazz) {
		Effect effectInstance;
		try {
			effectInstance = clazz.getConstructor().newInstance();
			effectInstance.load(prefs);
			return effectInstance;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException(String.format("Cannot create effect %s.", clazz));
		}
	}

	@Override
	public Map<Key, MacroSequence> getMacros() {
		assertCap(Capability.MACROS);
		String macroString = macros.getMacros();
		JsonObject jsonObject = JsonParser.parseString(macroString).getAsJsonObject();
		Map<Key, MacroSequence> macroSequences = new LinkedHashMap<>();
		for (String key : jsonObject.keySet()) {
			MacroSequence seq = new MacroSequence(Key.valueOf(key));
			JsonArray arr = jsonObject.get(key).getAsJsonArray();
			for (JsonElement el : arr) {
				JsonObject obj = el.getAsJsonObject();
				String type = obj.get("type").getAsString();
				Macro macro;
				if (type.equals("MacroKey")) {
					MacroKey macroKey = new MacroKey();
					macroKey.setKey(Key.valueOf(obj.get("key_id").getAsString()));
					macroKey.setPrePause(obj.get("pre_pause").getAsLong());
					macroKey.setState(State.valueOf(obj.get("state").getAsString()));
					macro = macroKey;
				} else if (type.equals("MacroURL")) {
					MacroURL macroURL = new MacroURL();
					macroURL.setUrl(obj.get("url").getAsString());
					macro = macroURL;
				} else if (type.equals("MacroScript")) {
					MacroScript macroScript = new MacroScript();
					macroScript.setScript(obj.get("script").getAsString());
					if (obj.has("args")) {
						List<String> args = new ArrayList<>();
						for (JsonElement argEl : obj.get("args").getAsJsonArray()) {
							args.add(argEl.getAsString());
						}
						macroScript.setArgs(args);
					}
					macro = macroScript;
				} else
					throw new UnsupportedOperationException();
				seq.add(macro);
			}
			macroSequences.put(seq.getMacroKey(), seq);
		}
		return macroSequences;
	}

	@Override
	public String exportMacros() {
		assertCap(Capability.MACROS);
		return macros.getMacros();
	}

	@Override
	public void importMacros(String macros) {
		for (MacroSequence s : getMacros().values())
			deleteMacro(s.getMacroKey());

		JsonObject jsonObject = JsonParser.parseString(macros).getAsJsonObject();
		for (String key : jsonObject.keySet()) {
			this.macros.addMacro(key, jsonObject.get(key).toString());
		}
	}

	@Override
	public void addMacro(MacroSequence macroSequence) {
		assertCap(Capability.MACROS);
		GsonBuilder gson = new GsonBuilder();
		gson.registerTypeAdapter(MacroKey.class, new JsonSerializer<MacroKey>() {
			@Override
			public JsonElement serialize(MacroKey src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject root = new JsonObject();
				root.addProperty("type", "MacroKey");
				root.addProperty("key_id", src.getKey().name());
				root.addProperty("pre_pause", src.getPrePause());
				root.addProperty("state", src.getState().name());
				return root;
			}
		});
		gson.registerTypeAdapter(MacroURL.class, new JsonSerializer<MacroURL>() {
			@Override
			public JsonElement serialize(MacroURL src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject root = new JsonObject();
				root.addProperty("type", "MacroURL");
				root.addProperty("url", src.getUrl());
				return root;
			}
		});
		gson.registerTypeAdapter(MacroScript.class, new JsonSerializer<MacroScript>() {
			@Override
			public JsonElement serialize(MacroScript src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject root = new JsonObject();
				root.addProperty("type", "MacroScript");
				root.addProperty("script", src.getScript());
				var arr = new JsonArray();
				if (src.getArgs() != null) {
					for (String s : src.getArgs())
						arr.add(s);
				}
				root.add("args", arr);
				return root;
			}
		});
		Gson parser = gson.create();
		var js = parser.toJson(macroSequence);
		macros.addMacro(macroSequence.getMacroKey().name(), js);
	}

	@Override
	public void deleteMacro(Key key) {
		assertCap(Capability.MACROS);
		macros.deleteMacro(key.name());
	}

	@Override
	public boolean isModeModifier() {
		assertCap(Capability.MACROS);
		return macros.getModeModifier();
	}

	@Override
	public void setModeModifier(boolean modify) {
		assertCap(Capability.MACROS);
		macros.setModeModifier(modify);
	}

	@SuppressWarnings("unchecked")
	protected <R extends Region> R getRegion(Name name) {
		for (Region r : regionList) {
			if (r.getName().equals(name))
				return (R) r;
		}
		return null;
	}

}