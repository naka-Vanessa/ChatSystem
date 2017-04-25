package jp.co.technica.communication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jp.co.technica.communication.CommunicationReceiver.IPacketHandler;
import jp.co.technica.communication.CommunicationSender.IPacketCreater;
import jp.co.technica.communication.data.Data;

/**
 * 通信を管理するマネージャークラスです。<br>
 * CommunicationReceiverが拾ったデータ・CommunicationSenderが送るべきデータを管理します。
 * 受信データをもらう場合は、IReceiveDataHookerを実装したインスタンスをsetHooker()で登録してください。
 *
 * @author masaki
 *
 */
public class CommunicationManager {
	/**
	 * 送信用クラス
	 */
	private CommunicationSender sender;
	/**
	 * 受信用クラス
	 */
	private CommunicationReceiver receiver;
	/**
	 * Receiverとして受けるポート番号
	 */
	private final int hostPortNumber;
	/**
	 * Senderとして送る先のポート番号
	 */
	private final int remotePortNumber;

	/**
	 * Receiver、DataPicker、Sender用のスレッド
	 */
	private final ExecutorService thread = Executors.newFixedThreadPool(3);
	/**
	 * Receiver、DataPicker、SenderのFuture<br>
	 * 主にブロック状態にあるスレッドの制御を解放するために使用
	 */
	private List<Future<?>> futures = new ArrayList<>();
	/**
	 * アプリケーションを起動しているマシンのIPアドレス<br>
	 * LANカードが複数刺さっている場合このアプリは死んでしまう…。
	 */
	private InetAddress hostAddress;

	/**
	 * 実行の継続を意味するフラグ<br>
	 * exit()がたたかれたときにfalseになる。
	 */
	private boolean executionFlg = true;

	/**
	 * パケットのバッファサイズ<br>
	 * この値にあまり意味はなく、適当につけた。
	 */
	private static final int PACKET_SIZE = 2048;

	/**
	 * 受信したデータをため込むキュー<br>
	 */
	private LinkedBlockingQueue<Data> receiveQueue;
	/**
	 * 送信するデータをため込むキュー
	 */
	private LinkedBlockingQueue<Data> sendQueue;

	/**
	 * 受信データを処理するインターフェースの実装<br>
	 * インターフェースを通してデータを渡す。<br>
	 */
	private volatile IReceiveDataHooker hooker = NULL_HOOKER;

	/**
	 * 何も処理が無いフック。<br>
	 * hooker変数がnullにならないようにするために定義されている。
	 */
	private static final IReceiveDataHooker NULL_HOOKER = (Data d)->{};

	/**
	 * 拾ったデータの処理を定義するためのインターフェース
	 * @author masaki
	 *
	 */
	public interface IReceiveDataHooker{
		void hook(Data d);
	}


	/**
	 * ファクトリーメソッドから生成
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	private CommunicationManager(int hostPortNumber,int remotePortNumber,boolean bloadCast) throws SocketException, UnknownHostException {
		hostAddress = Inet4Address.getLocalHost();
		this.hostPortNumber = hostPortNumber;
		this.remotePortNumber = remotePortNumber;
	}


	/**
	 * 受信スレッドに関する情報の生成<br>
	 * IPacketHandlerはCommunicationReceiverのパケットを処理するためのインターフェースである。<br>
	 *
	 */
	private void createCommunicationReceiver() {
		try {
			IPacketHandler handl = (pac) -> {
				byte[] data = pac.getData();
				try{
					ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(data));
					Object o = in.readObject();
					if(o instanceof Data){
						receiveQueue.offer((Data)o);
					}
				}catch(IOException | ClassNotFoundException e){

				}
			};

			receiver = new CommunicationReceiver(hostAddress, hostPortNumber,handl,PACKET_SIZE);

		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		receiveQueue = new LinkedBlockingQueue<>();
	}

	/**
	 * 送信スレッドに関する情報の生成<br>
	 * IPacketCreaterはCommunicationSenderのパケットを作成するためのインターフェースである。<br>
	 * 送信するDataが規定値のバイト数を超えている場合、パケットを作成しない。
	 *
	 */
	private void createCommunicationSender() {
		try {
			IPacketCreater creater = () ->{
				try{
				    DatagramPacket packet = null;
					while(executionFlg){
						Data d;
						try {
							d = sendQueue.take();
						    ByteArrayOutputStream bos = new ByteArrayOutputStream();
						    ObjectOutput out = new ObjectOutputStream(bos);
						    out.writeObject(d);
						    byte[] bytes = bos.toByteArray();
						    if(bytes.length <= PACKET_SIZE){
						    	packet = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(d.remoteIpAddress),this.remotePortNumber);
						    }
						} catch (InterruptedException e) {
						}
					    break;
					}
				    return packet;
				}catch(IOException e){

				}
				return null;
			};

			sender = new CommunicationSender(creater);
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		sendQueue = new LinkedBlockingQueue<>();
	}

