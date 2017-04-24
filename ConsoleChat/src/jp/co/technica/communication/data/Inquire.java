package jp.co.technica.communication.data;

/**
 * 接続可能かを問うコマンド<br>
 * @author masaki
 *
 */
public class Inquire extends Data{
	/**
	 * 問いかけ時、comandTypeに設定する値<br>
	 * [:search]実行時に使用
	 */
	public static final int COMAND_TYPE_INQUIRE = 1;
	/**
	 * 問いかけに対する回答時、comandTypeに設定する値<br>
	 * [:host]実行時に使用
	 */
	public static final int COMAND_TYPE_ANSWER = 2;

	/**
	 * 問いかけ・回答を意味するコマンドタイプ<br>
	 */
	public int comandType = 0;
	/**
	 * 問いかけに応じたときの名称<br>
	 * (ConnectionクラスのUserと同じ役割なので、Dataクラスにあった方が良かった？)
	 */
	public String name;
}
