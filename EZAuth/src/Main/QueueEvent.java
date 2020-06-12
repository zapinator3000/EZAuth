package Main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/*
 * This is an event that is executed by the handler
 * The event must be marked completed for the next event to execute;
 * @Author Zackery Painter
 */
public class QueueEvent extends Thread {
	private int id;
	private String eventName;
	private int status; // 0=pending, 1=executing,2=completed
	private HashMap<Integer, String> conversionList = new HashMap<Integer, String>();
	private CountDownLatch latch;
	private int timeout;
	private boolean exit;
	private EventQueueHandler eventQueue;
	private int expireCounter;
	private int inactiveTimeoutCounter;
	private boolean active;

	/*
	 * Construct a new Event
	 * 
	 * @param name
	 * 
	 * @param id
	 */
	public QueueEvent(String name, int id, int t, EventQueueHandler eventQueue) {
		this.setEventName(name);
		this.setId(id);
		this.status = 0;
		this.conversionList.put(0, "Pending");
		this.conversionList.put(1, "Executing");
		this.conversionList.put(2, "Completed");
		this.conversionList.put(-1, "Canceled");
		this.setTimeout(t);
		this.exit = false;
		this.eventQueue = eventQueue;
		this.setInactiveTimeoutCounter(0);
		this.setActive(false);
		if (EZAuthMain.logLevel < 2) {
			System.out.println("Event created " + this.id);
		}
	}

	public void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}

	public void setStatus(int stat) {
		this.status = stat;
	}

	public int getStatus() {
		return this.status;
	}

	public void interpretStatCode(int stat) {

		System.out.println("Current Status: " + this.conversionList.get(stat));

	}

	public void runEvent() {
		this.status = 1;
	}

	@Override
	public long getId() {
		return id;
	}

	public void run() {
		while (this.getStatus() == 0) {
			try {
				sleep(10);
				if (this.expireCounter == this.getTimeout()*2) {
					if (EZAuthMain.logLevel < 2) {
						System.out.println("Event has timed out ID: " + this.getId() + " Name: " + this.getEventName());
					}
					this.expireCounter=0;
					this.eventQueue.timeoutEvent((int) this.getId());
				} else {
					this.expireCounter++;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		;
		this.latch.countDown();
		if (EZAuthMain.logLevel == 1) {
			System.out.println("Executing: " + this.getId() + " : " + this.getEventName());
		}
	}

	public void startEvent(int timeoutMax) {
		this.latch = new CountDownLatch(1);
		this.start();
		int timeout = 0;
		this.setActive(true);
		if (EZAuthMain.logLevel < 2) {
			System.out.println("Event claimed: " + this.id);
		}
		try {
			while (!this.eventQueue.checkRunStatus(this.id)) {
				Thread.sleep(10);
				if (this.exit) {
					break;
				}
				if (timeout == timeoutMax) {
					System.out.println("\nPrevious event has not expired in the correct time, cancelling Event ID: "
							+ (this.getId() - 1) + " with name: "
							+ this.eventQueue.getEvent((int) this.getId() - 1).getEventName());
					this.eventQueue.timeoutEvent((int) this.getId() - 1);
				} else {
					timeout++;
				}
			}

			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void cancelEvent() {
		this.setStatus(-1);
	}

	public void updateInactiveEvent() {
		if (!this.isActive()) {
//			System.out.println("Update event");
			if (this.getInactiveTimeoutCounter() == this.timeout / 2) {
				this.setStatus(-1);
				System.out.println("Event was never claimed before timeout, cancelling event ID: " + this.id + " Name: "
						+ this.eventName);

			} else {
				this.setInactiveTimeoutCounter(this.getInactiveTimeoutCounter() + 1);
			}
		}
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getInactiveTimeoutCounter() {
		return inactiveTimeoutCounter;
	}

	public void setInactiveTimeoutCounter(int inactiveTimeoutCounter) {
		this.inactiveTimeoutCounter = inactiveTimeoutCounter;
	}
	public void claimEvent() {
		this.active=true;
	}
}
