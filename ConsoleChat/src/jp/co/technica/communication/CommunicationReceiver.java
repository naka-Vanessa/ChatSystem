package jp.co.technica.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * 受信専用のスレッド処理（Runnable）クラス
 * このクラスでは、やり取りするデータのフォーマットに依存させない。
 * IPacketHandlerインターフェースを通して上位にパケットデータを渡し、上位にデータを作らせる。
 * @author masaki
 *
 */
public class CommunicationReceiver implements Runnable{
	/**
	 * UDPソケット
	 */
	private final DatagramSocket socket;
	/**
	 * 継続実施フラグ
	 */
	private boolean continuationFlg = true;
	/**
	 * パケット
	 */
	private DatagramPacket packet;
	/**
	 * 受信したパケットを扱うためのインターフェース
	 */
	private final IPacketHandler handr;

	/**
	 * 受信したパケットを扱うためのインターフェース
	 */
	interface IPacketHandler{
		void popPackt(DatagramPacket packet);
	}

	/**
	 * パッケージプライベート
	 * @param socket
	 * @throws SocketException
	 */
	CommunicationReceiver(InetAddress hostAddress,int hostPortNumber, IPacketHandler handr,int packetSize) throws SocketException{
		socket = new DatagramSocket(hostPortNumber,hostAddress);
		socket.setBroadcast(true);
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

	/**
	 * 受信スレッドの終了
	 */
	public void exit(){
		continuationFlg = false;
		socket.close(); //Futuer.cancel() でブロックが解放されないため、強引に閉じる
	}
}
