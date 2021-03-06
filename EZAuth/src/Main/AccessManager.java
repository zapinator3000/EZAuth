package Main;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
 * This class handles external Access, including rolling keys
 * This also parses JSON requests (internal and external)
 * @Author Zackery Painter
 * 
 */
public class AccessManager {
	private final Validator<String> validator = new StringValidator() {
	};
	public static int EXPIRATION_COUNT = 30000;
	private long serverTicks;
	private HashMap<Key, Long> gameKeys;
	private Key currentKey;
	private Key finalKey;
	private JSONObject response;
	private HashMap<String, PrivateKey> privateKeys;
	private HashMap<String, PublicKey> publicKeys;
	private UserManager userManager;
	private HashMap<String, Key> decryptKeys;
	private EZAuthMain ezauthMain;
	private int killSwitch;
	private String killMsg;
	private JFrame frame;
	private Key accessKey;
	private int eventClientID;

	public AccessManager(Key accessKey, UserManager usrmgr, EZAuthMain ez, Key aKey) {
		this.finalKey = accessKey;
		this.setServerTicks((long) 0);
		this.gameKeys = new HashMap<Key, Long>();
		this.setCurrentKey(Key.generateKey());
		this.userManager = usrmgr;
		this.decryptKeys = new HashMap<String, Key>();
		this.publicKeys = new HashMap<String, PublicKey>();
		this.privateKeys = new HashMap<String, PrivateKey>();
		this.ezauthMain = ez;
		this.frame = new JFrame();
		this.frame.pack();
		this.frame.setVisible(false);
		this.killSwitch = 0;
		this.accessKey = aKey;
		this.setKillMsg("Unknown Reason");
	}

	/*
	 * Fix Null Pointer by forcing ID to be generated after EZAuth init
	 */
	public void finishInit() {
		this.eventClientID = this.ezauthMain.getEventHandler().getEventID();
	}

	/*
	 * Update the ticks
	 */
	public void update() {
		this.setServerTicks((long) (this.getServerTicks() + 1));
		if (this.killSwitch != 0) {
			System.err.println("\n>>>>>>> KILLSWITCH: Killswitch is not Zero! <<<<<<<<<<<<<");
			System.err.println(">> Start Forced Shutdown...");
			this.ezauthMain.getTimer().stop(); // Stop execution
			System.err.println(">>>> Server Timer Stopped...");
			this.ezauthMain.stopSubProcesses();
			System.err.println(">>>> Threads Ended...");
			System.err.println(">>>> Moving Main Thread to Recovery...");
			this.ezauthMain.setExit(true);
			System.err.println(">> Main Thread moved into recovery...");
			if (this.killSwitch == 1) {
				System.err.println(">> Forced Shutdown Completed... ");
				JOptionPane.showMessageDialog(this.frame,
						"The Access Manager has detected a security issue and will kill the server to prevent further damage.\n Reported Reason: "
								+ this.getKillMsg(),
						"Killswitch enabled", JOptionPane.WARNING_MESSAGE);
			} else if (this.killSwitch == 2) {
				System.err.println(">> Forced Shutdown Completed, Quiet mode enabled...");
				System.err.println("Quietly killing for the reason: " + this.getKillMsg() + "...");
			} else {
				System.err.println("KillSwitch is in invalid state, resetting to 0");
				this.killSwitch = 0;
			}
			System.exit(1);

		}
	}

