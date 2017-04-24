package jp.co.technica.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
	CommunicationReceiver(InetAddress hostAddress,int hostPortNumber, IPacketHandler handr,int packetSize) throws SocketException{
		socket = new DatagramSocket(hostPortNumber,hostAddress);
		socket.setBroadcast(true);
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
			} catch (SocketException e){
				if(continuationFlg == false){
					//終了処理でブロックを解放する方法がないため、継続フラグが落ちていたら無視する。
				}else{
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		socket.close();
	}

	public void exit(){
		continuationFlg = false;
		socket.close(); //Futuer.cancel() でブロックが解放されないため、強引に閉じる
	}
}
