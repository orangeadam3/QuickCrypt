package quickcrypt.core;
import java.util.ArrayList;
import java.util.zip.*;

/**
 * Wrapper for inflating and deflating byte arrays of any size with java Z-LIB
 * compression
 * 
 * @author Adam Spiegel
 *
 */

public class Compression {

	final static int ZLIBCHUNK = 16384; //size of temporary Z-LIB buffers

	/**
	 * Deflate or compress bytes with java Z-LIB Should never fail, except for
	 * maybe memory issues or bugs
	 * 
	 * @param input
	 *            bytes to compress
	 * @return compressed bytes
	 */
	public static byte[] deflate(byte[] input) {
		
		byte[] buf = new byte[ZLIBCHUNK]; //temporary storage buffer
		ArrayList<Byte> output = new ArrayList<Byte>(); //resizeable output holder

		//setup deflater with this input
		Deflater stream = new Deflater();
		stream.setInput(input);
		stream.finish();

		//fill buffer and add buffer to output
		int compressedDataLength = 0;
		do {
			compressedDataLength = stream.deflate(buf, 0, ZLIBCHUNK);

			for (int x = 0; x < compressedDataLength; x++)
				output.add(buf[x]);

		} while (compressedDataLength == ZLIBCHUNK); //buffer was completely filled keep going

		stream.end(); //end stream properly

		//convert output into byte array for return
		byte[] bytes = new byte[output.size()];
		for (int x = 0; x < output.size(); x++)
			bytes[x] = output.get(x).byteValue();

		return bytes;
	}

	/**
	 * Inflate or decompress compressed bytes
	 * 
	 * @param input
	 *            compressed bytes
	 * @return uncompressed output bytes
	 * @throws DataFormatException
	 *             When imput was not properly compressed or was corrupted.
	 */
	public static byte[] inflate(byte[] input) throws DataFormatException {

		byte[] buf = new byte[ZLIBCHUNK]; //temporary storage buffer
		ArrayList<Byte> output = new ArrayList<Byte>(); //resizeable output holder

		//setup inflater with this input
		Inflater stream = new Inflater();
		stream.setInput(input);

		//fill buffer and add buffer to output
		int decompressedDataLength = 0;
		do {
			decompressedDataLength = stream.inflate(buf, 0, ZLIBCHUNK);

			for (int x = 0; x < decompressedDataLength; x++)
				output.add(buf[x]);

		} while (decompressedDataLength != 0); //buffer was completely filled keep going

		stream.end(); //end stream properly

		//convert output into byte array for return
		byte[] bytes = new byte[output.size()];
		for (int x = 0; x < output.size(); x++)
			bytes[x] = output.get(x).byteValue();
		return bytes;
	}
}
