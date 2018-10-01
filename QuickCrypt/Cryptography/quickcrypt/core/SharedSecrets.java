package quickcrypt.core;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/** 
 * Manages a list of shared secrets
 * <p>
 * Always retains a default secret so it can always be used to encrypt
 * @author Adam Spiegel
 */
public class SharedSecrets extends Encryptor {
	Map<String,Secret> allSecrets;
	Secret currentSecret;
	
	String symtype;
	
	/**
	 * Creates an object with only the default secret
	 * @throws QCError if there was an error generating the default secret
	 */
	public SharedSecrets() throws QCError
	{
		Hexadecimal hex = new Hexadecimal();
		currentSecret = new Secret("DEFAULT",hex.to(Cryptography.SHA512(("passwordDEFAULT").getBytes(StandardCharsets.UTF_8))),hex);
		allSecrets = new HashMap<String,Secret>();
		allSecrets.put(currentSecret.label, currentSecret);
		symtype = "AS5"; //default AES-256
	}
	
	/**
	 * Wrapper for encrypting with currently selected secret
	 */
	public byte[] encrypt(byte[] in) throws QCError
	{
		return currentSecret.encrypt(in,symtype);
	}
	
	/**
	 * Wrapper for decrypting with currently selected secret
	 */
	public byte[] decrypt(byte[] in) throws QCError
	{
		return Secret.decrypt(in, allSecrets);
	}
	
	/**
	 * Change currently selected secret
	 * @param label unique label to search for
	 * @throws QCError if the secret was not found
	 */
	public void selectSecret(String label) throws QCError
	{
		Secret newSecret = allSecrets.get(label);
		if(newSecret==null)throw new QCError("Secret does not exist");
		currentSecret = newSecret;
	}
	
	/**
	 * Adds a secret
	 * If label is in use, it's current Secret will be removed
	 * @param s secret to add
	 */
	public void addSecret(Secret s)
	{
		allSecrets.put(s.label, s);
	}
	
	/**
	 * @param label of secret to remove
	 */
	public void removeSecret(String label)
	{
		allSecrets.remove(label);
	}
	
	/**
	 * @return set of labels
	 */
	public Set<String> getLabels()
	{
		return allSecrets.keySet();
	}
	
	/**
	 * @param label of secret to get
	 * @return secret or null if it doesn't exist
	 */
	public Secret getSecret(String label)
	{
		return allSecrets.get(label);
	}
	
	/**
	 * @return selected secret
	 */
	public Secret getSelectedSecret()
	{
		return currentSecret;
	}

	@Override
	public String base64Id() {
		return "SS";
	}

	@Override
	public String shortName() {
		return "Shared Secret";
	}

	@Override
	public String description() {
		return "Uses up to 512-bit symetric encryption algorithms to encrypt in such a way that, "
				+"the sender and reciver must know the same \"secret\" in order to transfer the message. "
				+"Any would be spys are unable to read an intercepted message without knowing te secret.\n"
				+"The secret takes the form of a Label and a Key. "
				+"The label cannot be shared by any other secret on this instance of QuickCrypt. "
				+"The label can be seen by people without the secret so do not use personal information or passwords for the label. "
				+"The key is generated from a password and should be the same password between both the sender and reciver. "
				+"In order properly share a secret, both the password and the key should be the same.";
	}
	
	/**
	 * Sets code for encryption algorithm to use when encrypting
	 * @param code 3 character ASCII code referencing algorithm e.x. "AS5" for AES-256 and "AS4" for AES-128
	 */
	public void setSymetricAlgorithmCode(String code)
	{
		symtype = code;
	}
	
	public String getSymetricAlgorithmCode()
	{
		return symtype;
	}

	/**
	 * Save secrets and context to a PrintStream
	 */
	@Override
	public void save(PrintStream ps) {
		Hexadecimal hex = new Hexadecimal();
		
		ps.println(allSecrets.size()+" "+hex.to(currentSecret.getLabel().getBytes(StandardCharsets.UTF_8)));
		
		for(Secret s:allSecrets.values())
		if(!s.getLabel().equals("DEFAULT"))
		{
			String export = s.exportAs(hex,":");
			ps.println(hex.to(export.getBytes(StandardCharsets.UTF_8)));
		}
	}
	
	/**
	 * Load saved secrets from a Scanner
	 */
	@Override
	public void load(Scanner in) throws QCError{
		
		if(!in.hasNext())throw new QCError("invalid Shared Secrets input");
		
		Hexadecimal hex = new Hexadecimal();
			
		int size = in.nextInt();
		String sellabel = new String(hex.from(in.nextLine().substring(1)),StandardCharsets.UTF_8);
		
		for(int x=0;x<size-1;x++)
		{
			if(!in.hasNext())throw new QCError("invalid Shared Secrets input");
			Secret s = new Secret(hex,":",new String(hex.from(in.nextLine()),StandardCharsets.UTF_8));
			addSecret(s);
		}
		
		selectSecret(sellabel);
			
	}
}
