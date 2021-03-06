package uk.co.bithatch.snake.lib.backend.openrazer;

import java.io.IOException;
import java.io.StringReader;
import java.lang.System.Logger.Level;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.NotConnected;
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

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.EventCode.Ev;
import uk.co.bithatch.snake.lib.BrandingImage;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.DeviceType;
import uk.co.bithatch.snake.lib.Key;
import uk.co.bithatch.snake.lib.Macro;
import uk.co.bithatch.snake.lib.MacroKey;
import uk.co.bithatch.snake.lib.MacroKey.State;
import uk.co.bithatch.snake.lib.MacroScript;
import uk.co.bithatch.snake.lib.MacroSequence;
import uk.co.bithatch.snake.lib.MacroURL;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.ValidationException;
import uk.co.bithatch.snake.lib.binding.ExecuteMapAction;
import uk.co.bithatch.snake.lib.binding.KeyMapAction;
import uk.co.bithatch.snake.lib.binding.MapAction;
import uk.co.bithatch.snake.lib.binding.MapMapAction;
import uk.co.bithatch.snake.lib.binding.MapSequence;
import uk.co.bithatch.snake.lib.binding.Profile;
import uk.co.bithatch.snake.lib.binding.ProfileMap;
import uk.co.bithatch.snake.lib.binding.ProfileMapAction;
import uk.co.bithatch.snake.lib.binding.ReleaseMapAction;
import uk.co.bithatch.snake.lib.binding.ShiftMapAction;
import uk.co.bithatch.snake.lib.binding.SleepMapAction;
import uk.co.bithatch.snake.lib.effects.Breath;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.lib.effects.Matrix;
import uk.co.bithatch.snake.lib.effects.Off;
import uk.co.bithatch.snake.lib.effects.On;
import uk.co.bithatch.snake.lib.effects.Pulsate;
import uk.co.bithatch.snake.lib.effects.Reactive;
import uk.co.bithatch.snake.lib.effects.Ripple;
import uk.co.bithatch.snake.lib.effects.Spectrum;
import uk.co.bithatch.snake.lib.effects.Starlight;
import uk.co.bithatch.snake.lib.effects.Static;
import uk.co.bithatch.snake.lib.effects.Wave;

public class NativeRazerDevice implements Device {

	abstract class AbstractRazerRegion<I extends DBusInterface> implements Region {
		short brightness = -1;
		Set<Capability> caps = new HashSet<>();
		Class<I> clazz;
		DBusConnection conn;
		Effect effect = new Off();
		String effectPrefix;
		Map<String, List<Class<?>>> methods = new HashMap<>();
		Name name;
		Set<Class<? extends Effect>> supportedEffects = new LinkedHashSet<>();
		I underlying;
		private Document document;

		AbstractRazerRegion(Class<I> clazz, Name name, DBusConnection conn, String effectPrefix) {
			this.effectPrefix = effectPrefix;
			this.clazz = clazz;
			this.name = name;
			this.conn = conn;
		}

		@Override
		public <E extends Effect> E createEffect(Class<E> clazz) {
			E effectInstance;
			try {
				effectInstance = clazz.getConstructor().newInstance();
				return effectInstance;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new IllegalStateException(String.format("Cannot create effect %s.", clazz));
			}
		}

