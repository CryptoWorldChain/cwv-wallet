package org.brewchain.cwv.wlt.enums;

public enum AddrTypeEnum {

	WALLET_ADDR("钱包地址","01"),
	ASSET_ADDR("资产地址","02"),;


	private String value;
	private String name;

	private AddrTypeEnum(String name, String value) {
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
