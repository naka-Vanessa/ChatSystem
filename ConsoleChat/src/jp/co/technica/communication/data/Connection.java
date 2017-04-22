package jp.co.technica.communication.data;

import jp.co.technica.communication.state.User;

public class Connection extends Data{
	public static final int COMAND_TYPE_REQUEST = 0;
	public static final int COMAND_TYPE_ANSWER = 1;

	public boolean connectionFlg = false;
	public int comandType = 0;
	public User user;
}
