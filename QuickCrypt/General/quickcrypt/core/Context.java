package quickcrypt.core;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;

import javax.imageio.ImageIO;

/**
 * Context for Quick Crypt data processing operations. A master context is
 * needed in order to control how things are encrypted. A child context can be
 * created from a header string most notably for decrypting. This will not
 * manage input and output or keyboard shortcuts. When a context is finished
 * being created you should use code() to encrypt Transferable data.
 * 
 * @see Context#code
 * @author Adam Spiegel
 */

public class Context {
	public static String frontHead = "<~`" + "E:";
	public static String backHead = ":E" + "`~>";

	public Hexadecimal hex;
	
	private Lock lock;

	private BinaryEncoder[] binaryEncoders;
	private Map<String, Encryptor> encryptors;

	private String encryption;
	private char encoding;
	private char compression;
	private int flags1;
	private int flags2;
	
	//non header settings
	public boolean ImgtoText; //TODO getter/setter
	public boolean TexttoImg;
	public boolean tryDecode;

	public int imageEncodingBlockSize = 1;
	public int paletteBits = 3;
	
	private String imageFormat = "PNG";

	/**
	 * Creates a context with default values, to be used as a master context
	 * 
	 * @throws QCError
	 */
	public Context() throws QCError {
		encryption = "NO";
		encoding = 'X';
		compression = '0';
		flags1 = 0;
		flags2 = 0;
		
		ImgtoText = true;
		TexttoImg = false;
		tryDecode = true;

		hex = new Hexadecimal();

		encryptors = new HashMap<String, Encryptor>();
		binaryEncoders = new BinaryEncoder[64];
		
		lock = new ReentrantLock();
	}

	/**
	 * Creates a sub context exactly the same as another. Adding or modifying a
	 * encryptor or binary encoder from the sub context will affect the parent,
	 * and vice versa.
	 * 
	 * @param a
	 *            Context to copy from
	 */
	public Context(Context a) {
		super();
		encryption = a.encryption;
		encoding = a.encoding;
		compression = a.compression;
		flags1 = a.flags1;
		flags2 = a.flags2;
		ImgtoText = a.ImgtoText;
		TexttoImg = a.TexttoImg;
		tryDecode = a.tryDecode;

		hex = a.hex;

		encryptors = a.encryptors;
		binaryEncoders = a.binaryEncoders;
		
		lock = new ReentrantLock();
	}

	/**
	 * Creates a sub context for decrypting unknown data from the Quick Crypt
	 * header of that data The context will try to recreate the properties of
	 * the context originally used to encrypt It will only retain attributes
	 * important to decoding any thing else will be copied from the parent Note:
	 * Extra information can be passed after header and will be ignored
	 * 
	 * @param head
	 *            Header string containing properties of the new sub context.
	 * @param parent
	 *            Parent context to get information not found in the header.
	 * @throws QCError
	 */
	public Context(String head, Context parent) throws QCError {
		this(parent);

		loadFromInfoHeader(head);
	}
	
	/**
	 * Gets properties found in header
	 * @param head Header string containing properties
	 * @throws QCError
	 */
	private void loadFromInfoHeader(String head) throws QCError
	{
		setEncryption(head.substring(0, 2));
		setEncoding(head.charAt(2));
		setCompression(head.charAt(3));

		flags1 = Base64URL.fromChar(head.charAt(4)); //convert from base 64
		flags2 = Base64URL.fromChar(head.charAt(5));

		if (flags1 == -1 || flags2 == -1)
			throw new QCError("Invalid Flags");
	}

	/**
	 * Static method for generating a standard context ready for use
	 * 
	 * @return Context with default encoders and encryptors.
	 * @throws QCError
	 */
	public static Context standardContext() throws QCError {
		Context sc = new Context();

		sc.addBinaryEncoder(new Hexadecimal());
		sc.addBinaryEncoder(new Base64URL());
		sc.addBinaryEncoder(new CJK4096());

		sc.addEncryptor(new SharedSecrets());

		return sc;
	}

