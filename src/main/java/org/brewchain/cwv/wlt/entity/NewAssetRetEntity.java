package org.brewchain.cwv.wlt.entity;

import lombok.Data;

@Data
public class NewAssetRetEntity {

	String err_code;
	String msg;
	String bc_txid;
	AssetEntity asset;
	String raw_data;
}
