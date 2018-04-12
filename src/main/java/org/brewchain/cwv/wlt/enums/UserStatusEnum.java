package org.brewchain.cwv.wlt.enums;

public enum UserStatusEnum {

	OK("OK","01"),
	FAIL("FAIL","02"),
	DEL("DEL","03");


	private String value;
	private String name;

	private UserStatusEnum(String name, String value) {
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