		@Override
		public void updateEffect(Effect effect) {
			throw new UnsupportedOperationException();
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
		public Set<Capability> getCapabilities() {
			return caps;
		}

		public Device getDevice() {
			return NativeRazerDevice.this;
		}

		@Override
		public final Effect getEffect() {
			return effect;
		}

		@Override
		public final Name getName() {
			return name;
		}

		@Override
		public Set<Class<? extends Effect>> getSupportedEffects() {
			return supportedEffects;
		}

		public final void load(String path) throws Exception {
			loadIntrospection(path);
			loadInterfaces(path);

			if (hasMethod("set" + effectPrefix + "None"))
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

		protected short doGetBrightness() {
			throw new UnsupportedOperationException();
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

		protected void doSetEffect(Effect effect) {
			doChangeEffect(effect, false);
			fireChange(this);
		}

		protected void doUpdateEffect(Effect effect) {
			doChangeEffect(effect, true);
		}

		protected void doChangeEffect(Effect effect, boolean update) {
			try {
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
					doSetMatrix((Matrix) effect, update);
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
			} catch (NotConnected nc) {
				// Ignore
			}
		}

		protected void doSetMatrix(Matrix matrix, boolean update) {
			throw new UnsupportedOperationException();
		}

		protected void doSetOff() {
			throw new UnsupportedOperationException();
		}

		protected void doSetOn(On monoStaticEffect) {
			throw new UnsupportedOperationException();
		}

		protected void doSetPulsate(Pulsate pulsate) {
			throw new UnsupportedOperationException();
		}

		protected void doSetReactive(Reactive reactive) {
			throw new UnsupportedOperationException();
		}

		protected void doSetRipple(Ripple ripple) {
			throw new UnsupportedOperationException();
		}

		protected void doSetRippleRandomColour(Ripple ripple) {
			throw new UnsupportedOperationException();
		}

		protected void doSetSpectrum() {
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

		protected void doSetStatic(Static staticEffect) {
			throw new UnsupportedOperationException();
		}

		protected void doSetWave(Wave wave) {
			throw new UnsupportedOperationException();
		}

		protected boolean hasMethod(String name, Class<?>... classes) {
			List<Class<?>> sig = methods.get(name);
			return sig != null && sig.equals(Arrays.asList(classes));
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

		void assertCap(Capability cap) {
			if (!caps.contains(cap))
				throw new UnsupportedOperationException(
						String.format("The capability %s is not supported by region %s on device %s.", cap, name.name(),
								getDevice().getName()));
		}
//
//		private Preferences regionPrefs() {
//			Preferences regionPrefs = prefs.node(this.name.name());
//			return regionPrefs;
//		}
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

	class NativeRazerRegionChroma extends AbstractRazerRegion<RazerRegionChroma> {
		RazerBW2013 underlyingBw2013;
		RazerCustom underlyingCustom;

		public NativeRazerRegionChroma(DBusConnection connection) {
			super(RazerRegionChroma.class, Name.CHROMA, connection, "");
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
		protected void doSetMatrix(Matrix matrix, boolean update) {
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
			if (!update)
				underlying.setCustom();

			underlying.setKeyRow(b);
		}

		@Override
		protected void doSetOff() {
			underlying.setNone();
		}

		@Override
		protected void doSetOn(On monoStaticEffect) {
			underlyingBw2013.setStatic();
		}

		@Override
		protected void doSetPulsate(Pulsate pulsate) {
			underlyingBw2013.setPulsate();
		}

		@Override
		protected void doSetReactive(Reactive reactive) {
			underlying.setReactive((byte) reactive.getColor()[0], (byte) reactive.getColor()[1],
					(byte) reactive.getColor()[2], (byte) reactive.getSpeed());
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
		protected void doSetSpectrum() {
			underlying.setSpectrum();
		}

		@Override
		protected void doSetStarlightDual(Starlight starlight) {
			underlying.setStarlightDual((byte) starlight.getColor1()[0], (byte) starlight.getColor1()[1],
					(byte) starlight.getColor1()[2], (byte) starlight.getColor2()[0], (byte) starlight.getColor2()[1],
					(byte) starlight.getColor2()[2], (byte) starlight.getSpeed());
		}

		@Override
		protected void doSetStarlightRandom(Starlight starlight) {
			underlying.setStarlightRandom((byte) starlight.getSpeed());
		}

		@Override
		protected void doSetStarlightSingle(Starlight starlight) {
			underlying.setStarlightSingle((byte) starlight.getColor()[0], (byte) starlight.getColor()[1],
					(byte) starlight.getColor()[2], (byte) starlight.getSpeed());
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
		protected void doSetOn(On on) {
			underlying.setLogoActive(true);
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
		protected void onCaps(Set<Capability> caps) {
			if (hasMethod("setLogoActive", boolean.class)) {
				supportedEffects.add(On.class);
				supportedEffects.add(Off.class);
			}
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

	final static System.Logger LOG = System.getLogger(NativeRazerDevice.class.getName());

	final static long MAX_CACHE_AGE = TimeUnit.DAYS.toMillis(7);

	public static String genericHash(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash).replace("/", "").replace("=", "");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private RazerBattery battery;
	private int batteryLevel;
	private ScheduledFuture<?> batteryTask;
	private Map<BrandingImage, String> brandingImages = new HashMap<>();
	private RazerBrightness brightness;
	private RazerRegionProfileLEDs leds;
	private Set<Capability> caps = new HashSet<>();
	private DBusConnection conn;
	private RazerDevice device;
	private String deviceName;
	private RazerDPI dpi;
	private String driverVersion;
	private Effect effect;
	private String firmware;
	private RazerGameMode gameMode;

	private short lastBrightness = -1;
	private List<Listener> listeners = new ArrayList<>();
	private RazerMacro macros;
	private int maxDpi = -1;
	private String path;
	private int pollRate = -1;
//	private Preferences prefs;
	private List<Region> regionList;
	private Set<Class<? extends Effect>> supportedEffects = new LinkedHashSet<>();

	private boolean wasCharging;
	private byte lowBatteryThreshold = 5;
	private int idleTime = (int) TimeUnit.MINUTES.toSeconds(5);
	private int[] matrix;
	private RazerBinding binding;
	private List<Profile> profiles;
	private RazerBindingLighting bindingLighting;
	private Set<EventCode> supportedInputEvents = new LinkedHashSet<>();
	private Set<Key> supportedLegacyKeys = new LinkedHashSet<>();

	private DeviceType deviceType;

	private String deviceSerial;

	NativeRazerDevice(String path, DBusConnection conn, OpenRazerBackend backend) throws Exception {
		this.path = path;
		this.conn = conn;
		device = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path), RazerDevice.class);

		// TODO actual supported events will need backend support (or implement our ownt
		// evdev caps discovery, or dervice from layout)

		var keyList = new ArrayList<>(Arrays.asList(EventCode.values()));
		Collections.sort(keyList, (k1, k2) -> k1.name().compareTo(k2.name()));
		supportedInputEvents.addAll(keyList);
		var legacyKeyList = new ArrayList<>(Arrays.asList(Key.values()));
		Collections.sort(legacyKeyList, (k1, k2) -> k1.name().compareTo(k2.name()));
		supportedLegacyKeys.addAll(legacyKeyList);

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
			RazerRegionProfileLEDs leds = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path),
					RazerRegionProfileLEDs.class);
			leds.getBlueLED();
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has profile LEDS control.", getName()));
			caps.add(Capability.PROFILE_LEDS);
			this.leds = leds;
		} catch (Exception e) {
			//
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has no profile LEDs.", getName()));
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
			if (!getType().equals(DeviceType.KEYBOARD)) {
				throw new UnsupportedOperationException(
						"Why do things other than keyboard have this DBus method if it doesn't work?");
			}
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
			RazerBinding binding = conn.getRemoteObject("org.razer", String.format("/org/razer/device/%s", path),
					RazerBinding.class);

			this.binding = binding;
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has macro profiles.", getName()));

			try {
				binding.getProfiles();
				caps.add(Capability.MACRO_PROFILES);
				caps.add(Capability.MACROS);
			} catch (Exception e) {
				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("%s has no legacy macros.", getName()));
			}

			try {
				RazerBindingLighting bindingLighting = conn.getRemoteObject("org.razer",
						String.format("/org/razer/device/%s", path), RazerBindingLighting.class);
				bindingLighting.getProfileLEDs(binding.getActiveProfile(), binding.getActiveMap());
				this.bindingLighting = bindingLighting;
				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("%s has binding lighting.", getName()));
				caps.add(Capability.MACRO_PROFILE_LEDS);

			} catch (Exception e) {
				//
				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("%s has no binding lighting.", getName()));
			}
		} catch (Exception e) {
			//

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s has no macro profiles.", getName()));
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

			try {
				macro.getMacros();
				if (!getType().equals(DeviceType.KEYBOARD)) {
					throw new UnsupportedOperationException(
							"Why do things other than keyboard have this DBus method if it doesn't work?");
				}
				this.macros = macro;

				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("%s has legacy macro control.", getName()));
				caps.add(Capability.MACROS);
			} catch (Exception e) {
			}

			try {
				macro.getMacroRecordingState();
				this.macros = macro;
				caps.add(Capability.MACRO_RECORDING);
				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("%s has no macro recording.", getName()));
			} catch (Exception e) {
			}

			try {
				macro.getModeModifier();
				this.macros = macro;
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

		try {
			JsonObject jsonObject = JsonParser.parseString(device.getRazerUrls()).getAsJsonObject();
			for (String key : jsonObject.keySet()) {
				String img = jsonObject.get(key).getAsString();
				try {
					BrandingImage bimg = BrandingImage.valueOf(key.substring(0, key.length() - 4).toUpperCase());
					brandingImages.put(bimg, img);
				} catch (Exception e) {
					LOG.log(Level.WARNING, String.format(
							"Unknow Razer URL image type. The image %s for %s will be ignored. Please report  this to Snake developers.",
							key, img), e);
				}
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void addListener(Listener listener) {
		listeners.add(listener);
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
				root.addProperty("key_id", src.getKey().nativeKeyName());
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
				root.addProperty("script", src.getScript() == null ? "" : src.getScript());
				if (src.getArgs() != null) {
					root.addProperty("args", String.join(" ", src.getArgs()));
				}
				return root;
			}
		});
		Gson parser = gson.create();
		var js = parser.toJson(macroSequence);
		macros.addMacro(macroSequence.getMacroKey().nativeKeyName(), js);
	}

	@Override
	public void close() throws Exception {
		if (batteryTask != null)
			batteryTask.cancel(false);
	}

	@Override
	public <E extends Effect> E createEffect(Class<E> clazz) {
		E effectInstance;
		try {
			effectInstance = clazz.getConstructor().newInstance();
			return effectInstance;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException(String.format("Cannot create effect %s.", clazz));
		}
	}

	@Override
	public void deleteMacro(Key key) {
		assertCap(Capability.MACROS);
		macros.deleteMacro(key.nativeKeyName());
	}

	@Override
	public String exportMacros() {
		assertCap(Capability.MACROS);
		return macros.getMacros();
	}

	@Override
	public int getBattery() {
		assertCap(Capability.BATTERY);
		return (int) battery.getBattery();
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
	public Set<Capability> getCapabilities() {
		return caps;
	}

	@Override
	public int[] getDPI() {
		assertCap(Capability.DPI);
		return dpi.getDPI();
	}

	@Override
	public String getDriverVersion() {
		if (driverVersion == null)
			driverVersion = device.getDriverVersion();
		return driverVersion;
	}

	@Override
	public Set<EventCode> getSupportedInputEvents() {
		return supportedInputEvents;
	}

	@Override
	public Set<Key> getSupportedLegacyKeys() {
		return supportedLegacyKeys;
	}

	@Override
	public Effect getEffect() {
		if (effect == null) {
			Class<? extends Effect> efClazz = getSupportedEffects().isEmpty() ? null
					: getSupportedEffects().iterator().next();
			if (efClazz != null) {
				effect = createEffect(efClazz);
			}
		}
		return effect;
	}

	@Override
	public String getFirmware() {
		if (firmware == null)
			firmware = device.getFirmware();
		return firmware;
	}

	@Override
	public int getIdleTime() {
		return idleTime;
	}

	@Override
	public String getImage() {
		try {
			String image = device.getDeviceImage();
			if (image != null && !image.equals("") && !image.startsWith("http:") && !image.startsWith("https:"))
				return image;
		} catch (Exception e) {
		}
		Iterator<String> it = brandingImages.values().iterator();
		while (it.hasNext()) {
			String v = it.next();
			if (v != null && !v.equals(""))
				return v;
		}
		return null;
	}

	@Override
	public String getImageUrl(BrandingImage image) {
		return brandingImages.get(image);
	}

	@Override
	public String getKeyboardLayout() {
		return device.getKeyboardLayout();
	}

	@Override
	public byte getLowBatteryThreshold() {
		return lowBatteryThreshold;
	}

	@Override
	public Map<Key, MacroSequence> getMacros() {
		assertCap(Capability.MACROS);
		String macroString = macros.getMacros();
		JsonObject jsonObject = JsonParser.parseString(macroString).getAsJsonObject();
		Map<Key, MacroSequence> macroSequences = new LinkedHashMap<>();
		for (String key : jsonObject.keySet()) {
			Key keyObj = Key.values()[0];
			try {
				keyObj = Key.fromNativeKeyName(key);
			} catch (IllegalArgumentException iae) {
				LOG.log(Level.WARNING, String.format(
						"Macro trigger has invalid key %s. This has been ignored and defaulted to the first available. If macros are saved now, the original key will be lost.",
						key));
				;
			}
			MacroSequence seq = new MacroSequence(keyObj);
			JsonArray arr = jsonObject.get(key).getAsJsonArray();
			for (JsonElement el : arr) {
				JsonObject obj = el.getAsJsonObject();
				String type = obj.get("type").getAsString();
				Macro macro;
				if (type.equals("MacroKey")) {
					MacroKey macroKey = new MacroKey();
					try {
						macroKey.setKey(Key.fromNativeKeyName(obj.get("key_id").getAsString()));
					} catch (IllegalArgumentException iae) {
						LOG.log(Level.WARNING, String.format(
								"Macro action has invalid key %s. This has been ignored and defaulted to the first available. If macros are saved now, the original key will be lost.",
								obj.get("key_id").getAsString()));
						macroKey.setKey(Key.values()[0]);
						;
					}
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
						macroScript.setArgs(Arrays.asList(obj.get("args").getAsString().split(" ")));
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
	public int[] getMatrixSize() {
		assertCap(Capability.MATRIX);
		if (matrix == null)
			matrix = device.getMatrixDimensions();
		return matrix;
	}

	@Override
	public int getMaxDPI() {
		assertCap(Capability.DPI);
		if (maxDpi == -1)
			maxDpi = dpi.maxDPI();
		return maxDpi;
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
	public int getPollRate() {
		if (pollRate == -1)
			pollRate = device.getPollRate();
		return pollRate;
	}

	@Override
	public List<Region> getRegions() {
		if (regionList == null) {
			regionList = new ArrayList<Region>();
			for (AbstractRazerRegion<?> r : Arrays.asList(new NativeRazerRegionChroma(conn),
					new NativeRazerRegionLeft(conn), new NativeRazerRegionRight(conn), new NativeRazerRegionLogo(conn),
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
	public String getSerial() {
		if (deviceSerial == null) {
			deviceSerial = device.getSerial();
		}
		return deviceSerial;
	}

	@Override
	public Set<Class<? extends Effect>> getSupportedEffects() {
		return supportedEffects;
	}

	@Override
	public DeviceType getType() {
		if (deviceType == null) {
			try {
				deviceType = DeviceType.valueOf(device.getDeviceType().toUpperCase());
			} catch (Exception e) {
				return DeviceType.UNRECOGNISED;
			}
		}
		return deviceType;
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
	public boolean isCharging() {
		assertCap(Capability.BATTERY);
		return battery.isCharging();
	}

	@Override
	public boolean isGameMode() {
		assertCap(Capability.GAME_MODE);
		return this.gameMode.getGameMode();
	}

	@Override
	public boolean isModeModifier() {
		assertCap(Capability.MACROS);
		return macros.getModeModifier();
	}

	@Override
	public boolean isSuspended() {
		// TODO err
		return false;
	}

	@Override
	public void removeListener(Listener listener) {
		listeners.remove(listener);
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
				this.brightness.setBrightness(brightness);
				fireChange(null);
			}
		}
	}

	@Override
	public void setDPI(short x, short y) {
		assertCap(Capability.DPI);
		dpi.setDPI(x, y);
		fireChange(null);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void setEffect(Effect effect) {
		this.effect = effect;
		lastBrightness = -1;
		for (Region r : getRegions()) {
			if (r.isSupported(effect))
				((AbstractRazerRegion) r).doSetEffect(effect);
		}
		fireChange(null);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void updateEffect(Effect effect) {
		boolean change = false;
		if (this.effect == null || this.effect.getClass() != effect.getClass()) {
			change = true;
		}
		this.effect = effect;
		lastBrightness = -1;
		for (Region r : getRegions()) {
			if (r.isSupported(effect)) {
				if (change)
					((AbstractRazerRegion) r).doSetEffect(effect);
				else
					((AbstractRazerRegion) r).doUpdateEffect(effect);
			}
		}
		if (change) {
			fireChange(null);
		}
	}

	@Override
	public void setGameMode(boolean gameMode) {
		assertCap(Capability.GAME_MODE);
		this.gameMode.setGameMode(gameMode);
		fireChange(null);
	}

	@Override
	public void setIdleTime(int idleTime) {
		assertCap(Capability.BATTERY);
		int old = this.idleTime;
		if (old != idleTime) {
			this.idleTime = idleTime;
			battery.setIdleTime(idleTime);
			fireChange(null);
		}
	}

	@Override
	public void setLowBatteryThreshold(byte threshold) {
		assertCap(Capability.BATTERY);
		int old = lowBatteryThreshold;
		if (old != threshold) {
			this.lowBatteryThreshold = threshold;
			battery.setLowBatteryThreshold(threshold);
			fireChange(null);
		}
	}

	@Override
	public void setModeModifier(boolean modify) {
		assertCap(Capability.MACROS);
		macros.setModeModifier(modify);
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
	public String toString() {
		return "NativeRazerDevice [deviceName=" + deviceName + ", driverVersion=" + driverVersion + ", firmware="
				+ firmware + ", path=" + path + "]";
	}

	@Override
	public boolean[] getProfileRGB() {
		assertCap(Capability.PROFILE_LEDS);
		return new boolean[] { leds.getRedLED(), leds.getGreenLED(), leds.getBlueLED() };
	}

	@Override
	public void setProfileRGB(boolean[] rgb) {
		assertCap(Capability.PROFILE_LEDS);
		leds.setRedLED(rgb[0]);
		leds.setGreenLED(rgb[1]);
		leds.setBlueLED(rgb[2]);
	}

	protected void fireMapAdded(ProfileMap map) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).mapAdded(map);
	}

	protected void fireMapRemoved(ProfileMap map) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).mapRemoved(map);
	}

	protected void fireMapChanged(ProfileMap map) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).mapChanged(map);
	}

	protected void fireChange(Region region) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).changed(this, region);
	}

	protected void fireProfileAdded(Profile profile) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).profileAdded(profile);
	}

	protected void fireProfileRemoved(Profile profile) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).profileRemoved(profile);
	}

	protected void fireActiveMapChanged(ProfileMap map) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).activeMapChanged(map);
	}

	void assertCap(Capability cap) {
		if (!caps.contains(cap))
			throw new UnsupportedOperationException(
					String.format("The capability %s is not supported on device %s.", cap, getName()));
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
	public List<Profile> getProfiles() {
		assertCap(Capability.MACRO_PROFILES);
		if (profiles == null) {
			profiles = new ArrayList<>();
			for (JsonElement el : JsonParser.parseString(binding.getProfiles()).getAsJsonArray()) {
				profiles.add(new RazerMacroProfile(bindingLighting, this, el.getAsString()));
			}
		}
		return profiles;
	}

	@Override
	public Profile getActiveProfile() {
		assertCap(Capability.MACRO_PROFILES);
		String activeProfile = binding.getActiveProfile();
		if (StringUtils.isBlank(activeProfile))
			return null;
		else
			return getProfile(activeProfile);
	}

	@Override
	public Profile getProfile(String name) {
		assertCap(Capability.MACRO_PROFILES);
		for (Profile p : getProfiles()) {
			if (p.getName().equals(name))
				return p;
		}
		return null;
	}

	@Override
	public Profile addProfile(String name) {
		assertCap(Capability.MACRO_PROFILES);
		binding.addProfile(name);
		Profile profile = new RazerMacroProfile(bindingLighting, this, name);
		if (profiles == null)
			profiles = new ArrayList<>();
		getProfiles();
		profiles.add(profile);
		fireProfileAdded(profile);
		profile.getDefaultMap().setLEDs(new boolean[3]);
		return profile;
	}

	static class RazerMacroProfile implements Profile {

		private NativeRazerDevice device;
		private String name;

		private List<uk.co.bithatch.snake.lib.binding.Profile.Listener> listeners = new ArrayList<>();
		private List<ProfileMap> profileMaps;
		private RazerBindingLighting bindingLighting;

		RazerMacroProfile(RazerBindingLighting bindingLighting, NativeRazerDevice device, String name) {
			this.device = device;
			this.bindingLighting = bindingLighting;
			this.name = name;

		}

		@Override
		public void addListener(uk.co.bithatch.snake.lib.binding.Profile.Listener listener) {
			listeners.add(listener);
		}

		@Override
		public void removeListener(uk.co.bithatch.snake.lib.binding.Profile.Listener listener) {
			listeners.remove(listener);
		}

		@Override
		public void activate() {
			device.binding.setActiveProfile(getName());
			ProfileMap map = getActiveMap();
			device.fireActiveMapChanged(map);
			fireActiveMapChanged(map);
		}

		@Override
		public boolean isActive() {
			return getName().equals(device.binding.getActiveProfile());
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public ProfileMap getDefaultMap() {
			return getMap(device.binding.getDefaultMap(getName()));
		}

		@Override
		public ProfileMap getActiveMap() {
			if (isActive())
				return getMap(device.binding.getActiveMap());
			else
				return null;
		}

		@Override
		public ProfileMap getMap(String id) {
			for (ProfileMap map : getMaps())
				if (map.getId().equals(id))
					return map;
			return null;
		}

		@Override
		public List<ProfileMap> getMaps() {
			if (profileMaps == null) {
				profileMaps = new ArrayList<>();
				for (JsonElement el : JsonParser.parseString(device.binding.getMaps(getName())).getAsJsonArray()) {
					profileMaps.add(new RazerMacroProfileMap(device, this, el.getAsString(), bindingLighting));
				}
			}
			return profileMaps;
		}

		@Override
		public String toString() {
			return "RazerMacroProfile [name=" + name + "]";
		}

		@Override
		public ProfileMap addMap(String id) {
			device.binding.addMap(getName(), id);
			ProfileMap map = new RazerMacroProfileMap(device, this, id, bindingLighting);
			if (profileMaps == null)
				profileMaps = new ArrayList<>();
			profileMaps.add(map);
			fireMapAdded(map);
			device.fireMapAdded(map);
			return map;
		}

		@Override
		public void setDefaultMap(ProfileMap map) {
			device.binding.setDefaultMap(getName(), map.getId());
			fireChanged();
		}

		@Override
		public void remove() {
			device.binding.removeProfile(getName());
			device.fireProfileRemoved(this);
			device.getProfiles();
			device.profiles.remove(this);
		}

		protected void fireMapAdded(ProfileMap map) {
			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).mapAdded(map);
		}

		protected void fireMapRemoved(ProfileMap map) {
			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).mapRemoved(map);
		}

		protected void fireMapChanged(ProfileMap map) {
			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).mapChanged(map);
		}

		protected void fireActiveMapChanged(ProfileMap map) {
			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).activeMapChanged(map);
		}

