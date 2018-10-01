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
	 */
	public static Image DataEncode(byte[] in, int blockSize) {
		//default palate
		Color[] palatte = new Color[16];
		palatte[0] = Color.BLACK;
		palatte[1] = Color.WHITE;
		palatte[2] = Color.CYAN;
		palatte[3] = Color.GREEN;
		palatte[4] = Color.LIGHT_GRAY;
		palatte[5] = Color.MAGENTA;
		palatte[6] = Color.ORANGE;
		palatte[7] = Color.RED;
		palatte[8] = Color.BLUE;

		//get where the header ends and data begins
		int dataoffbits = 3 * (8 + 1) + 32 + 32;
		if (dataoffbits % (8 * 3) != 0)
			dataoffbits += 24 - (dataoffbits % (8 * 3)); //must be multiple of 8 and palatebits
		int dataoffbyte = dataoffbits / 8;

		//header + data
		byte[] put = new byte[dataoffbyte + in.length];

		//get number of blocks to display
		int truelen = (put.length * 8) / 3;
		if ((put.length * 8) % 3 != 0)
			truelen++;

		for (int x = 0; x <= 8; x++) ///encoding pallet
			BinaryEncoder.setBits(put, 3 * x, 3, x % 8);

		///special key
		BinaryEncoder.setBits(put, 3 * (8 + 1), 32, 1234567890);

		///length
		BinaryEncoder.setBits(put, 3 * (8 + 1) + 32, 32, in.length);

		//rest of the data
		System.arraycopy(in, 0, put, dataoffbyte, in.length);

		//determine best width and height
		int width = blockSize, height = 1;

		while ((width / blockSize) * (height / blockSize) < truelen) {
			if (height > width * 1.5)
				width *= 2;
			else
				height++;
		}

		int bwidth = width / blockSize; //width in blocks

		//output
		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		//convert put to out by using blocks and the palate
		for (int i = 0; i < truelen; i++) {
			int val = (int) BinaryEncoder.getBits(put, i * 3, i * 3 + 3 < put.length * 8 ? 3 : put.length * 8 - i * 3);
			int rgb = palatte[val].getRGB();

			for (int x = 0; x < blockSize; x++)
				for (int y = 0; y < blockSize; y++)
					out.setRGB(x + (i % bwidth) * blockSize, y + (i / bwidth) * blockSize, rgb);
		}

		//fill the rest with black pixels
		for (int i = truelen; i < (width / blockSize) * (height / blockSize); i++)
			for (int x = 0; x < blockSize; x++)
				for (int y = 0; y < blockSize; y++)
					out.setRGB(x + (i % bwidth) * blockSize, y + (i / bwidth) * blockSize, Color.BLACK.getRGB());

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
	 * Attept to build palate using given blockSize
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
	private static Color[] tryPal(BufferedImage buffimg, int palatteSize, int blockSize, int width, int height) {
		int bwidth = width / blockSize;

		Color[] palatte = new Color[palatteSize];

		for (int i = 0; i < palatteSize; i++) {
			int x = (i % bwidth) * blockSize + blockSize / 2;
			int y = (i / bwidth) * blockSize + blockSize / 2;

			if (x >= width || y >= height)
				return null;

			palatte[i] = new Color(buffimg.getRGB(x, y));
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
		int bwidth = width / blockSize;

		int headerbitsize = 32 + 32;
		byte[] header = new byte[headerbitsize / 8 + 1];

		for (int i = palatte.length + 1; i < palatte.length + 1
				+ (headerbitsize / 3 + (headerbitsize % 3 == 0 ? 0 : 1)); i++) {
			int x = (i % bwidth) * blockSize + blockSize / 2;
			int y = (i / bwidth) * blockSize + blockSize / 2;

			if (x >= width || y >= height)
				return null;

			//get closest color in palate to this spot
			int val = roundToPallet(new Color(buffimg.getRGB(x, y)), palatte);
			BinaryEncoder.setBits(header, (i - (palatte.length + 1)) * 3, 3, val);
		}

		return header;
	}

	/**
	 * Decode Image that contatins encoded information by DataEncode()
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

		int palatteSize = 8;

		BufferedImage buffimg = toBufferedImage(in); //readable input

		//get most likely block size
		int tryBlockSize = 1;
		double highestavgdiff = 0;
		int bestBlockSize = 0;

		for (; tryBlockSize < width / 2 && tryBlockSize < height && tryBlockSize < 150; tryBlockSize++) {
			Color[] testPal = tryPal(buffimg, palatteSize, tryBlockSize, width, height);
			if (testPal == null || testPal.length != palatteSize)
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
		Color[] palatte = tryPal(buffimg, palatteSize, blockSize, width, height);
		if (palatte == null)
			return null;

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

		//find where header ends and data dtarts
		int dataoffbits = 3 * (palatteSize + 1) + 32 + 32;
		if (dataoffbits % (8 * 3) != 0)
			dataoffbits += 24 - (dataoffbits % (8 * 3));

		//output
		byte[] out = new byte[(int) len];

		//get remaining output
		int bitidx;
		int bitlen = out.length * 8;
		for (int i = dataoffbits / 3; (bitidx = i * 3 - dataoffbits) < bitlen; i++) {
			int x = (i % bwidth) * blockSize + blockSize / 2;
			int y = (i / bwidth) * blockSize + blockSize / 2;

			if (x >= width || y >= height)
				return null; //image not large enough for expected ouput

			//find nearest color in palate for bits
			int val = roundToPallet(new Color(buffimg.getRGB(x, y)), palatte);
			BinaryEncoder.setBits(out, bitidx, bitidx + 3 < bitlen ? 3 : bitlen - bitidx, val);
		}

		return out;
	}
}
