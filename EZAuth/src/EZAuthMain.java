
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;

public class EZAuthMain implements ActionListener {
	private Timer timer;
	private AccessManager accessManager;
	Key key;

	public static void main(String[] args) {
		new EZAuthMain();
	}

	public EZAuthMain() {
		this.timer = new Timer(10, this);
		this.accessManager = new AccessManager();
		key = this.accessManager.generateKey();
		this.timer.start();
		while(true);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println(
				"Ticks: " + this.accessManager.getServerTicks() + " : " + this.accessManager.checkGameValidity(key));
		this.accessManager.update();
	}
}
