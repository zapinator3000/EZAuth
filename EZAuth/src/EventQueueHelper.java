import java.util.concurrent.CountDownLatch;

public class EventQueueHelper extends Thread {
private EventQueueHandler eventQueue;
private QueueEvent event;
private CountDownLatch latch;
public EventQueueHelper(EventQueueHandler eventQueue,QueueEvent event,CountDownLatch latch) {
	this.eventQueue=eventQueue;
	this.latch=latch;
	this.event=event;
}

public void run() {
	while(this.event.getStatus()==0) {
		try {
			sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	};
	this.latch.countDown();
	System.out.println("Executing: "+this.event.getId());
}
}
