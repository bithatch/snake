package uk.co.bithatch.snake.ui.addons;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import uk.co.bithatch.snake.lib.Backend.BackendListener;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.lib.Region;

public class Script extends AbstractAddOn implements BackendListener, Listener {

	final static System.Logger LOG = System.getLogger(Script.class.getName());

	private groovy.lang.Script script;
	private Binding bindings;
	private App context;
	private int lastLevel;
	private GroovyScriptEngine groovy;

	public Script(Path scriptDir, App context, GroovyScriptEngine groovy) throws Exception {
		id = scriptDir.getFileName().toString();
		this.archive = scriptDir;
		this.context = context;
		this.groovy = groovy;
	}

	public Object run() throws Exception {

		List<Device> devices = context.getBackend().getDevices();
		for (Device d : devices) {
			d.addListener(this);
		}

		bindings = new Binding();
		bindings.setProperty("snake", context);
		bindings.setProperty("driver", context.getBackend());
		bindings.setProperty("devices", devices);
		bindings.setProperty("device", devices.isEmpty() ? null : devices.get(0));

		script = groovy.createScript(id + "/" + id + ".plugin.groovy", bindings);
		description = defaultIfBlank((String) script.getProperty("description"), "");
		author = defaultIfBlank((String) script.getProperty("author"), "");
		license = defaultIfBlank((String) script.getProperty("license"), "");
		name = defaultIfBlank((String) script.getProperty("name"), id);
		supportedLayouts = (String[]) script.getProperty("supportedLayouts");
		supportedModels = (String[]) script.getProperty("supportedModels");
		unsupportedLayouts = (String[]) script.getProperty("unsupportedLayouts");
		unsupportedModels = (String[]) script.getProperty("unsupportedModels");

		context.getBackend().addListener(this);

		return script.invokeMethod("run", null);
	}

	@Override
	public URL getScreenshot() {
		return null;
	}

	@Override
	public void close() throws Exception {
		context.getBackend().removeListener(this);
	}

	@Override
	public void deviceAdded(Device device) {
		invoke("deviceAdded", device);
	}

	@Override
	public void deviceRemoved(Device device) {
		invoke("deviceRemoved", device);
	}

	@Override
	public void changed(Device device, Region region) {
		if (device.getCapabilities().contains(Capability.BATTERY)) {
			int level = device.getBattery();
			if (level != lastLevel) {
				invoke("battery", device);
				lastLevel = level;
			}
		}
		invoke("change", device, region);
	}

	public void install() {
		invoke("install");
	}

	public void uninstall() {
		invoke("uninstall");
	}

	protected String defaultIfBlank(String val, String def) {
		return val == null || val.equals("") ? def : val;
	}

	protected boolean invoke(String method, Object... args) {
		List<Object> arglist = new ArrayList<>(Arrays.asList(args));
		do {
			try {
				/* Only bother to match signatures based on size */
				for (Method m : script.getClass().getMethods()) {
					if (m.getName().equals(method) && m.getParameterCount() == arglist.size()) {
						m.invoke(script, arglist.toArray(new Object[0]));
						return true;
					}
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| SecurityException e) {
				throw new IllegalStateException("Failed to invoke script method.", e);
			}
			if (!arglist.isEmpty())
				arglist.remove(arglist.size() - 1);
			else
				return false;
		} while (true);
	}
}
