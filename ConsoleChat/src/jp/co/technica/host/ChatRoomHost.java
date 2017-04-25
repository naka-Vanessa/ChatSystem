package jp.co.technica.host;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import jp.co.technica.communication.CommunicationManager;
import jp.co.technica.communication.data.Data;
import jp.co.technica.communication.data.Message;
import jp.co.technica.communication.state.User;

/**
 * ホストとして起動し、接続してきたユーザーの管理やテキストの拡散を行う
 *
 * @author masaki
 *
 */
public class ChatRoomHost {
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
	 * ホストのユーザー情報
	 */
	private final User hostState;

	/**
	 * コンソールから入力されたメッセージを捌くインターフェース
	 */
	public interface IPushMessageListener {
		void popMessage(Message message);
	}

	/**
	 * クライアントのステータスを管理します。
	 */
	ConcurrentHashMap<String, User> map = new ConcurrentHashMap<>();

	/**
	 * コンストラクタ
	 *
	 * @param systemPortNumber
	 *            システム側のポート番号
	 * @param consolePortNumber
	 *            入力コンソール側のポート番号
	 * @param hostUser
	 * @param listener
	 */
	public ChatRoomHost(int systemPortNumber, int consolePortNumber,
			User hostUser, IPushMessageListener listener) {
		this.ipml = listener;
		this.hostState = hostUser;
		this.systemPortNumber = systemPortNumber;
		this.consolePortNumber = consolePortNumber;
	}

	/**
	 * クライアントユーザー・入力コンソールから来たメッセージを処理する
	 *
	 * @param message
	 */
	public void pushMessage(Message message) {
		User st = map.get(message.sourceIpAddress);
		// メッセージが接続済みのユーザーのもの・入力コンソールからのものであれば出力コンソールに出力
		if (st != null && st.getUserName().equals(message.name)
				|| hostState.getUserName().equals(message.name)) {

			System.out.println(String.format("%s@%s>%s", message.name,
					message.messageSourceIpAddress, message.message));

			diffusionMessage(message);
		}
	}

	/**
	 * ユーザーの追加<br>
	 * ユーザーの登録に成功した場合trueが返ります<br>
	 * 既にユーザーが既に追加されている場合はfalseが返ります。
	 *
	 * @param user
	 * @return
	 */
	public boolean addClientUser(User user) {
		User u = map.put(user.getIpAddr(), user);
		return (u == null || !u.getUserName().equals(user.getUserName()));
	}

	/**
	 * ユーザー情報の削除<br>
	 * ユーザーの削除に成功した場合trueが返ります。<br>
	 * ユーザーが既に存在しない場合はfalseが返ります。
	 *
	 * @param user
	 * @return
	 */
	public boolean subClientUser(User user) {
		User u = map.remove(user.getIpAddr());
		return u != null;
	}

	/**
	 * 現在登録されているユーザーの情報を返す
	 *
	 * @return
	 */
	public Collection<User> getClientUserList() {
		return map.values();
	}

	/**
	 * チャットルームをスタートします。<br>
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
	 * 入力メッセージを拡散します。<br>
	 * ただし、入力メッセージの送信元と同じユーザーにはメッセージを送りません。
	 *
	 * @param message
	 */
	private void diffusionMessage(Message message) {
		for (Entry<String, User> s : map.entrySet()) {
			User st = s.getValue();
			if (st.getIpAddr().equals(message.messageSourceIpAddress)) {
				continue;
			}
			Message m = new Message();
			m.copy(message);
			m.remoteIpAddress = st.getIpAddr();
			m.sourceIpAddress = hostState.getIpAddr();
			ipml.popMessage(m);
		}
	}

	/**
	 * 入力コンソールから流れてきたデータを捌くインターフェスの実装
	 */
	private void startHostMessageReceive() {
		consoleInputManager.setHocker((Data d) -> {
			if (d instanceof Message) {
				Message m = (Message) d;
				m.name = hostState.getUserName();
				m.messageSourceIpAddress = hostState.getIpAddr();
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
				String.valueOf(consolePortNumber), hostState.getIpAddr(),
				hostState.getUserName());
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
