package quickcrypt.core;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a shared secret
 * @author Adam Spiegel
 */
public class Secret{
	
	String label;
	byte[] bytelabel;
	byte[] key;
	
	/**
	 * Creates a Secret from a label and an encoded key or, if load is null, key is treated as a password to be hashed
	 * @param label name of secret must be the same for both users
	 * @param key secret encoded key or password to be hashed
	 * @param load binary encoder to be used to decode the key into bytes, or null
	 * @throws QCError
	 */
	public Secret(String label, String key, BinaryEncoder load) throws QCError
	{
		label = label.toUpperCase(Locale.ROOT);
		if(label.indexOf(" ")!=-1)throw new QCError("Space Found in Label");
		bytelabel = label.getBytes(StandardCharsets.UTF_8);
		if(bytelabel.length>255)throw new QCError("Label too long");
		this.label = label;
		
		if(load!=null) //key is an encoded string so decode to get bytes
		{
			this.key = load.from(key);
		}
		else //key should be treated as a password and hashed with label to get the key
		{
			byte[] one = Cryptography.SHA512(key.getBytes(StandardCharsets.UTF_8));
			byte[] two = Cryptography.SHA512(label.getBytes(StandardCharsets.UTF_8));
			byte[] merge = new byte[one.length+two.length];
			
			System.arraycopy(one,0,merge,0         ,one.length);
			System.arraycopy(two,0,merge,one.length,two.length);
			
			this.key = Cryptography.SHA512(merge);
		}
		
		if(this.key.length!=64)throw new QCError("Invalid key size"); //input was not 64 bytes
	}
	
	/**
	 * @return unique refrence label
	 */
	public String getLabel()
	{
		return label;
	}
	
	/**
	 * Encodes key as a specified encoder
	 * @param encoding encoder to use to encode key
	 * @return encoded key
	 */
	public String getKeyAs(BinaryEncoder encoding) //encode key in a specific way
	{
		return encoding.to(key);
	}

	/**
	 * Encrypt any data with AES.
	 * @param input data to encrypt
	 * @return encrypted data
	 * @throws QCError something went wrong with encryption
	 */
	public byte[] encrypt(byte[] input) throws QCError
	{
		//chop key to 32 bytes
		byte[] k = new byte[32];
		System.arraycopy(key,0,k,0,32);

		byte[] iv = Cryptography.randomBytes(16); //get iv
		
		byte[] data = Cryptography.encryptAES(input,k,iv); ///actual encryption
		
		//format output
		byte[] out = new byte[4+bytelabel.length+iv.length+data.length];
		out[0] = 'A';out[1] = 'S';out[2] = '5'; //encryption type
		out[3] = (byte) bytelabel.length;
		
		System.arraycopy(bytelabel,0,out,4,bytelabel.length); ///store secret label
		System.arraycopy(iv, 0, out, 4+bytelabel.length, iv.length); //store iv
		System.arraycopy(data,0,out,4+bytelabel.length+iv.length,data.length); //actual encrypted text
		
		return out;
	}
	
	/**
	 * Decrypt data that was encrypted with AES
	 * @param input data to decrypt
	 * @param secretsList list of possible secrets
	 * @return decrypted data
	 * @throws QCError if data is invalid or some other problem encrypting
	 */
	public static byte[] decrypt(byte[] input, Map<String, Secret> secretsList) throws QCError
	{

		int labelsize = BinaryEncoder.byteToUnsignedInt(input[3]);
		
		if(input.length<6+16||input.length<labelsize+5+16||labelsize==0)throw new QCError("Shared Secret Header too small");
		
		//get label used to encrypt
		byte[] searchlabelb = new byte[labelsize];
		System.arraycopy(input,4,searchlabelb,0,input[3]);
		String searchlabel = new String(searchlabelb,StandardCharsets.UTF_8);
		
		//locate key if it exists using encoded label
		Secret secret = secretsList.get(searchlabel);
		if(secret==null)throw new QCError("Unknown secret needed for decryption; label=\""+searchlabel+"\"");
		
		//chop new key to 32 bytes
		byte[] key = new byte[32];
		System.arraycopy(secret.key,0,key,0,32);
		
		///get iv
		byte[] iv = new byte[16];
		System.arraycopy(input,4+searchlabelb.length,iv,0,iv.length);
		
		//get encrypted data
		byte[] data = new byte[input.length-(4+searchlabelb.length+iv.length)];
		System.arraycopy(input,4+searchlabelb.length+iv.length,data,0,data.length);
		
		return Cryptography.decryptAES(data, key, iv); //decrypt
	}
}
