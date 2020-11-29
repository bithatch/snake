package uk.co.bithatch.snake.lib.binding;

import java.util.ArrayList;
import java.util.List;

import uk.co.bithatch.snake.lib.InputEventCode;

public interface ExecuteMapAction extends MapAction {

	List<String> getArgs();

	String getCommand();

	void setArgs(List<String> args);

	void setCommand(String script);

	@Override
	default Class<? extends MapAction> getActionType() {
		return ExecuteMapAction.class;
	}

	default void setValue(String value) {
		List<String> l = ExecuteMapAction.parseQuotedString(value);
		setCommand(l.isEmpty() ? "" : l.remove(0));
		setArgs(l);
	}

	default String getValue() {
		StringBuilder v = new StringBuilder();
		v.append(formatArgument(getCommand()));
		for (String arg : getArgs()) {
			v.append(" ");
			v.append(formatArgument(arg));
		}
		return v.toString();
	}

	public static String formatArgument(String arg) {
		if (arg.indexOf(' ') != -1 || arg.indexOf('"') != -1 || arg.indexOf('\'') != -1) {
			StringBuilder a = new StringBuilder("'");
			for (char c : arg.toCharArray()) {
				if (c == '\\' || c == '\'' || c == '"')
					a.append("\\");
				a.append(c);
			}
			a.append("'");
			return a.toString();
		} else
			return arg;
	}

	public static List<String> parseQuotedString(String command) {
		List<String> args = new ArrayList<String>();
		boolean escaped = false;
		boolean quoted = false;
		StringBuilder word = new StringBuilder();
		for (int i = 0; i < command.length(); i++) {
			char c = command.charAt(i);
			if (c == '"' && !escaped) {
				if (quoted) {
					quoted = false;
				} else {
					quoted = true;
				}
			} else if (c == '\\' && !escaped) {
				escaped = true;
			} else if ((c == ' ' || c == '\n') && !escaped && !quoted) {
				if (word.length() > 0) {
					args.add(word.toString());
					word.setLength(0);
					;
				}
			} else {
				word.append(c);
			}
		}
		if (word.length() > 0)
			args.add(word.toString());
		return args;
	}
}
