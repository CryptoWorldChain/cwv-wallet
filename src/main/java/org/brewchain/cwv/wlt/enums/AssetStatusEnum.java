package org.brewchain.cwv.wlt.enums;

public enum AssetStatusEnum {

	//1自建；2挂单；3冻结；4转出；5转入...
	BUILD_SELF("自建","01"),//全部为自建的资产,发行资产后状态为自建
	//TODO 挂单中状态是否使用，挂单部分资产，剩余资产的状态，如果再次进行挂单，状态如何，结束后的状态变化等
	PNEDING("挂单中","02"),//挂单结束状态变更情况：1:转出（挂单交易完成，所有资产全部交易）；2:自建（挂单交易取消；挂单交易完成，交易部分资产）；4:冻结
	FREEZE("冻结","03"),//转移到错误的地址
	TRANSFER_IN("转入","04"),//全部为其他地方转入的资产
	TRANSFER_OUT("转出","05"),//资产全部耗尽
	INIT("初始化", "06");


	private String value;
	private String name;

	private AssetStatusEnum(String name, String value) {
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