	/**
	 * Adds a binary encoder to be used by the context to encode or decode data
	 * The function will throw QCError if the base64Id() of the encoder is
	 * already in use.
	 * 
	 * @param encoder
	 *            to add; Must extend BinaryEncoder
	 * @throws QCError
	 * 
	 * @see BinaryEncoder
	 */
	public void addBinaryEncoder(BinaryEncoder encoder) throws QCError {

		int idx = Base64URL.fromChar(encoder.base64Id());

		if (idx == -1)
			throw new QCError("base64Id returned invalid base 64 character");

		binaryEncoders[idx] = encoder;
	}

	/**
	 * Sets the type of encoding to be used while encoding or decoding. If an
	 * encoding with that Base64Id has not been added, QCError will be thrown.
	 * Note: This selection will not affect the decoding when there is an
	 * available header with that information.
	 * 
	 * @param enc
	 *            The Base64Id of the encoding to use.
	 * @throws QCError
	 */
	public void setEncoding(char enc) throws QCError {

		if (enc == 'X')
			encoding = enc;
		else {

			int idx = Base64URL.fromChar(enc);

			if (idx != -1 && binaryEncoders[idx] != null)
				encoding = enc;
			else
				throw new QCError("Unknown Binary Encoding Type");
		}
	}

	/**
	 * Sets the compression to be used while encoding or decoding. If a
	 * compression with that Base64Id has not been added, QCError will be
	 * thrown. Note: Currently there are only two supported compression types,
	 * 'z' and '0', '0' is no compression and 'z' is default ZLIB compression
	 * Note: This selection will not affect the decoding when there is an
	 * available header with that information.
	 * 
	 * @param cmp
	 *            Base64Id representing Compression type
	 * @throws QCError
	 */
	public void setCompression(char cmp) throws QCError {

		if (cmp != 'z' && cmp != '0')
			throw new QCError("Unknown Compression type");

		compression = cmp;
	}

	/**
	 * Sets the type of encryption to be used while encoding or decoding. If an
	 * encryptor with that Base64Id has not been added or the Base64Id is
	 * invalid, QCError will be thrown. Note: This selection will not affect the
	 * decoding when there is an available header with that information. Note:
	 * The universal option is "NO", meaning no encryption, while "SS" is for
	 * shared secrets if that was added.
	 * 
	 * @param enc
	 *            2-char Base64Id representing the encryption type
	 * @throws QCError
	 */
	public void setEncryption(String enc) throws QCError {

		if (!enc.equals("NO") && !encryptors.containsKey(enc))
			throw new QCError("Unknown Encryption type");

		encryption = enc;
	}

	/** 
	 * Change image format if valid
	 * 
	 * @param f informal name of format (PNG, JPG, BMP)
	 * @return if change was success
	 */
	public boolean setImageFormat(String f)
	{
		if( ImageIO.getImageReadersByFormatName(f).hasNext() && ImageIO.getImageWritersByFormatName(f).hasNext() )
			imageFormat = f;
		
		else return false;
		return true;
	}
	
	/**
	 * Enables a given flag
	 * 
	 * @param f
	 *            flag(s) to be set
	 */
	public void setFlag1(int f) {
		if (f < 64)
			flags1 |= f;
	}

	/**
	 * Toggles a given off or on a flag in the first set
	 * 
	 * @param f
	 *            flag to be set
	 */
	public void toggleFlag1(int f) {
		if (f < 64)
			flags1 ^= f;
	}
	
	/**
	 * gets the true false value of a flag
	 * @param f flag to get
	 * @return if flag is off or on
	 */
	public boolean isFlag1(int f) {
		return (flags1 & f) != 0;
	}

	/**
	 * Enables a given flag
	 * 
	 * @param f
	 *            flag(s) to be set
	 */
	public void setFlag2(int f) {
		if (f < 64)
			flags2 |= f;
	}

