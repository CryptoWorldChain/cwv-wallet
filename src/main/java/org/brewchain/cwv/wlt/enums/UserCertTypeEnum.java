package org.brewchain.cwv.wlt.enums;

public enum UserCertTypeEnum {

	ORGANATION("ORGANATION","1"),
	PERSONAL("PERSONAL","2"),;


	private String value;
	private String name;

	private UserCertTypeEnum(String name, String value) {
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
