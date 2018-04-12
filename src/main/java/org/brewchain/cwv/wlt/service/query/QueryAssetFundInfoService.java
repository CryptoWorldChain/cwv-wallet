package org.brewchain.cwv.wlt.service.query;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddrExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAssetExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFund;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFundExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUserExample;
import org.brewchain.cwv.wlt.enums.AssetReturnEnum;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.enums.UserStatusEnum;
import org.fc.wlt.gens.Asset.PMFundInfo;
import org.fc.wlt.gens.Query.PQRYCommand;
import org.fc.wlt.gens.Query.PQRYModule;
import org.fc.wlt.gens.Query.PRetGetAssetFundInfo;
import org.fc.wlt.gens.Query.PSGetAssetFundInfo;

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
public class QueryAssetFundInfoService extends SessionModules<PSGetAssetFundInfo> {

	@ActorRequire
	Daos daos;
	
	@Override
	public String[] getCmds() {		
		return new String[] { PQRYCommand.GAI.name() };
	}

	@Override
	public String getModule() {
		return PQRYModule.QRY.name();
	}
	public String toString(){
		return "QueryAssetFundInfoService";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSGetAssetFundInfo pbo, final CompleteHandler handler) {
		final PRetGetAssetFundInfo.Builder ret = PRetGetAssetFundInfo.newBuilder();
		if(pbo != null) {
			ret.setRequestNo(pbo.getRequestNo());
			if(StringUtils.isNotBlank(pbo.getUserCode())) {
				CWVWltUserExample userExample = new CWVWltUserExample();
				userExample.createCriteria().andUserCodeEqualTo(pbo.getUserCode()).andUserStatusEqualTo(UserStatusEnum.OK.getValue());
				Object userObj = daos.wltUserDao.selectOneByExample(userExample);
				if(userObj != null) {
					CWVWltUser user = (CWVWltUser)userObj;
					CWVWltAssetExample assetExample = new CWVWltAssetExample();
					CWVWltAddrExample addrExample = new CWVWltAddrExample();
					CWVWltFundExample fundExample = new CWVWltFundExample();
					String addrId = "";
					if(StringUtils.isNotBlank(pbo.getHexAddr())) {
						addrExample.createCriteria().andUserIdEqualTo(user.getUserId()).andHexAddrEqualTo(pbo.getHexAddr());
						Object addrObj = daos.wltAddrDao.selectOneByExample(addrExample);
						if(addrObj != null) {
							CWVWltAddr addr = (CWVWltAddr)addrObj;
							addrId = addr.getAddrId();
						}
					}
					CWVWltAssetExample.Criteria criteria = new CWVWltAssetExample.Criteria();
					criteria.andUserIdEqualTo(user.getUserId());
					if(StringUtils.isNotBlank(pbo.getBcTxid())) {
						criteria.andBcTxidEqualTo(pbo.getBcTxid());
					}
					if(StringUtils.isNotBlank(pbo.getType())) {
						criteria.andAssetTypeEqualTo(pbo.getType());
					}
					if(StringUtils.isNotBlank(pbo.getAlias())) {
						criteria.andAssetAliasEqualTo(pbo.getAlias());
					}
					if(StringUtils.isNotBlank(pbo.getDmtCname())) {
						criteria.andDmtCnameEqualTo(pbo.getDmtCname());
					}
					if(StringUtils.isNotBlank(pbo.getDmtEname())) {
						criteria.andDmtEnameEqualTo(pbo.getDmtEname());
					}

					if(StringUtils.isNotBlank(pbo.getDataTable())) {
						String[] keys = pbo.getDataTable().split("^");
						if(keys.length > 0) {
							List<String> keywords = new ArrayList<String>();
							for(String str : keys) {
								keywords.add(str);
							}
							criteria.andAssetKeywordsIn(keywords);
						}
					}
					if(StringUtils.isNotBlank(addrId)) {
						criteria.andAddrIdEqualTo(addrId);
					}
					Object assetObj = daos.wltAssetDao.selectOneByExample(assetExample);
					if(assetObj != null) {
						CWVWltAsset asset = (CWVWltAsset)assetObj;
						fundExample.createCriteria().andFundIdEqualTo(asset.getFundId());
						Object fundObj = daos.wltFundDao.selectOneByExample(fundExample);
						if(fundObj != null) {
							CWVWltFund fund = (CWVWltFund)fundObj;
							PMFundInfo.Builder fundInfo = PMFundInfo.newBuilder();
							fundInfo.setDateTime(fund.getCreatedTime().getTime());
							fundInfo.setDmtCname(fund.getDmtCname());
							fundInfo.setDmtEname(fund.getDmtEname());
							fundInfo.setGenisDeposit(fund.getGenisDeposit());
							fundInfo.setTotalCount(fund.getTotalCount());
							fundInfo.setTurnoverCount(fund.getTurnoverCount());
							ret.setFundInfo(fundInfo);
						}else {
							ret.setErrCode(AssetReturnEnum.FAIL_ERROR_NO_ASSET_INFO.getValue());
							ret.setMsg(AssetReturnEnum.FAIL_ERROR_NO_ASSET_INFO.getName());
						}
					}else {
						ret.setErrCode(AssetReturnEnum.FAIL_ERROR_NO_ASSET_INFO.getValue());
						ret.setMsg(AssetReturnEnum.FAIL_ERROR_NO_ASSET_INFO.getName());
					}
				}
			}
		}else {
			ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
			ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_PARAM.getName());
		}
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}

}
