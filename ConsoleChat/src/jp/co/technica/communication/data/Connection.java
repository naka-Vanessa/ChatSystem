package jp.co.technica.communication.data;

public class Connection {
	public static final int COMAND_TYPE_REQUEST = 0;
	public static final int COMAND_TYPE_ANSWER = 1;

	public boolean connectionFlg = false;
	public int comandType = 0;
}
