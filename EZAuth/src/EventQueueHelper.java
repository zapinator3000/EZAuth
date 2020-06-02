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

	public EventQueueHelper(EventQueueHandler eventQueue, QueueEvent event, CountDownLatch latch) {
		this.eventQueue = eventQueue;
		this.latch = latch;
		this.event = event;
	}

	public void run() {
		while (this.event.getStatus() == 0) {
			try {
				sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		;
		this.latch.countDown();
		if (EZAuthMain.logLevel == 1) {
			System.out.println("Executing: " + this.event.getId());
		}
	}
}