	/**
	 * Toggles a given off or on a flag in the first set
	 * 
	 * @param f
	 *            flag to be set
	 */
	public void toggleFlag2(int f) {
		if (f < 64)
			flags2 ^= f;
	}
	
	/**
	 * gets the true false value of a flag
	 * @param f flag to get
	 * @return if flag is off or on
	 */
	public boolean isFlag2(int f) {
		return (flags1 & f) != 0;
	}

	/**
	 * Creates a standard Quick Crypt header that can be used to recreate the
	 * parts of this context relevant to decoding. Note: Should only output
	 * ascii
	 * 
	 * @return Header with information required to decode
	 */
	public String getInfoHeader() {
		return encryption + encoding + compression + Base64URL.toChar(flags1) + Base64URL.toChar(flags2);
	}

	/**
	 * Decodes Text(a String) and without any headers or footers into a byte
	 * array using this Context
	 * 
	 * @param in
	 *            encoded Text to decode
	 * @return decoded byte array in raw (un-encoded) form
	 * @throws QCError
	 */
	public byte[] decodeTextToRaw(String in) throws QCError {
		return decompress(decrypt(binaryDecode(in)));
	}

	/**
	 * Encodes Text(a String) into a byte array without any headers or footers
	 * using this Context
	 * 
	 * @param in
	 *            raw un-encoded un-formated bytes to encode as a String
	 * @return encoded String
	 * @throws QCError
	 */
	public String encodeRawToText(byte[] in) throws QCError {
		return binaryEncode(encrypt(compress(in)));
	}

	/**
	 * Encodes bytes to bytes using encryption and compression
	 * 
	 * @param in
	 *            raw un-encoded un-formated bytes to encode
	 * @return encoded bytes
	 * @throws QCError
	 */
	public byte[] encodeRawToRaw(byte[] in) throws QCError {
		return encrypt(compress(in));
	}

	/**
	 * Decodes raw bytes into it's original byte array using encryption and
	 * compression in this Context
	 * 
	 * @param in
	 *            encoded bytes to decode
	 * @return decoded byte array in raw (un-encoded) form
	 * @throws QCError
	 */
	public byte[] decodeRawToRaw(byte[] in) throws QCError {
		return decompress(decrypt(in));
	}

	/**
	 * Searches for the last encoded Quick Crypt message in the input text. If
	 * and when the message is found, it is decoded and returned.
	 * 
	 * @param in
	 *            String to search for a Quick Crypt message
	 * 
	 * @return null if a encoded Quick Crypt message was not found
	 * 
	 * @return A Transferable ImageSelection of the original image if the found
	 *         Quick Crypt message encoded an image
	 * 
	 * @return A Transferable StringSelection of the input message but with an
	 *         encoded Quick Crypt message replaced with it's un-encoded
	 *         original message
	 * 
	 * @see StringSelection
	 * @see ImageSelection
	 * 
	 * @throws QCError
	 */
	public Transferable decodeText(String in) throws QCError {

		//find last Quick Crypt header
		int beg = in.length();

		while ((beg = in.lastIndexOf(frontHead, beg - 1)) != -1)
			if (beg == 0 || in.charAt(beg - 1) < 0xD800 || in.charAt(beg - 1) >= 0xE000) { //watch out for surrogates

				int end = in.indexOf(backHead, beg + 10); //try to find corresponding header
															//because this header was the last header found the next footer must correspond with it

				if (end == -1) //ending footer not found
					throw new QCError("Could not find \"" + backHead + "\" to match \"" + frontHead + "\"");

				Context context = new Context(in.substring(beg + 5), this); //try to recreate context that the message was encrypted under

				if ((context.flags1 & 2) == 0) //if text was encoded, decode with context and return
					return new StringSelection(in.substring(0, beg) //things found before message
							+ context.bytesToString(context.decodeTextToRaw(in.substring(beg + 11, end))) ///decoded message
							+ (end + 5 >= in.length() ? "" : in.substring(end + 5))); //items found after message

				//if an image was encoded return image
				return new ImageSelection(ImageEncoder.BinToImg(context.decodeTextToRaw(in.substring(beg + 11, end))));
			}

		return null; // no encoded message found
	}

