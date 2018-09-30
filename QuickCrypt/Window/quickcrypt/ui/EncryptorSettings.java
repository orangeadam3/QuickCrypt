package quickcrypt.ui;

import javax.swing.JFrame;

public abstract class EncryptorSettings {
	public abstract String base64Id();
	public abstract void start(JFrame parent);
	public abstract void stop();
}