	/*
	 * If the given key does not exist, return false If the key is expired, reject
	 * the key and return false If the key is valid return true and accept it
	 * 
	 * @param key
	 */
	public boolean checkGameValidity(Key key) {
		if (gameKeys.containsKey(key)) {
			long creationTime = gameKeys.get(key);
			long offset = EXPIRATION_COUNT + creationTime;
			if (offset > getServerTicks()) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/*
	 * Generate a new game key
	 * 
	 * @return key
	 */
	public Key generateKey() {
		Key key = Key.generateKey();
		this.gameKeys.put(key, getServerTicks());
		return key;
	}

	/*
	 * get the current server ticks
	 * 
	 * @return serverTIcks
	 */
	public long getServerTicks() {
		return serverTicks;
	}

	/*
	 * set server ticks
	 * 
	 * @param serverTicks
	 */
	public void setServerTicks(Long serverTicks) {
		this.serverTicks = serverTicks;
	}

	/*
	 * Change the access key
	 */
	public void changeKey(Key accessKey) {
		if (!accessKey.equals(this.finalKey)) {
			System.err.println("Correct key not provided!");
		} else {
			this.setCurrentKey(Key.generateKey());
		}
	}

	/*
	 * get the current accessKey
	 */
	public Key getCurrentKey(Key accessKey) {
		if (accessKey.equals(this.finalKey)) {
			return currentKey;
		} else {
			return Key.generateKey();
		}
	}

	/*
	 * Set the current Key
	 */
	public void setCurrentKey(Key currentKey) {
		this.currentKey = currentKey;
	}

	/*
	 * Execute the JSON command and return with the response
	 * 
	 * @param command
	 * 
	 * @return a JSON response
	 */
	public String executeJSON(String command, String ip) throws ParseException {
		Object Ob = new JSONParser().parse(command);
		JSONObject out = new JSONObject();
		JSONObject jsonOb = (JSONObject) Ob;
		String executeCommand = (String) jsonOb.get("Request");
		String encOut = null;
		int clientID = this.ezauthMain.getEventHandler().getEventID();
		int id = this.ezauthMain.getEventHandler().addEventToQueue("User Input", clientID);
		QueueEvent event = this.ezauthMain.getEventHandler().getEvent(id);
		event.startEvent(1000);
		if (executeCommand.equals("LOGIN")) {
			Key key = new Key((String) jsonOb.get("ACCESS_KEY"));
			String username = (String) jsonOb.get("USERNAME");
			String password = (String) jsonOb.get("PASSWORD");
			if (this.checkGameValidity(key)) {
				String tryLogin = this.userManager.login(username, password);
				if (tryLogin.equals("SUCCESS")) {
					out.put("RESPONSE", "200");
				} else if (tryLogin.equals("PASSWORD_FAIL")) {
					out.put("RESPONSE", "1001");
					out.put("MESSAGE", "Your password is incorrect");
				} else if (tryLogin.equals("USER_DOESNT_EXIST")) {
					out.put("RESPONSE", "1002");
					out.put("MESSAGE", "The user requested does not exist");
				} else if (tryLogin.equals("INVALID_KEY")) {
					out.put("RESPONSE", "500");
					out.put("MESSAGE", "A Key Mis-match occurred internally");
				}
			} else {
				out.put("RESPONSE", "401");
				out.put("MESSAGE", "Your key is invalid or expired");
			}
			encOut = this.encrypt(ip, JSONObject.toJSONString(out));
		} else if (executeCommand.equals("CREATE_USER")) {
			Key key = new Key((String) jsonOb.get("ACCESS_KEY"));
			String username = (String) jsonOb.get("USERNAME");
			String password = (String) jsonOb.get("PASSWORD");
			String email = (String) jsonOb.get("EMAIL");

			if (this.checkGameValidity(key)) {
				if (this.userManager.createUser(username, password, email)) {
					out.put("RESPONSE", "200");

				} else {
					out.put("RESPONSE", "1000");
					out.put("MESSAGE", "The user already exists");
				}
			} else {
				out.put("RESPONSE", "401");
				out.put("MESSAGE", "Your key is invalid or expired");
			}
			encOut = this.encrypt(ip, JSONObject.toJSONString(out));
		} else if (executeCommand.equals("NEW_CONNECTION")) {
			String pubKey = (String) jsonOb.get("PUBLIC_KEY");
			KeyFactory factory = null;
			try {
				factory = KeyFactory.getInstance("RSA");
			} catch (NoSuchAlgorithmException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			byte[] byteKey = Base64.getDecoder().decode(pubKey);
			try {
				KeyFactory keyFact = KeyFactory.getInstance("RSA");
			} catch (NoSuchAlgorithmException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			PublicKey clientPub = null;
			try {
				clientPub = factory.generatePublic(new X509EncodedKeySpec(byteKey));
			} catch (InvalidKeySpecException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			KeyPair keys = null;
			try {
				keys = this.GenerateConnectionKey();
				// System.out.println("Generated KeyPair");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// System.out.println(clientPub);
			this.publicKeys.put(ip, clientPub);
//			this.privateKeys.put(ip, keys.getPrivate());
			Key decryptionKey = this.generateKey();
			this.decryptKeys.put(ip, decryptionKey);
			out.put("RESPONSE", "200");
			out.put("ACCESS_KEY", this.generateKey().serialise());
			out.put("DECRYPTION_KEY", decryptionKey.serialise());
			try {
				encOut = this.doEncryptRSA(JSONObject.toJSONString(out), ip);
			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (executeCommand.equals("PING")) {
			Key key = new Key((String) jsonOb.get("ACCESS_KEY"));
			if (this.checkGameValidity(key)) {
				out.put("RESPONSE", "200");
			} else {
				out.put("RESPONSE", "401");
				out.put("MESSAGE", "Your Key is invalid or expired");
			}
			encOut = this.encrypt(ip, JSONObject.toJSONString(out));
		} else if (executeCommand.equals("CHANGE_PASSWORD")) {
			Key key = new Key((String) jsonOb.get("ACCESS_KEY"));
			if (this.checkGameValidity(key)) {
				String email = (String) jsonOb.get("EMAIL");
				String newPass = (String) jsonOb.get("NEW_PASSWORD");
				String oldPassTest = (String) jsonOb.get("OLD_PASS");
				String res = this.userManager.changePassword(email, oldPassTest, newPass);
				if (res.equals("SUCCESS")) {
					out.put("RESPONSE", "200");
				} else {
					out.put("RESPONSE", "1003");
					out.put("MESSAGE", "The old password was incorrect");
				}
			} else {
				out.put("RESPONSE", "401");
				out.put("MESSAGE", "Your key is invalid or expired");
			}
			encOut = this.encrypt(ip, JSONObject.toJSONString(out));
		} else {

			out.put("RESPONSE", "501");
			out.put("MESSAGE", "An invalid call was made (Unimplemented)");
		}
		event.setStatus(2);
		return encOut;

	}

	/*
	 * Encrypt RSA Based on https://niels.nu/blog/2016/java-rsa.html
	 * 
	 * @param plainText
	 * 
	 * @param user
	 * 
	 * @return String
	 */
	public String doEncryptRSA(String plainText, String ip)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		Cipher encryptCipher = Cipher.getInstance("RSA");

		encryptCipher.init(Cipher.ENCRYPT_MODE, this.publicKeys.get(ip));
		byte[] cipherText = null;
		try {
			cipherText = encryptCipher.doFinal(plainText.getBytes());
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Base64.getEncoder().encodeToString(cipherText);
	}

	/*
	 * Decrypt RSA Based on: https://niels.nu/blog/2016/java-rsa.html
	 * 
	 * @param cipherText
	 * 
	 * @param user
	 * 
	 * @return string
	 */
	public String doDecryptRSA(String cipherText, String ip) throws Exception {
		byte[] bytes = Base64.getDecoder().decode(cipherText);

		Cipher decriptCipher = Cipher.getInstance("RSA");
		decriptCipher.init(Cipher.DECRYPT_MODE, this.privateKeys.get(ip));

		return new String(decriptCipher.doFinal(bytes));
	}

	/*
	 * Generate a new Connection Keypair
	 * 
	 * @return pair
	 */
	public KeyPair GenerateConnectionKey() throws NoSuchAlgorithmException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, new SecureRandom());
		KeyPair pair = keyGen.generateKeyPair();
		return pair;
	}

	/*
	 * Decrypt a general message Using Fernet
	 */
	public String decrypt(String remoteIp, String token) {
		Token t = Token.fromString(token);
		// System.out.println("Key: "+this.decryptKeys.get(remoteIp));
		if (t.isValidSignature(this.decryptKeys.get(remoteIp))) {
			// System.out.println("Key is valid!");
		} else {
			// System.out.println("Key is invalid!");
		}
		return t.validateAndDecrypt(this.decryptKeys.get(remoteIp), validator);
	}

	/*
	 * Encrypt a message using Fernet
	 */
	public String encrypt(String remoteIp, String plainText) {
		return Token.generate(this.decryptKeys.get(remoteIp), plainText).serialise();
	}

	/*
	 * Set the user manager to point to
	 */
	public void setUserMgr(UserManager userManager2) {
		// TODO Auto-generated method stub
		this.userManager = userManager2;
	}

	/*
	 * Return the killswitch status
	 */
	public int getKillSwitch() {
		return killSwitch;
	}

	/*
	 * Set the kill switch
	 */
	public void setKillSwitch(int killSwitch, Key acKey) {
		if (acKey == this.accessKey) {
			this.killSwitch = killSwitch;
		} else {
			System.err.println("AccessManager: WARNING: access violation while trying to set the killswitch");
		}
	}

	/*
	 * Return the killswitch message
	 */
	public String getKillMsg() {
		return killMsg;
	}

	/*
	 * Set the kill message
	 */
	public void setKillMsg(String killMsg) {
		this.killMsg = killMsg;
	}
}
