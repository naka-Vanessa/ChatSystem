package jp.co.technica;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jp.co.technica.communication.state.User;
import jp.co.technica.control.Manipulator;

public class Main {
	/**
	 * 起動
	 * @param args  0:ユーザー名
	 */
	public static void main(String[] args){
		if(args.length == 0){
			throw new IllegalArgumentException("ユーザー名を入力してください。起動方法：[java -cp ConsoleChat.jar jp.co.technica.Main ユーザー名]");
		}
		String ip;
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
			User user = new User(args[0], ip);

			Manipulator manipulator = Manipulator.getInstance();
			manipulator.startInteraction(user);
			Thread.sleep(2000);
		} catch (UnknownHostException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
