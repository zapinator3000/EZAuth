
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import javax.swing.Timer;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;

/*
 * Handles event queues and give a console interface
 * Handles Safe Access times to prevent multiple edits at once or corruption
 * Handles all basic in and out operations.
 * Generates program clock
 * @Author Zackery Painter
 */
public class EZAuthMain implements ActionListener {
	private Timer timer;
	private AccessManager accessManager;
	private EventQueueHandler eventHandler;
	private int serverTicks;
	private int eventCounter;
	private final int CHANGE_KEY = 1000;
	private final int EVENT_FREQ = 100;
	private static Key accessKey;
	private UserManager userManager;
	private ArrayList<QueueEvent> eventQueue;

	public static void main(String[] args) {

		new EZAuthMain();
	}

	public EZAuthMain() {
		accessKey = Key.generateKey();
		this.serverTicks = 0;
		this.timer = new Timer(10, this);
		this.accessManager = new AccessManager(accessKey);
		this.userManager = new UserManager(accessKey, this.accessManager.getCurrentKey(accessKey));
		this.timer.start();
		this.eventHandler = new EventQueueHandler();
		this.eventCounter = 0;
		Scanner myScan = new Scanner(System.in);
		while (true) {
			System.out.println(">>");
			String nextCmd = myScan.nextLine();
			int id = this.eventHandler.addEventToQueue("User Input");
			CountDownLatch latch = new CountDownLatch(1);
			this.eventHandler.getEvent(id).setLatch(latch);
			this.eventHandler.getEvent(id).start();
			try {
				latch.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (nextCmd.equals("Create User")) {
				System.out.println("Username: ");
				String username = myScan.nextLine();
				System.out.println("Password: ");
				String password = myScan.nextLine();
				userManager.createUser(username, password);
				System.out.println("Done!");
			}
			if (nextCmd.equals("Get Password")) {
				System.out.println("Username: ");
				String username = myScan.nextLine();
				try {
					System.out.println("Password: " + userManager.getPass(accessKey, username));
				} catch (NullPointerException e) {
					System.err.println("User does not exist!");
				}
			}

		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		this.accessManager.update();
		if (this.eventCounter == EVENT_FREQ) {
			// System.out.println("TICK");
			this.eventHandler.clockCycle();
			this.eventCounter = 0;
		} else {
			this.eventCounter++;
		}
		if (this.serverTicks == CHANGE_KEY) {
			int id = this.eventHandler.addEventToQueue("RollingKey");
			CountDownLatch latch = new CountDownLatch(0);
			this.eventHandler.getEvent(id).setLatch(latch);
			this.eventHandler.getEvent(id).start();
			try {
				latch.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.accessManager.changeKey(accessKey);
			Key key = this.accessManager.getCurrentKey(accessKey);
			this.userManager.reEncrypt(key);
			// System.out.println("Changed Key!\n>>");
			this.serverTicks = 0;
		}
		this.serverTicks++;
	}

}
