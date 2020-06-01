import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class QueueEvent {
	private int id;
	private String eventName;
	private int status; // 0=pending, 1=executing,2=completed
	private HashMap<Integer, String> conversionList = new HashMap<Integer, String>();
	private CountDownLatch latch;
	public QueueEvent(String name, int id) {
		this.eventName = name;
		this.setId(id);
		this.status = 0;
		this.conversionList.put(0, "Pending");
		this.conversionList.put(1, "Executing");
		this.conversionList.put(2, "Completed");
	}
	public void setLatch(CountDownLatch latch) {
		this.latch=latch;
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
		this.status=1;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	}
