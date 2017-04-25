package jp.co.technica.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import jp.co.technica.communication.CommunicationManager;
import jp.co.technica.communication.data.Connection;
import jp.co.technica.communication.data.Data;
import jp.co.technica.communication.data.Inquire;
import jp.co.technica.communication.data.Message;
import jp.co.technica.communication.state.User;
import jp.co.technica.host.ChatRoomClient;
import jp.co.technica.host.ChatRoomHost;

/**
 * チャットシステムをコントロールするクラス<br>
 *
 * @author masaki
 *
 */
public class Manipulator {

	/**
	 * ブロードキャストアドレスを保持<br>
	 * [:search]使用時に入力したブロードキャストアドレスを記憶し、次回入力時に省略するため
	 */
	private String bloadCastAddress;
	/**
	 * 通信監理クラス
	 */
	private CommunicationManager manager;
	/**
	 * アプリケーションを起動したユーザーの情報
	 */
	private User hostState;
	/**
	 * メッセージのやり取りを行うために使用するポート番号
	 */
	private static final int MAIN_PROCESS_PORT_NUMBER = 54321;
	/**
	 * 入力コンソール側が利用するポート番号<br>
	 * 今のところ、存在する意味がない。
	 */
	private static final int INPUT_CONSOLE_SENDER_PORT_NUMBER = 54322;

	/**
	 * 当クラスが利用する入力コンソールから入力文字を受け取るポート番号
	 */
	private static final int INPUT_CONSOLE_RECEIVER_PORT_NUMBER = 54323;

	/**
	 * 当クラス唯一のインスタンス
	 */
	private static Manipulator THIS_INSTANCE = new Manipulator();

	/**
	 * シングルトンインスタンス
	 */
	private Manipulator() {
	}

	/**
	 * インスタンスの取得
	 *
	 * @return
	 */
	public static Manipulator getInstance() {
		return THIS_INSTANCE;
	}

