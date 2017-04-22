package jp.co.technica.communication.data;

public class Message extends Data{
	public String message;
	public String messageSourceIpAddress;
	public String name;

	public void copy(Message m){
		this.message = m.message;
		this.messageSourceIpAddress = m.messageSourceIpAddress;
		this.name = m.name;
	}
}
