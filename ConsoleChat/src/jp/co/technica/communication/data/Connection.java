package jp.co.technica.communication.data;

import jp.co.technica.communication.state.User;

/**
 * 接続・切断を行うコマンド
 * @author masaki
 *
 */
public class Connection extends Data{
	/**
	 * 要求時にcomandTypeに設定する
	 */
	public static final int COMAND_TYPE_REQUEST = 0;
	/**
	 * 応答時にcomandTypeに設定する
	 */
	public static final int COMAND_TYPE_ANSWER = 1;
	/**
	 * 強制的に実行させる際、comandTypeに設定する<br>
	 * 接続を強制的に切る場合などに使用される
	 */
	public static final int COMAND_TYPE_FORCED = 2;


	/**
	 * 接続・切断を示すフラグ<br>
	 * true : 接続   false : 切断
	 */
	public boolean connectionFlg = false;
	/**
	 * 要求・応答などを意味するコマンドタイプ
	 */
	public int comandType = 0;

	/**
	 * 送信元のユーザー情報<br>
	 * (Dataクラスにあったほうが良い？)
	 */
	public User user;
}
