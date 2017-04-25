package jp.co.technica.host;

import java.io.IOException;
import java.io.InputStream;

import jp.co.technica.communication.CommunicationManager;
import jp.co.technica.communication.data.Data;
import jp.co.technica.communication.data.Message;
import jp.co.technica.communication.state.User;

/**
 * 接続先とのメッセージをやり取りするためのクライアント<br>
 * @author masaki
 *
 */
public class ChatRoomClient {

	/**
	 * 入力コンソールで入力した文字列を受け取るための通信管理変数
	 */
	CommunicationManager consoleInputManager;
	/**
	 * システム側のポート番号
	 */
	private final int systemPortNumber;
	/**
	 * 入力コンソール側のポート番号
	 */
	private final int consolePortNumber;
	/**
	 * コンソールから入力されたメッセージを捌く実装インターフェース
	 */
	private final IPushMessageListener ipml;
	/**
	 * クライアントのユーザー情報
	 */
	private User clientState;
	/**
	 * ホストのユーザー情報
	 */
	private User remoteState;

	/**
	 * コンソールから入力されたメッセージを捌くインターフェース
	 */
	public interface IPushMessageListener {
		void popMessage(Message message);
	}

	/**
	 * コンストラクタ
	 * @param systemPortNumber システム側のポート番号
	 * @param consolePortNumber 入力コンソール側のポート番号
	 * @param clientUser
	 * @param remoteUser
	 * @param listener
	 */
	public ChatRoomClient(int systemPortNumber, int consolePortNumber,
			User clientUser, User remoteUser, IPushMessageListener listener) {
		this.ipml = listener;
		this.clientState = clientUser;
		this.remoteState = remoteUser;
		this.systemPortNumber = systemPortNumber;
		this.consolePortNumber = consolePortNumber;
	}

	/**
	 * ホストユーザー・入力コンソールから来たメッセージを処理する
	 * @param message
	 */
	public void pushMessage(Message message) {
		if(remoteState == null)return;
		//メッセージがホスト・入力コンソールのどちらかである場合に出力コンソールに出力
		if (message.sourceIpAddress.equals(clientState.getIpAddr())
				|| message.sourceIpAddress.equals(remoteState.getIpAddr())) {

			System.out.println(String.format("%s@%s>%s", message.name,
					message.messageSourceIpAddress, message.message));
			//メッセージの送信処理
			diffusionMessage(message);
		}
	}

	/**
	 * クライアントをスタートします。<br>
	 * コンソールが表示され、そこからテキストを入力します。<br>
	 * コンソールが閉じられるまで制御がブロックされます。
	 */
	public void executeHostInput() {
		createHostMessageReceiver();
		startHostMessageReceive();
		startMessageInputConsole(); // blocked

		consoleInputManager.exit();
	}

	/**
	 * ホストユーザーが終了させたときに呼ばれる
	 */
	public void executeForciblyLeaveRoom(){
		remoteState = null;
	}

	/**
	 * メッセージの送信処理
	 * @param message
	 */
	private void diffusionMessage(Message message) {
		//入力コンソールのメッセージのみ送信
		if (message.messageSourceIpAddress.equals(clientState.getIpAddr())) {
			Message m = new Message();
			m.copy(message);
			m.sourceIpAddress = clientState.getIpAddr();
			m.remoteIpAddress = remoteState.getIpAddr();
			ipml.popMessage(m);
		}
	}

	/**
	 * 入力コンソールから流れてきたデータを捌くインターフェスの実装
	 */
	private void startHostMessageReceive() {
		consoleInputManager.setHocker((Data d)->{
			if (d instanceof Message) {
				Message m = (Message) d;
				m.name = clientState.getUserName();
				m.messageSourceIpAddress = clientState.getIpAddr();

				pushMessage(m);
			}
		});
	}

	/**
	 * 入力コンソールの起動<br>
	 * 新規のコマンドプロンプトで入力コンソールを立ち上げます。
	 */
	private void startMessageInputConsole() {
		ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/C", "start",
				"java", "-cp", "ConsoleChat.jar",
				"jp.co.technica.host.InputConsole",
				String.valueOf(systemPortNumber),
				String.valueOf(consolePortNumber), clientState.getIpAddr(),
				clientState.getUserName());

		try {
			//コマンドプロンプト起動
			Process p = pb.start();

			//立ち上げたコマンドプロンプトが閉じられるまで待機
			InputStream is = p.getInputStream();
			try {
				while (is.read() >= 0)
					;
			} finally {
				is.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 入力コンソールと通信するための通信監理クラスを起動
	 */
	private void createHostMessageReceiver() {
		consoleInputManager = CommunicationManager
				.createCommunicationManagerReceiveOnly(systemPortNumber,
						consolePortNumber, false);
	}

}
