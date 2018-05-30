package org.brewchain.cwv.wlt.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddress;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddressKey;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltTx;
import org.brewchain.wallet.service.Wallet.MultiTransaction;
import org.brewchain.wallet.service.Wallet.MultiTransactionBody;
import org.brewchain.wallet.service.Wallet.MultiTransactionBodyImpl;
import org.brewchain.wallet.service.Wallet.MultiTransactionImpl;
import org.brewchain.wallet.service.Wallet.MultiTransactionInput;
import org.brewchain.wallet.service.Wallet.MultiTransactionInputImpl;
import org.brewchain.wallet.service.Wallet.MultiTransactionNodeImpl;
import org.brewchain.wallet.service.Wallet.MultiTransactionOutput;
import org.brewchain.wallet.service.Wallet.MultiTransactionOutputImpl;
import org.brewchain.wallet.service.Wallet.MultiTransactionSignature;
import org.brewchain.wallet.service.Wallet.MultiTransactionSignatureImpl;
import org.brewchain.wallet.service.Wallet.ReqCreateContractTransaction;
import org.brewchain.wallet.service.Wallet.ReqCreateMultiTransaction;
import org.brewchain.wallet.service.Wallet.RespCreateContractTransaction;
import org.brewchain.wallet.service.Wallet.RespCreateTransaction;
import org.brewchain.wallet.service.Wallet.RespGetTxByHash;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;
import com.googlecode.protobuf.format.JsonFormat;

import lombok.Data;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.bean.JsonPBFormat;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.JsonSerializer;