	/**
	 * Encodes byte array to a String using selected binary encoder. Note: Will
	 * probably never throw QCError unless another internal function has made a
	 * mistake.
	 * 
	 * @param in
	 *            byte array to encode
	 * @return Encoded string
	 * @throws QCError
	 */
	public String binaryEncode(byte[] in) throws QCError {

		if (encoding == 'X')
			return hex.to(in);

		int idx = Base64URL.fromChar(encoding);

		if (idx != -1 && binaryEncoders[idx] != null)
			return binaryEncoders[idx].to(in);

		throw new QCError("Unknown binary encoder selected");
	}

	/**
	 * Encrypts a byte array to another byte array using selected encrpytor.
	 * 
	 * @param in
	 *            bytes to encrypt
	 * @return Encrypted bytes
	 * @throws QCError
	 */
	public byte[] encrypt(byte[] in) throws QCError {

		if (in == null || in.length == 0)
			throw new QCError("Encryptor found empty input");

		if (encryption.equals("NO"))
			return in;

		return encryptors.get(encryption).encrypt(in);
	}

	/**
	 * Compress a byte array to another byte array using selected compressor.
	 * 
	 * @param in
	 *            bytes to compress
	 * @return Compressed bytes
	 * @throws QCError
	 */
	public byte[] compress(byte[] in) throws QCError {

		if (in == null || in.length == 0)
			throw new QCError("Compressor found empty input");

		if (compression == 'z')
			return Compression.deflate(in);

		return in;
	}

	/**
	 * Decodes a String that has been encoded using Context.binaryEncode() Note:
	 * To decode properly, the selected binary encoder must be the same as when
	 * encoded
	 * 
	 * @param in
	 *            String to decode
	 * @return original decoded bytes
	 * @throws QCError
	 */
	public byte[] binaryDecode(String in) throws QCError {

		if (encoding == 'X')
			return hex.from(in);

		int idx = Base64URL.fromChar(encoding);

		if (idx != -1 && binaryEncoders[idx] != null)
			return binaryEncoders[idx].from(in);

		throw new QCError("Unknown binary encoder selected");
	}

	/**
	 * Converts an array of unicode bytes to String Note: UTF_16LE will be
	 * assumed unless the flag for UTF_8 is active
	 * 
	 * @param in
	 *            unicode bytes to convert
	 * @return Java String with those characters
	 */
	public String bytesToString(byte[] in) {

		Charset charset = StandardCharsets.UTF_16LE;

		if ((flags1 & 1) != 0)
			charset = StandardCharsets.UTF_8;

		return new String(in, charset);
	}

	/**
	 * Converts a String to an array of unicode bytes Note: UTF_16LE will be
	 * assumed unless the flag for UTF_8 is active
	 * 
	 * @param in
	 *            Java String
	 * @return unicode bytes
	 */
	public byte[] stringToBytes(String in) {

		Charset charset = StandardCharsets.UTF_16LE;

		if ((flags1 & 1) != 0)
			charset = StandardCharsets.UTF_8;

		return in.getBytes(charset);
	}

	/**
	 * Decrypts bytes that have been encrypted using Context.encrypt() Note: To
	 * decrypt properly, the selected encrypted must be the same as when encoded
	 * 
	 * @param in
	 *            bytes to decrypt
	 * @return original decrypted bytes
	 * @throws QCError
	 */
	public byte[] decrypt(byte[] in) throws QCError {
		if (in == null || in.length == 0)
			throw new QCError("Decryptor found empty input");

		if (encryption.equals("NO"))
			return in;
		return encryptors.get(encryption).decrypt(in);
	}

