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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jp.co.technica.communication.CommunicationReceiver.IPacketHandler;
import jp.co.technica.communication.CommunicationSender.IPacketCreater;
import jp.co.technica.communication.data.Data;

public class CommunicationManager {
	private CommunicationSender sender;
	private CommunicationReceiver receiver;
//	private DatagramSocket socket;
	private final int hostPortNumber;
	private final int remotePortNumber;
	private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
	private InetAddress hostAddress;
	private boolean executionFlg = true;
	private static final int PACKET_SIZE = 2048;

	private ConcurrentLinkedQueue<Data> receiveQueue;
	private Object receiveLockObject = new Object();
	private ConcurrentLinkedQueue<Data> sendQueue;
	private Object sendLockObject = new Object();
	private final IPacketHandler handl;
	private final IPacketCreater creater;
	/**
	 * ファクトリーメソッドから生成
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	private CommunicationManager(int hostPortNumber,int remotePortNumber,boolean bloadCast) throws SocketException, UnknownHostException {
		hostAddress = Inet4Address.getLocalHost();
		System.out.println(hostAddress.getHostAddress() + "   " + hostPortNumber);
		this.hostPortNumber = hostPortNumber;
		this.remotePortNumber = remotePortNumber;
//		socket = new DatagramSocket(hostPortNumber,hostAddress);
//		socket.setBroadcast(true);

		handl = (pac) -> {
			byte[] data = pac.getData();
			try{
				ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(data));
				Object o = in.readObject();
				if(o instanceof Data){
					synchronized(receiveLockObject){
						receiveQueue.add((Data)o);
						receiveLockObject.notifyAll();
					}
				}
			}catch(IOException | ClassNotFoundException e){

			}
		};

		creater = () ->{
			try{
			    DatagramPacket packet = null;
				while(executionFlg){
					Data d = sendQueue.poll();
					if(d == null){
						synchronized(sendLockObject){
							try{
								sendLockObject.wait();
							}catch(InterruptedException e){
							}
						}
					}else{
					    ByteArrayOutputStream bos = new ByteArrayOutputStream();
					    ObjectOutput out = new ObjectOutputStream(bos);
					    out.writeObject(d);
					    byte[] bytes = bos.toByteArray();
					    if(bytes.length <= PACKET_SIZE){
					    	packet = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(d.remoteIpAddress),this.remotePortNumber);
					    }
					    break;
					}
				}
			    return packet;
			}catch(IOException e){

			}
			return null;
		};


	}

	private void createCommunicationReceiver() {
//		receiver = new CommunicationReceiver(socket, handl,PACKET_SIZE);
		try {
			receiver = new CommunicationReceiver(hostAddress, hostPortNumber,handl,PACKET_SIZE);
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		receiveQueue = new ConcurrentLinkedQueue<>();
	}

	private void createCommunicationSender() {
		try {
			sender = new CommunicationSender(creater);
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		sendQueue = new ConcurrentLinkedQueue<>();
	}

	/**
	 * 受信・送信クラスをスタートします。<br>
	 * CommunicationManagerが生成されたと同時に起動します。
	 */
	private void start() {
//		socket.connect(hostAddress, hostPortNumber);
//		System.out.println("socket : " + socket.isConnected());
		executionFlg=true;
		if (receiver != null) {
			System.out.println("receiver run start");
			threadPool.execute(receiver);
		}
		if (sender != null) {
			System.out.println("sender run start");
			threadPool.execute(sender);
		}
	}

	public void exit() {
		executionFlg = false;
		if (receiver != null) {
			receiver.exit();
			synchronized(receiveLockObject){
				receiveLockObject.notifyAll();
			}
		}
		if (sender != null) {
			sender.exit();
			synchronized(sendLockObject){
				sendLockObject.notifyAll();
			}
		}
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		socket.close();
	}

	public Data popData(){
		Data d = null;
		synchronized(receiveLockObject){
			while(executionFlg){
				d = receiveQueue.poll();
				if(d == null){
					try {
						receiveLockObject.wait();
					} catch (InterruptedException e) {
					}
				}else{
					break;
				}
			}
		}
		return d;
	}

	public void sendData(Data d){
		d.sourceIpAddress = hostAddress.getHostAddress();
		sendQueue.add(d);
		synchronized (sendLockObject) {
			sendLockObject.notifyAll();
		}
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

	public static CommunicationManager createCommunicationManagerReceiveOnly(
			int portNumber,boolean bloadcast) {
		return createCommunicationManagerReceiveOnly(portNumber,portNumber,bloadcast);
	}

}
