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

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.JCheckBox;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.awt.event.ItemEvent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class MainWindow {

	public String settingsFile = "Quick Crypt Settings.ini";
	public boolean saveOnExit = true;

	private JFrame frmQuickCrypt;
	
	private Context context;
	private SharedSecrets sharedsecrets;
	private ClipboardCoder clippy;
	private Action action;
	private HotKey hotkey;
	
	private Map<Character, BinaryEncoder> encoders;
	
	private Map<String,EncryptorSettings> encryptorsettings;
	
	JTextArea txtrTestMessage;
	private final JScrollPane scrollPane_1 = new JScrollPane();
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frmQuickCrypt.setVisible(true);
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
			context.addBinaryEncoder(new Emoji256());
			
			sharedsecrets = new SharedSecrets();
			context.addEncryptor(sharedsecrets);
			
			context.setEncryption("NO");
			context.setEncoding('X');
			context.setFlag1(1);
			
			try {
				context.load(new Scanner(new File(settingsFile)));
			} catch (Exception e) {
				System.err.println("Settings were invalid or non existant");
			}
			
			encoders = context.getBinaryEncoderMap();
			
			clippy = new ClipboardCoder(context);
			action = new Action(clippy);
			hotkey = null;
			
			encryptorsettings = new HashMap<String,EncryptorSettings>();
			
			addEncryptorSettings(new SharedSecretSettings(sharedsecrets,context));
			
		} catch (QCError e) {
			System.err.println("Error: "+e);
			return;
		}
		initialize();
	}

	public void addEncryptorSettings(EncryptorSettings es) {
		encryptorsettings.put(es.base64Id(), es);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmQuickCrypt = new JFrame();
		frmQuickCrypt.setTitle("Quick Crypt");
		frmQuickCrypt.setBounds(100, 100, 498, 508);
		frmQuickCrypt.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frmQuickCrypt.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
			    try {
					context.save(new PrintStream(new File(settingsFile)));
				} catch (FileNotFoundException e1) {}
			}
			  });

		
		JPanel panel = new JPanel();
		frmQuickCrypt.getContentPane().add(panel, BorderLayout.CENTER);
		
		final JComboBox<Object> comboBox = new JComboBox<Object>();
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				String enc = (String) comboBox.getSelectedItem();
				
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
			
		comboBox.setModel(new DefaultComboBoxModel<Object>(encodernames));
		try {
			comboBox.setSelectedItem(context.getBinaryEncoder((context.getEncoding())).shortName());
		} catch (QCError e2) {}
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
		
		final JComboBox<Object> encryptor = new JComboBox<Object>();
		
		String[] encryptornames = new String[context.getEncryptorMap().size()+1];
		encryptornames[0] = "No Encryption";
		x=1;
		for(Encryptor e : context.getEncryptorMap().values())
			encryptornames[x++] = e.shortName();
		
		encryptor.setModel(new DefaultComboBoxModel<Object>(encryptornames));
		encryptor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String enc = (String) encryptor.getSelectedItem();
				
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
		
		String selencid = context.getEncryptior();
		if(selencid.equals("NO"))encryptor.setSelectedIndex(0);
		else encryptor.setSelectedItem(context.getEncryptor(selencid).shortName());
		
		panel.add(encryptor);
		
		JButton btnSettings = new JButton("Settings");
		btnSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				context.lock();
				String enc = context.getEncryptior();
				context.unlock();
				
				if(enc=="NO")
					JOptionPane.showMessageDialog(null, "No encryption, all messages sent with this mode can be decoded by anyone", "No Encryption", JOptionPane.INFORMATION_MESSAGE);
				else
				{
					encryptorsettings.get(enc).start(frmQuickCrypt);
				}
			}
		});
		btnSettings.setBounds(150, 45, 89, 23);
		panel.add(btnSettings);
		
		JButton button = new JButton("Encrypt/Decrypt text");
		button.setActionCommand("");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				context.lock();
				boolean oldtxttoimg = context.TexttoImg;
				context.TexttoImg = false;
				
				try {
					if(txtrTestMessage.getText().length()==0)throw new QCError("Empty input");
					Transferable out = context.code(new StringSelection(txtrTestMessage.getText()));

					try {
						txtrTestMessage.setText((String) out.getTransferData(DataFlavor.stringFlavor));
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
		chckbxNewCheckBox_1.setSelected(!context.isFlag1(1));
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
		chckbxNewCheckBox_1.setBounds(10, 137, 212, 23);
		panel.add(chckbxNewCheckBox_1);
		
		JCheckBox chckbxEnableCompression = new JCheckBox("Enable Compression");
		chckbxEnableCompression.setSelected(context.getCompression()!='0');
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
		chckbxEnableCompression.setBounds(10, 111, 212, 23);
		panel.add(chckbxEnableCompression);
		
		JButton btnNewButton = new JButton("Encrypt/Decrypt clipboard");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				context.lock();
				try {
					clippy.code();
				} catch (QCError e1) {
					JOptionPane.showMessageDialog(null, "Error: "+e1.getMessage(), "Error coding", JOptionPane.ERROR_MESSAGE);
				}
				context.unlock();
			}
		});
		btnNewButton.setBounds(279, 440, 193, 23);
		panel.add(btnNewButton);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(243, 237, -106, -69);
		panel.add(scrollPane);
		scrollPane_1.setBounds(10, 295, 462, 140);
		panel.add(scrollPane_1);
		
		txtrTestMessage = new JTextArea();
		scrollPane_1.setViewportView(txtrTestMessage);
		txtrTestMessage.setText("Test Message");
		
		JCheckBox chckbxEncodeImagesAs = new JCheckBox("Encode images as text");
		chckbxEncodeImagesAs.setSelected(context.ImgtoText);
		chckbxEncodeImagesAs.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				context.lock();
				context.ImgtoText = (e.getStateChange() != ItemEvent.DESELECTED);
				context.unlock();
			}
		});
		chckbxEncodeImagesAs.setBounds(296, 111, 158, 23);
		panel.add(chckbxEncodeImagesAs);
		
		JCheckBox chckbxEncodeTextAs = new JCheckBox("Encode text as images");
		chckbxEncodeTextAs.setSelected(context.TexttoImg);
		chckbxEncodeTextAs.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				context.lock();
				context.TexttoImg = (e.getStateChange() != ItemEvent.DESELECTED);
				context.unlock();
			}
		});
		chckbxEncodeTextAs.setBounds(296, 137, 158, 23);
		panel.add(chckbxEncodeTextAs);
		
		JCheckBox chckbxDoNotAttempt = new JCheckBox("Do not attempt to decode");
		chckbxDoNotAttempt.setSelected(!context.tryDecode);
		chckbxDoNotAttempt.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				context.lock();
				context.tryDecode = (e.getStateChange() == ItemEvent.DESELECTED);
				context.unlock();
			}
		});
		chckbxDoNotAttempt.setBounds(296, 163, 176, 23);
		panel.add(chckbxDoNotAttempt);
		
		JButton btnBinaryEncoderSettings = new JButton("Image Settings");
		btnBinaryEncoderSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new ImageSettings(frmQuickCrypt,context);
			}
		});
		btnBinaryEncoderSettings.setBounds(249, 77, 138, 23);
		panel.add(btnBinaryEncoderSettings);
	}
}
