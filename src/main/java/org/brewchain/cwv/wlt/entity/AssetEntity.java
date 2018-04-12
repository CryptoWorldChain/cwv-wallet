package org.brewchain.cwv.wlt.entity;

import lombok.Data;

@Data
public class AssetEntity {

	AddressEntity address;
	String bc_txid;
	FundEntity fund;
	String alias;
	String metadata;
	FundEntity fbc_fund;
}
