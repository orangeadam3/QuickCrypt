package quickcrypt.ui;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JSlider;

import quickcrypt.core.Context;
import quickcrypt.core.QCError;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JLabel;
import java.awt.Font;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

public class ImageSettings {

	private JDialog frame;

	/**
	 * Create the application.
	 */
	public ImageSettings(JFrame parent, final Context context) {
		frame = new JDialog(parent,true);
		frame.setTitle("Image Settings");
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		final JLabel bitsDisplay = new JLabel(Integer.toString(context.paletteBits));
		bitsDisplay.setBounds(98, 44, 20, 14);
		frame.getContentPane().add(bitsDisplay);
		
		final JSlider slider = new JSlider();
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				bitsDisplay.setText(Integer.toString((int)slider.getValue()));
				context.lock();
				context.paletteBits = (int)slider.getValue();
				context.unlock();
			}
		});
		slider.setValue(context.paletteBits);
		slider.setMinimum(1);
		slider.setMaximum(8);
		slider.setBounds(121, 38, 142, 26);
		frame.getContentPane().add(slider);
		
		JLabel lblBitsPerEncoding = new JLabel("Bits per block:");
		lblBitsPerEncoding.setToolTipText("Number of Bits represented by any given monocolor block. Lower values increase noise resistance but, higher values reduce the final size and increase the variety of colors used.");
		lblBitsPerEncoding.setBounds(7, 44, 89, 14);
		frame.getContentPane().add(lblBitsPerEncoding);
		
		final JLabel scaleFactorDisplay = new JLabel(Integer.toString(context.imageEncodingBlockSize));
		scaleFactorDisplay.setBounds(98, 89, 20, 14);
		frame.getContentPane().add(scaleFactorDisplay);
		
		final JSlider slider_1 = new JSlider();
		slider_1.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				scaleFactorDisplay.setText(Integer.toString((int)slider_1.getValue()));
				context.lock();
				context.imageEncodingBlockSize = (int)slider_1.getValue();
				context.unlock();
			}
		});
		
		slider_1.setValue(context.imageEncodingBlockSize);
		slider_1.setMaximum(20);
		slider_1.setMinimum(1);
		slider_1.setBounds(121, 82, 142, 26);
		frame.getContentPane().add(slider_1);
		
		JLabel lblScaleFactor = new JLabel("Scale Factor: ");
		lblScaleFactor.setToolTipText("Image size factor, directly corresponds to block size. Higher values will increase size but improve noise resistance.");
		lblScaleFactor.setBounds(7, 89, 86, 14);
		frame.getContentPane().add(lblScaleFactor);
		
		JLabel lblHelloWorld = new JLabel("Encoding Settings");
		lblHelloWorld.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblHelloWorld.setBounds(7, 11, 256, 22);
		frame.getContentPane().add(lblHelloWorld);
		
		final JComboBox<Object> imageFormat = new JComboBox<Object>();
		imageFormat.setModel(new DefaultComboBoxModel<Object>(new String[] 
				{"PNG Portable Network Graphics", "BMP Bitmap", "JPG (JPEG) Joint Photographic Experts Group", "GIF Graphic Interchange Format",
						"WBMP Wireless Bitmap"}));
		
		String format = context.getImageFormat();
		for(int x = 0;x<imageFormat.getModel().getSize();x++)
		{
			String elem = imageFormat.getModel().getElementAt(x).toString();
			int f = elem.indexOf(" ");
			if(f==-1)new QCError("Format table invalid");
			
			if(imageFormat.getModel().getElementAt(x).toString().substring(0,f).equals(format))
			{
				imageFormat.setSelectedIndex(x);
				break;
			}
		}
		
		imageFormat.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				context.lock();
				String form = (String) imageFormat.getSelectedItem();
				int f = form.indexOf(" ");
				if(f!=-1)context.setImageFormat(form.substring(0, f));
				context.unlock();
			}
		});
		imageFormat.setBounds(7, 183, 266, 20);
		frame.getContentPane().add(imageFormat);
		
		JLabel lblImageFileType = new JLabel("Image File Type");
		lblImageFileType.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblImageFileType.setBounds(7, 158, 172, 22);
		frame.getContentPane().add(lblImageFileType);
		
		frame.setVisible(true);
	}
	
	public void stop()
	{
		frame.setVisible(false);
		frame.dispose();
	}
}
