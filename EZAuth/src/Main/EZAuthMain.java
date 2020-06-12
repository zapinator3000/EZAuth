package Main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import javax.swing.Timer;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;

import NetworkConnections.NetworkConnector;

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
	private NetworkConnector network;
	private int serverTicks;
	private final int CHANGE_KEY = 1000;
	private static Key accessKey;
	private UserManager userManager;
	public static int logLevel = 3;
	private boolean exit;
	private static ArrayList<String> helpList = new ArrayList<String>();

	public static void main(String[] args) {
		helpList.add("Help Menu: ");
		helpList.add("create user -> Create a user");
		helpList.add("get password -> Get the password of a user (This will be removed in the future)");
		helpList.add("set loglevel -> Set the log level number");
		helpList.add("login -> Test the login system by logging in");
		helpList.add("change password -> Change the password of a user");
		helpList.add("kill -> Enable killswitch");
		helpList.add("help -> Show this menu");
		new EZAuthMain();
	}

	/*
	 * Create a new instance and show a console
	 * 
	 */
	public EZAuthMain() {
		accessKey = Key.generateKey();
		this.serverTicks = 0;
		this.setTimer(new Timer(10, this));
		this.setExit(false);
		this.accessManager = new AccessManager(accessKey, this.userManager, this, accessKey);
		this.userManager = new UserManager(accessKey, this.accessManager.getCurrentKey(accessKey), this);
		this.accessManager.setUserMgr(this.userManager);
		this.getTimer().start();
		this.setEventHandler(new EventQueueHandler());
		this.getEventHandler().start();
		try {
			this.network = new NetworkConnector(6060, this.accessManager);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		this.network.start();
		Scanner myScan = new Scanner(System.in);
		while (!this.isExit()) {
			System.out.print("\n>> ");
			String nextCmd = myScan.nextLine();
			int id = this.getEventHandler().addEventToQueue("Console Command Input");
			QueueEvent event = this.getEventHandler().getEvent(id);

			if (nextCmd.equals("create user")) {
				event.claimEvent();
				System.out.print("\nUsername: ");
				String username = myScan.nextLine();
				System.out.print("\nPassword: ");
				String password = myScan.nextLine();
				System.out.print("\nEmail: ");
				String email = myScan.nextLine();
				event.startEvent(1000);
				this.userManager.createUser(username, password, email);
				System.out.println("Done!");
			}
			if (nextCmd.equals("get password")) {
				event.claimEvent();
				System.out.print("\nUsername: ");
				String username = myScan.nextLine();
				event.startEvent(1000);

				try {
					System.out.print("\nPassword: " + userManager.getPass(accessKey, username));
				} catch (NullPointerException e) {
					System.err.print("\nUser does not exist!");
				}
			}
			if (nextCmd.equals("set loglevel")) {
				event.claimEvent();
				System.out.print("\nLog Level: ");
				String number = myScan.nextLine();
				logLevel = Integer.parseInt(number);
			}
			if (nextCmd.equals("login")) {
				event.claimEvent();
				System.out.print("\nUsername: ");
				String username = myScan.nextLine();
				System.out.print("\nPassword: ");
				String password = myScan.nextLine();
				this.userManager.login(username, password);
			}
			if (nextCmd.equals("change password")) {
				event.claimEvent();
				System.out.print("Email: ");
				String email = myScan.nextLine();
				System.out.print("\nCurrent Password: ");
				String currentPass = myScan.nextLine();
				System.out.print("\nNew Password: ");
				String newPass = myScan.nextLine();
				String resp = this.userManager.changePassword(email, currentPass, newPass);
				System.out.println(resp);
			}
			if (nextCmd.equals("kill")) {
				event.claimEvent();
				System.out.println("Emergency Killswitch: ");
				System.out.println("This will stop the EZAuth Server immediately, possibly causing issues");
				System.out.println("Only use this in emergencies");
				System.out.print("Are you sure you would like to kill? (y/n): ");
				String ans = myScan.nextLine();
				if (ans.contains("y") || ans.contains("Y")) {
					this.accessManager.setKillMsg("Killed by user in Console");
					this.accessManager.setKillSwitch(2, accessKey);

				}
			}
			if (nextCmd.equals("help")) {
				event.claimEvent();
				for (String helpItem : helpList) {
					System.out.println(helpItem);
				}
			}
			event.setStatus(2); // You must end the event
		}
		this.exit = false;
		logLevel = 1;
		System.out.println(">>>>>> Log Level set to 1");
		System.out.println("View Kill Msg");
		System.out.println("View Kill Status");
		System.out.println("Stop Scanning");
		while (!this.exit) {
			String cmd = myScan.nextLine();
			if (cmd.equals("View Kill Msg")) {
				System.out.println(this.accessManager.getKillMsg());
			} else if (cmd.equals("View Kill Status")) {
				System.out.println(this.accessManager.getKillSwitch());
			} else if (cmd.equals("Stop Scanning")) {
				myScan.close();
				System.out.println("Scanner Ended...");
				this.exit = true;
			} else {

				System.out.println("Server is rejecting normal and empty commands");
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		this.accessManager.update();

		if (this.serverTicks == CHANGE_KEY) {
			int id = this.getEventHandler().addEventToQueue("Rolling Key Update");
			QueueEvent event = this.getEventHandler().getEvent(id);
			event.startEvent(1000);
			this.accessManager.changeKey(accessKey);
			Key key = this.accessManager.getCurrentKey(accessKey);
			this.userManager.reEncrypt(key);
			// System.out.println("Changed Key!\n>>");
			this.serverTicks = 0;
			event.setStatus(2); // Must Cleanup!
		}
		this.serverTicks++;
	}

	

	public EventQueueHandler getEventHandler() {
		return eventHandler;
	}

	public void setEventHandler(EventQueueHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

	public void setKillSwitch(String reason, int level) {
		this.accessManager.setKillMsg(reason);
		this.accessManager.setKillSwitch(level, accessKey);
	}

	public Timer getTimer() {
		return timer;
	}

	public void setTimer(Timer timer) {
		this.timer = timer;
	}

	public void stopSubProcesses() {
		this.eventHandler.newStop();
		this.network.newStop();

	}

	public boolean isExit() {
		return exit;
	}

	public void setExit(boolean exit) {
		this.exit = exit;
	}
}