	/**
	 * Decompresses bytes that have been compressed using Context.compress()
	 * Note: To decode properly, the selected compression type must be the same
	 * as when encoded
	 * 
	 * @param in
	 *            bytes to decompress
	 * @return original decompressed bytes
	 * @throws QCError
	 */
	public byte[] decompress(byte[] in) throws QCError {
		if (compression == 'z')
			try {
				return Compression.inflate(in);
			} catch (DataFormatException e) {
				throw new QCError("decompression failed: " + e.getMessage());
			}
		return in;
	}

	/**
	 * Encode a String to another String with header and footer information
	 * 
	 * @param in
	 *            String to encode
	 * @return encoded string with header and footer
	 * @throws QCError
	 */
	public String encodeTextToText(String in) throws QCError {

		if ((flags1 & 2) != 0) //tell context we are encoding text
			flags1 ^= 2;

		//encode to text and add header and footer with information of the current context
		return frontHead + getInfoHeader() + encodeRawToText(stringToBytes(in)) + backHead;
	}

	/**
	 * Encode an Image to a String with header and footer information Note:
	 * Encoding (ex. PNG, BMP, JPG) format does not matter an all accepted
	 * formats can be decoded without context
	 * 
	 * @param in
	 *            Image to encode
	 * @return encoded string with header and footer
	 * @throws QCError
	 */
	public String encodeImgToText(Image in) throws QCError {

		flags1 |= 2; //tell context we are encoding an image

		//convert image to bytes than encode like anything else
		return frontHead + getInfoHeader() + encodeRawToText(ImageEncoder.ImgToBin(in, imageFormat)) + backHead;
	}

	/**
	 * Encode a String to an Image using ImageEncoder
	 * 
	 * @param in
	 *            String to encode
	 * @return output Image representing encoded input Image
	 * @throws QCError
	 * 
	 * @see ImageEncoder
	 */
	public Image encodeTextToImg(String in) throws QCError {

		if ((flags1 & 2) != 0) //tell context we are encoding text
			flags1 ^= 2;

		byte[] head = getInfoHeader().getBytes(StandardCharsets.UTF_8); //convert ascii header to UTF_8 bytes for export
		byte[] body = encodeRawToRaw(stringToBytes(in)); //encode input to body, this is the main encoding

		//combine head and body into toenc
		byte[] toenc = new byte[head.length + body.length];
		System.arraycopy(head, 0, toenc, 0, head.length);
		System.arraycopy(body, 0, toenc, head.length, body.length);

		return ImageEncoder.DataEncode(toenc, imageEncodingBlockSize, ImageEncoder.spacedPallate(1<<paletteBits)); //encode toenc as Image
	}

	/**
	 * Encode an Image to another Image using ImageEncoder
	 * 
	 * @param in
	 *            Image to encode
	 * @return output Image representing encoded input Image
	 * @throws QCError
	 */
	public Image encodeImgToImg(Image in) throws QCError {

		flags1 |= 2; //tell context we are encoding an image

		byte[] head = getInfoHeader().getBytes(StandardCharsets.UTF_8); //convert ascii header to UTF_8 bytes for export
		byte[] body = encodeRawToRaw(ImageEncoder.ImgToBin(in, imageFormat)); //convert input to bytes than encode as Raw and store in body

		//combine head and body into toenc
		byte[] toenc = new byte[head.length + body.length];
		System.arraycopy(head, 0, toenc, 0, head.length);
		System.arraycopy(body, 0, toenc, head.length, body.length);

		return ImageEncoder.DataEncode(toenc, imageEncodingBlockSize, ImageEncoder.spacedPallate(1<<paletteBits)); //encode toenc as Image
	}

