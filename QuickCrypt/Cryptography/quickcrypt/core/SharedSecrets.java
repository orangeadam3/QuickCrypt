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
}
