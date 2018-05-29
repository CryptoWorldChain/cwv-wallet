package org.brewchain.cwv.wlt.dao;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddress;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltTx;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.IJPAClient;
import onight.tfw.ojpa.api.OJpaDAO;
import onight.tfw.ojpa.api.annotations.StoreDAO;

@iPojoBean
@Provides(specifications = { IJPAClient.class, ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "daos")
@Slf4j
@Data
public class Daos implements ActorService, IJPAClient {
	
	@ActorRequire
	public SysDBProvider dbprovider;
	
	@StoreDAO
	public OJpaDAO<CWVWltAddress> wltAddressDao;
	
	@StoreDAO
	public OJpaDAO<CWVWltTx> wltTxDao;
	
	@Override
	public void onDaoServiceAllReady() {
		log.debug("AllDao Ready........");
	}

	@Override
	public void onDaoServiceReady(DomainDaoSupport arg0) {

	}
}
