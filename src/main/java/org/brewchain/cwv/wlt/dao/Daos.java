package org.brewchain.cwv.wlt.dao;

import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.cwv.wlt.dbgens.bc.entity.CWVBcWwwUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAccessLog;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertOrg;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertPer;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFund;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltMonitor;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltPend;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltTransfer;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;

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
@Slf4j
@Data
public class Daos implements ActorService, IJPAClient {
	
	@ActorRequire
	public SysDBProvider dbprovider;
	
	@StoreDAO
	public OJpaDAO<CWVWltUser> wltUserDao;
	@StoreDAO
	public OJpaDAO<CWVWltCertOrg> wltCertOrgDao;
	@StoreDAO
	public OJpaDAO<CWVWltCertPer> wltCertPerDao;
	@StoreDAO
	public OJpaDAO<CWVWltAddr> wltAddrDao;
	@StoreDAO
	public OJpaDAO<CWVWltAsset> wltAssetDao;
	@StoreDAO
	public OJpaDAO<CWVWltFund> wltFundDao;
	@StoreDAO
	public OJpaDAO<CWVWltCertOrg> wltCerOrgDao;
	
	@StoreDAO
	public OJpaDAO<CWVWltCertPer> wltCerPerDao;
	
	@StoreDAO
	public OJpaDAO<CWVWltTransfer> wltTransferDao;
	
	@StoreDAO
	public OJpaDAO<CWVWltPend> wltPendDao;
	
	@StoreDAO
	public OJpaDAO<CWVWltAccessLog> wltAccessLogDao;
	
	@StoreDAO
	public OJpaDAO<CWVBcWwwUser> bcWwwUserDao;
	
	@StoreDAO
	public OJpaDAO<CWVWltMonitor> wltMonitorDao;
	
	@Override
	public void onDaoServiceAllReady() {
		log.debug("AllDao Ready........");
	}

	@Override
	public void onDaoServiceReady(DomainDaoSupport arg0) {

	}
}
