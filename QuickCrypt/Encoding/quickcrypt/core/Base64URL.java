package quickcrypt.core;

/**
 * Base-64 Encoder consistent with most URL Base64 implementations
 * Also contains static helper methods for single base64 characters
 * @author Adam Spiegel
 *
 */
public class Base64URL extends BinaryEncoder {
	
	/**
	 * converts bytes to a base-64 string
	 * @param in Bytes to convert
	 */
	public String to(byte[] in)
	{
		StringBuilder out = new StringBuilder();
		
		//cycle every 6 bits and add corresponding character
		for(int x=0;x<in.length*8;x+=6)
			out.append(toChar((int)getBits(in, x, x+6<=in.length*8?6:in.length*8-x)));
		
		return out.toString();
	}
	
	public byte[] from(String in)
	{
		int x; //remove trailing invalid chars
		for(x=in.length()-1;x>=0&&(fromChar(in.charAt(x))==-1||(x>0&&in.charAt(x-1)>0xD7FF&&in.charAt(x-1)<0xE000));x--);
		if(x!=in.length()-1) in = in.substring(0, x+1);
		
		byte[] out = new byte[(in.length()*6)/8]; //get expected size of output based on input, could be shorter if invalid characters are found
		out[(in.length()*6)/8 - 1] = 0;
		int strpos = 0;
		
		for(x=0;x<out.length*8;x+=6) //parse output bytes until theoretical maximum
		{
			if(strpos>=in.length()) ///reached end of string before end of array this means characters were skipped and the array must be resized
			{
				byte[] resized = new byte[x/8];
				
				System.arraycopy(out, 0, resized, 0, resized.length);
				return resized;
			}
			int debase = fromChar(in.charAt(strpos)); //get 0-64 value of the bit
			
			strpos = in.offsetByCodePoints(strpos, 1); //next codepoint
			
			if(debase!=-1)setBits(out,x,strpos<in.length()?6:8-x%8,debase); //set applicable bits in output to expected input if input is base64 char
			else x-=6;
		}
		return out;
	}
	
	/**
	 * Converts number to base64 char
	 * @param in integer 0-63 to be encoded as a char
	 * @return base64 char representing in or a char if in was greater than 63
	 */
	public static char toChar(int in)
	{
		if(in<10)return (char) ('0'+in);
		if(in<36)return (char) ('A'-10+in);
		if(in<62)return (char) ('a'-36+in);
		if(in<63)return '-';
		if(in<64)return '_';
		return 0;
	}
	
	/**
	 * Converts base64 char to a numerical value
	 * @param in base64 char to be decoded
	 * @return integer 0-63 represented by in or -1 if in was not a base64 char
	 */
	public static int fromChar(char in)
	{
		if('0'<=in&&in<='9')return in-'0';
		if('A'<=in&&in<='Z')return in-'A'+10;
		if('a'<=in&&in<='z')return in-'a'+36;
		if(in=='-')return 62;
		if(in=='_')return 63;
		return -1;
	}

	public String fullName() {
		return "URL safe standard base-64";
	}
	
	@Override
	public String shortName() {
		return "URL 64";
	}

	public char base64Id() {
		return 'U';
	}
	
	public String description()
	{
		return "Base 64 uses a character set made up of all lowercase and capital English letters"
                +" as well as the '-' (dash or minus) and '_' (underscore), Totaling 64 characters. "
                +"Without encryption or compression, This Encoder makes text that is approximately 2.667 times the input text + 16 characters for the header and footer. "
                +"Base 64 is useful because it only outputs ASCII code and can be used on normal UNICODE accepting environments but, also those only accept ASCII. "
                +"Because of this, Base 64 along with the No Encryption option could be used to store UNICODE data in non-UNICODE Environments"
                +"\n\nDisclaimer: Because English letters are used, text may contain a recognizable plain message. This is not the intent of the program or the programmer.";
	}
}
