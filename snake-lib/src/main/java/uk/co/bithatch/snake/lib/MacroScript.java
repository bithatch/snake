package uk.co.bithatch.snake.lib;

import java.io.File;
import java.util.List;

@Deprecated
public class MacroScript implements Macro {

	private List<String> args;
	private String script;

	public List<String> getArgs() {
		return args;
	}

	public String getScript() {
		return script;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	public void setScript(String script) {
		this.script = script;
	}

	@Override
	public String toString() {
		return "MacroScript [script=" + script + ", args=" + args  + "]";
	}

	@Override
	public void validate() throws ValidationException {
		if (script == null || script.length() == 0)
			throw new ValidationException("macroScript.missingScript");
		var f = new File(script);
		if (!f.exists() || !f.isFile()) {
			for(String path : System.getenv("PATH").split(":")) {
				if(new File(new File(path), script).exists())
					return;
			}
			throw new ValidationException("macroScript.invalidScript");
		}
	}

}