	/**
	 * Uses ImageEncoder to see if an Image has encoded information. If it does,
	 * this function will decode the information to it's original Transferable
	 * format
	 * 
	 * @param in
	 *            Image to try to decode
	 * @return null if a no encoded information was found in Image
	 * @return A Transferable ImageSelection of the original image if input
	 *         image encoded another image
	 * @return A Transferable StringSelection of the original message if input
	 *         image encoded a string
	 * @throws QCError
	 * 
	 * @see ImageEncoder
	 * @see StringSelection
	 * @see ImageSelection
	 */
	public Transferable decodeImg(Image in) throws QCError {

		//try to decode the Image to bytes
		byte[] data = ImageEncoder.DataDecode(in);
		if (data == null || data.length < 7)
			return null; //no data found

		//try to get the header as a string
		byte[] bhead = new byte[6];
		System.arraycopy(data, 0, bhead, 0, bhead.length);
		String head = new String(bhead, StandardCharsets.UTF_8);

		//create new context with header string
		Context context = new Context(head, this);

		int realHeadSize = context.getInfoHeader().length();//get real size of that header

		//get body, stuff after header
		byte[] body = new byte[data.length - realHeadSize];
		System.arraycopy(data, realHeadSize, body, 0, body.length);

		//if a string was encoded, decode and convert to a string
		if ((context.flags1 & 2) == 0)
			return new StringSelection(context.bytesToString(context.decodeRawToRaw(body)));

		//if an image was encoded, decode and convert to an image
		return new ImageSelection(ImageEncoder.BinToImg(context.decodeRawToRaw(body)));
	}

	/**
	 * Processes a Transferable. Will try to decode a the Transferable and
	 * return result. If that fails, meaning none of the data is encoded, encode
	 * the data. Note: Will throw a QCError if the Transferable has no useful
	 * data Note: It will only process one flavor at a time. If there are
	 * multiple flavors, all other will be discarded. If the flavor is a String
	 * and there are multiple encoded messages, one will be decoded and the
	 * others will be un-touched
	 * 
	 * @TODO Process all encoded messages or images found, not just one.
	 * 
	 * @param in
	 *            Transferable to encode or decode
	 * @return A Transferable object containing either a string or an image with
	 *         encoded data
	 * @throws QCError
	 * 
	 * @see Transferable
	 * @see StringSelection
	 * @see ImageSelection
	 */
	public Transferable code(Transferable in) throws QCError {

		try {

			//string flavor
			if (in.isDataFlavorSupported(DataFlavor.stringFlavor)) {

				//get input String
				String data = (String) in.getTransferData(DataFlavor.stringFlavor);

				if(tryDecode)
				{
					//try to decode the string
					Transferable dec = decodeText(data);
					if (dec != null)
						return dec;
				}

				//default, encode the string as text
				if (!TexttoImg)
					return new StringSelection(encodeTextToText(data));

				//if turned on, encode the string as an image
				return new ImageSelection(encodeTextToImg(data));

				//image flavor
			} else if (in.isDataFlavorSupported(DataFlavor.imageFlavor)) {

				//get input Image
				Image data = (Image) in.getTransferData(DataFlavor.imageFlavor);
				
				if(tryDecode)
				{
					//try to decode the image
					Transferable dec = decodeImg(data);
					if (dec != null)
						return dec;
				}

				//default, encode the image as an image
				if (!ImgtoText)
					return new ImageSelection(encodeImgToImg(data));

				//if turned on, encode the string as an image
				return new StringSelection(encodeImgToText(data));
			}

			//no flavor
			throw new QCError("No recognised clipboard formats found");

			// catch / contain exceptions
		} catch (UnsupportedFlavorException | IOException e) {
			throw new QCError("Error reading Transferable Input");
		}
	}

	/**
	 * Adds an Encryptor that can be used to encrypt or decrypt data. The
	 * function will throw QCError if the base64Id() of the encoder is already
	 * in use or invalid.
	 * 
	 * @param enc
	 *            Encryptor to add
	 * @throws QCError
	 */
	public void addEncryptor(Encryptor enc) throws QCError {
		String id = enc.base64Id();

		if (id.length() != 2)
			throw new QCError("Currently an encryptor's id's length must equal 2");

		for (int x = 0; x < id.length(); x++)
			if (Base64URL.fromChar(id.charAt(x)) == -1)
				throw new QCError("Invalid Base 64 Charater found in encryptor's id");

		encryptors.put(id, enc);
	}

