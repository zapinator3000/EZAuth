package Main;

import java.util.concurrent.CountDownLatch;

/*
 * Helps the event handler by starting a new thread to wait for the event.
 * Otherwise, the event will cause the event queue thread to hang.
 * 
 * @Author Zackery Painter
 */
public class EventQueueHelper extends Thread {
	private EventQueueHandler eventQueue;
	private QueueEvent event;
	private CountDownLatch latch;
	private int expireCounter;
	public EventQueueHelper(EventQueueHandler eventQueue, QueueEvent event, CountDownLatch latch) {
		this.setEventQueue(eventQueue);
		this.latch = latch;
		this.event = event;
		this.expireCounter=0;
	}

	public void run() {
		
	}
	public void cancelEvent() {
		this.event.setStatus(-1);
	}

	public EventQueueHandler getEventQueue() {
		return eventQueue;
	}

	public void setEventQueue(EventQueueHandler eventQueue) {
		this.eventQueue = eventQueue;
	}
}
