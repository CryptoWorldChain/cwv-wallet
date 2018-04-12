package org.brewchain.cwv.wlt.enums;

public enum PendTypeEnum {

	BUY_IN("买入","01"),
	SALE_OUT("卖出","02"),;


	private String value;
	private String name;

	private PendTypeEnum(String name, String value) {
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
