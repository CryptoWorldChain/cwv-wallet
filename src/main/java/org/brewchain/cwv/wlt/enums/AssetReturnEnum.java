package org.brewchain.cwv.wlt.enums;

public enum AssetReturnEnum {

	OK("success","000000"),
	/**
	 * 用户代码错误
	 */
	FAIL_ERROR_USER_CODE("用户代码错误","02"),
	FAIL_ERROR_USER_CODE_OR_PASSWD("用户代码或密码错误","03"),
	FAIL_ERROR_NO_PARAM("无用户信息","04"),
	FAIL_ERROR_CERTIFICATION_TYPE("认证类型错误","05"),
	FAIL_ERROR_NO_CERTIFICATE("未认证","06"),
	FAIL_ERROR_ORGID_OR_PERID("组织代码或用户代码错误","07"),
	FAIL_ERROR_NO_EMPTY_ADDR("用户无空地址用于创建/转入资产","08"),
	FAIL_ERROR_NO_ASSET_INFO("未找到资产信息","09"),
	FAIL_ERROR_CHAIN("数据入链错误", "10"),
	FAIL_ERROR_USER_CAN_NOT_CREATE_ASSET("该用户无权利发行资产", "11"),
	FAIL_ERROR_HEX_ADDR_IS_NULL("地址为空","12"),
	FAIL_ERROR_RPMD_HASH_IS_NULL("地址公钥hash为空","13"),
	FAIL_ERROR_CREATE_COUNT_TOO_SMALL("发布资产数量不能小于1","14"),
	FAIL_ERROR_HEX_ADDR_ERROR("地址错误","15")
	
	;


	private String value;
	private String name;

	private AssetReturnEnum(String name, String value) {
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
