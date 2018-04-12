package org.brewchain.cwv.wlt.service.query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dao.SysDBProvider;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddrExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddrKey;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAssetExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAssetKey;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFund;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFundKey;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltTransfer;
import org.brewchain.cwv.wlt.enums.AssetReturnEnum;
import org.brewchain.cwv.wlt.enums.TransferReturnEnum;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.util.DoubleUtil;
import org.fc.wlt.gens.Asset.PMFundInfo;
import org.fc.wlt.gens.Query.PMTransferDetail;
import org.fc.wlt.gens.Query.PQRYCommand;
import org.fc.wlt.gens.Query.PQRYModule;
import org.fc.wlt.gens.Query.PRetGetAssetTransfer;
import org.fc.wlt.gens.Query.PSGetAssetTransfer;
import org.fc.wlt.gens.Transfer.PMTransSignInfo;

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
public class QueryTransferService extends SessionModules<PSGetAssetTransfer> {

	@ActorRequire
	Daos daos;
	@ActorRequire
	SysDBProvider sqlprovider;
	
	
	
	@Override
	public String[] getCmds() {		
		return new String[] { PQRYCommand.GAT.name() };
	}

	@Override
	public String getModule() {
		return PQRYModule.QRY.name();
	}
	public String toString(){
		return "QueryTransferService";
	}
		