	/**
	 * 受信・送信クラスをスタートします。<br>
	 * CommunicationManagerが生成されたと同時に起動します。<br>
	 * DataPickerのスレッドも起動します。
	 */
	private void start() {
		executionFlg=true;
		if (receiver != null) {
			futures.add(thread.submit(receiver));
			//DataPicker
			futures.add(thread.submit(()->{
				while(executionFlg){
					Data d = popData();
					hooker.hook(d);
				}
			}));
		}
		if (sender != null) {
			futures.add(thread.submit(sender));
		}
	}

	/**
	 * 通信を終了します。
	 * Receiver・DataPicker・Senderのすべてが停止します。
	 */
	public void exit() {
		executionFlg = false;
		if (receiver != null) {
			receiver.exit();
		}
		if (sender != null) {
			sender.exit();
		}
		//Sender・Receiverでブロックされている制御を解放
		for(Future<?> future : futures){
			future.cancel(true);
		}
		thread.shutdown();
		try {
			thread.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 受信用キューからデータを取り出します。
	 * @return
	 */
	private Data popData(){
		Data d = null;
		try {
			d = receiveQueue.take();
		} catch (InterruptedException e) {
		}
		return d;
	}

	/**
	 * 受信データをもらって処理を行うIReceiveDataHooker実装インスタンスをセットします。
	 * @return
	 */
	public void setHocker(IReceiveDataHooker hooker){
		this.hooker = hooker;
	}

	/**
	 * 受信データをもらって処理を行うIReceiveDataHookerの空処理インスタンスをセットします。
	 */
	public void removeHocker(){
		hooker = NULL_HOOKER;
	}

	/**
	 * Dataを継承したコマンドを送信します。<br>
	 * このメソッド内でData.sourceIpAddressの値を設定します。
	 * @param d
	 */
	public void sendData(Data d){
		d.sourceIpAddress = hostAddress.getHostAddress();
		sendQueue.add(d);
	}

	/**
	 * CommunicationManagerインスタンスを返します
	 *
	 * @return
	 */
	public static CommunicationManager createCommunicationManager(
			int hostPortNumber,int remotePortNumber,boolean bloadcast) {
		CommunicationManager c = null;
		try {
			c = new CommunicationManager(hostPortNumber,remotePortNumber,bloadcast);
			c.createCommunicationSender();
			c.createCommunicationReceiver();
			c.start();
		} catch (SocketException | UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return c;
	}
	/**
	 * CommunicationManagerインスタンスを返します
	 *
	 * @return
	 */
	public static CommunicationManager createCommunicationManager(
			int portNumber,boolean bloadcast) {
		return createCommunicationManager(portNumber,portNumber,bloadcast);
	}
	/**
	 * 送信のみ可能なCommunicationManagerインスタンスを返します
	 *
	 * @return
	 */
	public static CommunicationManager createCommunicationManagerSendOnly(
			int hostPortNumber,int remotePortNumber,boolean bloadcast) {
		CommunicationManager c = null;
		try {
			c = new CommunicationManager(hostPortNumber,remotePortNumber,bloadcast);
			c.createCommunicationSender();
			c.start();
		} catch (SocketException | UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return c;
	}
	/**
	 * 送信のみ可能なCommunicationManagerインスタンスを返します
	 *
	 * @return
	 */
	public static CommunicationManager createCommunicationManagerSendOnly(
			int portNumber,boolean bloadcast) {
		//送り元・送り先のポート番号が同じであれば、ソケットは１つで良い
		//だが、余裕が無いのでそうなるような処理は入れない
		return createCommunicationManagerSendOnly(portNumber,portNumber,bloadcast);
	}

	/**
	 * 受信のみ可能なCommunicationManagerインスタンスを返します
	 *
	 * @return
	 */
	public static CommunicationManager createCommunicationManagerReceiveOnly(
			int hostPortNumber,int remotePortNumber,boolean bloadcast) {
		CommunicationManager c = null;
		try {
			c = new CommunicationManager(hostPortNumber,remotePortNumber,bloadcast);
			c.createCommunicationReceiver();
			c.start();
		} catch (SocketException | UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return c;
	}

	/**
	 * 受信のみ可能なCommunicationManagerインスタンスを返します
	 *
	 * @return
	 */
	public static CommunicationManager createCommunicationManagerReceiveOnly(
			int portNumber,boolean bloadcast) {
		return createCommunicationManagerReceiveOnly(portNumber,portNumber,bloadcast);
	}

}
