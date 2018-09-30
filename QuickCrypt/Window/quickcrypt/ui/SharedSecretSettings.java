package quickcrypt.ui;

import javax.swing.JFrame;

import quickcrypt.core.Context;
import quickcrypt.core.QCError;
import quickcrypt.core.Secret;
import quickcrypt.core.SharedSecrets;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import com.jgoodies.forms.factories.DefaultComponentFactory;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Font;
import javax.swing.DefaultComboBoxModel;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class SharedSecretSettings extends EncryptorSettings{

	private JDialog frame;
	SharedSecrets ss;
	private JPasswordField passwordField;
	private JTextField textField;
	private Context context;
	
	private JComboBox comboBox;
	
	/**
	 * Create the application.
	 */
	public SharedSecretSettings(SharedSecrets ss, Context c) {
		context = c;
		this.ss = ss;
	}

	
	public void stop()
	{
		frame.setVisible(false);
		frame.dispose();
	}
	
	/**
	 * Initialize the contents of the frame.
	 * @wbp.parser.entryPoint
	 */
	public void start(JFrame parent) {
		frame = new JDialog(parent,true);
		frame.setBounds(100, 100, 450, 300);
		frame.getContentPane().setLayout(null);
		
		comboBox = new JComboBox();
		comboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				String lab = (String) ((JComboBox)e.getSource()).getSelectedItem();
				context.lock();
				try {
					ss.selectSecret(lab);
				} catch (QCError e1) {
					System.err.println("Somehow an invalid label was selected this should not be possible");
				}
				context.unlock();
			}
		});
		refreshLabels();
		comboBox.setBounds(74, 11, 350, 20);
		frame.getContentPane().add(comboBox);
		
		JButton btnDelete = new JButton("Delete");
		btnDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String lab = ss.getSelectedSecret().getLabel();
				if(!lab.equals("DEFAULT"))
				{
					ss.removeSecret(lab);
					try {
						ss.selectSecret("DEFAULT");
					} catch (QCError e) {}
					
					refreshLabels();
				}
			}
		});
		btnDelete.setBounds(74, 38, 89, 23);
		frame.getContentPane().add(btnDelete);
		
		JButton btnExport = new JButton("Export");
		btnExport.setBounds(335, 38, 89, 23);
		frame.getContentPane().add(btnExport);
		
		JButton btnImport = new JButton("Import");
		btnImport.setBounds(335, 228, 89, 23);
		frame.getContentPane().add(btnImport);
		
		passwordField = new JPasswordField();
		passwordField.setBounds(74, 197, 350, 20);
		frame.getContentPane().add(passwordField);
		
		textField = new JTextField();
		textField.setBounds(74, 166, 350, 20);
		frame.getContentPane().add(textField);
		textField.setColumns(1);
		
		JButton btnAdd = new JButton("Add");
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				if(textField.getText().length()==0||textField.getText().length()>50) {
					JOptionPane.showMessageDialog(null, "The length of the label must be between 1 and 50", "Error", JOptionPane.INFORMATION_MESSAGE);return;}
				
				Secret sec;
				try {
					sec = new Secret(textField.getText(),new String(passwordField.getPassword()),null);
				} catch (QCError e) {
					JOptionPane.showMessageDialog(null, "Failed to calculate key from password", "Error", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				
				if(sec.getLabel().equals("DEFAULT")) {
					JOptionPane.showMessageDialog(null, "Cannot set the DEFAULT secret", "Error", JOptionPane.INFORMATION_MESSAGE);return;}
				
				context.lock();
				if(ss.getSecret(sec.getLabel())!=null)
				{
					context.unlock();
					if(JOptionPane.showConfirmDialog(null, "An existing secret also has this label!\n Would you like to overwrite existing secret?","Error",JOptionPane.YES_NO_OPTION)==1)
						return;
					context.lock();
				}
				ss.addSecret(sec);
				
				try {
					ss.selectSecret(sec.getLabel());
					refreshLabels();
				} catch (QCError e) {}
				
				context.unlock();
			}
		});
		btnAdd.setBounds(74, 228, 89, 23);
		frame.getContentPane().add(btnAdd);
		
		JLabel lblSecret = new JLabel("Secret:");
		lblSecret.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblSecret.setBounds(21, 11, 54, 17);
		frame.getContentPane().add(lblSecret);
		
		JLabel lblLabel = new JLabel("Label:");
		lblLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblLabel.setBounds(33, 166, 39, 17);
		frame.getContentPane().add(lblLabel);
		
		JLabel lblPassword = new JLabel("Password:");
		lblPassword.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblPassword.setBounds(8, 200, 67, 14);
		frame.getContentPane().add(lblPassword);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}
	
	public void refreshLabels()
	{
		comboBox.setModel(new DefaultComboBoxModel(ss.getLabels().toArray(new String[0])));
		comboBox.setSelectedItem(ss.getSelectedSecret().getLabel());
	}

	@Override
	public String base64Id() {
		return "SS";
	}
}
