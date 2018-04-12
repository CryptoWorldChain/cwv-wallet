package org.brewchain.cwv.wlt.entity;

import lombok.Data;

@Data
public class AddressEntity {

	String hex_addr;
	String pki;
	String pub;
	String alias;
	String rpmd_hash;
}
