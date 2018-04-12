package org.brewchain.cwv.wlt.enums;

public enum CertStatusEnum {


	RECEIVE("RECEIVE","00"),//已收到
	OK("OK","01"),//审核通过
	PROCESS("PROCESS","02"),//审核中
	FAIL("FAIL","03"),//审核未通过
	LOCK("LOCK","04");//锁定


	private String value;
	private String name;

	private CertStatusEnum(String name, String value) {
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
