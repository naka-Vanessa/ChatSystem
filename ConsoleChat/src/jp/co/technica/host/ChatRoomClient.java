package jp.co.technica.host;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.technica.communication.CommunicationManager;
import jp.co.technica.communication.data.Data;
import jp.co.technica.communication.data.Message;
import jp.co.technica.communication.state.User;

public class ChatRoomClient {
	CommunicationManager consoleInputManager;
	private ExecutorService consoleMessageThread = Executors
			.newSingleThreadExecutor();
	private final int systemPortNumber;
	private final int consolePortNumber;
	private boolean executionFlg = false;
	private final IPushMessageListener ipml;
	private final User hostState;
	private final User remoteState;

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
		executionFlg = true;
		createHostMessageReceiver();
		startHostMessageReceive();
		startMessageInputConsole(); // blocked

		executionFlg = false;
		consoleMessageThread.shutdown();
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
		consoleMessageThread.submit(() -> {
			while (executionFlg) {
				Data d = consoleInputManager.popData();
				if (d instanceof Message) {
					Message m = (Message) d;
					m.name = hostState.getUserName();
					m.messageSourceIpAddress = hostState.getIpAddr();

					pushMessage(m);
				}
			}
			System.out.println("console receive out");
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
