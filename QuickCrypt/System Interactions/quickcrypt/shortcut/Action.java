package quickcrypt.shortcut;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import quickcrypt.core.Context;
import quickcrypt.core.QCError;

/**
 * Handles multithreaded access to ClipboardCoder and clipboard and allows
 * ClipboardCoder.code() to be called without waiting for it to complete.
 * 
 * @author Adam
 */

public class Action extends Thread {
	private volatile Lock mainLock; //lock on the context
	private volatile Context context; //Context object linked to this
	private volatile ClipboardCoder clippy; //ClipboardCoder object linked to this
	private volatile boolean isDoing; //Is an action being performed
	private ActionReturn returnie = null; //who fired the latest action and who will be notified upon completion

	/**
	 * Creates an Action based on a ClipboardCoder who is in turn based on a
	 * Context
	 * 
	 * @param clipboard
	 *            ClipboardCoder to be used
	 */
	public Action(ClipboardCoder clipboard) {
		
		clippy = clipboard;
		context = clippy.context;

		isDoing = false;
		mainLock = new ReentrantLock();

		start();
	}

	/**
	 * Lock the Context and ClipboardCoder
	 */
	public synchronized void lock() {
		mainLock.lock();
	}

	/**
	 * Try to Lock the Context and ClipboardCoder
	 */
	public synchronized void tryLock() {
		mainLock.tryLock();
	}

	/**
	 * Unlock the Context and ClipboardCoder
	 */
	public synchronized void unlock() {
		mainLock.unlock();
	}

	/**
	 * Get the context
	 * 
	 * @return context
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * 
	 * @return weather or not Action is undergoing ClipboardCoder.code()
	 */
	public synchronized boolean isDoing() {
		return isDoing;
	}

	/**
	 * Tell thread to start ClipboardCoder.code() if not already doing it.
	 * 
	 * @param ret
	 *            object to call actionDone() on when the action is complete,
	 *            can be null
	 */
	public synchronized void goDo(ActionReturn ret) {
		if (!isDoing) {
			isDoing = true;
			returnie = ret;
			notify();
		}
	}

	/**
	 * Runs while the thread is running, controls the thread and any actions it
	 * is doing
	 */
	public synchronized void run() {

		while (true) { //loop forever

			//wait for notify()
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("Action Thread Interupted.");
			}

			//do it
			if (isDoing) {
				lock(); //lock context and ClipboardCoder
				doClippyfunc();
				unlock();
				isDoing = false; //reset

				//report that the actin has been completed
				if (returnie != null)
					returnie.actionDone();
			}
		}
	}

	/**
	 * Actually performs ClipboardCoder.code() along with copying and pasting
	 * from the open program
	 */
	private void doClippyfunc() {
		Robot robot;
		try {
			robot = new Robot();
		} catch (AWTException e1) {
			System.out.println("Failed to generate key pressing robot");
			return;
		}

		clippy.push();

		//copy from focused program
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress('C');
		robot.keyRelease('C');
		robot.waitForIdle();
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.delay(100); // wait for program to relinquish clipboard

		try {
			clippy.code();
		} catch (QCError e) {
			//manage errors
			clippy.pop();
			System.err.println("Error: " + e.getMessage());
			return;
		}

		//paste to focused program
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress('V');
		robot.keyRelease('V');
		robot.waitForIdle();
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.delay(100); // wait for program to relinquish clipboard

		//clippy.pop();
	}
}

/**
 * simple interface for any file looking to get a notification after the action is complete
 * @author Adam Spiegel
 */
interface ActionReturn {
	void actionDone();
}
