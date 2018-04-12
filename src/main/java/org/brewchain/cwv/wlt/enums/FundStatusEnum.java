package org.brewchain.cwv.wlt.enums;

public enum FundStatusEnum {

	OK("正常","01"),
	ERROR("不正常","02");


	private String value;
	private String name;

	private FundStatusEnum(String name, String value) {
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
