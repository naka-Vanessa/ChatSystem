package jp.co.technica.communication.state;

import java.io.Serializable;

/**
 * ユーザー情報を管理するクラス<br>
 * コンストラクタに渡されたユーザー名・IPアドレスを不変的に保持します。
 * @author masaki
 *
 */
public class User implements Serializable{
	/**
	 * ユーザー名
	 */
	private final String userName;
	/**
	 * IPアドレス
	 */
	private final String ipAddr;

	/**
	 * コンストラクタ
	 */
	public User(String userName,String ipAddr){
		this.userName = userName;
		this.ipAddr = ipAddr;
	}

	/**
	 * ユーザー名を返します
	 */
	public String getUserName(){
		return userName;
	}
	/**
	 * IPアドレスを返します
	 */
	public String getIpAddr(){
		return ipAddr;
	}
}
