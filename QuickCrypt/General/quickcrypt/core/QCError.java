package quickcrypt.core;
/**
 * Simple Template Exception for housing a variety of Quick Crypt related Errors
 *
 * @author The Internet mostly
 */

public class QCError extends Exception {

	private static final long serialVersionUID = 1L; //idk what that is for, but java wouldn't stop complaining till it was added
	public QCError() { super(); }
	public QCError(String message) { super(message); }
	public QCError(String message, Throwable cause) { super(message, cause); }
	public QCError(Throwable cause) { super(cause); }
}