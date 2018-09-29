package quickcrypt.ui;

import quickcrypt.core.*;
import quickcrypt.shortcut.*;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import java.util.Map;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Button;
import javax.swing.JTextPane;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.JCheckBox;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

public class MainWindow {

	private JFrame frame;
	
	private Context context;
	private SharedSecrets sharedsecrets;
	private ClipboardCoder clippy;
	private Action action;
	private HotKey hotkey;
	
	private Map<Character, BinaryEncoder> encoders;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		try {
			context = new Context();
			
			context.addBinaryEncoder(new Hexadecimal());
			context.addBinaryEncoder(new Base64URL());
			context.addBinaryEncoder(new CJK4096());
			
			sharedsecrets = new SharedSecrets();
			context.addEncryptor(sharedsecrets);
			
			context.setEncryption("NO");
			context.setEncoding('X');
			context.setFlag1(1);
			
			encoders = context.getBinaryEncoderMap();
			
			clippy = new ClipboardCoder(context);
			action = new Action(clippy);
			hotkey = null;
			
		} catch (QCError e) {
			System.err.println("Error: "+e);
			return;
		}
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 498, 508);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		
		JComboBox comboBox = new JComboBox();
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				String enc = (String) ((JComboBox)e.getSource()).getSelectedItem();
				
				for(BinaryEncoder c : encoders.values())
					if(c.shortName().equals(enc))
					{
						try {
							context.lock();
							context.setEncoding(c.base64Id());
							context.unlock();
						} catch(QCError r) {}
						return;
					}
			}
		});
		comboBox.setBounds(249, 46, 138, 20);
		String[] encodernames = new String[encoders.size()];
		int x=0;
		for(BinaryEncoder e : encoders.values())
			encodernames[x++] = e.shortName();
		panel.setLayout(null);
			
		comboBox.setModel(new DefaultComboBoxModel(encodernames));
		comboBox.setSelectedIndex(2);
		panel.add(comboBox);
		
		JLabel lblBinaryEncoder = new JLabel("Binary Encoder");
		lblBinaryEncoder.setBounds(249, 21, 138, 24);
		lblBinaryEncoder.setFont(new Font("Tahoma", Font.PLAIN, 14));
		panel.add(lblBinaryEncoder);
		
		JButton btnEncoderInfo = new JButton("Info");
		btnEncoderInfo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				BinaryEncoder be;
				try {
					be = context.getBinaryEncoder(context.getEncoding());
				} catch (QCError e) {return;}
				
				String desc = BinaryEncoder.standardDescription()+"\n\n"+be.fullName()+" ("+be.base64Id()+"):\n"+be.description();
				
				//really simple margin adder
				int lastNewLine = 0;
				for(int x=0;x<desc.length()-1;x++)
					if(x-lastNewLine>75&&desc.charAt(x)==' ')
					{
						desc = desc.substring(0, x) + "\n" + desc.substring(x+1);
						lastNewLine = x;
					}
					else if(desc.charAt(x)=='\n')lastNewLine = x;
				
				JOptionPane.showMessageDialog(null,desc,be.fullName(),JOptionPane.INFORMATION_MESSAGE);
			}
		});
		btnEncoderInfo.setBounds(387, 45, 85, 21);
		panel.add(btnEncoderInfo);
		
		JLabel lblEncrpytionMethod = new JLabel("Encrpytion Method");
		lblEncrpytionMethod.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblEncrpytionMethod.setBounds(10, 21, 126, 24);
		panel.add(lblEncrpytionMethod);
		
		JComboBox encryptor = new JComboBox();
		
		String[] encryptornames = new String[context.getEncryptorMap().size()+1];
		encryptornames[0] = "No Encryption";
		x=1;
		for(Encryptor e : context.getEncryptorMap().values())
			encryptornames[x++] = e.shortName();
		
		encryptor.setModel(new DefaultComboBoxModel(encryptornames));
		encryptor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String enc = (String) ((JComboBox)e.getSource()).getSelectedItem();
				
				context.lock();
				
				if(enc=="No Encryption")
				try{context.setEncryption("NO");} catch(QCError r) {}
				
				for(Encryptor c : context.getEncryptorMap().values())
					if(c.shortName().equals(enc))
					{
						try {
							context.setEncryption(c.base64Id());
						} catch(QCError r) {}
						context.unlock();
						return;
					}
				
				context.unlock();
			}
		});
		encryptor.setBounds(10, 46, 138, 20);
		panel.add(encryptor);
		
		JButton btnSettings = new JButton("Settings");
		btnSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO: Launch Encryptor Settings
			}
		});
		btnSettings.setBounds(150, 45, 89, 23);
		panel.add(btnSettings);
		
		final JTextPane txtpnHello = new JTextPane();
		txtpnHello.setText("Test Message");
		txtpnHello.setBounds(10, 299, 462, 141);
		panel.add(txtpnHello);
		
		Button button = new Button("Encrypt/Decrypt text");
		button.setActionCommand("");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				context.lock();
				boolean oldtxttoimg = context.TexttoImg;
				context.TexttoImg = false;
				
				try {
					if(txtpnHello.getText().length()==0)throw new QCError("Empty input");
					Transferable out = context.code(new StringSelection(txtpnHello.getText()));

					try {
						txtpnHello.setText((String) out.getTransferData(DataFlavor.stringFlavor));
					} catch (Exception e1) {
						throw new QCError("Decrypted data must be text");
					}
					
					context.TexttoImg = oldtxttoimg;
					context.unlock();
					
				} catch (QCError e1) {
					context.TexttoImg = oldtxttoimg;
					context.unlock();
					JOptionPane.showMessageDialog(null, "Error: "+e1.getMessage(), "Error while encrypting/decrypting", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		button.setBounds(10, 440, 186, 22);
		panel.add(button);
		
		JCheckBox chckbxNewCheckBox = new JCheckBox("Enable Keyboard Shortcut");
		chckbxNewCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				context.lock();
				if(e.getStateChange() == ItemEvent.DESELECTED)
				{
					if(hotkey!=null)
					hotkey.stop();
					hotkey = null;
				}
				else
				{
					try {
						hotkey = new HotKey(action, 'E');
					} catch (QCError e1) {
						JOptionPane.showMessageDialog(null, "Error: "+e1.getMessage(), "Error starting hotkey", JOptionPane.ERROR_MESSAGE);
					}
				}
				context.unlock();
			}
		});
		chckbxNewCheckBox.setBounds(296, 269, 186, 23);
		panel.add(chckbxNewCheckBox);
		
		JCheckBox chckbxNewCheckBox_1 = new JCheckBox("Use UTF-16 internally");
		chckbxNewCheckBox_1.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				context.lock();
				if(e.getStateChange() == ItemEvent.DESELECTED)
					context.setFlag1(1);
				else
				{
					if(context.isFlag1(1))
					context.toggleFlag1(1);
				}
				context.unlock();
			}
		});
		chckbxNewCheckBox_1.setBounds(10, 137, 138, 23);
		panel.add(chckbxNewCheckBox_1);
		
		JCheckBox chckbxEnableCompression = new JCheckBox("Enable Compression");
		chckbxEnableCompression.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				context.lock();
				try {
				if(e.getStateChange() == ItemEvent.DESELECTED)
					context.setCompression('0');
				else context.setCompression('z');
				}catch(QCError er) {}
				context.unlock();
			}
		});
		chckbxEnableCompression.setBounds(10, 111, 138, 23);
		panel.add(chckbxEnableCompression);
	}
}