	/**
	 * チャットシステムのメイン処理開始
	 *
	 * @param hostState
	 */
	public void startInteraction(User hostState) {
		this.hostState = hostState;
		if (hostState == null) {
			throw new IllegalArgumentException("Host information is not set");
		}
		// 通信監理クラス起動
		manager = CommunicationManager.createCommunicationManager(
				MAIN_PROCESS_PORT_NUMBER, true);

		System.out.println("(^_^) : Hello!! [" + hostState.getUserName() + "]");
		System.out.println("(^_^) : Please enter the command. The command can be checked with [:help].");
		// コンソール入力
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean loop = true;

		while (loop) {
			System.out.print("INPUT>");
			try {
				String input = br.readLine();
				String[] commands = input.split(" ");
				// 入力コマンド確認
				Order order = Order.getOrder(commands[0]);

				switch (order) {
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
		exit();
		System.out.println("(^_^) : see you soon !!");
	}

	/**
	 * ヘルプを表示
	 *
	 * @param commands
	 */
	private void executeHelp(String[] commands) {
		System.out.println(String.format("[%s]:%s",
				Order.OtherUserChatRoomCheck.commandDefinition,
				Order.OtherUserChatRoomCheck.message));

		System.out.println(String.format("[%s]:%s",
				Order.OherUserChatRoomAccess.commandDefinition,
				Order.OherUserChatRoomAccess.message));

		System.out.println(String.format("[%s]:%s",
				Order.HostChatRoomStart.commandDefinition,
				Order.HostChatRoomStart.message));

		System.out.println(String.format("[%s]:%s",
				Order.Exit.commandDefinition, Order.Exit.message));

		System.out.println("※+のついているものは引数。()で囲まれているものは省略可能");
	}

	/**
	 * 同じセグメントにいるマシンにルームを立ち上げているか確認
	 *
	 * @param commands
	 */
	private void executeOtherUserChatRoomCheck(String[] commands) {
		try {
			InetAddress remoteAddress;

			if (commands.length < 2 || commands[1].equals("")) {
				if (bloadCastAddress == null || bloadCastAddress.isEmpty()) {
					System.out
							.println("(^_^) : Please select a broadcast address.");

					// ブロードキャストアドレスの検索・表示
					Enumeration<NetworkInterface> e = NetworkInterface
							.getNetworkInterfaces();
					while (e.hasMoreElements()) {
						NetworkInterface nif = e.nextElement();
						List<InterfaceAddress> ifaList = nif
								.getInterfaceAddresses();
						for (InterfaceAddress ifa : ifaList) {
							InetAddress ad = ifa.getBroadcast();
							if (ad == null)
								continue;
							System.out.println(">" + ad.getHostAddress());
						}
					}
					System.out.print("BloadCastAddress INPUT>");

					//ブロードキャストアドレスの入力待ち
					BufferedReader br = new BufferedReader(
							new InputStreamReader(System.in));

					String address = br.readLine();
					bloadCastAddress = address;
					remoteAddress = Inet4Address.getByName(address);
				} else {
					remoteAddress = Inet4Address.getByName(bloadCastAddress);
				}

			} else {
				remoteAddress = Inet4Address.getByName(commands[1]);
			}
			Inquire idata = new Inquire();

			List<Inquire> list = new ArrayList<>();
			//相手から帰ってきたメッセージを捌くインターフェースの実装
			manager.setHocker((Data d) -> {
				if (d instanceof Inquire) {
					Inquire inquire = (Inquire) d;
					if (inquire.comandType == Inquire.COMAND_TYPE_ANSWER) {
						list.add(inquire);
					}
				}
			});

			//ルームを立ち上げているか確認するコマンドを生成・送信
			idata.remoteIpAddress = remoteAddress.getHostAddress();
			idata.sourceIpAddress = InetAddress.getLocalHost().getHostName();
			idata.comandType = Inquire.COMAND_TYPE_INQUIRE;
			manager.sendData(idata);

			System.out.print("(^_^) : Please wait ");

			//受信待ち
			for (int i = 0; i < 6; i++) {
				Thread.sleep(1000);
				System.out.print(".");
			}
			System.out.println();

			//相手から帰ってきたメッセージを捌くインターフェースを取り除く
			manager.removeHocker();

			System.out.println("(^_^) : Address that responded");
			//応答があったユーザーの情報を表示
			for (Inquire inq : list) {
				System.out.println(String.format(">%s@%s", inq.sourceIpAddress,
						inq.name));
			}
		} catch (IOException | InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		//相手から帰ってきたメッセージを捌くインターフェースを取り除く
		manager.removeHocker();
	}

	/**
	 * 同じセグメント内でルームを立ち上げているユーザーにアクセス
	 * @param commands
	 */
	private void executeOtherUserOherUserChatRoomAccess(String[] commands) {
		try {
			InetAddress remoteAddress;
			if (commands.length < 2 || commands[1].equals("")) {
				throw new IllegalArgumentException("接続先IPアドレスを指定してください");
			} else {
				remoteAddress = Inet4Address.getByName(commands[1]);
			}

			//接続要求コマンドを生成・送信
			Connection connect = new Connection();

			connect.remoteIpAddress = remoteAddress.getHostAddress();
			connect.sourceIpAddress = hostState.getIpAddr();
			connect.user = hostState;
			connect.comandType = Connection.COMAND_TYPE_REQUEST;
			connect.connectionFlg = true;

			manager.sendData(connect);

			System.out.println("(^_^) : Please wait ");

			Connection ret = new Connection();
			//相手から帰ってきたメッセージを捌くインターフェースを実装
			manager.setHocker((Data d) -> {
				if (d instanceof Connection) {
					Connection con = (Connection) d;
					if (con.sourceIpAddress.equals(remoteAddress.getHostAddress())
							&& con.comandType == Connection.COMAND_TYPE_ANSWER) {

						ret.connectionFlg = con.connectionFlg;
						ret.user = con.user;
					}
				}
			});

			//メッセージが返ってくるのを待機
			for (int i = 0; i < 10; i++) {
				Thread.sleep(1000);
				if (ret.connectionFlg)
					break;
			}

			//返答がtrueである場合、クライアントを生成
			if (ret.connectionFlg) {
				ChatRoomClient crc = new ChatRoomClient(
						INPUT_CONSOLE_RECEIVER_PORT_NUMBER,
						INPUT_CONSOLE_SENDER_PORT_NUMBER, hostState, ret.user,
						(Message m) -> manager.sendData(m));

				System.out.println(String.format("☆★☆[%s@%s]に参加☆★☆",
						ret.user.getUserName(), ret.user.getIpAddr()));

				//相手から送られてくるDataを捌くインタフェースを実装
				manager.setHocker((Data d) -> {
					if (d instanceof Message) {
						//メッセージであればクライアントに渡す
						crc.pushMessage((Message) d);
					} else if (d instanceof Connection) {
						//コネクションで切断要求である場合クライアントにメッセージを送信させないようにする。
						Connection c = (Connection) d;
						if (c.comandType == Connection.COMAND_TYPE_FORCED
								&& c.connectionFlg == false) {
							crc.executeForciblyLeaveRoom();
							System.out.println("接続先のルームが終了しました。");
						}
					}
				});

				//クライアントの起動
				crc.executeHostInput();
				//クライアントが終了したとき、相手に終了したことを伝える
				connect.connectionFlg = false;
				manager.sendData(connect);

				System.out.println("(^_^) : left the room.");

			} else {
				System.out.println("(;_;) : Connection failed...");
			}

		} catch (IOException | InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		//相手から帰ってきたメッセージを捌くインターフェースを取り除く
		manager.removeHocker();
	}

	/**
	 * ホストとしてルームを起動
	 * @param commands
	 */
	private void executeHostChatRoomStart(String[] commands) {
		//ホストを生成
		ChatRoomHost crh = new ChatRoomHost(INPUT_CONSOLE_RECEIVER_PORT_NUMBER,
				INPUT_CONSOLE_SENDER_PORT_NUMBER, hostState,
				(Message m) -> manager.sendData(m));

		//相手から送られてくるDataを捌くインタフェースを実装
		manager.setHocker((Data d) -> {
			if (d instanceof Message) {
				//メッセージならホストに渡す
				crh.pushMessage((Message) d);
			} else if (d instanceof Inquire) {
				//ホスト起動確認なら起動中として応答コマンドを送る
				Inquire inq = (Inquire) d;
				if (inq.comandType == Inquire.COMAND_TYPE_INQUIRE) {
					Inquire ans = new Inquire();
					ans.comandType = Inquire.COMAND_TYPE_ANSWER;
					ans.name = hostState.getUserName();
					ans.remoteIpAddress = inq.sourceIpAddress;
					ans.sourceIpAddress = hostState.getIpAddr();
					manager.sendData(ans);
				}
			} else if (d instanceof Connection) {
				Connection con = (Connection) d;
				if (con.comandType == Connection.COMAND_TYPE_REQUEST) {
					if (con.connectionFlg) {
						//接続要求である場合、OKの応答コマンドを返す。
						Connection ans = new Connection();
						ans.remoteIpAddress = con.sourceIpAddress;
						ans.comandType = Connection.COMAND_TYPE_ANSWER;
						ans.connectionFlg = true;
						ans.user = hostState;

						//新規追加ユーザーであったらメッセージを出力
						boolean b = crh.addClientUser(con.user);
						if(b){
							Message m = new Message();
							m.sourceIpAddress = hostState.getIpAddr();
							m.messageSourceIpAddress = hostState.getIpAddr();
							m.name = hostState.getUserName();
							m.message = String.format("☆★☆[%s@%s]が参加☆★☆",
									con.user.getUserName(),
									con.user.getIpAddr());
							crh.pushMessage(m);
							manager.sendData(ans);
						}
					} else {
						//切断要求である場合、ユーザー情報を削除
						boolean b = crh.subClientUser(con.user);

						//ちゃんと削除できたらメッセージを出力
						if(b){
							Message m = new Message();
							m.sourceIpAddress = hostState.getIpAddr();
							m.messageSourceIpAddress = hostState.getIpAddr();
							m.name = hostState.getUserName();
							m.message = String.format("### [%s@%s]が退出 ###",
									con.user.getUserName(),
									con.user.getIpAddr());

							crh.pushMessage(m);
						}
					}
				}
			}
		});
		System.out.println("(^_^) : created the room.");
		//ホストを起動
		crh.executeHostInput();

		System.out.println("(^_^) : closed the room.");
		// 残りのユーザーにルームを閉じたことを伝える
		Collection<User> users = crh.getClientUserList();
		for (User u : users) {
			Connection c = new Connection();
			c.remoteIpAddress = u.getIpAddr();
			c.comandType = Connection.COMAND_TYPE_FORCED;
			c.connectionFlg = false;
			c.user = hostState;
			manager.sendData(c);
		}

		//相手から帰ってきたメッセージを捌くインターフェースを取り除く
		manager.removeHocker();
	}

	/**
	 * システムの終了
	 */
	public void exit() {
		manager.exit();
	}


	/**
	 * システムが受け付けるモードの列挙
	 * @author masaki
	 *
	 */
	enum Order {
		Help(":help", ""),
		OtherUserChatRoomCheck(":search"," (+bloadcastAddress) 他のユーザーが部屋を起動しているか確認します。"),
		OherUserChatRoomAccess(":access","+ipAddress 他ユーザーが起動している部屋にアクセスします。"),
		HostChatRoomStart(":host", "自身が部屋を起動します"),
		Exit(":exit", "システムを終了します。"), NULL(
				null, null) {
			@Override
			boolean isAccurateCommand(String command) {
				//必ず一致しない
				return false;
			}
		};

		/**
		 * コマンドの定義値
		 */
		public final String commandDefinition;
		/**
		 * ヘルプで出力するメッセージ
		 */
		public final String message;

		Order(String commandDefinition, String message) {
			this.commandDefinition = commandDefinition;
			this.message = message;
		}

		/**
		 * コマンドが定義値と一致しているか確認
		 * @param command
		 * @return
		 */
		boolean isAccurateCommand(String command) {
			return commandDefinition.equals(command);
		};

		/**
		 * コマンドと一致する列挙を返す
		 * @param command
		 * @return
		 */
		private static Order getOrder(String command) {
			for (Order o : Order.values()) {
				if (o.isAccurateCommand(command))
					return o;
			}
			return NULL;
		}
	}
}
