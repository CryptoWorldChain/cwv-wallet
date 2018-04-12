package org.brewchain.cwv.wlt.util;

import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFund;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltTransfer;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.JsonSerializer;
import onight.tfw.outils.serialize.UUIDGenerator;

public class BackUpUtil {
	
	private final static ObjectMapper mapper = new ObjectMapper();
	
	public static void backUp(String data, IPacketSender sender, String chainapi, String hexAddr, String rpmdHash) {
		ObjectNode node = mapper.createObjectNode();
		node.put("trade_no", UUIDGenerator.generate());
		ArrayNode arrayNode = mapper.createArrayNode();
		ObjectNode arrNodeItem = mapper.createObjectNode();
		arrNodeItem.put("hex_addr", hexAddr);
		arrNodeItem.put("rpmd_hash",rpmdHash);
		arrayNode.add(arrNodeItem);
		node.put("co_signers", arrayNode);
		node.put("alias", "backupdata");
		node.put("signed_code", "skip");
		ObjectNode fundNode = mapper.createObjectNode();
		fundNode.put("count", 0);
		fundNode.put("dmt_ename", "");
		fundNode.put("dmt", 0);
		node.put("fund", fundNode);
		node.put("metadata", data);
		String sendJson = JsonSerializer.formatToString(node);
		FramePacket fp = PacketHelper.buildUrlFromJson(sendJson, "POST", chainapi);
		sender.send(fp, 30000);
	}
	
	public static void backUpAddr(CWVWltAddr addr, IPacketSender sender, String chainapi, String hexAddr, String rpmdHash) {
		ObjectNode node = mapper.createObjectNode();
		node.put("addrId", addr.getAddrId());
		node.put("addrAlias",addr.getAddrAlias());
		node.put("hexAddr",addr.getHexAddr());
		node.put("privatekey",addr.getPrivateKey());
		node.put("publicKey",addr.getPublicKey());
		node.put("publicKeyHash",addr.getPublicKeyHash());
		node.put("userId",addr.getUserId());
		
		ObjectNode data = mapper.createObjectNode();
		data.put("type","address");
		data.put("info", node);
		
		backUp(data.toString(), sender, chainapi, hexAddr, rpmdHash);
	}
	
	public static void backUpUser(CWVWltUser user, IPacketSender sender, String chainapi, String hexAddr, String rpmdHash) {
		ObjectNode node = mapper.createObjectNode();
		node.put("userId",user.getUserId());
		node.put("userCode",user.getUserCode());
		
		ObjectNode data = mapper.createObjectNode();
		data.put("type","user");
		data.put("info", node);
		
		backUp(data.toString(), sender, chainapi, hexAddr, rpmdHash);
	}
	
	public static void backUpFund(CWVWltFund fund, IPacketSender sender, String chainapi, String hexAddr, String rpmdHash) {
		ObjectNode node = mapper.createObjectNode();
		node.put("coloredFbc",fund.getColoredFbc());
		node.put("emtCname",fund.getDmtCname());
		node.put("dmtEname",fund.getDmtEname());
		node.put("exchangeFbc",fund.getExchangeFbc());
		node.put("fundId",fund.getFundId());
		node.put("genisAddr",fund.getGenisAddr());
		node.put("turnoverCount",fund.getTurnoverCount());
		node.put("userId",fund.getUserId());
		
		ObjectNode data = mapper.createObjectNode();
		data.put("type","user");
		data.put("info", node);
		
		backUp(data.toString(), sender, chainapi, hexAddr, rpmdHash);
	}
	
	public static void backUpAsset(CWVWltAsset asset, IPacketSender sender, String chainapi, String hexAddr, String rpmdHash) {
		ObjectNode node = mapper.createObjectNode();
		node.put("addrId",asset.getAddrId());
		node.put("assetAlias",asset.getAssetAlias());
		node.put("assetId",asset.getAssetId());
		node.put("assetPubHash",asset.getAssetPubHash());
		node.put("bcTxid",asset.getBcTxid());
		node.put("dmtCname",asset.getDmtCname());
		node.put("dmtEname",asset.getDmtEname());
		node.put("fundId",asset.getFundId());
		node.put("holdCount",asset.getHoldCount());
		node.put("userId",asset.getUserId());
		
		ObjectNode data = mapper.createObjectNode();
		data.put("type","asset");
		data.put("info", node);
		
		backUp(data.toString(), sender, chainapi, hexAddr, rpmdHash);
	}
	
	public static void backUpTransfer(CWVWltTransfer transfer, IPacketSender sender, String chainapi, String hexAddr, String rpmdHash) {
		ObjectNode node = mapper.createObjectNode();
		node.put("sourceAmount",transfer.getSourceAmount());
		node.put("sourceAssetId",transfer.getSourceAssetId());
		node.put("sourceFundId",transfer.getSourceFundId());
		node.put("sourceUserId",transfer.getSourceUserId());
		node.put("targetAmount",transfer.getTargetAmount());
		node.put("targetAssetId",transfer.getTargetAssetId());
		node.put("targetFundId",transfer.getTargetFundId());
		node.put("targetUserId",transfer.getTargetUserId());
		node.put("totalFee",transfer.getTotalFee());
		node.put("transferCode",transfer.getTransferCode());
		node.put("transferId",transfer.getTransferId());
		
		ObjectNode data = mapper.createObjectNode();
		data.put("type","transfer");
		data.put("info", node);
		
		backUp(data.toString(), sender, chainapi, hexAddr, rpmdHash);
	}
	
	
}
