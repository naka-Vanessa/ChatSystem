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
 * CommunicationReceiverが拾ったデータ・CommunicationSenderが送るべきデータ
 *
 * @author masaki
 *
 */
public class CommunicationManager {
	private CommunicationSender sender;
	private CommunicationReceiver receiver;
	private final int hostPortNumber;
	private final int remotePortNumber;
	private final ExecutorService threadPool = Executors.newFixedThreadPool(3);
	private List<Future<?>> futures = new ArrayList<>();
	private InetAddress hostAddress;
	private boolean executionFlg = true;
	private static final int PACKET_SIZE = 2048;

	private LinkedBlockingQueue<Data> receiveQueue;
	private LinkedBlockingQueue<Data> sendQueue;
//	private final IPacketHandler handl;
//	private final IPacketCreater creater;

	private volatile IReceiveDataHooker hooker;
	private static final IReceiveDataHooker NULL_HOOKER = (Data d)->{};
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

	private void createCommunicationReceiver() {
//		receiver = new CommunicationReceiver(socket, handl,PACKET_SIZE);
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
	 * CommunicationManagerが生成されたと同時に起動します。
	 */
	private void start() {
		executionFlg=true;
		if (receiver != null) {
			futures.add(threadPool.submit(receiver));
			futures.add(threadPool.submit(()->{
				while(executionFlg){
					Data d = popData();
					if(hooker != null){
						hooker.hook(d);
					}
				}
			}));
		}
		if (sender != null) {
			futures.add(threadPool.submit(sender));
		}
	}

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
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

//	public Data popData(){
	private Data popData(){
		Data d = null;
		try {
			d = receiveQueue.take();
		} catch (InterruptedException e) {
		}
		return d;
	}

	public void setHocker(IReceiveDataHooker hooker){
		this.hooker = hooker;
	}
	public void removeHocker(){
		hooker = NULL_HOOKER;
	}
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
