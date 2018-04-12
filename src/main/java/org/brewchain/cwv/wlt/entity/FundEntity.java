package org.brewchain.cwv.wlt.entity;

import lombok.Data;

@Data
public class FundEntity {

	double amount;
	double count;
	double total_fee;
	double net_fee;
	double discount;
	double fbc_count;
	double colored_btc;
	double colored_eth;
	double colored_xrp;
}
