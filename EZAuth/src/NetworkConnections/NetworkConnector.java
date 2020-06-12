package NetworkConnections;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import Main.AccessManager;
import Main.EZAuthMain;
/*
 * Manage all the network operations and pass JSON to the Access Manager
 */
public class NetworkConnector extends Thread {
	private ServerSocket socketServer;
	private AccessManager accessManager;
	private boolean exit;
	public NetworkConnector(int port, AccessManager acm) throws IOException {
		socketServer = new ServerSocket(port);
		socketServer.setSoTimeout(10000);
		this.accessManager = acm;
		this.exit=false;
		System.out.println("Network Connections Manager has been created");
	}

	/*
	 * Start a new thread to run the connections manager This might change later to
	 * try to allow multiple connections, but that's not possible right now.
	 * It should be fine because each operation (Including requesting time) is a separate connection
	 * So, the client MUST disconnect between operations
	 */
	public void run() {
		System.out.println("Network Connections Manager has been started");
		System.out.println("Waiting for you new connections: " + socketServer.getLocalPort());
		while (!this.exit) {
			try {
				Socket server = socketServer.accept();
				SocketAddress remoteIp = server.getRemoteSocketAddress();
				String[] IP = remoteIp.toString().split(":");

				System.out.println("Connected to: " + remoteIp + "\n>>");
				DataInputStream input = new DataInputStream(server.getInputStream());
				String incomingData = input.readUTF();
				// System.out.println("Received: " + incomingData);
				String out = null;
				Object Ob = null;
				/*
				 * Try to parse the incoming data, but if it can't, assume it's encrypted.
				 */
				try {
					Ob = new JSONParser().parse(incomingData);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
					if (EZAuthMain.logLevel < 2) {
						System.out.println("NetworkConnector: Notice: Assuming Parsing failure means encrypted data!");
					}
				} catch (NullPointerException exp) {
					System.out.println("NetworkConnector: Null Pointer Found in a place that is Unrecoverable!");
					System.out.println(incomingData);
					exp.printStackTrace();
					System.exit(1);
				}
				JSONObject jsonOb = (JSONObject) Ob;

				try {
					// System.out.println("Request was: " + jsonOb.get("Request"));
					if (jsonOb.get("Request").equals("NEW_CONNECTION")) {
						// System.out.println("Requested new connection");
						out = this.accessManager.executeJSON(incomingData, IP[0]);
					} else {
						String data = this.accessManager.decrypt(IP[0], incomingData);
						out = this.accessManager.executeJSON(data, IP[0]);
					}
				} catch (NullPointerException e) {
					if (EZAuthMain.logLevel < 2) {
						System.out.println(
								"NetworkConnector: Warn: Assuming Null pointer is a failure in parsing non-existant JSON (Trying to decrypt)");
					}
					String dummy = null;
					String data = this.accessManager.decrypt(IP[0], incomingData);
					out = this.accessManager.executeJSON(data, IP[0]);
				}
				DataOutputStream outStream = new DataOutputStream(server.getOutputStream());
				if (EZAuthMain.logLevel < 2) {
					System.out.println("NetworkConnector: Info: Responding to Client");
				}
				outStream.writeUTF(out);
			} catch (SocketTimeoutException s) {
				if (EZAuthMain.logLevel == 1) {
					System.out.println("NetworkConnector: Info: The server has timed out!");
				}
			} catch (IOException exp) {
				System.err.println("NetworkConnector: Unrecoverable Error");
				exp.printStackTrace();
				break;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.err.println(">>>>>> Network Connector has been stopped");
	}
	public void newStop() {
		this.exit = true;
	}
}
