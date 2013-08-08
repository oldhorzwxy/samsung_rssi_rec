package cn.navior.tool.sumsungrssirec;

/**
 * Java class model for the record of rssi measuring.
 * @author wangxiayang
 *
 */
public class RecordItem {

	private String mac;
	private String name;
	private int rssi;
	private String datetime;
	
	public RecordItem(String mac) {
		this.mac = mac;
	}
	
	public String getMac() {
		return mac;
	}
	
	public void setMac(String mac) {
		this.mac = mac;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getRssi() {
		return rssi;
	}
	
	public void setRssi(int rssi) {
		this.rssi = rssi;
	}

	public String getDatetime() {
		return datetime;
	}

	public void setDatetime( String datetime ) {
		this.datetime = datetime;
	}
}
