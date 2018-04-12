package org.brewchain.cwv.wlt.enums;

public enum PendStatusEnum {

	//01: 已成交； 02 ： 挂单中； 03 ： 已撤销；
	OK("已成交","01"),
	PEDDING("挂单中","02"),
	CANCEL("已撤销","03");


	private String value;
	private String name;

	private PendStatusEnum(String name, String value) {
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
