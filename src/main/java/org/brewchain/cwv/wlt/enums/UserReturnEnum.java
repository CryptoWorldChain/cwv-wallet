package org.brewchain.cwv.wlt.enums;

public enum UserReturnEnum {

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
	FAIL_ERROR_REGISTER_TO_CHAIN("链上注册用户失败","08"),
	FAIL_ERROR_REGISTER_TO_CHAIN_ERROR("链上注册用户错误","09")
	
	;


	private String value;
	private String name;

	private UserReturnEnum(String name, String value) {
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
