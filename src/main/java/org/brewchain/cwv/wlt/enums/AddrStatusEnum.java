package org.brewchain.cwv.wlt.enums;

public enum AddrStatusEnum {

	//查询用户地址只查询已使用和空地址，不使用错误地址，
	FULL("已使用","01"),//地址已使用
	ERROR("错误地址","02"),//错误地址
	EMPTY("空地址","03"),
	INIT("初始化","04");//空地址


	private String value;
	private String name;

	private AddrStatusEnum(String name, String value) {
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
