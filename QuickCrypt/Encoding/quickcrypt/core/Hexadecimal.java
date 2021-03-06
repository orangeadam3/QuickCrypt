package quickcrypt.core;

/**
 * Binary Encoder that converts to and from hexadecimal
 * 
 * @author Adam Spiegel
 *
 */

public class Hexadecimal extends BinaryEncoder {
	public String to(byte[] in)
	{
		StringBuilder out = new StringBuilder();
		for(byte c : in) //for add bytes
		{
			//4 high bits converted and added to string
			char tmp = (char) (byteToUnsignedInt(c)/16);
			if(tmp<10)tmp += '0';
			else tmp += 'A'-10;
			out.append(tmp);
			
			//4 low bits converted and added to string
			tmp = (char) (byteToUnsignedInt(c)%16);
			if(tmp<10)tmp += '0';
			else tmp += 'A'-10;
			out.append(tmp);
		}
		
		return out.toString();
	}
	
	public byte[] from(String in)
	{
		byte[] out = new byte[in.length()/2]; //output with hypothetical maximum
		byte nextByte = 0;
		int bitsPushed = 0;
		for(int x=0;x<in.length();x++) ///Every input char
		{
			//build up next byte to add
			nextByte *= 16;
			if('0'<=in.charAt(x) && in.charAt(x)<='9') nextByte += in.charAt(x)-'0';
			else if('A'<=in.charAt(x) && in.charAt(x)<='F') nextByte += in.charAt(x)-'A'+10;
			else if('a'<=in.charAt(x) && in.charAt(x)<='f') nextByte += in.charAt(x)-'a'+10;
			else continue; //this character contained a non hex value,ignore
			
			bitsPushed += 4;
			
			if(bitsPushed%8==0) //add byte when two new hex characters have been read
			{
				out[bitsPushed/8-1] = nextByte;
				nextByte = 0;
			}
		}
		
		//slim down output to actual size
		byte slim[] = new byte[bitsPushed/8];
		for(int x=0;x<bitsPushed/8;x++)
		{
			slim[x] = out[x];
		}
		
		return slim;
	}

	public String fullName() {
		return "Hexadecimal (Base 16)";
	}
	
	public String shortName() {
		return "Hex 16";
	}

	public char base64Id() {
		return 'X';
	}
	
	public String description()
	{
		return "Base 16 or Hexadecimal uses a character set made up of digits 0-9 as well as A-F "
                +"Without encryption or compression, This Encoder makes text that is approximately 4 times the input text + 16 characters for the header and footer. "
                +"Hexadecimal is useful because it only outputs ASCII code so can be used on normal UNICODE accepting environments but, also those only accept ASCII. "
                +"Hexadecimal could even, theoretically, be used in an environment that only takes uppercase letters or only takes lowercase letters. "
                +"(The header and footer would have to be stored elsewhere)."
                +"\n\nDisclaimer: Because English letters are used, text may contain a recognizable plain message. This is not the intent of the program or the programmer.";
	}
}
