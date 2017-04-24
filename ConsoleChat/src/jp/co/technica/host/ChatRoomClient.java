package jp.co.technica.host;

import java.io.IOException;
import java.io.InputStream;

import jp.co.technica.communication.CommunicationManager;
import jp.co.technica.communication.data.Data;
import jp.co.technica.communication.data.Message;
import jp.co.technica.communication.state.User;

public class ChatRoomClient {
	CommunicationManager consoleInputManager;
	private final int systemPortNumber;
	private final int consolePortNumber;
	private final IPushMessageListener ipml;
	private User hostState;
	private User remoteState;

	public interface IPushMessageListener {
		void pushMessage(Message message);
	}

	public ChatRoomClient(int systemPortNumber, int consolePortNumber,
			User hostUser, User remoteUser, IPushMessageListener listener) {
		this.ipml = listener;
		this.hostState = hostUser;
		this.remoteState = remoteUser;
		this.systemPortNumber = systemPortNumber;
		this.consolePortNumber = consolePortNumber;
	}

	public void pushMessage(Message message) {
		if(remoteState == null)return;
		if (message.sourceIpAddress.equals(hostState.getIpAddr())
				|| message.sourceIpAddress.equals(remoteState.getIpAddr())) {
			System.out.println(String.format("%s@%s>%s", message.name,
					message.messageSourceIpAddress, message.message));
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

	public void executeForciblyLeaveRoom(){
		remoteState = null;
	}

	private void diffusionMessage(Message message) {
		if (message.messageSourceIpAddress.equals(hostState.getIpAddr())) {
			Message m = new Message();
			m.copy(message);
			m.sourceIpAddress = hostState.getIpAddr();
			m.remoteIpAddress = remoteState.getIpAddr();
			ipml.pushMessage(m);
		}
	}

	private void startHostMessageReceive() {
		consoleInputManager.setHocker((Data d)->{
			if (d instanceof Message) {
				Message m = (Message) d;
				m.name = hostState.getUserName();
				m.messageSourceIpAddress = hostState.getIpAddr();

				pushMessage(m);
			}
		});
	}

	private void startMessageInputConsole() {
		ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/C", "start",
				"java", "-cp", "ConsoleChat.jar",
				"jp.co.technica.host.InputConsole",
				String.valueOf(systemPortNumber),
				String.valueOf(consolePortNumber), hostState.getIpAddr(),
				hostState.getUserName());
		try {
			Process p = pb.start();
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

	private void createHostMessageReceiver() {
		consoleInputManager = CommunicationManager
				.createCommunicationManagerReceiveOnly(systemPortNumber,
						consolePortNumber, false);
	}

}
