package quickcrypt.core;

/**
 * Base parent class for any self-respecting binary encoder
 * Also Has a number of static helper methods for aspiring binary encoders
 * 
 * All binary encoders should accept any byte data to encode but not necessarily all strings to decode.
 * However, it is recommended that these skip invalid values instead of throwing errors
 * 
 * @author Adam Spiegel
 *
 */

public abstract class BinaryEncoder {
	public abstract String to(byte[] in); //convert bytes to the encoding
	public abstract byte[] from(String in); //convert encoding to bytes
	
	public abstract String fullName(); //returns full name of the encoder
	public abstract char base64Id(); //returns a single unique char 0-9, A-Z, a-z, -, or _, that can be used to identify this encoder
	
	/**
	 * converts a signed byte to an unsigned value
	 * @param b byte to convert
	 * @return the unsigned value of b
	 */
	public static int byteToUnsignedInt(byte b) {
	    return 0x00 << 24 | b & 0xff;
	  }
	
	/**
	 * get a section of bits from an array of bytes
	 * (really complicated source, could have glitches)
	 * 
	 * @param bytes values to retrieve from (BigEndian)
	 * @param bit location of the first bit to retrieve (acts like an array of bits, byte*8 + bitofbyte) 
	 * 			(most significant byte is bitofbyte 0, least significant is bitofbyte 8)
	 * @param num number of bits to get (should not be more than 63)
	 * @return positive number less than 2^num that represents the retrieved bits
	 */
	public static long getBits(byte[] bytes, long bit,int num)
	{
		if(num==0)return 0; //must be less than
		
		int startByte = (int) (bit/8); //byte that holds first bit
		int bitInByte = (int) (bit%8); //location of first bit in said startbyte
		
		if(bitInByte+num<=8)
			return (byteToUnsignedInt(bytes[startByte])>>(8-(bitInByte+num)))%(1<<num);
		
		int numStartBytebits = 8-bitInByte;
		if(num<numStartBytebits)numStartBytebits = num;
		
		long out = byteToUnsignedInt(bytes[startByte]) & ((1<<numStartBytebits)-1); ///grab trailing bits from first byte
		
		for(int x=startByte+1; x < (bit+num)/8; x++) ///grab intermittent full bytes
		{
			out <<= 8;
			out |= byteToUnsignedInt(bytes[x]);
		}

		if((bit+num)%8 != 0)
		{
			out <<= (bit+num)%8; //grab leading bits from last byte
			out |= byteToUnsignedInt(bytes[(int) ((bit+num)/8)]) >> (8 - (bit+num)%8);
		}
		
		return out;
	}
	
	/**
	 * Root of getBits, sets specific bits in a byte based on their bigEndian integer values
	 * (really complicated source, could have glitches)
	 * 
	 * @param bytes bytes to modify
	 * @param bit location of the first bit to set (acts like an array of bits, byte*8 + bitofbyte) 
	 * 			(most significant byte is bitofbyte 0, least significant is bitofbyte 8)
	 * @param num number of bits to modify (should not be more than 63)
	 * @param in positive number less than 2^num that represents values of bits to set
	 */
	public static void setBits(byte[] bytes, long bit,int num, long in)
	{
		if(num==0)return;
		
		int startByte = (int) (bit/8); //location of byte containing the first bit
		int bitInByte = (int) (bit%8); //location of first bit in said startbyte
		
		if(bitInByte+num<=8) //all bits contained within one byte
		{
			int sb = byteToUnsignedInt(bytes[startByte]);
			//acts like a substring, sb.subbits(0,bitInByte) + in + sb.subbits(bitInByte+num)
			bytes[startByte] = (byte) (((sb>>(8-bitInByte))<<(8-bitInByte)) + ((in%(1<<num))<<(8-bitInByte-num)) + sb%(1<<(8-bitInByte-num)));
			return;
		}
		
		int numStartBytebits = 8-bitInByte;
		if(num<numStartBytebits)numStartBytebits = num;
		
		//first byte
		bytes[startByte] = (byte) ((((byteToUnsignedInt(bytes[startByte])>>numStartBytebits)<<numStartBytebits) 
				+ (in>>(num-numStartBytebits)))%256);
		
		for(int x=startByte+1; x < (bit+num)/8; x++) ///grab intermittent full bytes
			bytes[x] = (byte) ((in>>(num-numStartBytebits-(x-startByte)*8))%256);

		//last byte if not full
		if((bit+num)%8 != 0)
		{
			bytes[(int) ((bit+num)/8)] = 
					(byte) ((in%(1<<((bit+num)%8)) << (8 - (bit+num)%8)) 
							+ (byteToUnsignedInt(bytes[(int) ((bit+num)/8)])%(1<<(8 - (bit+num)%8))));
		}
	}
}
