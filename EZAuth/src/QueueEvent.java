import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class QueueEvent extends Thread {
	private int id;
	private String eventName;
	private int status; // 0=pending, 1=executing,2=completed
	private HashMap<Integer, String> conversionList = new HashMap<Integer, String>();
	CountDownLatch wait;
	public QueueEvent(String name, int id) {
		this.eventName = name;
		this.id = id;
		this.status = 0;
		this.conversionList.put(0, "Pending");
		this.conversionList.put(1, "Executing");
		this.conversionList.put(2, "Completed");
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
	public void setLatch(CountDownLatch wait) {
		this.wait=wait;
	}
	public void run() {
		
	
	
		while(this.status==0) {
			try {
//				System.out.println("Waiting for Event"+this.id);
				sleep(100);
			} catch (InterruptedException e) {
				System.err.println("What happened?");
			}
		}
		System.out.println("Event: "+this.id+" Executed");
		this.wait.countDown();
	}
	}

