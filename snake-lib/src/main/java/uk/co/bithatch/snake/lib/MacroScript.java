package uk.co.bithatch.snake.lib;

import java.io.File;
import java.util.List;

public class MacroScript implements Macro {

	private List<String> args;
	private Key macroKey;
	private String script;

	public List<String> getArgs() {
		return args;
	}

	public Key getMacroKey() {
		return macroKey;
	}

	public String getScript() {
		return script;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	public void setMacroKey(Key macroKey) {
		this.macroKey = macroKey;
	}

	public void setScript(String script) {
		this.script = script;
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
