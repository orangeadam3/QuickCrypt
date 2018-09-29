import java.awt.AWTException;
import java.awt.Robot;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.NativeInputEvent;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class GlobalKeyListener_JNativeHook implements NativeKeyListener, ActionReturn {
	
	boolean executing = false;
	boolean stopIncomingCtrlReleases = false;
	boolean ctrlDown = false;
	
	private final static boolean JNativeHookConsumeEnabled = false;
	
	int key;
	
	Action toCall;
	
	public void stop() throws QCError
	{
		GlobalScreen.removeNativeKeyListener(this);
		try {
			GlobalScreen.unregisterNativeHook();
		} catch (NativeHookException e) {
			throw new QCError("There was a problem unregistering the native hook: "+e.getMessage());
		}
	}
	
	public GlobalKeyListener(int vckey, Action to_call) throws QCError
	{
		toCall = to_call;
		
		key = vckey;
		
		// Get the logger for "org.jnativehook" and set the level to off.
		Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.OFF);
						
		// Don't forget to disable the parent handlers.
		logger.setUseParentHandlers(false);
						
		try {
			GlobalScreen.registerNativeHook();
		}
		catch (NativeHookException e) {
			throw new QCError("There was a problem registering the native hook: "+e.getMessage());
		}
		
		GlobalScreen.addNativeKeyListener(this);
	}
	
	private boolean trystop(NativeKeyEvent e, boolean keyup)
	{
		if(JNativeHookConsumeEnabled){
			//Attempt to stop key
			Field f;
			try {
				f = NativeInputEvent.class.getDeclaredField("reserved");
			} catch (NoSuchFieldException | SecurityException e1) {
				return false;
			}
			f.setAccessible(true);
			try {
				f.setShort(e, (short) 0x01);
			} catch (IllegalArgumentException | IllegalAccessException e1) {
				return false;
			}
		}
		else
		{
			try {
				if(keyup)(new Robot()).keyPress(e.getKeyCode());
				else (new Robot()).keyRelease(e.getKeyCode());
			} catch (AWTException e1) {
				return false;
			};
		}
		
		return true;

	}
	
	public void nativeKeyPressed(NativeKeyEvent e) {
		if(e.getKeyCode() == NativeKeyEvent.VC_CONTROL)ctrlDown = true;
		
		if (ctrlDown&&e.getKeyCode() == key) {
			trystop(e, false);
			toCall.goDo(this);
		}
	}

	public void nativeKeyReleased(NativeKeyEvent e) {
		if(e.getKeyCode() == NativeKeyEvent.VC_CONTROL)
		{
			ctrlDown = false;
			if(stopIncomingCtrlReleases)trystop(e, true);
		}
	}

	@Override
	public void nativeKeyTyped(NativeKeyEvent arg0) {}

	public void actionDone()
	{
		
		stopIncomingCtrlReleases = false;
		if(ctrlDown == false)
		{
			try {
				(new Robot()).keyRelease(NativeKeyEvent.VC_CONTROL);
			} catch (AWTException e) {
				return;
			}
		}
	}
}