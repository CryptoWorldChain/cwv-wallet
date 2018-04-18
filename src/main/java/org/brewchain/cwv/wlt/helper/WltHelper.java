package org.brewchain.cwv.wlt.helper;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddrExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAssetExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFund;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFundKey;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltTransfer;
import org.brewchain.cwv.wlt.enums.AddrStatusEnum;
import org.brewchain.cwv.wlt.enums.AssetStatusEnum;
import org.brewchain.cwv.wlt.enums.TransferReturnEnum;
import org.brewchain.cwv.wlt.enums.TransferStatusEnum;
import org.brewchain.cwv.wlt.enums.TransferTypeEnum;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.util.DoubleUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.fc.wlt.gens.Asset.PMAssetInfo;
import org.fc.wlt.gens.Asset.PMFullAddress;
import org.fc.wlt.gens.Asset.PMFundInfo;
import org.fc.wlt.gens.Asset.PMSignAddress;
import org.fc.wlt.gens.Transfer.PMTransInputInfo;
import org.fc.wlt.gens.Transfer.PMTransOutputInfo;
import org.fc.wlt.gens.Transfer.PRetAssetTransfer;
import org.fc.wlt.gens.Transfer.PSAssetTransfer;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.UUIDGenerator;

@iPojoBean
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Slf4j
@Data
public class WltHelper implements ActorService {
	
	private PropHelper props = new PropHelper(null);
	
	@ActorRequire(name="http",scope="global")
	IPacketSender sender;
	
	
	@ActorRequire
	EthereumJHelper ethHelper;
	
	@ActorRequire
	Daos daos;
	
