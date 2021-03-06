package Main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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
	private static final int CANCELLED_MAX = 5;
	private int lastSize;
	private int nextInt;
	private final int CLEANUP_TRIGGER = 3000;
	private int eventCounter;
	private int lastClearedState;
	private boolean exit;
	private static final int TIMEOUT_MAX = 500;
	private int queueTimeout;
	private int cancelledCount;
	private static final int RESET_TRIGGER = 4000;
	private static final int MAX_FAILS = 5;
	private int counter;
	private ArrayList<Integer> eventClientIDs;
	private HashMap<Integer, Integer> ClientFails;

	/*
	 * Construct a new event handler
	 */
	public EventQueueHandler() {
		this.eventQueue = new HashMap<Integer, QueueEvent>();
		this.nextInt = 0;
		this.currentEvent = 0;
		this.lastSize = this.eventQueue.size();
		this.eventCounter = 0;
		this.lastClearedState = 0;
		this.exit = false;
		this.queueTimeout = 0;
		this.counter = 0;
		this.eventClientIDs = new ArrayList<Integer>();
		this.ClientFails = new HashMap<Integer, Integer>();

	}

	/*
	 * Add an event to the queue
	 */
	public int addEventToQueue(String name, int caller) {

		this.eventQueue.put(nextInt, new QueueEvent(name, nextInt, TIMEOUT_MAX, this, caller));
		// System.out.println("Event Added:" + nextInt);
		this.nextInt++;
		return nextInt - 1;
	}

	/*
	 * Get the event
	 */
	public QueueEvent getEvent(int id) {
		return this.eventQueue.get(id);
	}

	/*
	 * Get an ID to create an Event
	 * 
	 * @return integer
	 */
	public int getEventID() {
		Random rdm = new Random();
		int randEvent = rdm.nextInt();
		this.eventClientIDs.add(randEvent);
		this.ClientFails.put(randEvent, 0);
		return randEvent;
	}

	/*
	 * See if the event ID exists
	 */
	public boolean checkEventID(int eventID) {
		if (this.eventClientIDs.contains(eventID)) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * Start the subprocess
	 */
	public void run() {
		System.out.println("Started Event Queue");
		Timer eventTimer = new Timer(20, this);
		eventTimer.start();

		while (!this.exit) {
			try {
				Thread.sleep(10);
				for (int id : this.eventQueue.keySet()) {
					int stat = this.eventQueue.get(id).getStatus();
					if (stat < 2 && stat != -1) {
						this.eventQueue.get(id).updateInactiveEvent();
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.err.println(">>>>>> Event Handler has been stopped");
		eventTimer.stop();
		for (int eventId : this.eventQueue.keySet()) {
			System.err.println(">>>>>> Canceling Event: " + eventId);
			this.eventQueue.get(eventId).setStatus(-1);
		}

	}

	/*
	 * If the previous event has executed, execute the next event
	 * 
	 * @returns false if the event failed, true if executed
	 */
	public boolean runNext(boolean override) {
		// TODO Auto-generated method stub
		if (override) {
			if (EZAuthMain.logLevel == 1) {
				System.out.println("Elevated Trigger: " + this.currentEvent);
			}
			QueueEvent event = this.eventQueue.get(this.currentEvent);
			event.runEvent();
			this.currentEvent++;
		} else {
			if (this.eventQueue.containsKey(this.currentEvent)) {
				try {

					if (this.eventQueue.get(this.currentEvent - 1).getStatus() == 1) {
						// System.out.println("Waiting for last event...");
						if (this.queueTimeout == this.eventQueue.get(this.currentEvent - 1).getTimeout()) {
							System.out.println(
									"Event: " + (this.currentEvent - 1) + " has stalled in the queue, cancelling");
							this.eventQueue.get(this.currentEvent - 1).setStatus(-1);
							this.queueTimeout = 0;
							this.cancelledCount++;
							QueueEvent prevEvent = this.eventQueue.get(this.currentEvent - 1);
							int fails = ClientFails.get(prevEvent.getCaller()) + 1;

							this.ClientFails.put(prevEvent.getCaller(), fails);
							if (fails == MAX_FAILS) {
								this.eventClientIDs.remove(this.eventClientIDs.indexOf(prevEvent.getCaller()));
								System.out.println(
										"Event Queue: ERROR: Too many fails from this client, removing client from Authorized list: "
												+ prevEvent.getCaller());
							}
						} else {
							this.queueTimeout++;
						}
						return false;
					} else {
						if (EZAuthMain.logLevel == 1) {
							System.out.println("Triggering: " + this.currentEvent + " : "
									+ this.eventQueue.get(this.currentEvent).getEventName());
						}
						QueueEvent event = this.eventQueue.get(this.currentEvent);
						if (!event.isActive()) {
							if (event.getInactiveTimeoutCounter() == event.getTimeout() / 4) {

								int fails = ClientFails.get(event.getCaller()) + 1;

								this.ClientFails.put(event.getCaller(), fails);
								if (fails == MAX_FAILS) {
									this.eventClientIDs.remove(this.eventClientIDs.indexOf(event.getCaller()));
									System.out.println(
											"Event Queue: ERROR: Too many fails from this client, removing client from Authorized list: "
													+ event.getCaller());
								}
								System.out.println("Event Queue: Warn: Not running Event because it wasn't claimed "
										+ event.getId());
								event.setStatus(-1);
								this.currentEvent++;
							} else {
								event.setInactiveTimeoutCounter(event.getInactiveTimeoutCounter() + 1);
							}

						} else {
							if (this.eventClientIDs.contains(event.getCaller())) {
								event.runEvent();
							} else {
								System.out.println("EventQueue: Error: Event Client not registered");
								event.setStatus(-1);
							}
							this.currentEvent++;
						}
						return true;
					}
				} catch (NullPointerException e) {
					if (EZAuthMain.logLevel == 1) {
						System.out.println("Triggering: " + this.currentEvent);
					}
					QueueEvent event = this.eventQueue.get(this.currentEvent);

					if (this.eventClientIDs.contains(event.getCaller())) {
						event.runEvent();
					} else {
						System.out.println("EventQueue: Error: Event Client not registered");
						event.setStatus(-1);
					}
					this.currentEvent++;
					return true;
				}
			} else {
				return false;
			}
		}
		return false;
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
				this.runNext(false);
				return true;

			}
		} else {
			this.runNext(false);
			return true;
		}

	}

	/*
	 * Run the next event on a timer trigger And cleanup old events on a scheduled
	 * routine
	 * 
	 * @param e
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (this.eventCounter == CLEANUP_TRIGGER) {
			if (EZAuthMain.logLevel == 2) {
				System.out.println("Running Cleanup on old events");
			}
			this.eventCounter = 0;
			if (this.eventQueue.size() > 10) {
				if (EZAuthMain.logLevel < 2) {
					System.out.println("Cleaning old events");
				}
				for (int i = this.lastClearedState; i < this.currentEvent; i++) {
					// System.out.println("Removing: " + i);
					if (this.eventQueue.get(i).getStatus() != 0) {
						this.eventQueue.remove(i);
					}
				}

				this.lastClearedState = this.currentEvent;
			} else {
				if (EZAuthMain.logLevel < 2) {
					System.out.println("Skipped Cleaning old events because the total events is less than 10");
				}
			}
		} else {
			this.eventCounter++;
		}
		if (this.cancelledCount == CANCELLED_MAX) {
			System.out.println("EVENT QUEUE: WARN: Too many cancelled events, resetting Event counters...");
			this.nextInt = this.currentEvent;
			this.cancelledCount = 0;
		} else if (this.counter == RESET_TRIGGER) {

		} else {

		}
		this.runNext(false);
	}

	/*
	 * Elevate Trigger
	 */
	public void EvelatedTrigger() {

		this.runNext(true);
	}

	/*
	 * Stop the event
	 */
	public void newStop() {
		this.exit = true;
	}

	/*
	 * Timeout the event
	 */
	public void timeoutEvent(int id) {
		this.eventQueue.get(id).setStatus(-1);
		this.cancelledCount++;
	}
}
