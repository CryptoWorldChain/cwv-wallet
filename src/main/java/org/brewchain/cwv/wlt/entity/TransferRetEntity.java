package org.brewchain.cwv.wlt.entity;

import lombok.Data;

@Data
public class TransferRetEntity {

	String err_code;
	String msg;
	String bc_txid;
}
