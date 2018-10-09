package quickcrypt.core;

import java.io.ByteArrayOutputStream;

/**
 * Binary Encoder that encodes to unicode using 256 emotion(emoji) characters
 * 
 * @author Adam Spiegel
 */
public class Emoji256 extends BinaryEncoder{
	
	/**
	 * Convert from raw bytes to encoded string
	 * @param in bytes to convert
	 * @return String with output data
	 */
	public String to(byte[] in)
	{
		///uses two codepoint sets. One byte per character for 256 characters.
	    ///set #1 (0-79) = 0x1F600-0x1F64F
	    ///set #2 (80-255) = 0x1F400 - 0x1F4AF
		
		StringBuilder out = new StringBuilder();
		
		for(int x=0;x<in.length;x++)
		{
			int c = byteToUnsignedInt(in[x]); //byte to encode
			out.appendCodePoint(c + (c<80? 0x1F600 : 0x1F3B0)); //write point from specified set
		}
		
		return out.toString(); //return correct output
	}

	/**
	 * Convert from encoded String to raw bytes, ignores invalid characters
	 * @param in String to decode
	 * @return original bytes that were decode
	 */
	public byte[] from(String in) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		for(int x=0;x<in.length();x = in.offsetByCodePoints(x, 1))
		{
			int code = in.codePointAt(x);
			
			if(!(code<0x1F400||code>0x1F64F)) //around valid range (points between them will be filtered out later)
			{
				if(code>=0x1F600)out.write(code - 0x1F600); //write from set 1
				else if(code<=0x1F4AF)out.write(code - 0x1F3B0); //write from set 2
				//otherwise it is in middle section
			}
		}
		
		return out.toByteArray(); //return correct output
	}

	@Override
	public String fullName() {
		return "Base 256 with Emoticons";
	}

	@Override
	public String shortName() {
		return "Emoji 256";
	}

	@Override
	public char base64Id() {
		return 'E';
	}

	@Override
	public String description() {
		return "This Encoder uses 0x1F400-0x1F4AF and 0x1F600-0x1F64F UNICODE characters from the \"Miscellaneous Symbols and Pictographs\" "+
                "and \"Emoticons\" blocks of the \"Supplementary Multilingual Plane\" (Plane 1) as its character set. "+
                "Without encryption or compression, This encoder makes text that is approximately 2 times the input text + 16 characters for the header and footer. "+
                "(If measured in UTF-16 bytes, not characters, it is actually 4 times the size). This encoder only uses characters outside the BMP so it will only work in "+
                "environments that allow 4-byte UTF-8 or 4-byte UTF-16. This encoder will not work in ASCII because ASCII lacks support for most emoticons."+
                "\n\nDisclaimer: Because symbols are used, text may be interpreted as a message. This is not the intent of the program or the programmer. "+
                "Furthermore, only default emoticons without modifiers were used. Because of this, gender and skin color among other things will be the UNICODE default version. "+
                "This is not meant to be a statement, merely a way to improve space efficiency and simplicity.";
	}
}
