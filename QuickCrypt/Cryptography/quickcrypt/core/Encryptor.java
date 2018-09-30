package quickcrypt.core;

import java.io.PrintStream;
import java.util.Scanner;

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
	public abstract void save(PrintStream ps); //save data to stream
	public abstract void load(Scanner in) throws QCError; //load data from stream
}
