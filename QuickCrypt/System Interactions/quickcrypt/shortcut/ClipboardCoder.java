package quickcrypt.shortcut;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;

import quickcrypt.core.Context;
import quickcrypt.core.QCError;

/**
 * Wrapper for context that allows calling code() on the clipboard data
 * 
 * @author Adam Spiegel
 */

public class ClipboardCoder implements ClipboardOwner {

	public Context context;
	public Transferable pushedData;

	/**
	 * Create a clipboard coder from a context
	 * 
	 * @param context
	 *            Context to be used to decode and encode
	 */

	public ClipboardCoder(Context context) {
		this.context = context;
	}

	/**
	 * Encodes or Decodes what is in the clipboard and does paste the result
	 * back to clipboard Same as <this>.code(true);
	 * 
	 * @return encoded or decoded result
	 * @throws QCError
	 */
	public Transferable code() throws QCError {
		return code(true);
	}

	/**
	 * Encodes or Decodes what is in the clipboard
	 * 
	 * @param paste
	 *            weather or not to paste result back into the clipboard
	 * @return encoded or decoded result
	 * @throws QCError
	 */
	Transferable code(boolean paste) throws QCError {
		Transferable out;

		out = context.code(get()); //code(copyied data)

		if (paste)
			set(out); //paste

		return out;
	}

	/**
	 * Encodes or Decodes input and optionally pastes result into clipboard.
	 * 
	 * @param in
	 *            data to encode or decode
	 * @param paste
	 *            weather or not to paste result back into the clipboard (if this is false than it is effectively the same as Context.code())
	 * @return encoded or decoded result
	 * @throws QCError
	 */
	public Transferable code(Transferable in, boolean paste) throws QCError {
		Transferable out;

		out = context.code(in);

		if (paste)
		{
			set(out);
		}

		return out;
	}

	/**
	 * Saves a copy of the current clipboard data to use pop() later
	 * 
	 * @return current clipboard data or null if inaccessable
	 */
	public Transferable push() {
		try {
			return pushedData = get();
		} catch (QCError e) {
			return null;
		}
	}

	/**
	 * Puts data that was in the clipboard the last time push() was called, back
	 * into the clipboard.
	 */
	public void pop() {
		if (pushedData == null)
			return;

		try {
			set(pushedData);
		} catch (QCError e) {
		}
	}

	public void set(Transferable data) throws QCError
	{
		long goal=System.currentTimeMillis()+3000;
		while(System.currentTimeMillis()<goal)
		{
			try {
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data, this);
			return;
			} catch(IllegalStateException e){}
			try {Thread.sleep(100);}
			catch (InterruptedException e) {}
		}
		throw new QCError("Clipboard took to long to relinquish");
	}
	
	public Transferable get() throws QCError
	{
		long goal=System.currentTimeMillis()+3000;
		while(System.currentTimeMillis()<goal)
		{
			try {
			return Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
			} catch(IllegalStateException e){}
			try {Thread.sleep(100);}
			catch (InterruptedException e) {}
		}
		throw new QCError("Clipboard took to long to relinquish");
	}
	
	/**
	 * Required function for implementing ClipboardOwner, does nothing
	 */
	@Override
	public void lostOwnership(Clipboard arg0, Transferable arg1) {

	}
}
