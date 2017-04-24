package jp.co.technica.communication.data;

/**
 * チャットルームで会話を行う時のコマンド
 * @author masaki
 *
 */
public class Message extends Data{
	/**
	 * メッセージの内容
	 */
	public String message;
	/**
	 * メッセージを生成した人のIPアドレス
	 */
	public String messageSourceIpAddress;
	/**
	 * メッセージを生成した人の名前
	 */
	public String name;

	/**
	 * 引数に渡されたメッセージをコピーします。<br>
	 * コピーされる内容は、Messageクラスが保有する変数のみです。
	 */
	public void copy(Message m){
		this.message = m.message;
		this.messageSourceIpAddress = m.messageSourceIpAddress;
		this.name = m.name;
	}
}
