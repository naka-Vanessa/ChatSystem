package jp.co.technica.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class CommunicationSender implements Runnable{
	private final DatagramSocket socket;
	private boolean continuationFlg = true;
	private final IPacketCreater creater;


	interface IPacketCreater{
		DatagramPacket createPacket();
	}

	/**
	 * パッケージプライベート
	 * @throws SocketException
	 */
//	CommunicationSender(DatagramSocket socket,IPacketCreater creater) {
	CommunicationSender(IPacketCreater creater) throws SocketException {
		socket = new DatagramSocket();
		this.creater = creater;
	}
	@Override
	public void run() {
		while(continuationFlg){
			DatagramPacket packet = creater.createPacket();
			if(packet != null){
				try {
					socket.send(packet);
				} catch (IOException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			}
		}
		socket.close();
	}

	public void exit(){
		continuationFlg = false;
	}

}
