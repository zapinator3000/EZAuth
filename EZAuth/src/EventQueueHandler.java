import java.util.ArrayList;

import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Validator;

public class EventQueueHandler {
	private ArrayList<QueueEvent> eventQueue;
	private int currentEvent;
	private int lastSize;
	public EventQueueHandler() {
		this.eventQueue = new ArrayList<QueueEvent>();
		int id1=this.addEventToQueue("Dummy");
		int id2=this.addEventToQueue("Dummy2");
		this.getEvent(id1).setStatus(2);
		this.getEvent(id2).setStatus(2);
		this.currentEvent=2;
		this.lastSize=this.eventQueue.size();
		
	}

	public int addEventToQueue(String name) {
		int id = this.eventQueue.size() + 1;

		this.eventQueue.add(new QueueEvent(name, id));
		//System.out.println(this.eventQueue.size());
		return id;
	}

	public QueueEvent getEvent(int id) {
		return this.eventQueue.get(id-1);
	}

	public void clockCycle() {
		//System.out.println("Clock Cycle");

				if (this.lastSize!=this.eventQueue.size()) {
					System.out.println("Executing: "+this.currentEvent);
					QueueEvent event = this.eventQueue.get(this.currentEvent-1);
					if (event.getStatus() == 0) {
						event.setStatus(1);
					}
					this.lastSize=this.eventQueue.size();
					this.currentEvent++;
				}else {
					//System.out.println(this.eventQueue.size());
				}
			}
		} 
	

