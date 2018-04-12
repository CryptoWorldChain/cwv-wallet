package org.brewchain.cwv.wlt.enums;

public enum TransferStatusEnum {

	OK("交易正确完成","01"),
	FAIL("交易失败","02");


	private String value;
	private String name;

	private TransferStatusEnum(String name, String value) {
		this.value = value;
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return this.getValue() + "-" + this.getName();
	}
	
}
