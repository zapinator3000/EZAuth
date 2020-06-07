package Main;

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
	private EventQueueHelper eventHelper;
	private int serverTicks;
	private final int CHANGE_KEY = 1000;
	private static Key accessKey;
	private UserManager userManager;
	public static int logLevel=3;
	//public static void main(String[] args) {

//		new EZAuthMain();
//	}

	/*
	 * Create a new instance and show a console
	 * 
	 */
	public EZAuthMain() {
		accessKey = Key.generateKey();
		this.serverTicks = 0;
		this.timer = new Timer(10, this);
		this.accessManager = new AccessManager(accessKey,this.userManager);
		this.userManager = new UserManager(accessKey, this.accessManager.getCurrentKey(accessKey));
		this.timer.start();
		this.eventHandler = new EventQueueHandler();
		this.eventHandler.start();
		Scanner myScan = new Scanner(System.in);
		while (true) {
			System.out.println(">>");
			String nextCmd = myScan.nextLine();
			int id = this.eventHandler.addEventToQueue("User Input");
			QueueEvent event = this.eventHandler.getEvent(id);

			if (nextCmd.equals("Create User")) {
				System.out.println("Username: ");
				String username = myScan.nextLine();
				System.out.println("Password: ");
				String password = myScan.nextLine();
				this.startEvent(event);
				this.userManager.createUser(username, password);
				System.out.println("Done!");
			}
			if (nextCmd.equals("Get Password")) {
				System.out.println("Username: ");
				String username = myScan.nextLine();
				this.startEvent(event);

				try {
					System.out.println("Password: " + userManager.getPass(accessKey, username));
				} catch (NullPointerException e) {
					System.err.println("User does not exist!");
				}
			}
			if (nextCmd.equals("Set LogLevel")) {
				System.out.println("Log Level: ");
				String number = myScan.nextLine();
				logLevel=Integer.parseInt(number);
			}
			if(nextCmd.equals("Login")) {
				System.out.println("Username: ");
				String username = myScan.nextLine();
				System.out.println("Password: ");
				String password = myScan.nextLine();
				this.userManager.login(username, password);
			}
			
			event.setStatus(2); //You must end the event
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		this.accessManager.update();

		if (this.serverTicks == CHANGE_KEY) {
			int id = this.eventHandler.addEventToQueue("RollingKey");
			QueueEvent event = this.eventHandler.getEvent(id);
			this.startEvent(event);
			this.accessManager.changeKey(accessKey);
			Key key = this.accessManager.getCurrentKey(accessKey);
			this.userManager.reEncrypt(key);
			// System.out.println("Changed Key!\n>>");
			this.serverTicks = 0;
			event.setStatus(2); // Must Cleanup!
		}
		this.serverTicks++;
	}

	public void startEvent(QueueEvent event) {
		CountDownLatch latch = new CountDownLatch(1);
		EventQueueHelper helper = new EventQueueHelper(this.eventHandler, event, latch);
		helper.start();
		try {
			while (!this.eventHandler.checkRunStatus(event.getId()))
				;
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
