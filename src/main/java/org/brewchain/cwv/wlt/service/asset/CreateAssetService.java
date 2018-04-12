package org.brewchain.cwv.wlt.service.asset;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddrExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFund;
import org.brewchain.cwv.wlt.entity.AddressEntity;
import org.brewchain.cwv.wlt.entity.AssetEntity;
import org.brewchain.cwv.wlt.entity.NewAssetRetEntity;
import org.brewchain.cwv.wlt.enums.AddrStatusEnum;
import org.brewchain.cwv.wlt.enums.AddrTypeEnum;
import org.brewchain.cwv.wlt.enums.AssetReturnEnum;
import org.brewchain.cwv.wlt.enums.AssetStatusEnum;
import org.brewchain.cwv.wlt.enums.FundStatusEnum;
import org.brewchain.cwv.wlt.util.BackUpUtil;
import org.brewchain.cwv.wlt.util.DoubleUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.fc.wlt.gens.Asset.PASTCommand;
import org.fc.wlt.gens.Asset.PASTModule;
import org.fc.wlt.gens.Asset.PMAssetInfo;
import org.fc.wlt.gens.Asset.PMFullAddress;
import org.fc.wlt.gens.Asset.PMFundInfo;
import org.fc.wlt.gens.Asset.PRetOrgAssetCreate;
import org.fc.wlt.gens.Asset.PSOrgAssetCreate;

import lombok.Data;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.JsonSerializer;
import onight.tfw.outils.serialize.UUIDGenerator;




@NActorProvider
@Slf4j
@Data
public class CreateAssetService extends SessionModules<PSOrgAssetCreate> {
	@ActorRequire
	Daos daos;
	
	private PropHelper props = new PropHelper(null);
	
	@ActorRequire(name="http",scope="global")
	IPacketSender sender;
	
	@Override
	public String[] getCmds() {		
		return new String[] { PASTCommand.ONA.name() };
	}

	@Override
	public String getModule() {
		return PASTModule.AST.name();
	}
	
