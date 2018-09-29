package quickcrypt.core;

/**
 * Base-4096 Binary encoder using many Chinese, Japanese, and Korean symbols on the Basic Multilingual Plane
 * 
 * @author Adam Spiegel
 */

public class CJK4096 extends BinaryEncoder {

	/**
	 * Convert from raw bytes to encoded string
	 * @param in bytes to convert
	 * @return String with output data
	 */
	@Override
	public String to(byte[] in) {
		StringBuilder out = new StringBuilder();
		for(int x=0;x<in.length;x+=3)
		{
			if(x+1>=in.length)out.append((char)(getBits(in,x*8,8)*16 + 0x3400)); ///end case 1: in.length%3 == 1: encode 1 byte as 1 char
			else
			{
				out.append((char)(getBits(in,x*8,12) + 0x3400));
				
				///end case 2: in.length%3 == 2: encode 1.5 bytes -> 1 char and remaining 0.5 byte -> 1 char with special character set
				if(x+2>=in.length)out.append((char)(getBits(in,x*8+12,4) + 0x4400));
				else out.append((char)(getBits(in,x*8+12,12) + 0x3400)); ///all other cases: 3 byte -> 2 char
			}
		}
		return out.toString();
	}

	/**
	 * Convert from encoded String to raw bytes, ignores invalid characters
	 * @param in String to decode
	 * @return original bytes that were decode
	 */
	@Override
	public byte[] from(String in) {
		
		int x; //remove trailing invalid chars
		for(x=in.length()-1;x>=0&&(in.charAt(x)<0x3400||in.charAt(x)>0x440F||(x>0&&in.charAt(x-1)>0xD7FF&&in.charAt(x-1)<0xE000));x--);
		if(x!=in.length()-1) in = in.substring(0, x+1);
		
		byte[] out = new byte[(in.length()/2)*3 + 3]; //output with hypothetical maximum size
		
		char c1 = 0,c2 = 0;
		
		int bytepos = 0;
		for(x=0;x<in.length();x=in.offsetByCodePoints(x, 1))
		{
			///skip invalid or wrongly placed characters
			if(in.charAt(x)<0x3400||in.charAt(x)>0x440F)continue;
			if(x<0&&in.charAt(x-1)>0xD7FF&&in.charAt(x-1)<0xE000)continue;
			if(in.charAt(x)>=0x4400&&(c1==0||x+1<in.length()))continue;
			
			if(c1==0) ///get first char
			{
				c1 = in.charAt(x);
				
				if(x+1>=in.length())//end case 1, 1st char -> 1 byte
					out[bytepos++] = (byte) ((c1-0x3400)/16);
			}
			else //get second char
			{
				c2 = in.charAt(x);
				
				setBits(out,(bytepos++)*8,12,c1-0x3400); //case 2 + all other cases, 1st char -> 1.5 bytes
				
				if(c2>=0x4400) //end case 2, 2nd char -> 0.5 bytes with special characters
					setBits(out,(bytepos++)*8+4,4,c2-0x4400);
				else
				{  //all other cases, 2nd char -> 1.5 bytes
					setBits(out,(bytepos++)*8+4,12,c2-0x3400);
					bytepos++;
					c1 = c2 = 0;
				}
			}
		}
		
		if(bytepos==out.length)return out; //right size already
		
		byte[] slim = new byte[bytepos]; //there were invalid chars trim off unused ouput bytes
		System.arraycopy(out, 0, slim, 0, slim.length);
		
		return slim;
	}

	@Override
	public String fullName() {
		return "Chinese/Jappenese/Korean Base 4096 Unicode BMP";
	}
	
	@Override
	public String shortName() {
		return "CJK 4096";
	}
	
	@Override
	public char base64Id() {
		return 'C';
	}
	
	public String description()
	{
		return "This Encoder uses the 0x3400-0x440F UNICODE characters from the \"CJK Unified Ideographs Extension A\" "
                +"section of the BMP as its character set. (There are 4112 characters not 4096, because of some technicalities in calculating the length while decoding). "
                +"Without encryption or compression, This encoder makes text that is approximately 1.333 times the input text + 16 characters for the header and footer. "
                +"This only uses characters from the BMP so it will work in environments that only allow BMP UNICODE as well as any other type of UNICODE environment. "
                +"An ASCII only environment will not allow output from this encoder because ASCII does not support any of it's Chinese/Japanese/Korean symbols."
                +"\n\nDisclaimer: Because Chinese/Japanese/Korean symbols are used, text may contain a recognizable plain message. "
				+"This is not the intent of the program or the programmer.";
	}

}
