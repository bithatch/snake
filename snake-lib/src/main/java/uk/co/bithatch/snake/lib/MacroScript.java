package uk.co.bithatch.snake.lib;

import java.io.File;
import java.util.List;

public class MacroScript implements Macro {

	private String script;
	private List<String> args;
	private Key macroKey;

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	public Key getMacroKey() {
		return macroKey;
	}

	public void setMacroKey(Key macroKey) {
		this.macroKey = macroKey;
	}

	@Override
	public String toString() {
		return "MacroScript [script=" + script + ", args=" + args + ", macroKey=" + macroKey + "]";
	}

	@Override
	public void validate() throws ValidationException {
		if (script == null || script.length() == 0)
			throw new ValidationException("macroScript.missingScript");
		var f = new File(script);
		if (!f.exists() || !f.isFile())
			throw new ValidationException("macroScript.invalidScript");
	}

}
