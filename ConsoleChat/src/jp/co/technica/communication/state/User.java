package jp.co.technica.communication.state;

public class User {
	private final String userName;
	private final String ipAddr;

	public User(String userName,String ipAddr){
		this.userName = userName;
		this.ipAddr = ipAddr;
	}

	public String getUserName(){
		return userName;
	}
	public String getIpAddr(){
		return ipAddr;
	}
}
