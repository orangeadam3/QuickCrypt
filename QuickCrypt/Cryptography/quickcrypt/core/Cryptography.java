package quickcrypt.core;
import java.security.SecureRandom;
import javax.crypto.Cipher;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Collection of static functions useful for cryptography
 * 
 * @author Adam Spiegel
 */
public class Cryptography {
	
	/**
	 * Generates random bytes with SecureRandom
	 * @param num desired size of random output
	 * @return random bytes of size num
	 */
	public static byte[] randomBytes(int num)
	{
		SecureRandom random = new SecureRandom();
	    byte bytes[] = new byte[num];
	    random.nextBytes(bytes);
		return bytes;
	}

	/**
	 * hashes any input data using SHA512 and MessageDigest
	 * @param input bytes to hash
	 * @return 64 bytes to return
	 */
	public static byte[] SHA512(byte[] input){
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {return null;}
		
		digest.reset();
		digest.update(input);
		return digest.digest();
	 }
	
	/**
	 * Encrypts any bytes with AES256
	 * @param input bytes to encrypt
	 * @param key 256-bit (32-byte) secret key
	 * @param iv 128-bit (16-byte) random and unique array
	 * @return encrypted output
	 * @throws QCError if an error occured while trying to encrypt or inputs were invalid
	 */
	public static byte[] encryptAES(byte[] input, byte[] key, byte[] iv) throws QCError 
	{
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
			return cipher.doFinal(input);
		} catch (InvalidKeyException e) {
			if(e.getMessage().equals("Illegal key size"))
				throw new QCError("Illegal key size, this probobly means that, AES-"+(key.length*8)
								+" is not supported or not allowed by your machine's java try a smaller AES");
			throw new QCError(e.getMessage());
		} catch (IllegalBlockSizeException e) {
			throw new QCError("Block size, "+key.length+", is illegal");
		} catch (BadPaddingException e) {
			throw new QCError("Bad Padding: "+e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Decrypts message encrypted with AES256
	 * @param input encrypted bytes
	 * @param key 256-bit (32-byte) secret key used to encrypt
	 * @param iv 128-bit (16-byte) random and unique array, but same as encrypted with
	 * @return decrypted output
	 * @throws QCError if input is invalid, the key or the iv was not the same used to encrypt, or they were wrong sizes
	 */
	public static byte[] decryptAES(byte[] input, byte[] key, byte[] iv) throws QCError 
	{
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
			return cipher.doFinal(input);
			
		} catch (InvalidKeyException e) {
			if(e.getMessage().equals("Illegal key size"))
				throw new QCError("Illegal key size, this probobly means that, AES-"+(key.length*8)
								+" is not supported or not allowed by your machine's java try a smaller AES");
			throw new QCError(e.getMessage());
		} catch (IllegalBlockSizeException e) {
			throw new QCError("Block size, "+key.length+", is illegal");
		} catch (BadPaddingException e) {
			throw new QCError("Bad Padding or Incorrect key: "+e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			throw new QCError("No Such Algorithm Exception");
		} catch (NoSuchPaddingException e) {
			throw new QCError("No Such Padding Exception");
		} catch (InvalidAlgorithmParameterException e) {
			throw new QCError("Invalid Algorithm Parrameter");
		}
	}
}