		protected void fireChanged() {
			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).changed(this);
		}

		@Override
		public Device getDevice() {
			return device;
		}
	}

	static class RazerMacroProfileMap implements ProfileMap {

		private String id;
		private RazerMacroProfile profile;
		private NativeRazerDevice device;
		private RazerBindingLighting bindingLighting;
		private Map<EventCode, MapSequence> sequences;

		RazerMacroProfileMap(NativeRazerDevice device, RazerMacroProfile profile, String id,
				RazerBindingLighting bindingLighting) {
			this.id = id;
			this.profile = profile;
			this.device = device;
			this.bindingLighting = bindingLighting;
		}

		@Override
		public boolean[] getLEDs() {
			device.assertCap(Capability.MACRO_PROFILE_LEDS);
			ThreeTuple<Boolean, Boolean, Boolean> profileLEDs = bindingLighting.getProfileLEDs(profile.getName(),
					getId());
			return new boolean[] { profileLEDs.a, profileLEDs.b, profileLEDs.c };
		}

		@Override
		public void setLEDs(boolean red, boolean green, boolean blue) {
			device.assertCap(Capability.MACRO_PROFILE_LEDS);
			bindingLighting.setProfileLEDs(profile.getName(), getId(), red, green, blue);
			((RazerMacroProfile) profile).fireMapChanged(this);
			device.fireMapChanged(this);
		}

		@Override
		public Profile getProfile() {
			return profile;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public boolean isActive() {
			return profile.isActive() && getId().equals(device.binding.getActiveMap());
		}

		@Override
		public void activate() {
			if (!profile.isActive())
				profile.activate();
			device.binding.setActiveMap(getId());
			device.fireActiveMapChanged(this);
			((RazerMacroProfile) profile).fireActiveMapChanged(this);
		}

		@Override
		public Map<EventCode, MapSequence> getSequences() {
			if (sequences == null) {
				sequences = new LinkedHashMap<>();
				JsonObject actions = JsonParser
						.parseString(device.binding.getActions(getProfile().getName(), getId(), "")).getAsJsonObject();
				for (String key : actions.keySet()) {
					RazerMapSequence seq = new RazerMapSequence(this,
							EventCode.fromCode(Ev.EV_KEY, Integer.parseInt(key)));
					JsonArray actionsArray = actions.get(key).getAsJsonArray();
					for (JsonElement actionElement : actionsArray) {
						JsonObject actionObject = actionElement.getAsJsonObject();
						seq.add(createMapAction(seq, actionObject));
					}
					sequences.put(seq.getMacroKey(), seq);
				}
			}
			return sequences;
		}

		protected RazerMapAction createMapAction(RazerMapSequence seq, JsonObject actionObject) {
			String type = actionObject.get("type").getAsString();
			String value = actionObject.get("value").getAsString();
			switch (type) {
			case "release":
				try {
					return new RazerReleaseMapAction(this, seq, device.binding, value);
				} catch (NumberFormatException nfe) {
					// TODO get last pressed key if available instead of next available
					return new RazerReleaseMapAction(this, seq, device.binding,
							String.valueOf(getNextFreeKey().code()));
				}
			case "execute":
				return new RazerExecuteMapAction(this, seq, device.binding, value);
			case "map":
				return new RazerMapMapAction(this, seq, device.binding, value);
			case "profile":
				return new RazerProfileMapAction(this, seq, device.binding, value);
			case "shift":
				return new RazerShiftMapAction(this, seq, device.binding, value);
			case "sleep":
				try {
					return new RazerSleepMapAction(this, seq, device.binding, value);
				} catch (NumberFormatException nfe) {
					return new RazerSleepMapAction(this, seq, device.binding, "1");
				}
			default:
				try {
					return new RazerKeyMapAction(this, seq, device.binding, value);
				} catch (Exception e) {
					return new RazerKeyMapAction(this, seq, device.binding, String.valueOf(getNextFreeKey().code()));
				}
			}
		}

		@Override
		public void remove() {
			device.binding.removeMap(profile.getName(), getId());
			((RazerMacroProfile) profile).profileMaps.remove(this);
			((RazerMacroProfile) profile).fireMapRemoved(this);
			device.fireMapRemoved(this);
		}

		@Override
		public String toString() {
			return "RazerMacroProfileMap [id=" + id + "]";
		}

		@Override
		public void record(int keyCode) {
			device.assertCap(Capability.MACRO_RECORDING);
			device.macros.startMacroRecording(profile.getName(), getId(), keyCode);
		}

		@Override
		public int getRecordingMacroKey() {
			return device.macros.getMacroKey();
		}

		@Override
		public boolean isRecording() {
			device.assertCap(Capability.MACRO_RECORDING);
			return device.macros.getMacroRecordingState();
		}

		@Override
		public void stopRecording() {
			device.assertCap(Capability.MACRO_RECORDING);
			device.macros.stopMacroRecording();
		}

		@Override
		public void setMatrix(Matrix matrix) {
			device.assertCap(Capability.MATRIX);
			// TODO
			// bindingLighting.setMatrix(profile.getName(), getId(), "");
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public Matrix getMatrix() {
			device.assertCap(Capability.MATRIX);
			// TODO
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public boolean isDefault() {
			return device.binding.getDefaultMap(profile.getName()).equals(getId());
		}

		@Override
		public void makeDefault() {
			device.binding.setDefaultMap(profile.getName(), getId());
			((RazerMacroProfile) profile).fireMapChanged(this);
			device.fireMapChanged(this);
		}

		@Override
		public MapSequence addSequence(EventCode key, boolean addDefaultActions) {
			RazerMapSequence seq = new RazerMapSequence(this, key);
			if (addDefaultActions)
				seq.addAction(KeyMapAction.class, key);
			synchronized (sequences) {
				if (sequences.containsKey(key))
					throw new IllegalArgumentException(String.format("Key %s already mapped.", key));
				sequences.put(key, seq);
			}
			device.fireMapChanged(this);
			return seq;

		}

	}

	static abstract class RazerMapAction implements MapAction {

		private RazerBinding binding;
		private ProfileMap map;
		private RazerMapSequence sequence;

		RazerMapAction(ProfileMap map, RazerMapSequence sequence, RazerBinding binding, String value) {
			this.binding = binding;
			this.sequence = sequence;
			this.map = map;
			setValue(value);
		}

		@Override
		public final ProfileMap getMap() {
			return map;
		}

		@Override
		public final int getActionId() {
			int idx = getSequence().indexOf(this);
			if (idx == -1)
				throw new IllegalStateException("Found not found action ID for " + getActionType() + " in " + sequence);
			return idx;
		}

		@Override
		public void remove() {
			doRemove();
			sequence.remove(this);
			if (sequence.isEmpty())
				sequence.remove();
		}

		protected void doRemove() {
			binding.removeAction(map.getProfile().getName(), map.getId(), sequence.getMacroKey().code(), getActionId());
		}

		@Override
		public MapSequence getSequence() {
			return sequence;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + " [hashCode=" + hashCode() + ",actionId=" + getActionId() + ", value="
					+ getValue() + "]";
		}

		@SuppressWarnings("unchecked")
		@Override
		public <A extends MapAction> A update(Class<A> actionType, Object value) {
			RazerMacroProfileMap razerProfileMap = (RazerMacroProfileMap) getMap();
			int actionId = getActionId();

			razerProfileMap.device.binding.updateAction(getMap().getProfile().getName(), getMap().getId(),
					getSequence().getMacroKey().code(), toNativeActionName(actionType),
					value == null ? "" : String.valueOf(value), actionId);
			JsonElement actions = JsonParser
					.parseString(razerProfileMap.device.binding.getActions(getMap().getProfile().getName(),
							getMap().getId(), String.valueOf(getSequence().getMacroKey().code())));
			JsonArray actionsArray = actions.getAsJsonArray();
			JsonObject actionObject = actionsArray.get(actionId).getAsJsonObject();

			MapAction currentAction = sequence.get(actionId);
			if (currentAction.getActionType().equals(actionType)) {
				currentAction.setValue(value == null ? null : String.valueOf(value));
				return (A) currentAction;
			} else {
				MapAction mapAction = razerProfileMap.createMapAction((RazerMapSequence) getSequence(), actionObject);

				/* If switching between key types, keep the key code */
				if ((actionType.equals(KeyMapAction.class)
						&& currentAction.getActionType().equals(ReleaseMapAction.class))
						|| (actionType.equals(ReleaseMapAction.class)
								&& currentAction.getActionType().equals(KeyMapAction.class))) {
					mapAction.setValue(currentAction.getValue());
					razerProfileMap.device.binding.updateAction(getMap().getProfile().getName(), getMap().getId(),
							getSequence().getMacroKey().code(), toNativeActionName(actionType), mapAction.getValue(),
							actionId);
				}

				getSequence().set(actionId, mapAction);
				((RazerMacroProfile) razerProfileMap.profile).fireMapChanged(razerProfileMap);
				return (A) mapAction;
			}
		}

		@Override
		public void commit() {
			update(getActionType(), getValue());
		}
	}

	@SuppressWarnings("serial")
	static class RazerMapSequence extends MapSequence {

		public RazerMapSequence(ProfileMap map, EventCode key) {
			super(map, key);
		}

		public RazerMapSequence(ProfileMap map) {
			super(map);
		}

		@Override
		public void remove() {
			for (MapAction act : this) {
				((RazerMapAction) act).doRemove();
			}
			clear();
			((RazerMacroProfileMap) getMap()).sequences.remove(getMacroKey());
		}

		@SuppressWarnings("unchecked")
		@Override
		public <A extends MapAction> A addAction(Class<A> actionType, Object value) {
			RazerMacroProfileMap razerProfileMap = (RazerMacroProfileMap) getMap();
			razerProfileMap.device.binding.addAction(getMap().getProfile().getName(), getMap().getId(),
					getMacroKey().code(), toNativeActionName(actionType),
					value instanceof EventCode ? String.valueOf(((EventCode) value).code()) : String.valueOf(value));

			JsonElement actions = JsonParser.parseString(razerProfileMap.device.binding.getActions(
					getMap().getProfile().getName(), getMap().getId(), String.valueOf(getMacroKey().code())));
			JsonArray actionsArray = actions.getAsJsonArray();
			JsonObject actionObject = actionsArray.get(actionsArray.size() - 1).getAsJsonObject();
			MapAction mapAction = razerProfileMap.createMapAction(this, actionObject);
			add(mapAction);
			((RazerMacroProfile) razerProfileMap.profile).fireMapChanged(razerProfileMap);
			return (A) mapAction;
		}

		@Override
		public void commit() {
			/*
			 * There is no DBus function to update the key for a map, so have to remove the
			 * entire map and recreate all of the actions
			 */
			RazerMacroProfileMap razerProfileMap = (RazerMacroProfileMap) getMap();
			razerProfileMap.device.binding.removeMap(getMap().getProfile().getName(), getMap().getId());
			razerProfileMap.device.binding.addMap(getMap().getProfile().getName(), getMap().getId());
			for (Map.Entry<EventCode, MapSequence> seqEn : razerProfileMap.sequences.entrySet()) {
				for (MapAction action : seqEn.getValue()) {
					razerProfileMap.device.binding.addAction(getMap().getProfile().getName(), getMap().getId(),
							seqEn.getValue().getMacroKey().code(), toNativeActionName(action.getActionType()),
							action.getValue());
				}
			}
		}

		@Override
		public void record() {
			RazerMacroProfileMap razerProfileMap = (RazerMacroProfileMap) getMap();
			razerProfileMap.device.macros.startMacroRecording(razerProfileMap.getProfile().getName(),
					razerProfileMap.getId(), getMacroKey().code());
		}

		@Override
		public boolean isRecording() {
			return ((RazerMacroProfileMap) getMap()).device.macros.getMacroRecordingState();
		}

		@Override
		public void stopRecording() {
			((RazerMacroProfileMap) getMap()).device.macros.stopMacroRecording();
		}
	}

	static class RazerKeyMapAction extends RazerMapAction implements KeyMapAction {

		private EventCode press;

		RazerKeyMapAction(ProfileMap map, RazerMapSequence sequence, RazerBinding binding, String press) {
			super(map, sequence, binding, press);
		}

		@Override
		public void validate() throws ValidationException {
			if (press == null)
				throw new ValidationException("keyMapAction.missingPress");
		}

		@Override
		public EventCode getPress() {
			return press;
		}

		@Override
		public void setPress(EventCode press) {
			this.press = press;
		}

	}

	static class RazerReleaseMapAction extends RazerMapAction implements ReleaseMapAction {

		private EventCode release;

		RazerReleaseMapAction(ProfileMap map, RazerMapSequence sequence, RazerBinding binding, String value) {
			super(map, sequence, binding, value);
		}

		@Override
		public void validate() throws ValidationException {
			if (release == null)
				throw new ValidationException("keyReleaseAction.missingRelease");
		}

		@Override
		public EventCode getRelease() {
			return release;
		}

		@Override
		public void setRelease(EventCode release) {
			this.release = release;
		}

	}

	static class RazerExecuteMapAction extends RazerMapAction implements ExecuteMapAction {

		private String command;
		private List<String> args;

		RazerExecuteMapAction(ProfileMap map, RazerMapSequence sequence, RazerBinding binding, String value) {
			super(map, sequence, binding, value);
		}

		@Override
		public void validate() throws ValidationException {
			if (command == null || command.length() == 0)
				throw new ValidationException("executeMapAction.missingCommand");
		}

		@Override
		public List<String> getArgs() {
			return args;
		}

		@Override
		public String getCommand() {
			return command;
		}

		@Override
		public void setArgs(List<String> args) {
			this.args = args;
		}

		@Override
		public void setCommand(String script) {
			this.command = script;
		}

	}

	static class RazerMapMapAction extends RazerMapAction implements MapMapAction {

		private String mapName;

		RazerMapMapAction(ProfileMap map, RazerMapSequence sequence, RazerBinding binding, String value) {
			super(map, sequence, binding, value);
		}

		@Override
		public void validate() throws ValidationException {
			if (mapName == null || mapName.length() == 0)
				throw new ValidationException("mapMapAction.missingMapName");
		}

		@Override
		public String getMapName() {
			return mapName;
		}

		@Override
		public void setMapName(String mapName) {
			this.mapName = mapName;
		}
	}

	static class RazerProfileMapAction extends RazerMapAction implements ProfileMapAction {

		private String profileName;

		RazerProfileMapAction(ProfileMap map, RazerMapSequence sequence, RazerBinding binding, String value) {
			super(map, sequence, binding, value);
		}

		@Override
		public void validate() throws ValidationException {
			if (profileName == null || profileName.length() == 0)
				throw new ValidationException("profileMapAction.missingProfileName");
		}

		@Override
		public String getProfileName() {
			return profileName;
		}

		@Override
		public void setProfileName(String profileName) {
			this.profileName = profileName;
		}
	}

	static class RazerShiftMapAction extends RazerMapAction implements ShiftMapAction {

		private String mapName;

		RazerShiftMapAction(ProfileMap map, RazerMapSequence sequence, RazerBinding binding, String value) {
			super(map, sequence, binding, value);
		}

		@Override
		public void validate() throws ValidationException {
			if (mapName == null || mapName.length() == 0)
				throw new ValidationException("shiftMapAction.missingMapName");
		}

		@Override
		public String getMapName() {
			return mapName;
		}

		@Override
		public void setMapName(String mapName) {
			this.mapName = mapName;
		}
	}

	static class RazerSleepMapAction extends RazerMapAction implements SleepMapAction {

		private float seconds;

		RazerSleepMapAction(ProfileMap map, RazerMapSequence sequence, RazerBinding binding, String value) {
			super(map, sequence, binding, value);
		}

		@Override
		public void validate() throws ValidationException {
		}

		@Override
		public float getSeconds() {
			return seconds;
		}

		@Override
		public void setSeconds(float seconds) {
			this.seconds = seconds;
		}
	}

	static String toNativeActionName(Class<? extends MapAction> actionType) {
		String actionTypeName = actionType.getSimpleName();
		actionTypeName = actionTypeName.substring(0, actionTypeName.length() - 9).toLowerCase();
		return actionTypeName;
	}
}