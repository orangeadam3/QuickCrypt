package quickcrypt.core;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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
	}
	
	/**
	 * Wrapper for encrypting with currently selected secret
	 */
	public byte[] encrypt(byte[] in) throws QCError
	{
		return currentSecret.encrypt(in);
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
	 * @return secret of null if it doesn't exist
	 */
	public Secret getSecret(String label)
	{
		return allSecrets.get(label);
	}

	@Override
	public String base64Id() {
		return "SS";
	}
}
