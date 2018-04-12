package org.brewchain.cwv.wlt.service.transfer;

import java.util.Date;

import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAssetExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltPend;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltPendExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUserExample;
import org.brewchain.cwv.wlt.enums.PendStatusEnum;
import org.brewchain.cwv.wlt.enums.TransferReturnEnum;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.enums.UserStatusEnum;
import org.brewchain.cwv.wlt.util.StringUtil;
import org.fc.wlt.gens.Transfer.PRetBuySellCancel;
import org.fc.wlt.gens.Transfer.PSBuySellCancel;
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

@NActorProvider
@Slf4j
@Data
public class TransferCancelPendService extends SessionModules<PSBuySellCancel> {
	
	@Override
	public String[] getCmds() {		
		return new String[] { PTRSCommand.BSC.name() };
	}

	@Override
	public String getModule() {
		return PTRSModule.TRS.name();
	}
	public String toString(){
		return "PTRSCancelpend";
	}
	
	@ActorRequire
	Daos daos;
	
	@Override
	public void onPBPacket(final FramePacket pack, PSBuySellCancel pbo, final CompleteHandler handler) {
		final PRetBuySellCancel.Builder ret = PRetBuySellCancel.newBuilder();
		/**
		 * //######资产挂单撤销接口请求
message PSBuySellCancel
{
	string userCode = 1;//用户代码
	string bsCode =2 ;//挂单代码
	string requstNo = 79;//接口请求唯一序号
}
		 */
		if(pbo != null) {
			Date current = new Date();
			CWVWltUserExample userExample = new CWVWltUserExample();
			userExample.createCriteria().andUserCodeEqualTo(pbo.getUserCode()).andUserStatusEqualTo(UserStatusEnum.OK.getValue());
			Object userObj = daos.wltUserDao.selectOneByExample(userExample);
			if(userObj != null) {
				CWVWltUser user = (CWVWltUser)userObj;
				CWVWltPendExample pendExample = new CWVWltPendExample();
				pendExample.createCriteria().andUserIdEqualTo(user.getUserId()).andBsCodeEqualTo(pbo.getBsCode()).andPendStatusEqualTo(PendStatusEnum.PEDDING.getValue());
				Object pendObj = daos.wltPendDao.selectOneByExample(pendExample);
				if(pendObj != null) {
					CWVWltPend pend = (CWVWltPend)pendObj;
					CWVWltAssetExample assetExample = new CWVWltAssetExample();
					assetExample.createCriteria().andAssetIdEqualTo(pend.getSourceAssetId());
					Object assetObj = daos.wltAssetDao.selectOneByExample(assetExample);
					if(assetObj != null) {
						CWVWltAsset asset = (CWVWltAsset)assetObj;
						double assetNewHold = asset.getHoldCount() + pend.getSourceAmount() + StringUtil.getTotalFee(pend.getSourceAmount());
						asset.setHoldCount(assetNewHold);
						asset.setUpdatedTime(current);
						daos.wltAssetDao.updateByPrimaryKeySelective(asset);
						
						pend.setPendStatus(PendStatusEnum.CANCEL.getValue());
						pend.setUpdatedTime(current);
						daos.wltPendDao.updateByPrimaryKeySelective(pend);
						ret.setErrCode(TransferReturnEnum.OK.getValue());
						ret.setMsg(TransferReturnEnum.OK.getName());
					}else {
						ret.setErrCode(TransferReturnEnum.FAIL_ERROR_ASSET_ID.getValue());
						ret.setMsg(TransferReturnEnum.FAIL_ERROR_ASSET_ID.getName());
					}
					
				}else {
					ret.setErrCode(TransferReturnEnum.FAIL_ERROR_PEND_NOT_FOUND.getValue());
					ret.setMsg(TransferReturnEnum.FAIL_ERROR_PEND_NOT_FOUND.getName());
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