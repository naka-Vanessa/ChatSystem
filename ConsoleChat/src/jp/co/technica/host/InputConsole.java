package jp.co.technica.host;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jp.co.technica.communication.CommunicationManager;
import jp.co.technica.communication.data.Message;

/**
 * クライアント・ホストに対し送信するメッセージを入力するための入力コンソール
 * @author masaki
 *
 */
public class InputConsole {

	/**
	 * インスタンス生成不可
	 */
	private InputConsole(){
	}

	/**
	 * 入力用コンソール
	 * @param args [ SystemPortNumber,ConsolePortNumber,RemoteIpAddress , RemoteUserName ]
	 */
	public static void main(String[] args){
		if(args.length < 4)throw new IllegalArgumentException();
		int systemPortNumber = Integer.parseInt(args[0]);
		int consolePortNumber = Integer.parseInt(args[1]);
		String remoteIpAddress = args[2];
		String remoteUserName = args[3];

		//送信のみを行う通信監理クラスを起動
		CommunicationManager manager = CommunicationManager.createCommunicationManagerSendOnly(consolePortNumber,systemPortNumber,false);
		System.out.println("(^_^) : Enter [:exit] to end input");
		while(true){
			//コンソール入力
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String input;

			try {
				System.out.println(String.format("%s@%s>", remoteUserName,remoteIpAddress));
				input = br.readLine();

				//終了コマンドを叩かれたら終了
				if(":exit".equals(input))break;

				//入力文字をメッセージとして作成し、システム側に送信
				Message m = new Message();
				m.remoteIpAddress = remoteIpAddress;
				m.message = input;

				manager.sendData(m);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		manager.exit();
	}
}
