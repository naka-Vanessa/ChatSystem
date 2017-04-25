package jp.co.technica.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * 送信用スレッド処理（Runnable）
 * このクラスでは、やり取りするデータのフォーマットに依存させない。
 * IPacketCreaterインターフェースを通して上位にパケットデータを作らせる。
 *
 * @author masaki
 *
 */
public class CommunicationSender implements Runnable{

	/**
	 * UDPソケット
	 */
	private final DatagramSocket socket;

	/**
	 * 継続実施フラグ
	 */
	private boolean continuationFlg = true;
	/**
	 * パケットを作るためのインターフェース
	 */
	private final IPacketCreater creater;


	/**
	 * パケットを作るためのインターフェース<br>
	 */
	interface IPacketCreater{
		DatagramPacket createPacket();
	}

	/**
	 * パッケージプライベート
	 * @throws SocketException
	 */
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

	/**
	 * 送信スレッド終了
	 */
	public void exit(){
		continuationFlg = false;
	}

}
