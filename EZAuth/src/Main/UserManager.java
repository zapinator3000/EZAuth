package Main;

import java.util.ArrayList;



import com.macasaet.fernet.Key;
import com.macasaet.fernet.TokenValidationException;

/*
 * Manages User logins and encryption. Handles Re-encryption methods provided
 * rolling keys
 * @Author Zackery Painter
 */
public class UserManager {
	private Key currentKey;
	private ArrayList<User> users;
	private Key accessKey;
	private Key previousKey;

	public UserManager(Key accessKey, Key currentKey) {
		// TODO Auto-generated constructor stub
		this.setAccessKey(accessKey);
		this.users = new ArrayList<User>();
		this.currentKey = currentKey;
		this.previousKey = currentKey;
	}

	public boolean login(String username, String password) {

		String decryptedPass=null;
		try {
			 decryptedPass=this.searchForUser(username).decryptPass(accessKey,this.currentKey);
		}catch(TokenValidationException e) {
			try {
			 decryptedPass=this.searchForUser(username).decryptPass(accessKey,this.previousKey);
			}catch(TokenValidationException exp){
				System.err.println("FATAL ERROR: An invalid key was produced! Someone's playing dirty!");
				return false;
			}
			
		}
		if(decryptedPass.equals(password)) {
			//Log stuff here
			System.out.println("Logged in!");
			return true;
		}else {
			System.err.println("Password was not correct");
			return false;
		}
		
	}

	public boolean createUser(String username, String password) {
		for(User user: this.users) {
			if(user.getUsername()==username) {
				return false;
			}
		}
		this.users.add(new User(username, password, this));
		return true;

	}

	public boolean reEncrypt(Key newKey) {
		if (EZAuthMain.logLevel == 0) {
			System.out.println("Running re-encrypt");
		}
		String p;
		for (User user : users) {
			try {
				p = user.decryptPass(this.accessKey, this.currentKey);
			} catch (TokenValidationException e) {
				if (EZAuthMain.logLevel < 2) {
					System.err.println("Mis-match keys detected!");
					System.err.println("This is probably caused by a timing mis-match");
					System.err.println("most likely a hanging event");
				}
				p = user.decryptPass(this.accessKey, this.previousKey);
			}
			// System.out.println("Password: "+p);
			this.previousKey = this.currentKey;
			this.currentKey = newKey;
			/*
			 * Wait a few seconds so that new things
			 */
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			user.setPassword(p);
		}
		return true;
	}

	public Key getCurrentKey() {
		return currentKey;
	}

	public void setCurrentKey(Key currentKey) {
		this.currentKey = currentKey;
	}

	public Key getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(Key accessKey) {
		this.accessKey = accessKey;
	}

	public String getPass(Key accessKey, String user) {
		if (accessKey.equals(this.accessKey)) {
			User us = searchForUser(user);
			return us.decryptPass(this.accessKey, this.currentKey);
		} else {
			System.out.println("Fail");
			return "Fail";
		}
	}

	public User searchForUser(String username) {
		for (User user : users) {
			// System.out.println(user.getUsername());

			if (user.getUsername().equals(username)) {
				return user;
			}
		}
		// System.out.println("Fail");
		return null;
	}
}