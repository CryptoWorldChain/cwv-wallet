package org.brewchain.cwv.wlt.service.query;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddrExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAssetExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFund;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFundKey;
import org.brewchain.cwv.wlt.enums.AssetReturnEnum;
import org.brewchain.cwv.wlt.enums.TransferReturnEnum;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.util.DoubleUtil;
import org.fc.wlt.gens.Asset.PMAssetInfo;
import org.fc.wlt.gens.Asset.PMFullAddress;
import org.fc.wlt.gens.Asset.PMFundInfo;
import org.fc.wlt.gens.Query.PQRYCommand;
import org.fc.wlt.gens.Query.PQRYModule;
import org.fc.wlt.gens.Query.PRetGetAsset;
import org.fc.wlt.gens.Query.PSGetAsset;

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
public class QueryAssetService extends SessionModules<PSGetAsset> {

	@ActorRequire
	Daos daos;
	
	@Override
	public String[] getCmds() {		
		return new String[] { PQRYCommand.GOA.name() };
	}

	@Override
	public String getModule() {
		return PQRYModule.QRY.name();
	}
	public String toString(){
		return "QueryAssetService";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSGetAsset pbo, final CompleteHandler handler) {
		final PRetGetAsset.Builder ret = PRetGetAsset.newBuilder();
		
		if(pbo != null) {
			ret.setRequestNo(pbo.getRequestNo());
			
			if(StringUtils.isNotBlank(pbo.getHexAddr())) {
				CWVWltAddrExample addrExample = new CWVWltAddrExample();
				addrExample.createCriteria().andHexAddrEqualTo(pbo.getHexAddr());
				Object addrObj = daos.wltAddrDao.selectOneByExample(addrExample);
				if(addrObj != null) {
					CWVWltAddr addr = (CWVWltAddr)addrObj;
					CWVWltAssetExample assetExample = new CWVWltAssetExample();
					assetExample.createCriteria().andAddrIdEqualTo(addr.getAddrId());
					Object assetObj = daos.wltAssetDao.selectOneByExample(assetExample);
					if(assetObj != null) {
						CWVWltAsset asset = (CWVWltAsset)assetObj;
						PMAssetInfo.Builder assetInfo = PMAssetInfo.newBuilder();
						assetInfo.setAlias(asset.getAssetAlias());
						assetInfo.setDataTable(asset.getAssetKeywords());
						assetInfo.setDateTime(asset.getCreatedTime().getTime());
						assetInfo.setDmtCname(asset.getDmtCname());
						assetInfo.setDmtEname(asset.getDmtEname());
						assetInfo.setHoldCount(DoubleUtil.formatMoney(asset.getHoldCount()));
						
						if(StringUtils.isNotBlank(asset.getFundId())) {
							CWVWltFundKey fundKey = new CWVWltFundKey(asset.getFundId());
							CWVWltFund fund = daos.wltFundDao.selectByPrimaryKey(fundKey);
							if(fund != null) {
								PMFundInfo.Builder fundInfo = PMFundInfo.newBuilder();
								fundInfo.setDateTime(fund.getCreatedTime().getTime());
								fundInfo.setDmtCname(StringUtils.isNotBlank(fund.getDmtCname()) ? fund.getDmtCname() : "");
								fundInfo.setDmtEname(StringUtils.isNotBlank(fund.getDmtEname()) ? fund.getDmtEname() : "");
								fundInfo.setGenisDeposit(DoubleUtil.formatMoney(fund.getGenisDeposit()));
								fundInfo.setTotalCount(DoubleUtil.formatMoney(fund.getTotalCount()));
								fundInfo.setTurnoverCount(DoubleUtil.formatMoney(fund.getTurnoverCount()));
								assetInfo.setFund(fundInfo);
							}
						}
						
						PMFullAddress.Builder addrInfo = PMFullAddress.newBuilder();
						addrInfo.setAlias(StringUtils.isBlank(addr.getAddrAlias()) ? "" : addr.getAddrAlias());
						addrInfo.setHexAddr(addr.getHexAddr());
						addrInfo.setRpmdHash(addr.getPublicKeyHash());
						
						assetInfo.setAddress(addrInfo);
						
						ret.setAsset(assetInfo);
						ret.setErrCode(TransferReturnEnum.OK.getValue());
						ret.setMsg(TransferReturnEnum.OK.getName());
					}else {
						ret.setErrCode(AssetReturnEnum.FAIL_ERROR_NO_ASSET_INFO.getValue());
						ret.setMsg(AssetReturnEnum.FAIL_ERROR_NO_ASSET_INFO.getName());
					}
				}else {
					ret.setErrCode(AssetReturnEnum.FAIL_ERROR_HEX_ADDR_ERROR.getValue());
					ret.setMsg(AssetReturnEnum.FAIL_ERROR_HEX_ADDR_ERROR.getName());
				}
			}else {
				ret.setErrCode(AssetReturnEnum.FAIL_ERROR_HEX_ADDR_IS_NULL.getValue());
				ret.setMsg(AssetReturnEnum.FAIL_ERROR_HEX_ADDR_IS_NULL.getName());
			}
		}else {
			ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
			ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_PARAM.getName());
		}

		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}

}