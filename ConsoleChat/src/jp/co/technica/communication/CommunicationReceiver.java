package jp.co.technica.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class CommunicationReceiver implements Runnable{
	private final DatagramSocket socket;
	private boolean continuationFlg = false;
	private DatagramPacket packet;
	private final IPacketHandler handr;

	interface IPacketHandler{
		void popPackt(DatagramPacket packet);
	}

	/**
	 * パッケージプライベート
	 * @param socket
	 * @throws SocketException
	 */
//	CommunicationReceiver(DatagramSocket socket,IPacketHandler handr,int packetSize){
	CommunicationReceiver(int hostPortNumber, IPacketHandler handr,int packetSize) throws SocketException{
		this.socket = new DatagramSocket(hostPortNumber);
		continuationFlg = true;
		packet = new DatagramPacket(new byte[2048], 2048);
		this.handr = handr;
	}

	@Override
	public void run() {
		while(continuationFlg){
			try {
				socket.receive(packet);
				handr.popPackt(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void exit(){
		continuationFlg = false;
		socket.close();
	}
}
