package br.com.rp;


public class DankiaFPTemplate {

	private int size;
	private int userId;
	private String strMinutiae;
	private int status;

	public int getUserId() {
		return this.userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getSize() {
		return this.size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getStrMinutiae() {
		return this.strMinutiae;
	}

	public void setStrMinutiae(String strMinutiae) {
		this.strMinutiae = strMinutiae;
	}



	public void showData() {
		System.out.println("UserId : " + this.userId + "\n" + "pDataBase64: " + this.strMinutiae + "\n" + "Size: "
				+ this.size );
	}

	public int getStatus() {
		return this.status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
}
