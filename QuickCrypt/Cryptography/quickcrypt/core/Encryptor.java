package quickcrypt.core;

/**
 * Template for any self respecting encryptor
 * @author Adam Spiegel
 *
 */

public abstract class Encryptor {
	public abstract byte[] encrypt(byte[] in) throws QCError;
	public abstract byte[] decrypt(byte[] in) throws QCError;
	
	public abstract String base64Id(); //unique two base64 char Identifier
	public abstract String shortName(); //name
	public abstract String description();
}
