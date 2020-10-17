package uk.co.bithatch.snake.lib;

@SuppressWarnings("serial")
public class ValidationException extends Exception {

	public ValidationException(String key) {
		super(key);
	}
	
	public ValidationException(String key, Throwable cause) {
		super(key, cause);
	}
}

