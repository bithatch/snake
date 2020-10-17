package uk.co.bithatch.snake.lib;

import java.net.MalformedURLException;
import java.net.URL;

public class MacroURL implements Macro {

	private String url;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		return "MacroURL [url=" + url + "]";
	}

	@Override
	public void validate() throws ValidationException {
		if (url == null || url.length() == 0)
			throw new ValidationException("macroURL.missingURL");
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			throw new ValidationException("macroURL.invalidURL");
		}
	}
}
