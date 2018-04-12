package org.brewchain.cwv.wlt.enums;

public enum TransferReturnEnum {

	OK("success","000000"),
	/**
	 * 用户代码错误
	 */
	
	FAIL_ERROR_ASSET_ID("资产代码错误","02"),
	FAIL_ERROR_SOURCE_AMOUNT_SHORT("余额不足","03"),
	FAIL_ERROR_SOURCE_FUND_NOT_FOUND("源资产金融信息错误","04"),
	FAIL_ERROR_TARGET_FUND_NOT_FOUND("目标资产金融信息错误","05"),
	FAIL_ERROR_NO_PARAM("无参数","06"),
	FAIL_ERROR_TARGET_ADDR_NOT_FOUND("目标地址未找到","07"),
	FAIL_ERROR_PEND_NOT_FOUND("挂单信息错误","08"),
	FAIL_ERROR_TRANSFER("转账失败", "09"),
	FAIL_ERROR_TARGET_USER_CODE("目标用户代码错误","10"),
	FAIL_ERROR_SOURCE_ADDR_NOT_FOUND("转账用户无钱包地址","11"),
	FAIL_ERRRO_SOURCE_ASSET_NOT_FUND("转账用户资产未找到","12"),
	FAIL_ERROR_IN_HEX_ADDR_IS_NULL("转账地址为空","13"),
	FAIL_ERROR_IN_PUB_HASH_IS_NULL("转账地址公钥hash为空","14"),
	FAIL_ERROR_IN_PKI_IS_NULL("转账地址私钥为空","15"),
	FAIL_ERROR_OUT_HEX_ADDR_IS_NULL("收账地址为空","16"),
	FAIL_ERROR_OUT_PUB_HASH_IS_NULL("收账地址公钥hash为空","17"),
	FAIL_ERROR_HEX_ADDR_ERROR("地址错误","18"),
	FAIL_ERROR_FORBIDDEN_ERROR("相同地址间不能转账", "19")
	;


	private String value;
	private String name;

	private TransferReturnEnum(String name, String value) {
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
