package NetworkConnections;

// File Name GreetingClient.java
import java.net.*;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import javax.annotation.Generated;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.io.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;

/*
 * This is just a test client
 * @Author Zackery Painter
 */
public class TestClient {
	private static JSONObject request;
	private final static Validator<String> validator = new StringValidator() {
	};

	public static void main(String[] args) throws ParseException {
		String serverName = "localhost";
		int port = 6060;
		request = new JSONObject();
		try {
			System.out.println("Generating my encryption keys..");
			KeyPair keys = GenerateConnectionKey();
			PublicKey pubKey = keys.getPublic();
			PrivateKey privKey = keys.getPrivate();
			byte[] pubKeyEnc = pubKey.getEncoded();
			String pubKeyStr = Base64.getEncoder().encodeToString(pubKeyEnc);
			request.put("Request", "NEW_CONNECTION");
			request.put("PUBLIC_KEY", pubKeyStr);
			Socket client = new Socket(serverName, port);
			System.out.println("Connecting to " + serverName + " on port " + port);
			System.out.println("Just connected to " + client.getRemoteSocketAddress());
			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF(JSONObject.toJSONString(request));
			InputStream inFromServer = client.getInputStream();
			DataInputStream in = new DataInputStream(inFromServer);
			String response = in.readUTF();
			// System.out.println("Server says " + response);
			String decrypted = null;
			try {
				decrypted = doDecryptRSA(response, keys.getPrivate());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Object Ob = new JSONParser().parse(decrypted);
			JSONObject jsonOb = (JSONObject) Ob;
			JSONObject response2 = new JSONObject();
			String responseCode = (String) jsonOb.get("RESPONSE");
			client.close();
			client = new Socket(serverName, port);
			if (responseCode.equals("200")) {
				System.out.println("Replying...");
				Key accessKey = new Key((String) jsonOb.get("ACCESS_KEY"));
				Key decryptionKey = new Key((String) jsonOb.get("DECRYPTION_KEY"));
				response2.put("Request", "CREATE_USER");
				response2.put("ACCESS_KEY", accessKey.serialise());
				response2.put("USERNAME", "JohnSmith");
				response2.put("PASSWORD", "HelloWorld");
				response2.put("EMAIL","testEmail@mail.com");
				String encrtn = Token.generate(decryptionKey, JSONObject.toJSONString(response2)).serialise();
				// System.out.println("Sending:"+encrtn);
				outToServer = client.getOutputStream();
				out = new DataOutputStream(outToServer);
				out.writeUTF(encrtn);
				inFromServer = client.getInputStream();
				in = new DataInputStream(inFromServer);
				response = in.readUTF();
				decrypted = Token.fromString(response).validateAndDecrypt(decryptionKey, validator);
				Ob = new JSONParser().parse(decrypted);
				jsonOb = (JSONObject) Ob;
				responseCode = (String) jsonOb.get("RESPONSE");
				if (responseCode.equals("200")) {
					client.close();
					client = new Socket(serverName, port);
					System.out.println("Server has accepted our request to create the user");
					System.out.println("Trying to Log in now...");
					JSONObject response3 = new JSONObject();
					response3.put("Request", "LOGIN");
					response3.put("ACCESS_KEY", accessKey.serialise());
					response3.put("USERNAME", "testEmail@mail.com"); //Login through email or password
					response3.put("PASSWORD", "HelloWorld");
					 encrtn = Token.generate(decryptionKey, JSONObject.toJSONString(response3)).serialise();
					// System.out.println("Sending:"+encrtn);
					outToServer = client.getOutputStream();
					out = new DataOutputStream(outToServer);
					out.writeUTF(encrtn);
					inFromServer = client.getInputStream();
					in = new DataInputStream(inFromServer);
					response = in.readUTF();
					decrypted = Token.fromString(response).validateAndDecrypt(decryptionKey, validator);
					Ob = new JSONParser().parse(decrypted);
					jsonOb = (JSONObject) Ob;
					responseCode = (String) jsonOb.get("RESPONSE");
					if(responseCode.equals("200")) {
						System.out.println("Login Success!");
					}else {
						System.out.println("Login Failed for the following reason:");
						System.out.println("Response Code: "+responseCode);
						System.out.println("Message: "+jsonOb.get("MESSAGE"));
					}
				} else {
					System.out.println("Server has rejected our request!");
					System.out.println("Response Code: " + responseCode);
					System.out.println("Message: " + jsonOb.get("MESSAGE"));
				}
			} else {
				System.out.println("The server rejected the connection!");
				System.out.println("Server Response Code: " + jsonOb.get("RESPONSE"));
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static KeyPair GenerateConnectionKey() throws NoSuchAlgorithmException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, new SecureRandom());
		KeyPair pair = keyGen.generateKeyPair();
		return pair;
	}

	public String doEncryptRSA(String plainText, PublicKey pubKey)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		Cipher encryptCipher = Cipher.getInstance("RSA");

		encryptCipher.init(Cipher.ENCRYPT_MODE, pubKey);
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
	public static String doDecryptRSA(String cipherText, PrivateKey privKey) throws Exception {
		byte[] bytes = Base64.getDecoder().decode(cipherText);

		Cipher decriptCipher = Cipher.getInstance("RSA");
		decriptCipher.init(Cipher.DECRYPT_MODE, privKey);

		return new String(decriptCipher.doFinal(bytes));
	}
}