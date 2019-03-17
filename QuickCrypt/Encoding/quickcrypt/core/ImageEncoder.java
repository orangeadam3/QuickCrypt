package quickcrypt.core;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Colection of various static functions needed to process Binary data and Images
 * 
 * @author Adam Spiegel
 */

public class ImageEncoder {

	/**
	 * Convert an image to a binary file byte array
	 * 
	 * @param img
	 *            Image to convert
	 * @param format
	 *            Format of output (ex. "PNG", "JPG", "BMP")
	 * @return bytes identical to a file of the image
	 * @throws QCError
	 */
	public static byte[] ImgToBin(Image img, String format) throws QCError {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			ImageIO.write(toBufferedImage(img), format, os);
		} catch (IOException e) {
			throw new QCError("Failed to get bytes from image");
		}
		return os.toByteArray();
	}

	/**
	 * Convert a image stored like a binary file to a Image type Note: Will
	 * decode any supported type without context
	 * 
	 * @param in
	 *            bytes to read
	 * @return converted Image
	 * @throws QCError
	 */
	public static Image BinToImg(byte[] in) throws QCError {
		ByteArrayInputStream is = new ByteArrayInputStream(in);
		try {
			return ImageIO.read(is);
		} catch (IOException e) {
			throw new QCError("Failed to get image from input");
		}
	}

	/**
	 * Converts any Image object into a valid BufferedImage with the same data
	 * 
	 * @param img
	 *            Image to convert
	 * @return BufferedImage of input
	 */
	public static BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage) //easy mode
		{
			return (BufferedImage) img;
		}

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		// Draw the image on to the buffered image
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		//fixBadJPEG(bimage);

		// Return the buffered image
		return bimage;
	}

	/**
	 * Encode to my new Image encoding format designed to resist some
	 * compression and corruption.
	 * 
	 * @param in
	 *            data to encode
	 * @param blockSize
	 *            size of the side length of the square blocks making up the
	 *            format (sma)
	 * @return Image holding encoded data
	 * @throws QCError 
	 */
	public static Image DataEncode(byte[] in, int blockSize, Color[] palette) throws QCError {
		
		/*palatte[0] = Color.BLACK;
		palatte[1] = Color.WHITE;
		palatte[2] = Color.CYAN;
		palatte[3] = Color.GREEN;
		palatte[4] = Color.LIGHT_GRAY;
		palatte[5] = Color.MAGENTA;
		palatte[6] = Color.ORANGE;
		palatte[7] = Color.RED;*/
		
		int pallateBits = 0;
		for(int i=palette.length;i>1;i/=2)pallateBits++;
		int lcm = (8 * pallateBits)/gcd(pallateBits,8);
		if(1<<pallateBits != palette.length)throw new QCError("Pallate length not power of two");
		
		//get where the header ends and data begins
		int dataoffbits = pallateBits * (palette.length + 1) + 32 + 32;
		if (dataoffbits % lcm != 0)
			dataoffbits += lcm - (dataoffbits % lcm); //must be multiple of 8 and palatebits
		int dataoffbyte = dataoffbits / 8;

		//header + data
		byte[] put = new byte[dataoffbyte + in.length];

		//get number of blocks to display
		int truelen = (put.length * 8) / pallateBits;
		if ((put.length * 8) % pallateBits != 0)
			truelen++;

		for (int x = 0; x <= palette.length; x++) ///encoding palette
			BinaryEncoder.setBits(put, pallateBits * x, pallateBits, x % palette.length);

		///magic number
		BinaryEncoder.setBits(put, pallateBits * (palette.length + 1), 32, 1234567890);

		///length
		BinaryEncoder.setBits(put, pallateBits * (palette.length + 1) + 32, 32, in.length);

		//rest of the data
		System.arraycopy(in, 0, put, dataoffbyte, in.length);

		//determine best width and height
		int width = blockSize, height = blockSize;

		while ((width / blockSize) * (height / blockSize) < truelen + palette.length) {
			if (height > width * 1.1)
				width += blockSize;
			else
				height += blockSize;
		}

		int bwidth = width / blockSize; //width in blocks

		//output
		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		//convert put to out by using blocks and the palate
		for (int i = 0; i < truelen; i++) {
			
			int val = (int) BinaryEncoder.getBits(put, i * pallateBits, i * pallateBits + pallateBits < put.length * 8 
					? pallateBits : put.length * 8 - i * pallateBits);
			
			int rgb = palette[val].getRGB();

			//create block
			for (int x = 0; x < blockSize; x++)
				for (int y = 0; y < blockSize; y++)
					out.setRGB(x + (i % bwidth) * blockSize, y + (i / bwidth) * blockSize, rgb);
		}

		//add extra palette for identification
		for (int i = truelen; i < truelen + palette.length; i++)
			for (int x = 0; x < blockSize; x++)
				for (int y = 0; y < blockSize; y++)
					out.setRGB(x + (i % bwidth) * blockSize, y + (i / bwidth) * blockSize, palette[i - truelen].getRGB());
		
		//fill the end with palette[0]
		for (int i = truelen + palette.length; i < (width / blockSize) * (height / blockSize); i++)
			for (int x = 0; x < blockSize; x++)
				for (int y = 0; y < blockSize; y++)
					out.setRGB(x + (i % bwidth) * blockSize, y + (i / bwidth) * blockSize, palette[0].getRGB());
		
		return out;
	}

	public static Color[] spacedPallate(int bits) {
		Color[] out = new Color[1<<bits];
		int option = (int) Math.ceil(Math.pow(out.length,1.0/3));
		
		for(int x=0;x<out.length;x++)
		{
			int y = x;
			double r = (y%option)/(double)(option-1);
			y /= option;
			double g = (y%option)/(double)(option-1);
			y /= option;
			double b = (y%option)/(double)(option-1);
			
			out[x] = new Color((int)(255*r),(int)(255*g),(int)(255*b));
		}
		
		return out;
	}

	/**
	 * Crude measurement of how close a color is using the Pythagorean Theorem
	 * 
	 * @param a
	 *            Color
	 * @param b
	 *            Color
	 * @return how close a is to b
	 */
	public static double colorDist(Color a, Color b) {
		return Math.sqrt((a.getAlpha() - b.getAlpha()) * (a.getAlpha() - b.getAlpha())
				+ (a.getRed() - b.getRed()) * (a.getRed() - b.getRed())
				+ (a.getGreen() - b.getGreen()) * (a.getGreen() - b.getGreen())
				+ (a.getBlue() - b.getBlue()) * (a.getBlue() - b.getBlue()));
	}

	/**
	 * Get the index of the closet item in pall to
	 * 
	 * @param in
	 *            Color to find closest to
	 * @param pall
	 *            Array of Colors to chose from
	 * @return index of closest Color
	 */
	public static int roundToPallet(Color in, Color[] pall) {
		double minDist = 1000;
		int closest = 0;
		for (int x = 0; x < pall.length; x++) {
			double dist = colorDist(in, pall[x]);
			if (dist < minDist) {
				minDist = dist;
				closest = x;
			}
		}
		return closest;
	}

	/**
	 * Attempt to build palate using given blockSize
	 * 
	 * @param buffimg
	 *            image to read from
	 * @param palatteSize
	 * 			@TODO find this automatically
	 * @param blockSize
	 *            size of block side to try
	 * @param width
	 *            width of image
	 * @param height
	 *            height of image
	 * @return retrieved palate
	 * @return null if could not get a valid palate
	 */
	private static Color[] tryPal(BufferedImage buffimg, int maxPalatteSize, int blockSize, int width, int height) {
		int bwidth = width / blockSize;

		Color[] palatte = new Color[maxPalatteSize];

		for (int i = 0; i < maxPalatteSize; i++) {
			int x = (i % bwidth) * blockSize + blockSize / 2;
			int y = (i / bwidth) * blockSize + blockSize / 2;

			if (x >= width || y >= height)
				return null;

			palatte[i] = new Color(buffimg.getRGB(x, y));
			
			if(i>0&&colorDist(palatte[i],palatte[0])<5) //end palatte
			{
				Color[] old = palatte;
				palatte = new Color[i];
				System.arraycopy(old, 0, palatte, 0, i);
				break;
			}
		}

		return palatte;
	}

	/**
	 * Try to get header information or first 64 bits
	 * 
	 * @param buffimg
	 *            image to read from
	 * @param palatte
	 *            palatte to pick from
	 * @param blockSize
	 *            size of block side to try
	 * @param width
	 *            width of image
	 * @param height
	 *            height of image
	 * 
	 * @return byte array of header
	 * @return null if it could not be retrieved
	 */
	private static byte[] tryHeader(BufferedImage buffimg, Color[] palatte, int blockSize, int width, int height) {
		
		int palatteBits = 0;
		for(int i=palatte.length;i>1;i/=2)palatteBits++;
		
		int bwidth = width / blockSize;

		int headerbitsize = 32 + 32;
		byte[] header = new byte[headerbitsize / 8 + 1];
		
		for (int i = palatte.length + 1; i < palatte.length + 1
				+ (headerbitsize / palatteBits + (headerbitsize % palatteBits == 0 ? 0 : 1)); i++) {
			int x = (i % bwidth) * blockSize + blockSize / 2;
			int y = (i / bwidth) * blockSize + blockSize / 2;

			if (x >= width || y >= height)
				return null;

			//get closest color in palate to this spot
			int val = roundToPallet(new Color(buffimg.getRGB(x, y)), palatte);
			BinaryEncoder.setBits(header, (i - (palatte.length + 1)) * palatteBits, palatteBits, val);
		}

		return header;
	}
	
	// Recursive method to return gcd of a and b 
    static int gcd(int a, int b) 
    { 
        // Everything divides 0  
        if (a == 0 || b == 0) 
           return 0; 
       
        // base case 
        if (a == b) 
            return a; 
       
        // a is greater 
        if (a > b) 
            return gcd(a-b, b); 
        return gcd(a, b-a); 
    } 

	/**
	 * Decode Image that contains encoded information by DataEncode()
	 * 
	 * @param in
	 *            Image to decode
	 * @return data output
	 * @return null if no data was found, probably just a normal image
	 */
	public static byte[] DataDecode(Image in) {
		int width = in.getWidth(null);
		int height = in.getHeight(null);

		int blockSize = width;
		while (blockSize % 2 == 0)
			blockSize /= 2;

		int maxPalatteSize = 256;

		BufferedImage buffimg = toBufferedImage(in); //readable input

		//get most likely block size
		int tryBlockSize = 1;
		double highestavgdiff = 0;
		int bestBlockSize = 0;

		for (; tryBlockSize < width / 2 && tryBlockSize < height && tryBlockSize < 150; tryBlockSize++) {
			Color[] testPal = tryPal(buffimg, maxPalatteSize, tryBlockSize, width, height);
			if (testPal == null || testPal.length < 2)
				continue;

			byte[] testHead = tryHeader(buffimg, testPal, tryBlockSize, width, height);
			if (testHead != null && BinaryEncoder.getBits(testHead, 0, 32) == 1234567890) {
				//passed all the required tests, could be correct block size, how likely is it?
				
				//@TODO have a more extensive secondary blocksize screening process 

				double avgDiff = 0;
				for (int i = 1; i < 28; i++) {
					int x = (i % (width / tryBlockSize)) * tryBlockSize;
					int y = (i / (width / tryBlockSize)) * tryBlockSize + tryBlockSize / 2;

					if (x >= width || y >= height)
						avgDiff = -1000000;

					if (x > 0) {
						Color col = new Color(buffimg.getRGB(x, y));
						Color col2 = new Color(buffimg.getRGB(x - 1, y));
						avgDiff += colorDist(col, col2);
					}
				}
				if (avgDiff >= highestavgdiff) {
					highestavgdiff = avgDiff;
					bestBlockSize = tryBlockSize;
				}
			}
		}
		
		if (bestBlockSize == 0)
			return null; //no candidate block sizes found
		blockSize = bestBlockSize;

		int bwidth = width / blockSize; //width in blocks

		//get palate
		Color[] palatte = tryPal(buffimg, maxPalatteSize, blockSize, width, height);
		if (palatte == null)
			return null;

		int palatteBits = 0;
		for(int i=palatte.length;i>1;i/=2)palatteBits++;
		int lcm = (8*palatteBits)/gcd(8,palatteBits);
		
		//get header
		byte[] header = tryHeader(buffimg, palatte, blockSize, width, height);
		if (header == null)
			return null;

		if (BinaryEncoder.getBits(header, 0, 32) != 1234567890)
			return null; //special code, confirm this is an encoding

		//get length
		long len = BinaryEncoder.getBits(header, 32, 32);
		if (len > 1073741824)
			return null;
		
		//find where header ends and data starts
		int dataoffbits = palatteBits * (palatte.length + 1) + 32 + 32;
		if (dataoffbits % lcm != 0)
			dataoffbits += lcm - (dataoffbits % lcm);

		//output
		byte[] out = new byte[(int) len];

		//get remaining output
		int bitidx;
		int bitlen = out.length * 8;
		for (int i = dataoffbits / palatteBits; (bitidx = i * palatteBits - dataoffbits) < bitlen; i++) {
			int x = (i % bwidth) * blockSize + blockSize / 2;
			int y = (i / bwidth) * blockSize + blockSize / 2;

			if (x >= width || y >= height)
				return null; //image not large enough for expected output

			//find nearest color in palate for bits
			int val = roundToPallet(new Color(buffimg.getRGB(x, y)), palatte);
			BinaryEncoder.setBits(out, bitidx, bitidx + palatteBits < bitlen ? palatteBits : bitlen - bitidx, val);
		}

		return out;
	}
}