/**
 * @author jack
 * 
 *         address 账户相关信息获取
 * 
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "txHelper")
@Slf4j
@Data
public class TransactionHelper implements ActorService {

	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@ActorRequire(name = "http", scope = "global")
	IPacketSender sender;

	@ActorRequire(name = "daos", scope = "global")
	Daos daos;

	private static PropHelper props = new PropHelper(null);
	
	ObjectMapper mapper = new ObjectMapper();

	private static String CREATE_TRANSACTION = "http://127.0.0.1:8000/fbs/txt/pbmtx.do";
	private static String CREATE_CONTRACT = "http://127.0.0.1:8000/fbs/txt/pbsct.do";
	private static String QUERY_TRANSACTION = "http://127.0.0.1:8000/fbs/txt/pbgtx.do";
	static {
		CREATE_TRANSACTION = props.get("create_transaction", "http://127.0.0.1:8000/fbs/txt/pbmtx.do");
		CREATE_CONTRACT = props.get("create_contract", "http://127.0.0.1:8000/fbs/txt/pbsct.do");
		QUERY_TRANSACTION = props.get("query_transaction", "http://127.0.0.1:8000/fbs/txt/pbgtx.do");

		/**
		 * runtime配置参数：
		 * create_transaction=http://127.0.0.1:8000/fbs/txt/pbmtx.do
		 * create_contract=http://127.0.0.1:8000/fbs/txt/pbsct.do
		 * query_transaction=http://127.0.0.1:8000/fbs/txt/pbgtx.do
		 */
	}

	/**
	 * 创建交易
	 * 
	 * @param reqBody
	 * @return
	 */
	public RespCreateTransaction.Builder createTransaction(MultiTransactionImpl reqTransaction) {
		
		/**
		 * 接收对象为 transactionImpl
		 * 先转成 transaction
		 * 进行签名
		 * 再转成 transactionImpl
		 * 发送
		 */
		RespCreateTransaction.Builder ret = null;
		if(reqTransaction.getTxBody() != null){
			MultiTransactionBodyImpl reqBodyImpl = reqTransaction.getTxBody();
			if(reqBodyImpl.getInputsList() != null && !reqBodyImpl.getInputsList().isEmpty() && reqBodyImpl.getOutputsList() != null && !reqBodyImpl.getOutputsList().isEmpty()){
				MultiTransaction.Builder oTransaction = MultiTransaction.newBuilder();
				MultiTransactionBody.Builder oBody = MultiTransactionBody.newBuilder();
				//构建 oBody
				
				List<MultiTransactionInputImpl> reqInputsImpl = reqBodyImpl.getInputsList();
				List<MultiTransactionOutputImpl> reqOutputsImpl = reqBodyImpl.getOutputsList();
				
				for(MultiTransactionOutputImpl reqOutputImpl : reqOutputsImpl){
					String reqAddress = reqOutputImpl.getAddress();
					long reqAmount = reqOutputImpl.getAmount();
					String reqCryptoToken = reqOutputImpl.getCryptoToken();
					String reqSymbol = reqOutputImpl.getSymbol();
					
					if(StringUtils.isBlank(reqAddress)){
						log.warn("output's address is null");
						return null;
					}
					
					MultiTransactionOutput.Builder oOutput = MultiTransactionOutput.newBuilder();
					oOutput.setAddress(ByteString.copyFrom(encApi.hexDec(reqAddress)));
					oOutput.setAmount(reqAmount);
					oOutput.setCryptoToken(StringUtils.isNotBlank(reqCryptoToken) ? ByteString.copyFrom(encApi.hexDec(reqCryptoToken)) : ByteString.EMPTY);
					oOutput.setSymbol(StringUtils.isNotBlank(reqSymbol) ? reqSymbol : "");
					oBody.addOutputs(oOutput);
				}
				
				Map<String, String> keys = new HashMap<String, String>();
				for(MultiTransactionInputImpl reqInputImpl : reqInputsImpl){
					String reqAddress = reqInputImpl.getAddress();
					long reqAmount = reqInputImpl.getAmount();
					String reqCryptoToken = reqInputImpl.getCryptoToken();
					int reqFee = reqInputImpl.getFee();
					int reqFeeLimit = reqInputImpl.getFeeLimit();
					int reqNonce = reqInputImpl.getNonce();
					String reqPubKey = reqInputImpl.getPubKey();
					String reqSymbol = reqInputImpl.getSymbol();
					String reqToken = reqInputImpl.getToken();
					
					if(StringUtils.isBlank(reqAddress)){
						log.warn("input's address is null");
						return null;
					}
					
					CWVWltAddress addressEntity = getAddress(reqAddress);
					if(StringUtils.isBlank(reqPubKey)){
						reqPubKey = addressEntity.getPublicKey();
					}
					
					MultiTransactionInput.Builder oInput = MultiTransactionInput.newBuilder();
					oInput.setAddress(ByteString.copyFrom(encApi.hexDec(reqAddress)));
					oInput.setAmount(reqAmount);
					oInput.setCryptoToken(StringUtils.isNotBlank(reqCryptoToken) ? ByteString.copyFrom(encApi.hexDec(reqCryptoToken)) : ByteString.EMPTY);
					oInput.setFee(reqFee);
					oInput.setFeeLimit(reqFeeLimit);
					oInput.setNonce(reqNonce);
					oInput.setPubKey(reqPubKey);
					oInput.setSymbol(StringUtils.isNotBlank(reqSymbol) ? reqSymbol : "");
					oInput.setToken(StringUtils.isNotBlank(reqToken) ? reqToken : "");
					oBody.addInputs(oInput);
					
					keys.put(reqPubKey, addressEntity.getPrivateKey());
				}
				
				if(StringUtils.isNotBlank(reqBodyImpl.getExdata())){
					oBody.setExdata(ByteString.copyFrom(encApi.hexDec(reqBodyImpl.getExdata())));
				}
				
				if(reqBodyImpl.getDelegateList() != null && !reqBodyImpl.getDelegateList().isEmpty()){
					for(String str : reqBodyImpl.getDelegateList()){
						oBody.addDelegate(ByteString.copyFrom(encApi.hexDec(str)));
					}
				}
				
				if(StringUtils.isNotBlank(reqBodyImpl.getData())){
					oBody.setData(ByteString.copyFrom(encApi.hexDec(reqBodyImpl.getData())));
				}
				
				oBody.setTimestamp(System.currentTimeMillis());
				
				String pub = "";
				MultiTransactionSignature.Builder oMultiTransactionSignature21 = null;
				//签名
				List<MultiTransactionSignature.Builder> signatures = new ArrayList<MultiTransactionSignature.Builder>();
				for(MultiTransactionInputImpl reqInput : reqInputsImpl){
					String reqAddress = reqInput.getAddress();
					CWVWltAddress addressEntity = getAddress(reqAddress);
					pub = addressEntity.getPublicKey();
					String pri = addressEntity.getPrivateKey();
					oMultiTransactionSignature21 = MultiTransactionSignature.newBuilder();
					oMultiTransactionSignature21.setPubKey(pub);
					oMultiTransactionSignature21.setSignature(encApi.hexEnc(encApi.ecSign(pri, oBody.build().toByteArray())));
					signatures.add(oMultiTransactionSignature21);
				}
				
				for(MultiTransactionSignature.Builder s: signatures){
					oBody.addSignatures(s);
				}
				
				oTransaction.setTxBody(oBody);
				
				ReqCreateMultiTransaction.Builder oCreate = ReqCreateMultiTransaction.newBuilder();
				
				MultiTransactionImpl.Builder oTransactionImpl = parseToImpl(oTransaction.build());
				oCreate.setTransaction(oTransactionImpl);
				
				JsonFormat jsonFormat = new JsonFormat();
				String jsonView = jsonFormat.printToString(oCreate.build());
				FramePacket fposttx = PacketHelper.buildUrlFromJson(jsonView, "POST", CREATE_TRANSACTION);
				val qryTxRet = sender.send(fposttx, 30000);
				JsonNode retNode = null;
				try{
					retNode = mapper.readTree(qryTxRet.getBody());
				} catch (Exception e){
					log.error("parse create transaction return error : " + e.getMessage());
				}
				
				if(retNode != null && retNode.has("retCode") && retNode.get("retCode").asInt() == 1){
					ret = RespCreateTransaction.newBuilder();
					ret.setTxHash(retNode.has("txHash") ? retNode.get("txHash").asText() : "");
					ret.setRetCode(retNode.get("retCode").asInt());
					ret.setRetMsg(retNode.has("retMsg") ? retNode.get("retMsg").asText() : "");
				}
			}else{
				log.warn("inputs or outputs is null");
			}
		}else{
			log.warn("tx body is null");
		}

		return ret;
	}

	/**
	 * 根据 txHash 获取 交易详情
	 * 
	 * @param txHash
	 * @return
	 */
	@SuppressWarnings("static-access")
	public RespGetTxByHash.Builder queryTransactionByTxHash(String txHash) {
		RespGetTxByHash.Builder ret = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("hexTxHash", txHash);

		String sendJson = JsonSerializer.getInstance().formatToString(param);
		FramePacket fposttx = PacketHelper.buildUrlFromJson(sendJson, "POST", QUERY_TRANSACTION);
		val qryTxRet = sender.send(fposttx, 30000);
		
		JsonNode retNode = null;
		try{
			retNode = mapper.readTree(qryTxRet.getBody());
		} catch(Exception e){
			log.error("parse query transaction error : " + e.getMessage());
		}
		
		if(retNode != null && retNode.has("retCode") && retNode.get("retCode").asInt() == 1){
			ret = parseJson2RespGetTxByHash(retNode);
		}
		
		return ret;
	}

	/**
	 * 创建合约
	 * 
	 * @param reqBody
	 * @return
	 */
	public RespCreateContractTransaction.Builder createContract(ReqCreateContractTransaction pb) {
		/**
		 * 拿到 InputImpl 等对象
		 * 转成 Input 对象
		 * 构建 Transaction 对象
		 * 签名
		 * 转成TransactionImpl 对象
		 * 
		 */
		
		RespCreateContractTransaction.Builder ret = null;
		ReqCreateContractTransaction.Builder reqCreate = ReqCreateContractTransaction.newBuilder();
		
		MultiTransactionInputImpl reqInputImpl = pb.getInput();
		if(reqInputImpl != null){
			String reqAddress = reqInputImpl.getAddress();
			
			CWVWltAddress addressEntity = getAddress(reqAddress);
			
			long currentTime = System.currentTimeMillis();
			
			MultiTransactionBody.Builder oMultiTransactionBody = MultiTransactionBody.newBuilder();
			oMultiTransactionBody.setData(ByteString.copyFrom(encApi.hexDec(pb.getData())));
			for (String delegate : pb.getDelegateList()) {
				oMultiTransactionBody.addDelegate(ByteString.copyFrom(encApi.hexDec(delegate)));
			}
			oMultiTransactionBody.setExdata(ByteString.copyFrom(encApi.hexDec(pb.getExdata())));

			MultiTransactionInput.Builder oMultiTransactionInput = MultiTransactionInput.newBuilder();
			oMultiTransactionInput.setAddress(ByteString.copyFrom(encApi.hexDec(pb.getInput().getAddress())));
			oMultiTransactionInput.setAmount(pb.getInput().getAmount());
			oMultiTransactionInput.setCryptoToken(ByteString.copyFrom(encApi.hexDec(pb.getInput().getCryptoToken())));
			oMultiTransactionInput.setFee(pb.getInput().getFee());
			oMultiTransactionInput.setNonce(pb.getInput().getNonce());
			oMultiTransactionInput.setSymbol(pb.getInput().getSymbol());
			oMultiTransactionInput.setToken(pb.getInput().getToken());
			oMultiTransactionBody.addInputs(oMultiTransactionInput);
			oMultiTransactionBody.setTimestamp(currentTime);
			
			MultiTransactionSignatureImpl.Builder oMultiTransactionSignatureImpl = MultiTransactionSignatureImpl.newBuilder();
			oMultiTransactionSignatureImpl.setPubKey(addressEntity.getPublicKey());
			oMultiTransactionSignatureImpl.setSignature(encApi.hexEnc(encApi.ecSign(addressEntity.getPrivateKey(), oMultiTransactionBody.build().toByteArray())));
			
			
			//transactionImpl -> createContract
			reqCreate.setData(pb.getData());
			reqCreate.setTimestamp(currentTime);
			reqCreate.setInput(pb.getInput());
			reqCreate.setExdata(pb.getExdata());
			reqCreate.setSignature(oMultiTransactionSignatureImpl);
			if(pb.getDelegateList() != null && !pb.getDelegateList().isEmpty()){
				for(String delegate : pb.getDelegateList()){
					reqCreate.addDelegate(delegate);
				}
			}
			
			JsonPBFormat jsonFormat = new JsonPBFormat();
			String jsonView = jsonFormat.printToString(reqCreate.build());
			FramePacket fposttx = PacketHelper.buildUrlFromJson(jsonView, "POST", CREATE_CONTRACT);
			val crtTxRet = sender.send(fposttx, 30000);
			
			JsonNode retNode = null;
			try {
				retNode = mapper.readTree(crtTxRet.getBody());
			} catch (IOException e) {
				log.error("parse ret error : " + new String(crtTxRet.getBody()));
			}
			
			if(retNode != null && retNode.has("retCode") && retNode.get("retCode").asInt() == 0){
				ret = RespCreateContractTransaction.newBuilder();
				ret.setContractAddress(retNode.has("contractAddress") ? retNode.get("contractAddress").asText() : "");
				ret.setRetCode(1);
				ret.setRetMsg(retNode.has("retMsg") ? retNode.get("retMsg").asText() : "");
				ret.setTxHash(retNode.has("txHash") ? retNode.get("txHash").asText() : "");
			}
			
		}else{
			log.warn("input or data is null");
		}
		
		return ret;
	}

	/**
	 * @param reqBody
	 * @return
	 */
	public RespCreateTransaction.Builder doContract(byte[] reqBody) {
		// 执行合约走的是创建交易的流程，所以结构需要与创建交易保持一致，参与合约的地址为 input，合约地址为 output
		RespCreateTransaction.Builder ret = null;
//		ret = createTransaction(reqBody);

		return ret;
	}
	
	/*********************************************************************************************/
	
	/**
	 * @param retNode
	 * @return
	 */
	public RespGetTxByHash.Builder parseJson2RespGetTxByHash(JsonNode retNode){
		RespGetTxByHash.Builder ret = RespGetTxByHash.newBuilder();
		MultiTransactionImpl.Builder transaction = MultiTransactionImpl.newBuilder();
		if(retNode.has("transaction")) {
			JsonNode tNode = retNode.get("transaction");
			transaction.setTxBody(getTxBodyFromTransaction(tNode.get("txBody")));
			if(tNode.has("node"))
				transaction.setNode(getMultiTransactionNode(retNode.get("node")));
			transaction.setStatus(tNode.has("status") ? tNode.get("status").asText() : "");
			transaction.setTxHash(tNode.has("txHash") ? tNode.get("txHash").asText() : "");
			
			ret.setTransaction(transaction);
		}
		ret.setRetCode(retNode.get("retCode").asInt());
		
		return ret;
	}
	
	/**
	 * @param node = ReqCreateContractTransaction
	 * @return
	 */
	public MultiTransactionBodyImpl getTxBodyFromCreateContract(JsonNode node){
		MultiTransactionBodyImpl.Builder body = MultiTransactionBodyImpl.newBuilder();
		
		body.setData(node.has("data") ? node.get("data").asText() : "");
		body.setExdata(node.has("exdata") ? node.get("exdata").asText() : "");
		
		if(node.has("deledate")){
			ArrayNode array = (ArrayNode) node.get("delegate");
			for (int i = 0; i < array.size(); i++){
				body.addDelegate(array.get(i).asText());
			}
		}
		
		if(node.has("input")){
			JsonNode input = node.get("input");
			body.addInputs(getInput(input));
		}
		
		if(node.has("output")){
			JsonNode output = node.get("output");
			body.addOutputs(getOutput(output));
		}
		
		body.setTimestamp(node.has("timestamp") ? node.get("timestamp").asLong() : 0l);
		
		return body.build();
	}

	/**
	 * 签名前，body中的内容需要补充完整
	 * 
	 * @param node = txBody
	 * @return
	 */
	public MultiTransactionBodyImpl getTxBodyFromTransaction(JsonNode node){
		MultiTransactionBodyImpl.Builder body = MultiTransactionBodyImpl.newBuilder();
		
		body.setData(node.has("data") ? node.get("data").asText() : "");
		body.setExdata(node.has("exdata") ? node.get("exdata").asText() : "");
		
		if(node.has("deledate")){
			ArrayNode array = (ArrayNode) node.get("delegate");
			for (int i = 0; i < array.size(); i++){
				body.addDelegate(array.get(i).asText());
			}
		}
		
		if(node.has("inputs")){
			ArrayNode inputs = (ArrayNode) node.get("inputs");
			for (JsonNode input : inputs){
				body.addInputs(getInput(input));
			}
		}
		
		if(node.has("outputs")){
			ArrayNode outputs = (ArrayNode) node.get("outputs");
			for(JsonNode output : outputs){
				body.addOutputs(getOutput(output));
			}
		}
		
		body.setTimestamp(node.has("timestamp") ? node.get("timestamp").asLong() : 0l);
		
		return body.build();
	}
	
	/**
	 * @param input
	 * @return
	 */
	public MultiTransactionInputImpl.Builder getInput(JsonNode input){
		MultiTransactionInputImpl.Builder inputB = MultiTransactionInputImpl.newBuilder();
		inputB.setAddress(input.has("address") ? input.get("address").asText() : "");
		inputB.setAmount(input.has("amount") ? input.get("amount").asLong() : 0l);
		inputB.setCryptoToken(input.has("cryptoToken") ? input.get("cryptoToken").asText() : "");
		inputB.setFee(input.has("fee") ? input.get("fee").asInt() : 0);
		inputB.setFeeLimit(input.has("feeLimit") ? input.get("feeLimit").asInt() : 0);
		inputB.setNonce(input.has("nonce") ? input.get("nonce").asInt() : 0);
		inputB.setPubKey(input.has("pubKey") ? input.get("pubKey").asText() : "");
		inputB.setSymbol(input.has("symbol") ? input.get("symbol").asText() : "");
		inputB.setToken(input.has("token") ? input.get("token").asText() : "");
		
		return inputB;
	}
	
	/**
	 * @param output
	 * @return
	 */
	public MultiTransactionOutputImpl.Builder getOutput(JsonNode output){
		MultiTransactionOutputImpl.Builder outputB = MultiTransactionOutputImpl.newBuilder();
		outputB.setAddress(output.has("address") ? output.get("address").asText() : "");
		outputB.setAmount(output.has("amount") ? output.get("amount").asLong() : 0l);
		outputB.setCryptoToken(output.has("cryptoToken") ? output.get("cryptoToken").asText() : "");
		outputB.setSymbol(output.has("symbol") ? output.get("symbol").asText() : "");
		return outputB;
	}
	
	/**
	 * @param retNode
	 * @return
	 */
	public MultiTransactionNodeImpl.Builder getMultiTransactionNode(JsonNode retNode){
		MultiTransactionNodeImpl.Builder node = MultiTransactionNodeImpl.newBuilder();
		node.setBcuid(retNode.has("bcuid") ? retNode.get("bcuid").asText() : "");
		node.setIp(retNode.has("ip") ? retNode.get("ip").asText() : "");
		node.setNode(retNode.has("node") ? retNode.get("node").asText() : "");
		return node;
	}
	
	/**
	 * @param address
	 * @return
	 */
	public CWVWltAddress getAddress(String address){
		CWVWltAddress addr = daos.wltAddressDao.selectByPrimaryKey(new CWVWltAddressKey(address));
		if(addr != null){
			return addr;
		}else{
			return null;
		}
	}

	/**
	 * @param txHash
	 */
	public void insertTxHash(String txHash) {
		CWVWltTx tx = new CWVWltTx();
		tx.setTxId(txHash);
		tx.setCreatedTime(new Date());
		tx.setUpdatedTime(tx.getCreatedTime());
		daos.wltTxDao.insert(tx);
	}
	
	/*********************************** account 对象转换 **********************************************************/
	
	/**
	 * 映射为接口类型
	 * 
	 * @param oTransaction
	 * @return
	 */
	public MultiTransactionImpl.Builder parseToImpl(MultiTransaction oTransaction) {
		MultiTransactionBody oMultiTransactionBody = oTransaction.getTxBody();

		MultiTransactionImpl.Builder oMultiTransactionImpl = MultiTransactionImpl.newBuilder();
		oMultiTransactionImpl.setTxHash(encApi.hexEnc(oTransaction.getTxHash().toByteArray()));
		
		oMultiTransactionImpl.setStatus(StringUtils.isNotBlank(oTransaction.getStatus()) ? oTransaction.getStatus() : "");

		MultiTransactionBodyImpl.Builder oMultiTransactionBodyImpl = MultiTransactionBodyImpl.newBuilder();
		oMultiTransactionBodyImpl.setData(encApi.hexEnc(oMultiTransactionBody.getData().toByteArray()));
		for (ByteString delegate : oMultiTransactionBody.getDelegateList()) {
			oMultiTransactionBodyImpl.addDelegate(encApi.hexEnc(delegate.toByteArray()));
		}
		oMultiTransactionBodyImpl.setExdata(encApi.hexEnc(oMultiTransactionBody.getExdata().toByteArray()));
		for (MultiTransactionInput input : oMultiTransactionBody.getInputsList()) {
			MultiTransactionInputImpl.Builder oMultiTransactionInputImpl = MultiTransactionInputImpl.newBuilder();
			oMultiTransactionInputImpl.setAddress(encApi.hexEnc(input.getAddress().toByteArray()));
			oMultiTransactionInputImpl.setAmount(input.getAmount());
			oMultiTransactionInputImpl.setCryptoToken(encApi.hexEnc(input.getCryptoToken().toByteArray()));
			oMultiTransactionInputImpl.setFee(input.getFee());
			oMultiTransactionInputImpl.setNonce(input.getNonce());
			oMultiTransactionInputImpl.setPubKey(input.getPubKey());
			oMultiTransactionInputImpl.setSymbol(input.getSymbol());
			oMultiTransactionInputImpl.setToken(input.getToken());
			oMultiTransactionBodyImpl.addInputs(oMultiTransactionInputImpl);
		}
		for (MultiTransactionOutput output : oMultiTransactionBody.getOutputsList()) {
			MultiTransactionOutputImpl.Builder oMultiTransactionOutputImpl = MultiTransactionOutputImpl.newBuilder();
			oMultiTransactionOutputImpl.setAddress(encApi.hexEnc(output.getAddress().toByteArray()));
			oMultiTransactionOutputImpl.setAmount(output.getAmount());
			oMultiTransactionOutputImpl.setCryptoToken(encApi.hexEnc(output.getCryptoToken().toByteArray()));
			oMultiTransactionOutputImpl.setSymbol(output.getSymbol());
			oMultiTransactionBodyImpl.addOutputs(oMultiTransactionOutputImpl);
		}
		// oMultiTransactionBodyImpl.setSignatures(index, value)
		for (MultiTransactionSignature signature : oMultiTransactionBody.getSignaturesList()) {
			MultiTransactionSignatureImpl.Builder oMultiTransactionSignatureImpl = MultiTransactionSignatureImpl
					.newBuilder();
			oMultiTransactionSignatureImpl.setPubKey(signature.getPubKey());
			oMultiTransactionSignatureImpl.setSignature(signature.getSignature());
			oMultiTransactionBodyImpl.addSignatures(oMultiTransactionSignatureImpl);
		}
		oMultiTransactionBodyImpl.setTimestamp(oMultiTransactionBody.getTimestamp());
		oMultiTransactionImpl.setTxBody(oMultiTransactionBodyImpl);
		return oMultiTransactionImpl;
	}

	public MultiTransaction.Builder parse(MultiTransactionImpl oTransaction) {
		MultiTransactionBodyImpl oMultiTransactionBodyImpl = oTransaction.getTxBody();

		MultiTransaction.Builder oMultiTransaction = MultiTransaction.newBuilder();
		oMultiTransaction.setTxHash(ByteString.copyFrom(encApi.hexDec(oTransaction.getTxHash())));

		MultiTransactionBody.Builder oMultiTransactionBody = MultiTransactionBody.newBuilder();
		oMultiTransactionBody.setData(ByteString.copyFrom(encApi.hexDec(oMultiTransactionBodyImpl.getData())));
		for (String delegate : oMultiTransactionBodyImpl.getDelegateList()) {
			oMultiTransactionBody.addDelegate(ByteString.copyFrom(encApi.hexDec(delegate)));
		}
		oMultiTransactionBody.setExdata(ByteString.copyFrom(encApi.hexDec(oMultiTransactionBodyImpl.getExdata())));
		for (MultiTransactionInputImpl input : oMultiTransactionBodyImpl.getInputsList()) {
			MultiTransactionInput.Builder oMultiTransactionInput = MultiTransactionInput.newBuilder();
			oMultiTransactionInput.setAddress(ByteString.copyFrom(encApi.hexDec(input.getAddress())));
			oMultiTransactionInput.setAmount(input.getAmount());
			oMultiTransactionInput.setCryptoToken(ByteString.copyFrom(encApi.hexDec(input.getCryptoToken())));
			oMultiTransactionInput.setFee(input.getFee());
			oMultiTransactionInput.setNonce(input.getNonce());
			oMultiTransactionInput.setPubKey(input.getPubKey());
			oMultiTransactionInput.setSymbol(input.getSymbol());
			oMultiTransactionInput.setToken(input.getToken());
			oMultiTransactionBody.addInputs(oMultiTransactionInput);
		}
		for (MultiTransactionOutputImpl output : oMultiTransactionBodyImpl.getOutputsList()) {
			MultiTransactionOutput.Builder oMultiTransactionOutput = MultiTransactionOutput.newBuilder();
			oMultiTransactionOutput.setAddress(ByteString.copyFrom(encApi.hexDec(output.getAddress())));
			oMultiTransactionOutput.setAmount(output.getAmount());
			oMultiTransactionOutput.setCryptoToken(ByteString.copyFrom(encApi.hexDec(output.getCryptoToken())));
			oMultiTransactionOutput.setSymbol(output.getSymbol());
			oMultiTransactionBody.addOutputs(oMultiTransactionOutput);
		}
		// oMultiTransactionBodyImpl.setSignatures(index, value)
		for (MultiTransactionSignatureImpl signature : oMultiTransactionBodyImpl.getSignaturesList()) {
			MultiTransactionSignature.Builder oMultiTransactionSignature = MultiTransactionSignature.newBuilder();
			oMultiTransactionSignature.setPubKey(signature.getPubKey());
			oMultiTransactionSignature.setSignature(signature.getSignature());
			oMultiTransactionBody.addSignatures(oMultiTransactionSignature);
		}
		oMultiTransactionBody.setTimestamp(oMultiTransactionBodyImpl.getTimestamp());
		oMultiTransaction.setTxBody(oMultiTransactionBody);
		return oMultiTransaction;
	}
}