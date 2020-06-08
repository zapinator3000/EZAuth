package Main;

import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;
/*
 * Handles user-specific operations, such as user encryption/decryption
 * @Author Zackery Painter
 * 
 */
public class User {
	private Token password;
	private String username;
	private String email;
	private int uniqueId;
	private UserManager myUserManager;

	private final Validator<String> validator = new StringValidator() {
	};
	public User(String user, String password, UserManager manager,String email) {
		this.myUserManager = manager;
		this.password = this.encryptPass(password);
		this.setUsername(user);
		this.email=email;
	}

	public void setPassword(String pass) {
		this.password = this.encryptPass(pass);
	}

	public void setUsername(String user) {
		this.username = user;
	}

	public Token encryptPass(String pass) {
		return Token.generate(this.myUserManager.getCurrentKey(), pass);
	}

	public String decryptPass(Key accessKey, Key keyToDecrypt) {
		if (this.myUserManager.getAccessKey().equals(accessKey)) {
			return validator.validateAndDecrypt(keyToDecrypt, this.password);
		} else {
			if (EZAuthMain.logLevel == 2) {
				System.err.println("Invalid accessKey was given");
			}
			return "INVALID";
		}
	}


	public String getUsername() {
		return this.username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