	public void transfer2Another(PSAssetTransfer pbo,PRetAssetTransfer.Builder ret){
		if(pbo != null) {
			ret.setRequestNo(pbo.getRequestNo());
			Date current = new Date();
			PMTransInputInfo inputs = pbo.getInputs();
			PMFullAddress inputAddress = inputs.getAddress();
			String inHexAddr = inputAddress.getHexAddr();
			String inPki = inputAddress.getPki();
			String inHash = inputAddress.getRpmdHash();
			String inAlias = inputAddress.getAlias();
			double inAmount = inputs.getAmount();
			PMTransOutputInfo outputs = pbo.getOutputs();
			PMSignAddress outAddress = outputs.getAddress();
			String outHexAddr = outAddress.getHexAddr();
			String outHash = outAddress.getRpmdHash();
			String outAlias = outAddress.getAlias();
			double outAmount = outputs.getAmount();
			
			if(inAmount != outAmount) {
				log.warn("input amount is not equal to output amount...");
			}
			
			if(StringUtils.isNotBlank(inHexAddr)) {
				if(StringUtils.isNotBlank(inHash)) {
					if(StringUtils.isNotBlank(inPki)) {
						if(StringUtils.isNotBlank(outHexAddr)) {
							if(!inHexAddr.equals(outHexAddr)) {
								if(StringUtils.isNotBlank(outHash)) {
									if(StringUtils.isBlank(inAlias)) {
										inAlias = "inAlias";
									}
									if(StringUtils.isBlank(outAlias)) {
										outAlias = "outAlias";
									}
									
									CWVWltAddrExample addrExample = new CWVWltAddrExample();
									addrExample.createCriteria().andHexAddrEqualTo(inHexAddr).andPublicKeyHashEqualTo(inHash).andPublicKeyEqualTo(inPki);
									Object addrObj = daos.wltAddrDao.selectOneByExample(addrExample);
									CWVWltAddr addr = null;
									if(addrObj != null) {
										addr = (CWVWltAddr)addrObj;
									}
									
									if(addr != null) {
										CWVWltAssetExample assetExample = new CWVWltAssetExample();
										assetExample.createCriteria().andAddrIdEqualTo(addr.getAddrId());
										Object assetObj = daos.wltAssetDao.selectOneByExample(assetExample);
										if(assetObj != null) {
											CWVWltAsset asset = (CWVWltAsset)assetObj;
											String bcTxid = asset.getBcTxid();
											if(asset.getHoldCount() >= inAmount) {
												addrExample.clear();
												addrExample.createCriteria().andHexAddrEqualTo(outHexAddr).andPublicKeyHashEqualTo(outHash);
												Object targetAddrObj = daos.wltAddrDao.selectOneByExample(addrExample);
												CWVWltAddr targetAddr = null;
												if(targetAddrObj != null) {
													targetAddr = (CWVWltAddr)targetAddrObj;
												}
												
												CWVWltAsset targetAsset = new CWVWltAsset();
												if(targetAddr != null) {
													assetExample.clear();
													assetExample.createCriteria().andAddrIdEqualTo(targetAddr.getAddrId());
													Object targetAssetObj = daos.wltAssetDao.selectOneByExample(assetExample);
													if(targetAssetObj != null) {
														targetAsset = (CWVWltAsset)targetAssetObj;
													}else {
														targetAsset.setAssetAlias("");
														targetAsset.setAddrId(targetAddr.getAddrId());
														targetAsset.setAssetId(UUIDGenerator.generate());
														targetAsset.setAssetKeywords("");
														targetAsset.setAssetPubHash(targetAddr.getPublicKeyHash());
														targetAsset.setAssetStatus(AssetStatusEnum.TRANSFER_IN.getValue());
														targetAsset.setAssetType("");
														targetAsset.setBcTxid("");
														targetAsset.setCreatedTime(current);
														targetAsset.setDmtCname("");
														targetAsset.setDmtEname("");
														targetAsset.setFundId("");
														targetAsset.setHoldCount(0.000000);
														targetAsset.setMetadata("");
														targetAsset.setReserved1("");
														targetAsset.setReserved2("");
														targetAsset.setUpdatedTime(current);
														targetAsset.setUserId(targetAddr.getUserId());
														daos.wltAssetDao.insert(targetAsset);
													}
													String transferCode = transfer(inHexAddr, inHash, bcTxid, outHexAddr, outHash, (int)inAmount, (int)outAmount);
													
													if(StringUtils.isNotBlank(transferCode)) {
														ret.setTransferCode(transferCode);
														
														double targetNewHold = targetAsset.getHoldCount() + outAmount;
														targetAsset.setAssetAlias(asset.getAssetAlias());
														targetAsset.setAssetKeywords(asset.getAssetKeywords());
														targetAsset.setAssetType(asset.getAssetType());
														targetAsset.setBcTxid(transferCode);
														targetAsset.setDmtCname(asset.getDmtCname());
														targetAsset.setDmtEname(asset.getDmtEname());
														targetAsset.setFundId(asset.getFundId());
														targetAsset.setHoldCount(DoubleUtil.formatMoney(targetNewHold));
														targetAsset.setUpdatedTime(new Date());
														daos.wltAssetDao.updateByPrimaryKeySelective(targetAsset);
														
														if(targetAddr.getAddrStatus().equals(AddrStatusEnum.EMPTY.getValue())) {
															targetAddr.setAddrStatus(AddrStatusEnum.FULL.getValue());
															targetAddr.setUpdatedTime(current);
															daos.wltAddrDao.updateByPrimaryKey(targetAddr);
														}
														
														CWVWltAsset sourceNewAsset = new CWVWltAsset();
														sourceNewAsset.setAssetId(asset.getAssetId());
														double sourceNewHold = asset.getHoldCount() - inAmount;
														sourceNewAsset.setHoldCount(DoubleUtil.formatMoney(sourceNewHold));
														sourceNewAsset.setUpdatedTime(current);
														daos.wltAssetDao.updateByPrimaryKeySelective(sourceNewAsset);
														
														//添加转账记录
														CWVWltTransfer transfer = new CWVWltTransfer();
														transfer.setCreatedTime(current);
														transfer.setPendId("");
														transfer.setReserved01(pbo.getRequestNo());
														transfer.setReserved02("");
														transfer.setSourceAmount(DoubleUtil.formatMoney(inAmount));
														transfer.setSourceAssetId(asset.getAssetId());
														transfer.setSourceFundId(asset.getFundId());
														transfer.setSourceUserId(asset.getUserId());
														transfer.setTargetAmount(DoubleUtil.formatMoney(outAmount));
														transfer.setTargetAssetId(targetAsset.getAssetId());
														transfer.setTargetFundId(targetAsset.getFundId());
														transfer.setTargetUserId(targetAsset.getUserId());
														transfer.setTotalFee(0.000000);
														transfer.setTransferId(UUIDGenerator.generate());
														transfer.setTransferCode(transferCode);
														transfer.setTransferStatus(TransferStatusEnum.OK.getValue());
														transfer.setTransferType(TransferTypeEnum.TRANSFER.getValue());
														transfer.setUpdatedTime(current);
														daos.wltTransferDao.insert(transfer);
														
														String chainapi = props.get("chainapi", "http://127.0.0.1:8000");
														chainapi += "/fbs/pbnew.do";
														
														try {
															//TODO 暂时关闭资产信息入链
//															BackUpUtil.backUpTransfer(transfer, sender, chainapi, inHexAddr, inHash);
															
														}catch(Exception e) {
															
														}
														
														ret.setErrCode(TransferReturnEnum.OK.getValue());
														ret.setMsg(TransferReturnEnum.OK.getName());
														
														//构造返回信息
														PMAssetInfo.Builder assetInfo= PMAssetInfo.newBuilder();
														assetInfo.setAlias(asset.getAssetAlias());
														assetInfo.setDataTable(asset.getAssetKeywords());
														assetInfo.setDateTime(asset.getCreatedTime().getTime());
														assetInfo.setDmtCname(asset.getDmtCname());
														assetInfo.setDmtEname(asset.getDmtEname());
														assetInfo.setHoldCount(DoubleUtil.formatMoney(sourceNewHold));
														assetInfo.setMetadata(asset.getMetadata());
														
														
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
														addrInfo.setAlias(addr.getAddrAlias());
														addrInfo.setHexAddr(addr.getHexAddr());
														addrInfo.setRpmdHash(addr.getPublicKeyHash());
														
														assetInfo.setAddress(addrInfo);
														
														ret.setAsset(assetInfo);
														ret.setErrCode(TransferReturnEnum.OK.getValue());
														ret.setMsg(TransferReturnEnum.OK.getName());
														
													}else {
														ret.setErrCode(TransferReturnEnum.FAIL_ERROR_TRANSFER.getValue());
														ret.setMsg(TransferReturnEnum.FAIL_ERROR_TRANSFER.getName());
													}
													
												}else {
													ret.setErrCode(TransferReturnEnum.FAIL_ERROR_TARGET_ADDR_NOT_FOUND.getValue());
													ret.setMsg(TransferReturnEnum.FAIL_ERROR_TARGET_ADDR_NOT_FOUND.getName());
												}
											}else {
												ret.setErrCode(TransferReturnEnum.FAIL_ERROR_HEX_ADDR_ERROR.getValue());
												ret.setMsg(TransferReturnEnum.FAIL_ERROR_HEX_ADDR_ERROR.getName());
											}
										}else{
											ret.setErrCode(TransferReturnEnum.FAIL_ERRRO_SOURCE_ASSET_NOT_FUND.getValue());
											ret.setMsg(TransferReturnEnum.FAIL_ERRRO_SOURCE_ASSET_NOT_FUND.getName());
										}
									}else {
										ret.setErrCode(TransferReturnEnum.FAIL_ERRRO_SOURCE_ASSET_NOT_FUND.getValue());
										ret.setMsg(TransferReturnEnum.FAIL_ERRRO_SOURCE_ASSET_NOT_FUND.getName());
									}
									
								}else {
									ret.setErrCode(TransferReturnEnum.FAIL_ERROR_OUT_PUB_HASH_IS_NULL.getValue());
									ret.setMsg(TransferReturnEnum.FAIL_ERROR_OUT_PUB_HASH_IS_NULL.getName());
								}
							}else {
								ret.setErrCode(TransferReturnEnum.FAIL_ERROR_FORBIDDEN_ERROR.getValue());
								ret.setMsg(TransferReturnEnum.FAIL_ERROR_FORBIDDEN_ERROR.getName());								
							}
						}else {
							ret.setErrCode(TransferReturnEnum.FAIL_ERROR_OUT_HEX_ADDR_IS_NULL.getValue());
							ret.setMsg(TransferReturnEnum.FAIL_ERROR_OUT_HEX_ADDR_IS_NULL.getName());
						}
					}else {
						ret.setErrCode(TransferReturnEnum.FAIL_ERROR_IN_PKI_IS_NULL.getValue());
						ret.setMsg(TransferReturnEnum.FAIL_ERROR_IN_PKI_IS_NULL.getName());
					}
				}else {
					ret.setErrCode(TransferReturnEnum.FAIL_ERROR_IN_PUB_HASH_IS_NULL.getValue());
					ret.setMsg(TransferReturnEnum.FAIL_ERROR_IN_PUB_HASH_IS_NULL.getName());
				}
			}else {
				ret.setErrCode(TransferReturnEnum.FAIL_ERROR_IN_HEX_ADDR_IS_NULL.getValue());
				ret.setMsg(TransferReturnEnum.FAIL_ERROR_IN_HEX_ADDR_IS_NULL.getName());
			}
		}else {
			ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
			ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_PARAM.getName());
		}
	}
	
	private String transfer (String in_addr, String in_hash, String bc_txid, String out_addr, String out_hash, int inAmount, int outAmount) {
		String transfer_code = "";
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		node.put("trade_no", UUIDGenerator.generate());
		node.put("alias", "tranfer");
		ArrayNode inputs = mapper.createArrayNode();
		ObjectNode inputItem = mapper.createObjectNode();
		ObjectNode inputItemAddr = mapper.createObjectNode();
		inputItemAddr.put("hex_addr", in_addr);
		inputItemAddr.put("alias", "a");
		inputItemAddr.put("rpmd_hash", in_hash);
		inputItem.put("address", inputItemAddr);
		inputItem.put("bc_txid","");
		ObjectNode inputItemFund = mapper.createObjectNode();
		inputItemFund.put("amount", 0);
		inputItemFund.put("count", inAmount);
		inputItemFund.put("dmt", "TOKEN");
		inputItemFund.put("dmt_ename", "abc");
		inputItem.put("fund", inputItemFund);
		inputs.add(inputItem);

		ArrayNode outputs = mapper.createArrayNode();
		ObjectNode outputItem = mapper.createObjectNode();
		ObjectNode outputItemAddr = mapper.createObjectNode();
		outputItemAddr.put("hex_addr", out_addr);
		outputItemAddr.put("alias", "b");
		outputItemAddr.put("rpmd_hash", out_hash);
		outputItem.put("address", outputItemAddr);
		ObjectNode outputItemFund = mapper.createObjectNode();
		outputItemFund.put("amount", 0);
		outputItemFund.put("count", outAmount);
		outputItemFund.put("dmt", "TOKEN");
		outputItemFund.put("dmt_ename", "abc");
		
		outputItem.put("fund", outputItemFund);
		outputs.add(outputItem);
		
		node.put("inputs", inputs);
		node.put("outputs", outputs);
		node.put("signed_code", "abc");
		
		node.put("metadata", "");
		System.out.println(node);
		return null;
		//TODO 调用CWB接口，此处需要路由，各类币种调用不同接口
//		String chainapi = props.get("chainapi", "http://127.0.0.1:8000");
//		chainapi += "/fbs/pbtrn.do";
//		String sendJson = JsonSerializer.formatToString(node);
//		FramePacket fp = PacketHelper.buildUrlFromJson(sendJson, "POST", chainapi);
//		val regRet = sender.send(fp, 30000);
//		TransferRetEntity transferRetEntity = JsonSerializer.getInstance().deserialize(regRet.getBody(), TransferRetEntity.class);
//		
//		if(transferRetEntity == null) {
//			throw new NullPointerException("connect to blockchain error...");
//		}
//		if(transferRetEntity.getErr_code().equals("0")) {
//			transfer_code = transferRetEntity.getBc_txid();
//		}
//		
//		return transfer_code;
	}
	
	/**
	 *  交易转账
	 * @param sign 签名字符串
	 * @param coin 币种路由
	 * @throws Exception 
	 */
	public Map<String, Object> transfer(String signStr,String coin) throws Exception{
		//TODO 枚举路由
		if("ETH".equals(coin)){
			return ethHelper.transfer(signStr);
		}else{
			throw new Exception("暂不支持该币种");
		}
		
	}
	
	/**
	 * 查询账户余额
	 * @param coin
	 * @param address
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> queryAccount(String coin,String address) throws Exception{
		if("ETH".equals(coin)){
			return ethHelper.checkWalletETH(address);
		}else{
			throw new Exception("暂不支持该币种");
		}
	} 
	
}
