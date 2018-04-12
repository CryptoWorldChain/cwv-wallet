package org.brewchain.cwv.wlt.service.query;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAssetKey;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFund;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFundExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltPend;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltPendExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltTransfer;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltTransferExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUserExample;
import org.fc.wlt.gens.Asset.PMAssetInfo;
import org.fc.wlt.gens.Asset.PMFundInfo;
import org.fc.wlt.gens.Query.PQRYCommand;
import org.fc.wlt.gens.Query.PQRYModule;
import org.fc.wlt.gens.Query.PRetGetBuySell;
import org.fc.wlt.gens.Query.PSGetBuySell;
import org.fc.wlt.gens.Transfer.PMTransaction;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.JsonSerializer;


// http://localhost:8000/usr/pbreg.do?fh=REGUSR0000000J00&resp=bd&bd={"username":"aaa","userid":"1111"}

@NActorProvider
@Slf4j
@Data
public class QueryAssetPendService extends SessionModules<PSGetBuySell> {

	@ActorRequire
	Daos daos;
	
	@Override
	public String[] getCmds() {		
		return new String[] { PQRYCommand.GBS.name() };
	}

	@Override
	public String getModule() {
		return PQRYModule.QRY.name();
	}
	public String toString(){
		return "QueryAssetPendService";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSGetBuySell pbo, final CompleteHandler handler) {
		final PRetGetBuySell.Builder ret = PRetGetBuySell.newBuilder();
		

		String userid= null;
		ret.setRequestNo(pbo.getRequestNo());
		ret.setPageNo(pbo.getPageNo());
		ret.setPageSize(pbo.getPageSize());
		if(pbo == null) {
			ret.setErrCode("99").setMsg("参数为空");
			handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
			return;
		}
		if(StringUtils.isNotBlank(pbo.getUserCode())) {
			CWVWltUserExample userExample = new CWVWltUserExample();
			userExample.createCriteria().andUserCodeEqualTo(pbo.getUserCode());
			
			List<Object> userObjList = daos.wltUserDao.selectByExample(userExample);
			
			if(userObjList == null || userObjList.size() == 0) {
				ret.setErrCode("01").setMsg("未找到用户");
				handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
				return;
			}
			if(userObjList.size() > 1) {
				ret.setErrCode("02").setMsg("找到多个用户");
				handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
				return;
			}
			if(userObjList.get(0) == null) {
				ret.setErrCode("01").setMsg("未找到用户");
				handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
				return;
			}
			CWVWltUser user = (CWVWltUser)userObjList.get(0);
			if(user == null || StringUtils.isBlank(user.getUserId())) {
				ret.setErrCode("01").setMsg("未找到用户");
				handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
				return;
			}
			userid = user.getUserId();
		}
		

		CWVWltTransferExample tansferExample = new CWVWltTransferExample();
		CWVWltTransferExample.Criteria transc = tansferExample.createCriteria();
		if(StringUtils.isNotBlank(userid)) {
			transc.andSourceUserIdEqualTo(userid);
		}
		
		List<Object> pendobjList = null;
		if(StringUtils.isNotBlank(pbo.getBsCode())) {
			CWVWltPendExample pendExample = new CWVWltPendExample();
			pendExample.createCriteria().andBsCodeEqualTo(pbo.getBsCode());
			
			pendExample.setLimit(pbo.getPageSize());
			pendExample.setOffset((pbo.getPageNo() -1)*pbo.getPageSize());
			pendobjList = daos.wltPendDao.selectByExample(pendExample);
			
		}
		
		if(pendobjList != null && pendobjList.size() > 0) {
			for(Object obj : pendobjList) {
				if(obj == null) {
					continue;
				}
				CWVWltPend pendobj = (CWVWltPend)obj;
				pendobj.getPendId();
				
				CWVWltTransferExample transExample = new CWVWltTransferExample();
				CWVWltTransferExample.Criteria transC = transExample.createCriteria();
				if(StringUtils.isNotBlank(userid)) {
					transC.andSourceUserIdEqualTo(userid);
				}
				if(StringUtils.isNotBlank(pendobj.getPendId())) {
					
					transC.andPendIdEqualTo(pendobj.getPendId());
				}
				if(StringUtils.isNotBlank(pendobj.getSourceAssetId())) {
					transC.andSourceAssetIdEqualTo(pendobj.getSourceAssetId());
				}
				
				Object transobj = daos.wltTransferDao.selectOneByExample(transExample);
				//if(StringUtils.isNotBlank(pendobj.get))
				if(transobj == null) {
					continue;
				}
				CWVWltTransfer transf =(CWVWltTransfer)transobj;
				
				PMTransaction.Builder tb = PMTransaction.newBuilder();
				tb.setUserCode(pbo.getUserCode());
				
				if(StringUtils.isNotBlank(pbo.getType())) {
					tb.setType(pendobj.getPendType());
				}
				if(StringUtils.isNotBlank(pbo.getStatus())) {
					tb.setStatus(pbo.getStatus());
//					tb.setStatus(NumberUtils.toInt(pendobj.getPendStatus()));
				}
				if(StringUtils.isNotBlank(pbo.getBsCode())) {
					tb.setBsCode(pendobj.getBsCode());
				}
				
				tb.setTargetAmount(transf.getTargetAmount());
				PMAssetInfo.Builder assetinfob= PMAssetInfo.newBuilder();
				CWVWltAssetKey assetKey = new CWVWltAssetKey();
				assetKey.setAssetId(transf.getTargetAssetId());
				CWVWltAsset assetinfoobj = daos.wltAssetDao.selectByPrimaryKey(assetKey); //TARGET_ASSET_ID
				assetinfob.setAlias(assetinfoobj.getAssetAlias());
				assetinfob.setDmtCname(assetinfoobj.getDmtCname());
				assetinfob.setDmtEname(assetinfoobj.getDmtEname());
				//TODO 这里返回值逻辑好像有问题--fundinfo返回了两次
				PMFundInfo.Builder fundb = PMFundInfo.newBuilder();
				
				CWVWltFundExample fundExample = new CWVWltFundExample();
				fundExample.createCriteria().andUserIdEqualTo(pendobj.getUserId());
				
				Object fundobj = daos.wltFundDao.selectOneByExample(fundExample);
				if(fundobj != null) {
					CWVWltFund fundo = (CWVWltFund)fundobj;
					fundb.setDateTime(fundo.getDateTime().getTime());
					fundb.setDmtCname(fundo.getDmtCname());
					fundb.setDmtEname(fundo.getDmtEname());
					fundb.setTotalCount(fundo.getTotalCount());
					fundb.setTurnoverCount(fundo.getTurnoverCount());
					assetinfob.setFund(fundb);
					
				}
				tb.setBsAsset(assetinfob);
				ret.addBsInfo(tb);
			}

		}
		ret.setErrCode("0").setMsg("success");
		
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}

}