	@Override
	public void onPBPacket(final FramePacket pack, PSGetAssetTransfer pbo, final CompleteHandler handler) {
		final PRetGetAssetTransfer.Builder ret = PRetGetAssetTransfer.newBuilder();
		if(pbo != null) {
			ret.setRequestNo(pbo.getRequestNo());
			int pageNo = pbo.getPageNo();
			int pageSize = pbo.getPageSize();
			if(pageNo > 0) {
				pageNo -= 1;
			}
			if(pageSize < 1) {
				pageSize = 5;
			}
			ret.setPageNo(pageNo);
			ret.setPageSize(pageSize);
			
			if(StringUtils.isNotBlank(pbo.getHexAddr())) {
				CWVWltAddrExample sourceAddrExample = new CWVWltAddrExample();
				sourceAddrExample.createCriteria().andHexAddrEqualTo(pbo.getHexAddr());
				Object sourceAddrObj = daos.wltAddrDao.selectOneByExample(sourceAddrExample);
				if(sourceAddrObj != null) {
					CWVWltAddr sourceAddr = (CWVWltAddr)sourceAddrObj;
					CWVWltAssetExample sourceAssetExample = new CWVWltAssetExample();
					sourceAssetExample.createCriteria().andAddrIdEqualTo(sourceAddr.getAddrId());
					Object sourceAssetObj = daos.wltAssetDao.selectOneByExample(sourceAssetExample);
					if(sourceAssetObj != null) {
						CWVWltAsset sourceAsset = (CWVWltAsset)sourceAssetObj;
						PMFundInfo.Builder fundInfo = PMFundInfo.newBuilder();
						if(StringUtils.isNotBlank(sourceAsset.getFundId())) {
							CWVWltFundKey sourceFundKey = new CWVWltFundKey(sourceAsset.getFundId());
							CWVWltFund sourceFund = daos.wltFundDao.selectByPrimaryKey(sourceFundKey);
							if(sourceFund != null) {
								fundInfo.setDateTime(sourceFund.getCreatedTime().getTime());
								fundInfo.setDmtCname(StringUtils.isNotBlank(sourceFund.getDmtCname()) ? sourceFund.getDmtCname() : "");
								fundInfo.setDmtEname(StringUtils.isNotBlank(sourceFund.getDmtEname()) ? sourceFund.getDmtEname() : "");
								fundInfo.setGenisDeposit(DoubleUtil.formatMoney(sourceFund.getGenisDeposit()));
								fundInfo.setTotalCount(DoubleUtil.formatMoney(sourceFund.getTotalCount()));
								fundInfo.setTurnoverCount(DoubleUtil.formatMoney(sourceFund.getTurnoverCount()));
							}
						}
						
						String sql = "SELECT * FROM WLT_TRANSFER WHERE SOURCE_ASSET_ID = '" + sourceAsset.getAssetId() + "' or TARGET_ASSET_ID = '" + sourceAsset.getAssetId() + "' ORDER BY CREATED_TIME desc limit " + pageNo * pageSize + "," + pageSize;
						List<Map<String, Object>> res = sqlprovider.getCommonSqlMapper().executeSql(sql);
						List<CWVWltTransfer> transfers = getTransfers(res);
						if(transfers != null && !transfers.isEmpty()) {
							for(CWVWltTransfer transfer : transfers) {
								String targetHex = "";
								CWVWltAssetKey targetAssetKey = new CWVWltAssetKey(transfer.getTargetAssetId());
								CWVWltAsset targetAsset = daos.wltAssetDao.selectByPrimaryKey(targetAssetKey);
								if(targetAsset != null) {
									CWVWltAddrKey tatgetAddrkey = new CWVWltAddrKey(targetAsset.getAddrId());
									CWVWltAddr targetAddr = daos.wltAddrDao.selectByPrimaryKey(tatgetAddrkey);
									if(targetAddr != null) {
										targetHex = targetAddr.getHexAddr();
									}
								}
								
								String sourceHex = "";
								CWVWltAssetKey sourceAssetKey = new CWVWltAssetKey(transfer.getSourceAssetId());
								CWVWltAsset sourceAsset1 = daos.wltAssetDao.selectByPrimaryKey(sourceAssetKey);
								if(sourceAsset1 != null) {
									CWVWltAddrKey sourceAddrKey = new CWVWltAddrKey(sourceAsset1.getAddrId());
									CWVWltAddr sourceAddr1 = daos.wltAddrDao.selectByPrimaryKey(sourceAddrKey);
									if(sourceAddr1 != null) {
										sourceHex = sourceAddr1.getHexAddr();
									}
								}
								
								
								PMTransferDetail.Builder detail = PMTransferDetail.newBuilder();
								PMTransSignInfo.Builder in = PMTransSignInfo.newBuilder();
								in.setAmount(DoubleUtil.formatMoney(transfer.getSourceAmount()));
								in.setFund(fundInfo);
								in.setHexAddr(sourceHex);
								PMTransSignInfo.Builder out = PMTransSignInfo.newBuilder();
								out.setAmount(DoubleUtil.formatMoney(transfer.getTargetAmount()));
								out.setFund(fundInfo);
								out.setHexAddr(targetHex);
								detail.setInput(in);
								detail.setOutput(out);
								detail.setRequestNo(transfer.getReserved01());
								detail.setDateTime(transfer.getCreatedTime().getTime());
								ret.addTfs(detail);
							}
						}
						
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
	
	public List<CWVWltTransfer> getTransfers(List<Map<String, Object>> res){
		List<CWVWltTransfer> ret = new ArrayList<CWVWltTransfer>();
		/*[
		 * {
		 * 	SOURCE_FUND_ID=2c909514000003f7016174ed444e0008, 
		 * 	RESERVED_02=,
		 *  RESERVED_01=,
		 *  SOURCE_USER_ID=2c909514000003f7016174ea5d8c0004,
		 *  SOURCE_AMOUNT=2.0,
		 *  TRANSFER_ID=2c909514000003f7016174ef5602000d, 
		 *  PEND_ID=, 
		 *  UPDATED_TIME=2018-02-08 18:21:32.0, 
		 *  TRANSFER_CODE=713b3d21da511573e2ea0bd296ae008536cce5834e16ff4ef72c18458e48edaa, 
		 *  SOURCE_ASSET_ID=2c909514000003f7016174ed44560009, 
		 *  TARGET_FUND_ID=2c909514000003f7016174ed444e0008, 
		 *  TRANSFER_TYPE=01, 
		 *  TARGET_AMOUNT=0.0, 
		 *  TOTAL_FEE=0.0, 
		 *  TARGET_ASSET_ID=2c9095140000042b016174ffbe7a0002, 
		 *  TARGET_USER_ID=2c909514000003f7016174ea5d8c0004, 
		 *  TRANSFER_STATUS=01, 
		 *  CREATED_TIME=2018-02-08 18:21:32.0
		 * },
		 * 
		 */
		if(res != null && res.size() > 0) {
			for(Map<String, Object> map : res) {
				String sourceFundId = (String)map.get("SOURCE_FUND_ID");
				String reserved2 = (String)map.get("RESERVED_02");
				String reserved1 = (String) map.get("RESERVED_01");
				String sourceUserId = (String)map.get("SOURCE_USER_ID");
				double sourceAmount = (double) map.get("SOURCE_AMOUNT");
				String transferId = (String)map.get("TRANSFER_ID");
				String pendId = (String)map.get("PEND_ID");
				Date updatedTime = (Date)map.get("UPDATED_TIME");
				String transferCode = (String)map.get("TRANSFER_CODE");
				String sourceAssetId = (String)map.get("SOURCE_ASSET_ID");
				String targetFundId = (String)map.get("TARGET_FUND_ID");
				String transferType = (String) map.get("TRANSFER_TYPE");
				double targetAmount = (double)map.get("TARGET_AMOUNT");
				double totalFee = (double) map.get("TOTAL_FEE");
				String targetAssetId = (String)map.get("TARGET_ASSET_ID");
				String targetUserId = (String)map.get("TARGET_USER_ID");
				String transferStatus = (String)map.get("TRANSFER_STATUS");
				Date createdTime = (Date)map.get("CREATED_TIME");
				CWVWltTransfer trf = new CWVWltTransfer();
				trf.setCreatedTime(createdTime);
				trf.setPendId(pendId);
				trf.setReserved01(reserved1);
				trf.setReserved02(reserved2);
				trf.setSourceAmount(sourceAmount);
				trf.setSourceAssetId(sourceAssetId);
				trf.setSourceFundId(sourceFundId);
				trf.setSourceUserId(sourceUserId);
				trf.setTargetAmount(targetAmount);
				trf.setTargetAssetId(targetAssetId);
				trf.setTargetFundId(targetFundId);
				trf.setTargetUserId(targetUserId);
				trf.setTotalFee(totalFee);
				trf.setTransferCode(transferCode);
				trf.setTransferId(transferId);
				trf.setTransferStatus(transferStatus);
				trf.setTransferType(transferType);
				trf.setUpdatedTime(updatedTime);
				ret.add(trf);
			}
		}
		return ret;
	}

}