package jp.co.technica.host;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.technica.communication.CommunicationManager;
import jp.co.technica.communication.data.Data;
import jp.co.technica.communication.data.Message;
import jp.co.technica.communication.state.User;

public class ChatRoomHost {
	CommunicationManager consoleInputManager;
	private ExecutorService consoleMessageThread = Executors.newSingleThreadExecutor();
	private final int  systemPortNumber;
	private final int consolePortNumber;
	private boolean executionFlg = false;
	private final IPushMessageListener ipml;
	private final User hostState;

	public interface IPushMessageListener{
		void pushMessage(Message message);
	}


	/**
	 * クライアントのステータスを管理します。
	 */
	ConcurrentHashMap<String, User> map = new ConcurrentHashMap<>();

	public ChatRoomHost(int systemPortNumber,int consolePortNumber,User hostUser,IPushMessageListener listener){
		this.ipml = listener;
		this.hostState =hostUser;
		this.systemPortNumber = systemPortNumber;
		this.consolePortNumber = consolePortNumber;
	}

	public void pushMessage(Message message){
		System.out.println(String.format("%s@%s>%s", message.name,message.messageSourceIpAddress,message.message));
		diffusionMessage(message);
	}
	public void addClientUser(User user){
		map.put(user.getIpAddr(), user);
	}

	/**
	 * チャットルームをスタートします。<br>
	 * コンソールが表示され、そこからテキストを入力します。<br>
	 * コンソールが閉じられるまで制御がブロックされます。
	 */
	public void executeHostInput(){
		executionFlg = true;
		createHostMessageReceiver();
		startHostMessageReceive();
		startMessageInputConsole(); //blocked

		executionFlg = false;
		consoleMessageThread.shutdown();
	}

	private void diffusionMessage(Message message){
		for(Entry<String, User> s : map.entrySet()){
			User st = s.getValue();
			if(st.getIpAddr().equals(message.messageSourceIpAddress)){
				continue;
			}
			Message m = new Message();
			m.copy(message);
			m.remoteIpAddress = st.getIpAddr();
			m.sourceIpAddress = hostState.getIpAddr();
			ipml.pushMessage(m);
		}

	}

	private void startHostMessageReceive(){
		consoleMessageThread.submit(()->{
			while(executionFlg){
				Data d = consoleInputManager.popData();
				if(d instanceof Message){
					Message m = (Message)d;
					m.name = hostState.getUserName();
					m.messageSourceIpAddress = hostState.getIpAddr();
					pushMessage(m);
				}
			}
		});

	}

	private void startMessageInputConsole(){
		ProcessBuilder pb = new ProcessBuilder("cmd.exe","/C","start","java","jp.co.technica.host.InputConsole",String.valueOf(systemPortNumber),String.valueOf(consolePortNumber),hostState.getIpAddr(),hostState.getUserName());
		pb.directory(new File(System.getProperty("user.dir") + "\\bin"));
		System.out.println(pb.directory().getPath());
		try {
			Process p = pb.start();
			InputStream is = p.getInputStream();
			try {
				while(is.read() >= 0);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createHostMessageReceiver(){
		consoleInputManager = CommunicationManager.createCommunicationManagerReceiveOnly(systemPortNumber,consolePortNumber,false);
	}
}