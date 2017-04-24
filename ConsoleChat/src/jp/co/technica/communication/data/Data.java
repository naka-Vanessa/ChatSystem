package jp.co.technica.communication.data;

import java.io.Serializable;

/**
 * コマンドのベースクラス
 * @author masaki
 *
 */
public class Data implements Serializable{
	/**
	 * 送信先のIPアドレス<br>
	 * "xxx.xxx.xxx.xxx"
	 */
	public String remoteIpAddress;
	/**
	 * 送信元のIPアドレス<br>
	 * "xxx.xxx.xxx.xxx"
	 */
	public String sourceIpAddress;
}