	public String toString(){
		return "createAsset";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSOrgAssetCreate pbo, final CompleteHandler handler) {
		final PRetOrgAssetCreate.Builder ret = PRetOrgAssetCreate.newBuilder();
		if(pbo != null) {
			ret.setRequestNo(pbo.getRequestNo());
			if(StringUtils.isNotBlank(pbo.getHexAddr())) {
				if(StringUtils.isNotBlank(pbo.getRpmdHash())) {
					if(pbo.getGenisCount() > 1.00) {
						String chainapi = props.get("chainapi", "http://127.0.0.1:8000");
						chainapi += "/fbs/pbnew.do";
						
						ObjectMapper mapper = new ObjectMapper();
						ObjectNode node = mapper.createObjectNode();
						node.put("trade_no", UUIDGenerator.generate());
						ArrayNode arrayNode = mapper.createArrayNode();
						ObjectNode arrNodeItem = mapper.createObjectNode();
						arrNodeItem.put("hex_addr", pbo.getHexAddr());
						arrNodeItem.put("rpmd_hash",pbo.getRpmdHash());
						arrayNode.add(arrNodeItem);
						node.put("co_signers", arrayNode);
						node.put("alias", pbo.getAlias());
						node.put("signed_code", "skip");
						ObjectNode fundNode = mapper.createObjectNode();
						fundNode.put("count", pbo.getGenisCount());
						fundNode.put("dmt_ename", pbo.getDmtEname());
						fundNode.put("dmt", 10);
						node.put("fund", fundNode);
						node.put("metadata", "");
						
						String sendJson = JsonSerializer.formatToString(node);
						FramePacket fp = PacketHelper.buildUrlFromJson(sendJson, "POST", chainapi);
						val regRet = sender.send(fp, 30000);
						NewAssetRetEntity newAssetRetEntity = JsonSerializer.getInstance().deserialize(regRet.getBody(), NewAssetRetEntity.class);
						if(newAssetRetEntity == null) {
							throw new NullPointerException("connect to blockchain error...");
						}
						
						if(newAssetRetEntity.getErr_code().equals("0")) {
							Date current = new Date();
							AssetEntity assetEntity = newAssetRetEntity.getAsset();
							AddressEntity addressEntity = assetEntity.getAddress();
							CWVWltAddrExample addrExample = new CWVWltAddrExample();
							addrExample.createCriteria().andHexAddrEqualTo(pbo.getHexAddr()).andPublicKeyHashEqualTo(pbo.getRpmdHash());
							Object addrObj = daos.wltAddrDao.selectOneByExample(addrExample);
							String userId = "";
							String pub = "";
							if(addrObj != null) {
								CWVWltAddr addr = (CWVWltAddr)addrObj;
								pub = addr.getPublicKey();
								userId = addr.getUserId();
							}
							
							CWVWltAddr assetAddr = new CWVWltAddr();
							assetAddr.setAddrId(UUIDGenerator.generate());
							assetAddr.setAddrStatus(AddrStatusEnum.FULL.getValue());
							assetAddr.setAddrType(AddrTypeEnum.ASSET_ADDR.getValue());
							assetAddr.setAddrAlias(StringUtils.isBlank(pbo.getAlias()) ? "" : pbo.getAlias());
							assetAddr.setCreatedTime(current);
							assetAddr.setHexAddr(StringUtils.isBlank(addressEntity.getHex_addr()) ? "" : addressEntity.getHex_addr());
							assetAddr.setPrivateKey(StringUtils.isBlank(addressEntity.getPki()) ? "" : addressEntity.getPki());
							assetAddr.setPublicKey(StringUtils.isBlank(addressEntity.getPub()) ? pub : addressEntity.getPub());
							assetAddr.setPublicKeyHash(StringUtils.isBlank(addressEntity.getRpmd_hash()) ? "" : addressEntity.getRpmd_hash());
							assetAddr.setReserved1("");
							assetAddr.setReserved2("");
							assetAddr.setUpdatedTime(current);
							assetAddr.setUserId(userId);
							
							daos.wltAddrDao.insert(assetAddr);
							
							CWVWltFund fund = new CWVWltFund();
							fund.setCreatedTime(current);
							fund.setDmtCname(StringUtils.isBlank(pbo.getDmtCname()) ? "" : pbo.getDmtCname());
							fund.setDmtEname(StringUtils.isBlank(pbo.getDmtEname()) ? "" : pbo.getDmtEname());
							fund.setFundId(UUIDGenerator.generate());
							fund.setFundStatus(FundStatusEnum.OK.getValue());
							fund.setGenisAddr(pbo.getHexAddr());
							fund.setGenisDeposit(DoubleUtil.formatMoney(pbo.getGenisDeposit()));
							fund.setGenisOrgId("");
							fund.setReserved1("");
							fund.setReserved2("");
							fund.setTotalCount(DoubleUtil.formatMoney(pbo.getGenisCount()));
							fund.setTurnoverCount(DoubleUtil.formatMoney(pbo.getGenisCount()));
							fund.setUpdatedTime(current);
							fund.setUserId(userId);
							fund.setColoredFbc(0.00);
							fund.setDateTime(current);
							fund.setExchangeFbc(1.00);
							
							daos.wltFundDao.insert(fund);
							
							CWVWltAsset asset = new CWVWltAsset();
							asset.setAddrId(assetAddr.getAddrId());
							asset.setAssetAlias(StringUtils.isBlank(pbo.getAlias()) ? "" : pbo.getAlias());
							asset.setAssetId(UUIDGenerator.generate());
							asset.setAssetKeywords(StringUtils.isBlank(pbo.getDataTable()) ? "" : pbo.getDataTable());
							asset.setAssetPubHash(assetAddr.getPublicKeyHash());
							asset.setAssetStatus(AssetStatusEnum.BUILD_SELF.getValue());
							asset.setAssetType(StringUtils.isBlank(pbo.getType()) ? "" : pbo.getType());
							asset.setBcTxid(assetEntity.getBc_txid());
							asset.setCreatedTime(current);
							asset.setDmtCname(StringUtils.isBlank(pbo.getDmtCname()) ? "" : pbo.getDmtCname());
							asset.setDmtEname(StringUtils.isBlank(pbo.getDmtEname()) ? "" : pbo.getDmtEname());
							asset.setFundId(fund.getFundId());
							asset.setHoldCount(DoubleUtil.formatMoney(pbo.getGenisCount()));
							asset.setMetadata(StringUtils.isBlank(pbo.getMetadata()) ? "" : pbo.getMetadata());
							asset.setReserved1("");
							asset.setReserved2("");
							asset.setUpdatedTime(current);
							asset.setUserId(userId);
							
							daos.wltAssetDao.insert(asset);
							
							try {
								
								BackUpUtil.backUpAddr(assetAddr, sender, chainapi, assetAddr.getHexAddr(), assetAddr.getPublicKeyHash());
								BackUpUtil.backUpFund(fund, sender, chainapi, assetAddr.getHexAddr(), assetAddr.getPublicKeyHash());
								BackUpUtil.backUpAsset(asset, sender, chainapi, assetAddr.getHexAddr(), assetAddr.getPublicKeyHash());
								
							}catch(Exception e) {
								
							}
							
							PMAssetInfo.Builder assetInfo = PMAssetInfo.newBuilder();
							assetInfo.setAlias(StringUtils.isBlank(asset.getAssetAlias()) ? "" : asset.getAssetAlias());
							assetInfo.setDataTable(asset.getAssetKeywords());
							assetInfo.setDateTime(asset.getCreatedTime().getTime());
							assetInfo.setDmtCname(asset.getDmtCname());
							assetInfo.setDmtEname(asset.getDmtEname());
							assetInfo.setHoldCount(DoubleUtil.formatMoney(asset.getHoldCount()));
							
							PMFullAddress.Builder addrInfo = PMFullAddress.newBuilder();
							addrInfo.setAlias(StringUtils.isBlank(assetAddr.getAddrAlias()) ? "" : assetAddr.getAddrAlias());
							addrInfo.setHexAddr(assetAddr.getHexAddr());
							addrInfo.setPki(assetAddr.getPublicKey());
							addrInfo.setRpmdHash(assetAddr.getPublicKeyHash());
							assetInfo.setAddress(addrInfo);
							
							PMFundInfo.Builder fundInfo = PMFundInfo.newBuilder(); 
							fundInfo.setDateTime(fund.getCreatedTime().getTime());
							fundInfo.setDmtCname(fund.getDmtCname());
							fundInfo.setDmtEname(fund.getDmtEname());
							fundInfo.setGenisDeposit(DoubleUtil.formatMoney(fund.getGenisDeposit()));
							fundInfo.setTotalCount(DoubleUtil.formatMoney(fund.getTotalCount()));
							fundInfo.setTurnoverCount(DoubleUtil.formatMoney(fund.getTurnoverCount()));
							
							assetInfo.setFund(fundInfo);
							
							ret.setAssets(assetInfo);
							ret.setErrCode(AssetReturnEnum.OK.getValue());
							ret.setMsg(AssetReturnEnum.OK.getName());
							ret.setRequestNo(pbo.getRequestNo());
						}else {
							ret.setErrCode(AssetReturnEnum.FAIL_ERROR_CHAIN.getValue());
							ret.setMsg(newAssetRetEntity.getMsg());
							ret.setRequestNo(pbo.getRequestNo());
						}
					}else {
						ret.setErrCode(AssetReturnEnum.FAIL_ERROR_CREATE_COUNT_TOO_SMALL.getValue());
						ret.setMsg(AssetReturnEnum.FAIL_ERROR_CREATE_COUNT_TOO_SMALL.getName());
					}
				}else {
					ret.setErrCode(AssetReturnEnum.FAIL_ERROR_RPMD_HASH_IS_NULL.getValue());
					ret.setMsg(AssetReturnEnum.FAIL_ERROR_RPMD_HASH_IS_NULL.getName());
				}
			}else {
				ret.setErrCode(AssetReturnEnum.FAIL_ERROR_HEX_ADDR_IS_NULL.getValue());
				ret.setMsg(AssetReturnEnum.FAIL_ERROR_HEX_ADDR_IS_NULL.getName());
			}
		}else {
			ret.setErrCode(AssetReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
			ret.setMsg(AssetReturnEnum.FAIL_ERROR_NO_PARAM.getName());
		}
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}