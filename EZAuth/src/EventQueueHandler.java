import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.Timer;

import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Validator;

/*
 * Handle events and scheduling
 * This should NOT need to be created besides in the main 
 * @Author Zackery Painter 
 */
public class EventQueueHandler extends Thread implements ActionListener {
	private HashMap<Integer, QueueEvent> eventQueue;
	private int currentEvent;
	private int lastSize;
	private int nextInt;

	/*
	 * Construct a new event handler
	 */
	public EventQueueHandler() {
		this.eventQueue = new HashMap<Integer, QueueEvent>();
		this.nextInt = 0;
		this.currentEvent = 0;
		this.lastSize = this.eventQueue.size();

	}

	public int addEventToQueue(String name) {

		this.eventQueue.put(nextInt, new QueueEvent(name, nextInt));
		// System.out.println("Event Added:" + nextInt);
		this.nextInt++;
		return nextInt - 1;
	}

	public QueueEvent getEvent(int id) {
		return this.eventQueue.get(id);
	}

	public void run() {
		System.out.println("Started Event Queue");
		Timer eventTimer = new Timer(20, this);
		eventTimer.start();
	}

	/*
	 * If the previous event has executed, execute the next event
	 * 
	 * @returns false if the event failed, true if executed
	 */
	public boolean runNext() {
		// TODO Auto-generated method stub
		if (this.eventQueue.containsKey(this.currentEvent)) {
			try {
				if (this.eventQueue.get(this.currentEvent - 1).getStatus() == 1) {
					// System.out.println("Waiting for last event...");
					return false;
				} else {
					System.out.println("Triggering: " + this.currentEvent);
					QueueEvent event = this.eventQueue.get(this.currentEvent);
					event.runEvent();
					this.currentEvent++;
					return true;
				}
			} catch (NullPointerException e) {
				System.out.println("Triggering: " + this.currentEvent);
				QueueEvent event = this.eventQueue.get(this.currentEvent);
				event.runEvent();
				this.currentEvent++;
				return true;
			}
		} else {
			return false;
		}
	}

	/*
	 * Check if the previous event has run
	 * 
	 * @param check
	 * 
	 * @return true if the event was triggered
	 * 
	 */
	public boolean checkRunStatus(int check) {
		if (this.eventQueue.containsKey(check - 1)) {
			if (this.eventQueue.get(check - 1).getStatus() == 1) {
				return false;
			} else {
				this.runNext();
				return true;

			}
		} else {
			this.runNext();
			return true;
		}

	}

	/*
	 * Run the next event on a timer trigger
	 * 
	 * @param e
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		this.runNext();
	}
}
