//package org.brewchain.cwv.wlt.helper;
//
//import java.math.BigDecimal;
//import java.util.Date;
//import java.util.List;
//
//import org.apache.commons.lang3.StringUtils;
//import org.apache.felix.ipojo.annotations.Provides;
//import org.brewchain.cwv.wlt.dao.Daos;
//
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import onight.osgi.annotation.iPojoBean;
//import onight.tfw.ntrans.api.ActorService;
//import onight.tfw.ntrans.api.annotation.ActorRequire;
//import onight.tfw.outils.serialize.UUIDGenerator;
//
///**
// * baseHelper
// * 
// * @author jack
// *
// */
//@iPojoBean
//@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
//@Slf4j
//@Data
//public class BaseHelper implements ActorService {
//
//	@ActorRequire
//	Daos daos;
//
//	/**
//	 * 查询用户钱包账户信息 <br>
//	 * 包含查询平台钱包账户信息<br>
//	 * 
//	 * 如果查询平台账户信息， 则 userId 赋值为 <strong>空</strong>
//	 * 
//	 * @param coinType
//	 *            币种类型
//	 * @param userId
//	 *            用户ID
//	 * @return 用户信息
//	 * @throws Exception
//	 */
//	public TXTpsAccountDetail getUserAccount(String coinType, String userId) throws Exception {
//		String accountId = "";
//		/**
//		 * userId 为空 则查询平台钱包账户 userId 不为空 则查询平台用户钱包账户
//		 */
//		if (StringUtils.isNotBlank(userId)) {
//			// 根据币种查询当前操作人账户id
//			TXTpsAccountExample accountExample = new TXTpsAccountExample();
//			accountExample.createCriteria().andUserIdEqualTo(userId).andStatusEqualTo("1");
//			List<Object> accountList = daos.txTpsAccountDao.selectByExample(accountExample);
//			TXTpsAccount account = new TXTpsAccount();
//			if (accountList != null && accountList.size() > 0) {
//				account = (TXTpsAccount) accountList.get(0);
//				accountId = account.getTxTpsAccountId();
//			} else {
//				throw new Exception("无此账户信息");
//			}
//		} else {
//			accountId = "1";
//		}
//		// 获取平台钱包信息
//		TXTpsAccountDetailExample accountDetailExample = new TXTpsAccountDetailExample();
//		accountDetailExample.createCriteria().andAccountIdEqualTo(accountId).andStatusEqualTo("1")
//				.andCoinIdEqualTo(coinType);
//		List<Object> accountDetailList = daos.txTpsAccountDetailDao.selectByExample(accountDetailExample);
//		TXTpsAccountDetail accountDetail = null;
//		if (accountDetailList.size() > 0) {
//			accountDetail = (TXTpsAccountDetail) accountDetailList.get(0);
//		}
//
//		return accountDetail;
//	}
//
//	/**
//	 * 变更账户余额
//	 * 
//	 * @param accDetail
//	 * @param holdCount
//	 */
//	public void updateAccDetailHoldCount(TXTpsAccountDetail accDetail, BigDecimal holdCount) {
//		// 增加账户可用余额
//		accDetail.setUsableFund(holdCount);
//		accDetail.setModifiedTime(new Date());
//		daos.txTpsAccountDetailDao.updateByPrimaryKey(accDetail);
//	}
//
//	/**
//	 * 添加充值提现记录
//	 * 
//	 * @param accountDetailId
//	 *            账户ID
//	 * @param fromAddress
//	 *            账户地址
//	 * @param userId
//	 *            用户ID
//	 * @param coinId
//	 *            币种
//	 * @param amount
//	 *            金额
//	 * @param operatorType
//	 *            充值/提现
//	 * @param status
//	 *            状态
//	 */
//	public void addUpdateLog(String accountDetailId, String fromAddress, String userId, String coinId, String amount,
//			String operatorType, String status) {
//		TXTpsAccountRecord accRec = new TXTpsAccountRecord();
//		accRec.setAccountDetailId(accountDetailId);
//		accRec.setCreatedTime(new Date());
//		accRec.setModifiedTime(accRec.getCreatedTime());
//		accRec.setFromAddress(fromAddress);
//		accRec.setUserId(userId);
//		accRec.setOperateType(operatorType);// 1:充值 2:提现
//		accRec.setStatus(status);// 1:充值到账 2：提现中 3:提现到账 4:提现失败
//		accRec.setCoin(coinId);
//		accRec.setAmount(amount);
//		// 缺手续费
//		accRec.setTxTpsAccountRecordId(UUIDGenerator.generate());
//		daos.txTpsAccountRecord.insert(accRec);
//	}
//}
