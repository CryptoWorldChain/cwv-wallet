package org.brewchain.cwv.wlt.helper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddrExample;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.outils.serialize.UUIDGenerator;

/**
 * baseHelper
 * 
 * @author jack
 *
 */
@iPojoBean
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Slf4j
@Data
public class AccountHelper implements ActorService {

	@ActorRequire
	Daos daos;

	/**
	 * 查询用户钱包账户信息 <br>
	 * 包含查询平台钱包账户信息<br>
	 * 
	 * 如果查询平台账户信息， 则 userId 赋值为 <strong>空</strong>
	 * 
	 * @param coinType
	 *            币种类型
	 * @param userId
	 *            用户ID
	 * @return 用户信息
	 * @throws Exception
	 */
	public CWVWltAddr getUserAccount(String coinType, String userId,String addrType) throws Exception {
		/**
		 * userId 为空 则查询平台钱包账户 userId 不为空 则查询平台用户钱包账户
		 */
		CWVWltAddrExample accountExample = new CWVWltAddrExample();
		
		if (StringUtils.isNotBlank(userId)) {
			// 根据币种查询当前操作人账户id
			accountExample.createCriteria().andAddrTypeEqualTo(addrType).andUserIdEqualTo(userId);
		} else {
			// 获取平台钱包信息
			accountExample.createCriteria().andAddrTypeEqualTo(addrType);
		}
		
		List<Object> obj = daos.wltAddrDao.selectByExample(accountExample);
		if(obj!=null && obj.size()>0){
			return (CWVWltAddr) obj.get(0);
		}
		return null;
	}

}
