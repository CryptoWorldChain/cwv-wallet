package org.brewchain.cwv.wlt.enums;

public enum TransferTypeEnum {

	TRANSFER("转账交易","01"),
	PEND("挂单交易","02");


	private String value;
	private String name;

	private TransferTypeEnum(String name, String value) {
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