	/**
	 * 
	 * @param base64Id
	 *            of desired encryptor
	 * @return desired encryptor
	 * @return null if an encryptor with that base64Id was not found
	 */
	public Encryptor getEncryptor(String label) {
		return encryptors.get(label);
	}
	
	/**
	 * Get a registered binary encoder by base64id
	 * 
	 * @param id base64id of encoder
	 * @return encoder
	 * @throws QCError if id doesn't match any binary encoders
	 */
	public BinaryEncoder getBinaryEncoder(char id) throws QCError {
		int v = Base64URL.fromChar(id);
		if(v==-1||binaryEncoders[v]==null)throw new QCError("Unknown encoder Base 64 Id");
		return binaryEncoders[v];
	}
	
	/**
	 * Get map of binary encoders
	 * @return map of binary encoders
	 */
	public Map<Character,BinaryEncoder> getBinaryEncoderMap()
	{
		Map<Character,BinaryEncoder> out = new HashMap<Character,BinaryEncoder>();
		for(int x=0;x<64;x++)
		{
			if(binaryEncoders[x]!=null)out.put(Base64URL.toChar(x), binaryEncoders[x]);
		}
		return out;
	}
	
	/**
	 * Get map of encryptors
	 * @return map of encryptors
	 */
	public Map<String, Encryptor> getEncryptorMap()
	{
		return encryptors;
	}
	
	public char getEncoding()
	{
		return encoding;
	}
	
	public String getEncryptior()
	{
		return encryption;
	}
	
	public String getImageFormat()
	{
		return imageFormat;
	}
	
	/**
	 * Locks built in lock
	 */
	public void lock()
	{
		lock.lock();
	}
	
	/**
	 * Unlocks built in lock
	 */
	public void unlock()
	{
		lock.unlock();
	}
	
	/**
	 * Save information to PrintStream
	 * @param ps stream to save to
	 */
	public void save(PrintStream ps)
	{
		ps.println("Quick Crypt Context");
		ps.println("Info Header="+getInfoHeader());
		ps.println("Encode Images as Text="+ImgtoText);
		ps.println("Encode Text as Images="+TexttoImg);
		ps.println("Attempt to Decode="+tryDecode);
		ps.println("Image Block Width="+imageEncodingBlockSize);
		ps.println("Bits per Image Block="+paletteBits);
		ps.println("Encryptors");
		for(Encryptor e : encryptors.values())
		{
			ps.println(e.base64Id());
			e.save(ps);
		}
	}
	
	public void load(Scanner in) throws QCError
	{
		if(!in.hasNextLine()||!in.nextLine().equals("Quick Crypt Context"));
		
		//get standard info
		while(in.hasNextLine())
		{
			String next = in.nextLine();
			if(next.equals("Encryptors"))break;
			
			int f = next.indexOf("=");
			if(f==-1)continue;
			
			String setting = next.substring(0, f);
			next = next.substring(f+1);
			
			switch(setting)
			{
				case "Info Header":
					loadFromInfoHeader(next);
					break;
					
				case "Encode Images as Text":
					ImgtoText = Boolean.parseBoolean(next);
					break;

				case "Encode Text as Images":
					TexttoImg = Boolean.parseBoolean(next);
					break;
					
				case "Attempt to Decode":
					tryDecode = Boolean.parseBoolean(next);
					break;
					
				case "Image Block Width":
					imageEncodingBlockSize = Integer.parseInt(next);
					break;
					
				case "Bits per Image Block":
					paletteBits = Integer.parseInt(next);
					break;
			}
		}
		
		while(in.hasNextLine())
		{
			encryptors.get(in.nextLine()).load(in);
		}
		
	}

	public char getCompression() {
		return compression;
	}
}