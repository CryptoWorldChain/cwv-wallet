package org.brewchain.cwv.wlt.helper;

import java.util.Date;
import java.util.HashMap;
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
import org.brewchain.cwv.wlt.util.ByteUtil;
import org.brewchain.cwv.wlt.util.DoubleUtil;
import org.brewchain.wallet.service.Wallet.RespGetAccount;
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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.Data;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.JsonSerializer;
import onight.tfw.outils.serialize.UUIDGenerator;

@iPojoBean
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Slf4j
@Data
public class CWBHelper implements ActorService {
	
	private PropHelper props = new PropHelper(null);
	
	@ActorRequire(name="http",scope="global")
	IPacketSender sender;
	
	@ActorRequire
	Daos daos;
	
	private final String TXT_STX = "http://ip:port/rootpath/txt/pbstx.do";
	private final String ACT_GAC = "http://ip:port/rootpath/act/pbgac.do";
	private final String ACT_CAC = "http://ip:port/rootpath/act/pbcac.do";
	
	/**
	 * CWB创建账户
	 * 
	 * 
	 */
	public void createAccount(){
		
	}
	
	/**
	 * CWB交易
	 * @throws Exception 
	 * 
	 * 
	 */
	public boolean transfer(int amount,int fee,int feeLimit,String senderAddr,String receiveAddr,String pubKey) throws Exception{
		RespGetAccount.Builder senderAccount;
		try {
			senderAccount = queryAccount(senderAddr);
		} catch (InvalidProtocolBufferException e1) {
			throw new InvalidProtocolBufferException("获取账户信息发生错误");
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("nonce", senderAccount.getAccount().getValue().getNonce());
		params.put("fee", fee);
		params.put("feeLimit", feeLimit);
		params.put("amount", amount);
		params.put("timestamp", new Date().getTime());
		params.put("data", ByteString.copyFrom(ByteUtil.EMPTY_BYTE_ARRAY));
		params.put("exdata", ByteString.copyFrom(ByteUtil.EMPTY_BYTE_ARRAY));
		params.put("receiveAddress", ByteString.copyFrom(receiveAddr.getBytes()));
		params.put("senderAddress", ByteString.copyFrom(senderAddr.getBytes()));
		params.put("pubKey", pubKey);
		params.put("txHash", ByteString.copyFrom(ByteUtil.EMPTY_BYTE_ARRAY));
		String sendJson = JsonSerializer.formatToString(params);
		
		FramePacket fposttx = PacketHelper.buildUrlFromJson(sendJson, "POST", TXT_STX);
		val txretReg = sender.send(fposttx, 30000);
		Map<String, Object> jsonRet = JsonSerializer.getInstance().deserialize(new String(txretReg.getBody()), Map.class);
		if(jsonRet.get("retCode")!=null&&Integer.parseInt(jsonRet.get("retCode").toString())==1){
			return true;
		}else{
			throw new Exception(jsonRet.get("retMsg")!=null?jsonRet.get("retMsg").toString():"未知错误");
		}
	}
	
	/**
	 * CWB交易
	 * @throws InvalidProtocolBufferException 
	 * 
	 * 
	 */
	public RespGetAccount.Builder queryAccount(String address) throws InvalidProtocolBufferException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", new Date().getTime());
		String sendJson = JsonSerializer.formatToString(params);
		
		FramePacket fposttx = PacketHelper.buildUrlFromJson(sendJson, "POST", ACT_GAC);
		val txretReg = sender.send(fposttx, 30000);
		RespGetAccount.Builder account = RespGetAccount.newBuilder().mergeFrom(txretReg.getBody());
		return account;
	}
}
