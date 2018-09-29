package quickcrypt.shortcut;
import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.JIntellitype;

import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import quickcrypt.core.QCError;

/**
 * Listens for a hotkey before triggering Action
 * 
 * @author Adam Spiegel
 */

public class HotKey implements ActionReturn{
	
	private Action action;
	private HotKey This;
	private int key;
	
	HotkeyListener jintlist;
	
	/**
	 * Creates a HotKey object
	 * 
	 * @param act Action object to do coding
	 * @param key keycode to listen for
	 * @throws QCError
	 */
	public HotKey(Action act, int key) throws QCError
	{
		 action = act;
		 This = this;
		 this.key = key;
		 
		 String osname = System.getProperty("os.name");
		 
		 //will use different methods for different OSes based on efficiency and availability
		 if(osname.startsWith("Windows"))setup_JIntellitype();
		 else throw new QCError("OS, "+osname+" does not support java keyboard shortcuts");
	}
	
	/**
	 * Setups listener for JIntellitype, only on windows 
	 */
	private void setup_JIntellitype()
	{
		
	    JIntellitype.getInstance().registerHotKey(6969, JIntellitype.MOD_CONTROL, key);
	    jintlist = new HotkeyListener() {
	 	   
		    //If my hotkey is detected then trigger action
		    @Override
		    public void onHotKey(int cut) {
		           if (cut == 6969)
		            action.goDo(This); //call action with this object as the caller
		    }};
	    JIntellitype.getInstance().addHotKeyListener(jintlist);
	}
	
	/**
	 * Stops listener
	 */
	public void stop()
	{
		JIntellitype.getInstance().unregisterHotKey(6969);
		JIntellitype.getInstance().removeHotKeyListener(jintlist);
	}

	/**
	 * Callback for when the Action object is done
	 */
	public void actionDone() {
		
	}
}
