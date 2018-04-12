package org.brewchain.cwv.wlt.entity;

import java.util.List;

import lombok.Data;

@Data
public class RegisterRetEntity {

	String err_code;
	String msg;
	List<AddressEntity> addrs;
	AssetEntity fbc_asset;
}
