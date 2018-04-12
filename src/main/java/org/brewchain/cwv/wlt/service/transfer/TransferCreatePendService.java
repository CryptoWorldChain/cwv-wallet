package org.brewchain.cwv.wlt.service.transfer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAssetExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFundExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltPend;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUserExample;
import org.brewchain.cwv.wlt.enums.AssetStatusEnum;
import org.brewchain.cwv.wlt.enums.FundStatusEnum;
import org.brewchain.cwv.wlt.enums.PendStatusEnum;
import org.brewchain.cwv.wlt.enums.TransferReturnEnum;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.enums.UserStatusEnum;
import org.brewchain.cwv.wlt.util.StringUtil;
import org.fc.wlt.gens.Transfer.PRetBuySellNew;
import org.fc.wlt.gens.Transfer.PSBuySellNew;
import org.fc.wlt.gens.Transfer.PTRSCommand;
import org.fc.wlt.gens.Transfer.PTRSModule;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.JsonSerializer;
import onight.tfw.outils.serialize.UUIDGenerator;


@NActorProvider
@Slf4j
@Data
public class TransferCreatePendService extends SessionModules<PSBuySellNew> {

	@ActorRequire
	Daos daos;
	
	@Override
	public String[] getCmds() {		
		return new String[] { PTRSCommand.BSN.name() };
	}

	@Override
	public String getModule() {
		return PTRSModule.TRS.name();
	}
	public String toString(){
		return "PSTRSBSN";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSBuySellNew pbo, final CompleteHandler handler) {
		final PRetBuySellNew.Builder ret = PRetBuySellNew.newBuilder();
		//确定用户已经登录 通过filter
		//确定用户资产是否充足
		//进行挂单
		if(pbo != null) {
			String userCode = pbo.getUserCode();
			CWVWltUserExample userExample = new CWVWltUserExample();
			userExample.createCriteria().andUserCodeEqualTo(userCode).andUserStatusEqualTo(UserStatusEnum.OK.getValue());
			Object userObj = daos.wltUserDao.selectOneByExample(userExample);
			if(userObj != null) {
				CWVWltUser user = (CWVWltUser)userObj;
				CWVWltAssetExample assetExample = new CWVWltAssetExample();
				List<String> assetStatusList = new ArrayList<String>();
				assetStatusList.add(AssetStatusEnum.BUILD_SELF.getValue());
				assetStatusList.add(AssetStatusEnum.TRANSFER_IN.getValue());
				
				assetExample.createCriteria().andAssetIdEqualTo(pbo.getSourceAssetId()).andAssetStatusIn(assetStatusList);
				Object assetObj = daos.wltAssetDao.selectOneByExample(assetExample);
				if(assetObj != null) {
					CWVWltAsset asset = (CWVWltAsset)assetObj;
					CWVWltFundExample fundExample = new CWVWltFundExample();
					fundExample.createCriteria().andFundIdEqualTo(asset.getFundId()).andFundStatusEqualTo(FundStatusEnum.OK.getValue());
					Object sourceFundObj = daos.wltFundDao.selectOneByExample(fundExample);
					//确定源fund and targetfund
					
					fundExample.clear();
					fundExample.createCriteria().andFundIdEqualTo(pbo.getTargetFundId()).andFundStatusEqualTo(FundStatusEnum.OK.getValue());
					Object targetFundObj = daos.wltFundDao.selectOneByExample(fundExample);
					
					if(sourceFundObj != null) {
						if(targetFundObj != null) {
//							WLTFund fund = (WLTFund)sourceFundObj;
							if(asset.getHoldCount() >= pbo.getSourceAmount()) {//发送者余额>=要发送的值
								//挂单交易需要获取另一方的地址与公钥，所以交易是发生在挂单后的
								Date current = new Date();
								CWVWltPend pend = new CWVWltPend();
								pend.setAutoCommit("false");
								pend.setBsCode(UUIDGenerator.generate());
								pend.setCreatedTime(current);
								pend.setPendId(pend.getBsCode());
								pend.setPendStatus(PendStatusEnum.PEDDING.getValue());
								pend.setPendType(pbo.getType());
								pend.setReserved1("");
								pend.setReserved2("");
								pend.setSourceAmount(pbo.getSourceAmount());
								pend.setSourceAssetId(asset.getAssetId());
								pend.setTargetAmount(pbo.getTargetAmount());
								pend.setTargetFundId(pbo.getTargetFundId());
								pend.setTotalFee(StringUtil.getTotalFee(pbo.getSourceAmount()));//TODO 确定手续费（添加参数表）
								pend.setUpdatedTime(current);
								pend.setUserId(user.getUserId());
								
								daos.wltPendDao.insert(pend);
								
								//相应的用于余额需要减少，状态不变，
								double sourceHold = asset.getHoldCount() - pbo.getSourceAmount() - StringUtil.getTotalFee(pbo.getSourceAmount());
								CWVWltAsset sourceNewAsset = new CWVWltAsset();
								sourceNewAsset.setAssetId(asset.getAssetId());
								sourceNewAsset.setHoldCount(sourceHold);
								sourceNewAsset.setUpdatedTime(current);
								daos.wltAssetDao.updateByPrimaryKeySelective(sourceNewAsset);
								
								ret.setErrCode(TransferReturnEnum.OK.getValue());
								ret.setMsg(TransferReturnEnum.OK.getName());
							}else {
								//余额不足
								ret.setRequestNo(pbo.getRequestNo());
								ret.setErrCode(TransferReturnEnum.FAIL_ERROR_SOURCE_AMOUNT_SHORT.getValue());
								ret.setMsg(TransferReturnEnum.FAIL_ERROR_SOURCE_AMOUNT_SHORT.getName());
							}
						}else {
							//资产金融信息错误
							ret.setRequestNo(pbo.getRequestNo());
							ret.setErrCode(TransferReturnEnum.FAIL_ERROR_TARGET_FUND_NOT_FOUND.getValue());
							ret.setMsg(TransferReturnEnum.FAIL_ERROR_TARGET_FUND_NOT_FOUND.getName());
						}
					}else {
						//资产金融信息错误
						ret.setRequestNo(pbo.getRequestNo());
						ret.setErrCode(TransferReturnEnum.FAIL_ERROR_SOURCE_FUND_NOT_FOUND.getValue());
						ret.setMsg(TransferReturnEnum.FAIL_ERROR_SOURCE_FUND_NOT_FOUND.getName());
					}
				}else {
					ret.setErrCode(TransferReturnEnum.FAIL_ERROR_ASSET_ID.getValue());
					ret.setMsg(TransferReturnEnum.FAIL_ERROR_ASSET_ID.getName());
				}
			}else {
				ret.setErrCode(UserReturnEnum.FAIL_ERROR_USER_CODE.getValue());
				ret.setMsg(UserReturnEnum.FAIL_ERROR_USER_CODE.getName());
			}
		}else {
			ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
			ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_PARAM.getName());
		}
		
		
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}

}