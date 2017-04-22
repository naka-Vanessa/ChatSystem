package jp.co.technica.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;

import jp.co.technica.communication.CommunicationManager;
import jp.co.technica.communication.data.Data;
import jp.co.technica.communication.data.Inquire;
import jp.co.technica.communication.data.Message;
import jp.co.technica.communication.data.User;
import jp.co.technica.host.ChatRoom;

public class Manipulator {
	private String bloadCastAddress;
	private CommunicationManager manager;
	private User hostState;
	private static final int MAIN_PROCESS_PORT_NUMBER = 54321;
	private static final int INPUT_CONSOLE_SENDER_PORT_NUMBER = 54322;
	private static final int INPUT_CONSOLE_RECEIVER_PORT_NUMBER = 54323;

	private IReceiveDataHooker hooker;
	private boolean exitFlg =false;

	private static Manipulator THIS_INSTANCE = new Manipulator();

	interface IReceiveDataHooker{
		void hook(Data d);
	}

	/**
	 * シングルトンインスタンス
	 */
	private Manipulator(){
	}

	public static Manipulator getInstance(){
		return THIS_INSTANCE;
	}

	public void startInteraction(User hostState){
		this.hostState = hostState;
		if(hostState == null){
			throw new IllegalArgumentException("Host information is not set");
		}

		manager = CommunicationManager.createCommunicationManager(MAIN_PROCESS_PORT_NUMBER,true);

		Executors.newSingleThreadExecutor().submit(()->{
			while(exitFlg == false){
				Data d = manager.popData();
				if(hooker != null){
					hooker.hook(d);
				}
			}
		});

		System.out.println("Hello [" + hostState.getUserName() + "]");
		System.out.println("Please enter the command. The command can be checked with [:help].");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean loop = true;
		while(loop){
			System.out.print("INPUT>");
			try {
				String input = br.readLine();
				String[] commands = input.split(" ");
				Order order = Order.getOrder(commands[0]);

				switch(order){
				case Help:
					executeHelp(commands);
					break;
				case OtherUserChatRoomCheck:
					executeOtherUserChatRoomCheck(commands);
					break;
				case OherUserChatRoomAccess:
					executeOtherUserOherUserChatRoomAccess(commands);
					break;
				case HostChatRoomStart:
					executeHostChatRoomStart(commands);
					break;
				case Exit:
					loop = false;
					break;
				default:
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("see you soon !!");
	}

	private void executeHelp(String[] commands){
		System.out.println("[" + Order.OtherUserChatRoomCheck.commandDefinition +" (bloadcastAddress)]:他のユーザーが部屋を起動しているか確認します。");
		System.out.println("[" + Order.OherUserChatRoomAccess.commandDefinition + " ipAddress]:他ユーザーが起動している部屋にアクセスします。");
		System.out.println("[" + Order.HostChatRoomStart.commandDefinition + "]:自身が部屋を起動します");
		System.out.println("[" + Order.Exit.commandDefinition + "]:システムを終了します。");
		System.out.println("※()で囲まれているものは省略可能");
	}

	private void executeOtherUserChatRoomCheck(String[] commands){
		try {
			InetAddress remoteAddress;
			if(commands.length  <= 1 || commands[1].equals("")){
				if(bloadCastAddress == null || bloadCastAddress.isEmpty()){
					System.out.println("Please select a broadcast address.");

					//ブロードキャストアドレスの検索・表示
					Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
					while(e.hasMoreElements()){
						NetworkInterface nif = e.nextElement();
						List<InterfaceAddress> ifaList = nif.getInterfaceAddresses();
						for(InterfaceAddress ifa : ifaList){
							InetAddress ad = ifa.getBroadcast();
							if(ad == null)continue;
							System.out.println(ad.getHostAddress());
						}
					}

					System.out.println("INPUT>");
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					String address = br.readLine();
					bloadCastAddress = address;
					remoteAddress = Inet4Address.getByName(address);
				}else{
					remoteAddress = Inet4Address.getByName(bloadCastAddress);
				}

			}else{
				remoteAddress = Inet4Address.getByName(commands[1]);
			}
			Inquire idata = new Inquire();

			idata.remoteIpAddress = remoteAddress.getHostAddress();
			idata.sourceIpAddress = InetAddress.getLocalHost().getHostName();
			idata.comandType = Inquire.COMAND_TYPE_INQUIRE;
			manager.sendData(idata);

			System.out.print("Please wait ");

			List<Inquire> list = new ArrayList<>();

			hooker = (Data d) ->{
				if(d instanceof Inquire){
					Inquire inquire = (Inquire)d;
					if(inquire.comandType == Inquire.COMAND_TYPE_ANSWER){
						list.add(inquire);
						System.out.println(inquire.sourceIpAddress);
					}
				}
			};

			for(int i= 0;i<5;i++){
				Thread.sleep(1000);
				System.out.print(".");
			}
			System.out.println();
			hooker = null;
			System.out.println("Address that responded");
			for(Inquire inq : list){
				System.out.println(String.format("%s@%s", inq.sourceIpAddress,inq.name));
			}
		} catch (IOException | InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
	private void executeOtherUserOherUserChatRoomAccess(String[] commands){

	}
	private void executeHostChatRoomStart(String[] commands){
		ChatRoom cr = new ChatRoom(INPUT_CONSOLE_RECEIVER_PORT_NUMBER,INPUT_CONSOLE_SENDER_PORT_NUMBER,hostState,(Message m)->{
			manager.sendData(m);
		});
		hooker = (Data d)->{
			if(d instanceof Message){
				cr.pushMessage((Message)d);
			}else if(d instanceof Inquire){
				Inquire inq = (Inquire)d;
				if(inq.comandType == Inquire.COMAND_TYPE_INQUIRE){
					Inquire ans = new Inquire();
					ans.comandType = Inquire.COMAND_TYPE_ANSWER;
					ans.name = hostState.getUserName();
					ans.remoteIpAddress = inq.sourceIpAddress;
					manager.sendData(ans);
				}
			}
		};
		cr.executeHostInput();
	}


	enum Order{
		Help(":help"),
		OtherUserChatRoomCheck(":search"),
		OherUserChatRoomAccess(":access"),
		HostChatRoomStart(":host"),
		Exit(":exit"),
		NULL(null);
		public final String commandDefinition;

		Order(String commandDefinition){
			this.commandDefinition = commandDefinition;
		}

		boolean isAccurateCommand(String command){
			boolean ret = false;
			if(commandDefinition != null)ret = commandDefinition.equals(command);
			return ret;
		};

		private static Order getOrder(String command){
			for(Order o : Order.values()){
				if(o.isAccurateCommand(command))return o;
			}
			return NULL;
		}
	}
}
