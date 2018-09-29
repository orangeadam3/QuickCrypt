
import quickcrypt.core.*;
import quickcrypt.shortcut.*;

public class Main {
	
	public static void main(String[] args) throws QCError
	{
		Context context = new Context();
		
		context.addBinaryEncoder(new Hexadecimal());
		context.addBinaryEncoder(new Base64URL());
		context.addBinaryEncoder(new CJK4096());
		
		SharedSecrets sharedsecrets = new SharedSecrets();
		context.addEncryptor(sharedsecrets);
		
		context.setEncryption("SS");
		context.setEncoding('U');
		//context.setCompression('z');

		Action action = new Action(new ClipboardCoder(context));
		
		new HotKey(action, 'E');
		
		//GlobalKeyListener gkl = new GlobalKeyListener('E', action);
		
		//gkl.stop();
	}
}
