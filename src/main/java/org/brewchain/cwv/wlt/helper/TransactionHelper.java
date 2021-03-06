package org.brewchain.cwv.wlt.helper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddress;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddressExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltContract;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltParameter;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltParameterExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltTx;
import org.brewchain.cwv.wlt.enums.TransTypeEnum;
import org.brewchain.cwv.wlt.utils.ByteUtil;
import org.brewchain.wallet.service.Wallet.BlockMinerImpl;
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
import org.brewchain.wallet.service.Wallet.ReqDoContractTransaction;
import org.brewchain.wallet.service.Wallet.RespBlockDetail;
import org.brewchain.wallet.service.Wallet.RespBlockDetail.Builder;
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
import onight.tfw.outils.serialize.JsonSerializer;
import onight.tfw.outils.serialize.UUIDGenerator;

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

	ObjectMapper mapper = new ObjectMapper();

	BigDecimal ws = new BigDecimal("1000000000000000000");

	private static String CREATE_TRANSACTION = "createTransactionURL";
	private static String CREATE_CONTRACT = "createContractURL";
	private static String QUERY_TRANSACTION = "queryTransaction";
	private static String QUERY_LASTBLOCK = "queryLastBlock";

	/**
	 * 创建交易
	 * 
	 * 如果 erc20 或者 erc721 交易，需要所有发起方的 token 或者 symbol 均相同，即同一笔交易，只能实现一种token的转移
	 * 
	 * @param reqBody
	 * @return
	 */
	public RespCreateTransaction.Builder createTransaction(MultiTransactionImpl reqTransaction) {

		/**
		 * 接收对象为 transactionImpl 先转成 transaction 进行签名 再转成 transactionImpl 发送
		 */
		RespCreateTransaction.Builder ret = RespCreateTransaction.newBuilder();
		if (reqTransaction.getTxBody() == null) {
			ret.setRetCode(-1);
			ret.setRetMsg("tx body is null");
			return ret;
		}

		MultiTransactionBodyImpl reqBodyImpl = reqTransaction.getTxBody();
		if (reqBodyImpl.getInputsList() == null || reqBodyImpl.getInputsList().isEmpty()) {
			ret.setRetCode(-1);
			ret.setRetMsg("inputs is null");
			return ret;
		}

		MultiTransaction.Builder oTransaction = MultiTransaction.newBuilder();
		MultiTransactionBody.Builder oBody = MultiTransactionBody.newBuilder();
		// 构建 oBody

		List<MultiTransactionInputImpl> reqInputsImpl = reqBodyImpl.getInputsList();
		int reqType = 0;

		for (MultiTransactionInputImpl reqInputImpl : reqInputsImpl) {
			String reqAddress = reqInputImpl.getAddress();
			BigDecimal reqAmount = StringUtils.isEmpty(reqInputImpl.getAmount()) ? new BigDecimal("0")
					: ws.multiply(new BigDecimal(reqInputImpl.getAmount()));
			log.debug("input's amount is :------------->" + reqAmount.toBigInteger());
			String reqCryptoToken = reqInputImpl.getCryptoToken();
			int reqFee = reqInputImpl.getFee();
			int reqFeeLimit = reqInputImpl.getFeeLimit();
			int reqNonce = reqInputImpl.getNonce();
			String reqSymbol = reqInputImpl.getSymbol();
			String reqToken = reqInputImpl.getToken();

			if (StringUtils.isBlank(reqAddress)) {
				return ret.setRetCode(-1).setRetMsg("input's address is nul");
			}

			MultiTransactionInput.Builder oInput = MultiTransactionInput.newBuilder();
			oInput.setAddress(ByteString.copyFrom(encApi.hexDec(reqAddress)));
			oInput.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(reqAmount.toBigInteger())));
			if (StringUtils.isNotBlank(reqToken)) {
				oInput.setToken(reqToken);
				if (reqType == 0) {
					reqType = TransTypeEnum.TYPE_TokenTransaction.value();
					oBody.setType(TransTypeEnum.TYPE_TokenTransaction.value());
				} else if (reqType == TransTypeEnum.TYPE_CryptoTokenTransaction.value()) {
					return ret.setRetCode(-1).setRetMsg("erc2.0 and erc721 can not send together");
				}
			} else {
				oInput.setToken("");
			}
			oInput.setNonce(reqNonce);
			if (StringUtils.isNoneBlank(reqSymbol, reqCryptoToken)) {
				oInput.setSymbol(reqSymbol);
				oInput.setCryptoToken(ByteString.copyFrom(encApi.hexDec(reqCryptoToken)));
				if (reqType == 0) {
					reqType = TransTypeEnum.TYPE_CryptoTokenTransaction.value();
					oBody.setType(TransTypeEnum.TYPE_CryptoTokenTransaction.value());
				} else if (reqType == TransTypeEnum.TYPE_TokenTransaction.value()) {
					return ret.setRetCode(-1).setRetMsg("erc2.0 and erc721 can not send together");
				}
			} else {
				oInput.setSymbol("");
				oInput.setCryptoToken(ByteString.EMPTY);
			}
			if (reqBodyImpl.getType() != 0) {
				oBody.setType(reqBodyImpl.getType());
			}
			oBody.addInputs(oInput);

		}

		if (reqBodyImpl.getOutputsList() != null && !reqBodyImpl.getOutputsList().isEmpty()) {
			List<MultiTransactionOutputImpl> reqOutputsImpl = reqBodyImpl.getOutputsList();

			for (MultiTransactionOutputImpl reqOutputImpl : reqOutputsImpl) {
				String reqAddress = reqOutputImpl.getAddress();
				BigDecimal reqAmount = StringUtils.isEmpty(reqOutputImpl.getAmount()) ? new BigDecimal("0")
						: ws.multiply(new BigDecimal(reqOutputImpl.getAmount()));
				log.debug("output's amount is :------------->" + reqAmount.toBigInteger());
				log.debug("");
				String reqCryptoToken = reqOutputImpl.getCryptoToken();
				String reqSymbol = reqOutputImpl.getSymbol();

				if (StringUtils.isBlank(reqAddress)) {
					return ret.setRetCode(-1).setRetMsg("output's address is nul");
				}

				MultiTransactionOutput.Builder oOutput = MultiTransactionOutput.newBuilder();
				oOutput.setAddress(ByteString.copyFrom(encApi.hexDec(reqAddress)));
				oOutput.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(reqAmount.toBigInteger())));
				oOutput.setCryptoToken(StringUtils.isNotBlank(reqCryptoToken)
						? ByteString.copyFrom(encApi.hexDec(reqCryptoToken)) : ByteString.EMPTY);
				oOutput.setSymbol(StringUtils.isNotBlank(reqSymbol) ? reqSymbol : "");
				oBody.addOutputs(oOutput);
			}

		} else {
			// 创建房产
			oBody.setType(TransTypeEnum.TYPE_CreateCryptoToken.value());
		}

		if (StringUtils.isNotBlank(reqBodyImpl.getData())) {
			// 如果 data 不为空， 说明是创建合约交易 修改执行合约bug
			// oBody.setData(ByteString.copyFromUtf8(reqBodyImpl.getData()));
			oBody.setData(ByteString.copyFrom(encApi.hexDec(reqBodyImpl.getData())));

		}

		if (StringUtils.isNotBlank(reqBodyImpl.getExdata())) {
			oBody.setExdata(ByteString.copyFrom(encApi.hexDec(reqBodyImpl.getExdata())));
		}

		if (reqBodyImpl.getDelegateList() != null && !reqBodyImpl.getDelegateList().isEmpty()) {
			for (String str : reqBodyImpl.getDelegateList()) {
				oBody.addDelegate(ByteString.copyFrom(encApi.hexDec(str)));
			}
		}

		oBody.setTimestamp(System.currentTimeMillis());
		log.debug("signature param:--->" + new JsonFormat().printToString(oBody.build()));
		String pub = "";
		MultiTransactionSignature.Builder oMultiTransactionSignature21 = null;
		// 签名
		// List<MultiTransactionSignature.Builder> signatures = new
		// ArrayList<MultiTransactionSignature.Builder>();
		MultiTransactionInputImpl reqInput = reqInputsImpl.get(0);
		String reqAddress = reqInput.getAddress();
		CWVWltAddress addressEntity = getAddress(reqAddress);
		pub = addressEntity.getPublicKey();
		String pri = addressEntity.getPrivateKey();
		oMultiTransactionSignature21 = MultiTransactionSignature.newBuilder();

		byte[] sign = encApi.ecSign(pri, oBody.build().toByteArray());
		log.debug("signature is :--->" + encApi.hexEnc(sign));
		oMultiTransactionSignature21.setSignature(ByteString.copyFrom(sign));

		boolean flag = encApi.ecVerify(pub, oBody.build().toByteArray(), sign);
		log.debug("verify signatrue result:--->" + flag);
		// signatures.add(oMultiTransactionSignature21);

		oBody.addSignatures(oMultiTransactionSignature21);

		oTransaction.setTxBody(oBody);

		ReqCreateMultiTransaction.Builder oCreate = ReqCreateMultiTransaction.newBuilder();

		MultiTransactionImpl.Builder oTransactionImpl = parseToImpl(oTransaction.build());
		oCreate.setTransaction(oTransactionImpl);

		JsonFormat jsonFormat = new JsonFormat();
		String jsonView = jsonFormat.printToString(oCreate.build());
		CWVWltParameterExample parameterExample = new CWVWltParameterExample();
		parameterExample.createCriteria().andParamCodeEqualTo(CREATE_TRANSACTION);
		Object parameterObj = daos.wltParameterDao.selectOneByExample(parameterExample);
		if (parameterObj == null) {
			return ret.setRetCode(-2).setRetMsg("CREATE_TRANSACTION url is null");
		}

		CWVWltParameter parameter = (CWVWltParameter) parameterObj;
		log.debug("send param ----------:" + jsonView);
		String[] nodes = this.getNodeList();
		if(nodes == null) {
			return ret.setRetCode(-2).setRetMsg("CREATE_TRANSACTION url is null");
		}
		
		for(String node : nodes) {
			FramePacket fposttx = PacketHelper.buildUrlFromJson(jsonView, "POST", node + parameter.getParamValue());
			val qryTxRet = sender.send(fposttx, 30000);
			if (qryTxRet.getBody() == null) {
				log.warn("chain return data is null ");
				ret.setRetCode(-2).setRetMsg("chain return data is null");
				continue ;
			}
			JsonNode retNode = null;
			try {
				retNode = mapper.readTree(qryTxRet.getBody());
			} catch (Exception e) {
				log.error("parse create transaction return error : " + e.getMessage());
				ret.setRetCode(-2).setRetMsg("parse create transaction return error");
				continue ;
			}
			ret.setRetCode(retNode.has("retCode") ? retNode.get("retCode").asInt() : -1);
			ret.setRetMsg(retNode.has("retMsg") ? retNode.get("retMsg").asText() : "");

			if (retNode != null && ret.getRetCode() == 1) {
				ret.setTxHash(retNode.has("txHash") ? retNode.get("txHash").asText() : "");
				if (retNode.has("txHash")) {
					insertTxHash(retNode.get("txHash").asText());
				}
			}
			
			break ;
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
		RespGetTxByHash.Builder ret = RespGetTxByHash.newBuilder();
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("hash", txHash);

		String sendJson = JsonSerializer.getInstance().formatToString(param);
		CWVWltParameterExample parameterExample = new CWVWltParameterExample();
		parameterExample.createCriteria().andParamCodeEqualTo(QUERY_TRANSACTION);
		Object parameterObj = daos.wltParameterDao.selectOneByExample(parameterExample);
		if (parameterObj == null) {
			return ret.setRetCode(-2).setRetMsg("QUERY_TRANSACTION url is null ");
		}
			
		CWVWltParameter parameter = (CWVWltParameter) parameterObj;
		String[] nodes = this.getNodeList();
		HashMap<String, Object[]> statusmMap = new HashMap<String, Object[]>() {
			{
				this.put("done", new Object[] { 0, null });
				this.put("none", new Object[] { 0, null });
				this.put("error", new Object[] { 0, null });
				this.put("exception", new Object[] { 0, null });

			}
		};
		// DONE NONE ERROR
		for (String node : nodes) {

			FramePacket fposttx = PacketHelper.buildUrlFromJson(sendJson, "POST", node + parameter.getParamValue());
			val qryTxRet = sender.send(fposttx, 30000);
			if (qryTxRet.getBody() == null) {
				log.warn("chain return data is null ");
				Object[] os = statusmMap.get("exception");
				Integer count = (Integer) os[0];
				os[0] = ++count;
				continue;
			}

			JsonNode retNode = null;
			try {
				retNode = mapper.readTree(qryTxRet.getBody());
			} catch (Exception e) {
				log.error("parse query transaction error : " + e.getMessage());
				Object[] os = statusmMap.get("exception");
				Integer count = (Integer) os[0];
				os[0] = ++count;
				continue;
			}

			if (retNode != null && retNode.has("retCode") && retNode.get("retCode").asInt() == 1) {
				String status = null;
				JsonNode tNode = retNode.has("transaction") ? retNode.get("transaction") : null;
				status = tNode != null && tNode.has("status") ? tNode.get("status").asText() : null;
				if (status == null) {
					Object[] os = statusmMap.get("none");
					Integer count = (Integer) os[0];
					os[0] = ++count;
					if (os[1] == null) {
						os[1] = retNode;
					}
				} else {
					Object[] os = statusmMap.get(status);
					Integer count = (Integer) os[0];
					os[0] = ++count;
					if (os[1] == null) {
						os[1] = retNode;
					}
				}

			}else{
				Object[] os = statusmMap.get("exception");
				Integer count = (Integer) os[0];
				os[0] = ++count;
			}

		}
		// 处理多个节点状态确定最终状态
		int edge = new BigDecimal(nodes.length * 2).divide(new BigDecimal(3)).setScale(0, RoundingMode.CEILING)
				.intValue();
		
		if (((Integer) statusmMap.get("done")[0]) >= edge) {
			return parseJson2RespGetTxByHash((JsonNode) statusmMap.get("done")[1]);
		}else if(((Integer) statusmMap.get("error")[0]) >= edge){
			return parseJson2RespGetTxByHash((JsonNode) statusmMap.get("error")[1]);
		}else if(((Integer) statusmMap.get("none")[0]) >= 0){//其他情况存在未确定记录时
			return parseJson2RespGetTxByHash((JsonNode) statusmMap.get("none")[1]);
		}else{
			return ret.setRetCode(-2).setRetMsg("trans exception status ");
		}

	}

	/**
	 * 获取节点集合
	 * 
	 * @return
	 */
	private String[] getNodeList() {
		CWVWltParameterExample parameterExample = new CWVWltParameterExample();
		parameterExample.createCriteria().andParamCodeEqualTo("chain_node_list");
		Object parameterObj = daos.wltParameterDao.selectOneByExample(parameterExample);
		return parameterObj == null ? null : ((CWVWltParameter) parameterObj).getParamValue().split(",");
	}

	/**
	 * 创建合约
	 * 
	 * @param reqBody
	 * @return
	 */
	public RespCreateContractTransaction.Builder createContract(ReqCreateContractTransaction pb) {
		/**
		 * 拿到 InputImpl 等对象 转成 Input 对象 构建 Transaction 对象 签名 转成TransactionImpl
		 * 对象
		 * 
		 */

		RespCreateContractTransaction.Builder ret = RespCreateContractTransaction.newBuilder();

		MultiTransactionInputImpl reqInputImpl = pb.getInput();
		if (reqInputImpl == null) {
			return ret.setRetCode(-1).setRetMsg("input is null");
		}
		
		String reqAddress = reqInputImpl.getAddress();
		BigDecimal reqAmount = ws.multiply(StringUtils.isEmpty(pb.getInput().getAmount()) ? new BigDecimal(0)
				: new BigDecimal(pb.getInput().getAmount()));
		CWVWltAddress addressEntity = getAddress(reqAddress);

		long currentTime = System.currentTimeMillis();
		MultiTransaction.Builder oMultiTransaction = MultiTransaction.newBuilder();
		MultiTransactionBody.Builder oMultiTransactionBody = MultiTransactionBody.newBuilder();

		for (String delegate : pb.getDelegateList()) {
			oMultiTransactionBody.addDelegate(ByteString.copyFrom(encApi.hexDec(delegate)));
		}

		// oMultiTransactionBody.setExdata(ByteString.copyFromUtf8(pb.getExdata()));

		MultiTransactionInput.Builder oMultiTransactionInput = MultiTransactionInput.newBuilder();
		oMultiTransactionInput.setAddress(ByteString.copyFrom(encApi.hexDec(pb.getInput().getAddress())));
		oMultiTransactionInput.setAmount(ByteString.copyFrom(ByteUtil.bigIntegerToBytes(reqAmount.toBigInteger())));
		// oMultiTransactionInput.setCryptoToken(ByteString.copyFrom(encApi.hexDec(pb.getInput().getCryptoToken())));
		// oMultiTransactionInput.setFee(pb.getInput().getFee());
		oMultiTransactionInput.setNonce(pb.getInput().getNonce());
		// oMultiTransactionInput.setSymbol(pb.getInput().getSymbol());
		// oMultiTransactionInput.setToken(pb.getInput().getToken());
		oMultiTransactionBody.addInputs(oMultiTransactionInput);
		oMultiTransactionBody.setTimestamp(currentTime);
		oMultiTransactionBody.setType(TransTypeEnum.TYPE_CreateContract.value());
		oMultiTransactionBody.setData(ByteString.copyFrom(encApi.hexDec(pb.getData())));
		oMultiTransactionBody.setExdata(ByteString.copyFrom(encApi.hexDec(pb.getExdata())));

		oMultiTransaction.clearTxHash();
		oMultiTransactionBody.clearSignatures();
		oMultiTransactionBody.setTimestamp(System.currentTimeMillis());

		// 签名
		MultiTransactionSignature.Builder oMultiTransactionSignature21 = MultiTransactionSignature.newBuilder();
		oMultiTransactionSignature21.setSignature(ByteString.copyFrom(
				encApi.ecSign(addressEntity.getPrivateKey(), oMultiTransactionBody.build().toByteArray())));
		oMultiTransactionBody.addSignatures(oMultiTransactionSignature21);
		oMultiTransaction.setTxBody(oMultiTransactionBody);

		ReqCreateMultiTransaction.Builder oCreate = ReqCreateMultiTransaction.newBuilder();

		MultiTransactionImpl.Builder oTransactionImpl = parseToImpl(oMultiTransaction.build());
		oCreate.setTransaction(oTransactionImpl);

		// transactionImpl -> createContract

		// reqCreate.setData(pb.getData());
		// reqCreate.setTimestamp(currentTime);
		// reqCreate.setInput(pb.getInput());
		// reqCreate.setExdata(pb.getExdata());
		// reqCreate.setSignature(oMultiTransactionSignatureImpl);
		// if(pb.getDelegateList() != null &&
		// !pb.getDelegateList().isEmpty()){
		// for(String delegate : pb.getDelegateList()){
		// reqCreate.addDelegate(delegate);
		// }
		// }

		JsonPBFormat jsonFormat = new JsonPBFormat();
		String jsonView = jsonFormat.printToString(oCreate.build());
		CWVWltParameterExample parameterExample = new CWVWltParameterExample();
		parameterExample.createCriteria().andParamCodeEqualTo(CREATE_TRANSACTION);
		Object parameterObj = daos.wltParameterDao.selectOneByExample(parameterExample);
		if (parameterObj == null) {
			log.warn("input or data is null");
			return ret.setRetCode(-1).setRetMsg("create contract error: input or data is null");
		}
		
		CWVWltParameter parameter = (CWVWltParameter) parameterObj;
		
		String[] nodes = this.getNodeList();
		for(String node : nodes) {
			
			FramePacket fposttx = PacketHelper.buildUrlFromJson(jsonView, "POST", node + parameter.getParamValue());
			val crtTxRet = sender.send(fposttx, 30000);
			if (crtTxRet.getBody() == null) {
				log.warn("chain return data is null ");
				ret.setRetCode(-1).setRetMsg("chain return data is null");
				continue ;
			}

			JsonNode retNode = null;
			try {
				retNode = mapper.readTree(crtTxRet.getBody());
			} catch (IOException e) {
				log.error("parse ret error : " + new String(crtTxRet.getBody()));
				ret.setRetCode(-1).setRetMsg("parse ret error : " + new String(crtTxRet.getBody()));
				continue ;
			}
			ret.setRetCode(retNode.has("retCode") ? retNode.get("retCode").asInt() : -1);
			ret.setRetMsg(retNode.has("retMsg") ? retNode.get("retMsg").asText() : "");

			if (retNode != null && ret.getRetCode() == 1) {
				ret.setContractAddress(retNode.has("contractHash") ? retNode.get("contractHash").asText() : "");
				ret.setTxHash(retNode.has("txHash") ? retNode.get("txHash").asText() : "");
				if (retNode.has("contractHash")) {
					insertContract(retNode.get("contractHash").asText(), retNode.get("txHash").asText());
				}
			}
			
			break ;
		}

		return ret;
	}

	/**
	 * @param reqBody
	 * @return
	 */
	public RespCreateTransaction.Builder doContract(ReqDoContractTransaction pb) {
		// 执行合约走的是创建交易的流程，所以结构需要与创建交易保持一致，参与合约的地址为 input，合约地址为 output
		MultiTransactionImpl.Builder trans = pb.getTransaction().toBuilder();
		trans.setTxBody(trans.getTxBody().toBuilder().setType(TransTypeEnum.TYPE_CallContract.value()));

		RespCreateTransaction.Builder ret = createTransaction(trans.build());

		return ret;
	}

	/*********************************************************************************************/

	/**
	 * @param retNode
	 * @return
	 */
	public RespGetTxByHash.Builder parseJson2RespGetTxByHash(JsonNode retNode) {
		RespGetTxByHash.Builder ret = RespGetTxByHash.newBuilder();
		MultiTransactionImpl.Builder transaction = MultiTransactionImpl.newBuilder();
		if (retNode.has("transaction")) {
			JsonNode tNode = retNode.get("transaction");
			transaction.setTxBody(getTxBodyFromTransaction(tNode.get("txBody")));
			if (tNode.has("node"))
				transaction.setNode(getMultiTransactionNode(retNode.get("node")));
			transaction.setStatus(tNode.has("status") ? tNode.get("status").asText() : "");
			transaction.setTxHash(tNode.has("txHash") ? tNode.get("txHash").asText() : "");
			transaction.setResult(tNode.has("result") ? tNode.get("result").asText() : "");
			ret.setTransaction(transaction);
		}
		ret.setRetCode(retNode.get("retCode").asInt());

		return ret;
	}

	/**
	 * @param node
	 *            = ReqCreateContractTransaction
	 * @return
	 */
	public MultiTransactionBodyImpl getTxBodyFromCreateContract(JsonNode node) {
		MultiTransactionBodyImpl.Builder body = MultiTransactionBodyImpl.newBuilder();

		body.setData(node.has("data") ? node.get("data").asText() : "");
		body.setExdata(node.has("exdata") ? node.get("exdata").asText() : "");

		if (node.has("deledate")) {
			ArrayNode array = (ArrayNode) node.get("delegate");
			for (int i = 0; i < array.size(); i++) {
				body.addDelegate(array.get(i).asText());
			}
		}

		if (node.has("input")) {
			JsonNode input = node.get("input");
			body.addInputs(getInput(input));
		}

		if (node.has("output")) {
			JsonNode output = node.get("output");
			body.addOutputs(getOutput(output));
		}

		body.setTimestamp(node.has("timestamp") ? node.get("timestamp").asLong() : 0l);

		return body.build();
	}

	/**
	 * 签名前，body中的内容需要补充完整
	 * 
	 * @param node
	 *            = txBody
	 * @return
	 */
	public MultiTransactionBodyImpl getTxBodyFromTransaction(JsonNode node) {
		MultiTransactionBodyImpl.Builder body = MultiTransactionBodyImpl.newBuilder();

		body.setData(node.has("data") ? node.get("data").asText() : "");
		body.setExdata(node.has("exdata") ? node.get("exdata").asText() : "");

		if (node.has("deledate")) {
			ArrayNode array = (ArrayNode) node.get("delegate");
			for (int i = 0; i < array.size(); i++) {
				body.addDelegate(array.get(i).asText());
			}
		}

		if (node.has("inputs")) {
			ArrayNode inputs = (ArrayNode) node.get("inputs");
			for (JsonNode input : inputs) {
				body.addInputs(getInput(input));
			}
		}

		if (node.has("outputs")) {
			ArrayNode outputs = (ArrayNode) node.get("outputs");
			for (JsonNode output : outputs) {
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
	public MultiTransactionInputImpl.Builder getInput(JsonNode input) {
		MultiTransactionInputImpl.Builder inputB = MultiTransactionInputImpl.newBuilder();
		inputB.setAddress(input.has("address") ? input.get("address").asText() : "");
		inputB.setAmount(input.has("amount") ? input.get("amount").asText() : "0");
		inputB.setCryptoToken(input.has("cryptoToken") ? input.get("cryptoToken").asText() : "");
		inputB.setFee(input.has("fee") ? input.get("fee").asInt() : 0);
		inputB.setFeeLimit(input.has("feeLimit") ? input.get("feeLimit").asInt() : 0);
		inputB.setNonce(input.has("nonce") ? input.get("nonce").asInt() : 0);
		inputB.setSymbol(input.has("symbol") ? input.get("symbol").asText() : "");
		inputB.setToken(input.has("token") ? input.get("token").asText() : "");

		return inputB;
	}

	/**
	 * @param output
	 * @return
	 */
	public MultiTransactionOutputImpl.Builder getOutput(JsonNode output) {
		MultiTransactionOutputImpl.Builder outputB = MultiTransactionOutputImpl.newBuilder();
		outputB.setAddress(output.has("address") ? output.get("address").asText() : "");
		outputB.setAmount(output.has("amount") ? output.get("amount").asText() : "0");
		outputB.setCryptoToken(output.has("cryptoToken") ? output.get("cryptoToken").asText() : "");
		outputB.setSymbol(output.has("symbol") ? output.get("symbol").asText() : "");
		return outputB;
	}

	/**
	 * @param retNode
	 * @return
	 */
	public MultiTransactionNodeImpl.Builder getMultiTransactionNode(JsonNode retNode) {
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
	public CWVWltAddress getAddress(String address) {
		CWVWltAddressExample example = new CWVWltAddressExample();
		example.createCriteria().andAddressEqualTo(address);
		Object obj = daos.wltAddressDao.selectOneByExample(example);

		CWVWltAddress addr = null;
		if (obj != null) {
			addr = (CWVWltAddress) obj;
		}
		if (addr == null || StringUtils.isBlank(addr.getPublicKey()) || StringUtils.isBlank(addr.getPrivateKey())) {
			throw new NullPointerException("address's publicKey or privateKey is null");
		}
		return addr;
	}

	/**
	 * @param txHash
	 */
	public void insertTxHash(String txHash) {
		CWVWltTx tx = new CWVWltTx();
		tx.setTxId(UUIDGenerator.generate());
		tx.setTxHash(txHash);
		tx.setCreatedTime(new Date());
		tx.setUpdatedTime(tx.getCreatedTime());
		try {
			daos.wltTxDao.insert(tx);
		} catch (Exception e) {
			log.error("save tx to db error");
		}
	}

	/**
	 * @param contractAddress
	 * @param contractTxHash
	 * @param contractType
	 */
	public void insertContract(String contractAddress, String contractTxHash) {
		CWVWltContract contract = new CWVWltContract();
		contract.setContractAddress(contractAddress);
		contract.setContractTxHash(contractTxHash);
		contract.setContractId(UUIDGenerator.generate());
		contract.setCreatedTime(new Date());
		contract.setUpdatedTime(contract.getCreatedTime());
		try {
			daos.wltContractDao.insert(contract);
		} catch (Exception e) {
			log.error("save contract to db error");
		}
	}

	/***********************************
	 * account 对象转换
	 **********************************************************/

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

		oMultiTransactionImpl
				.setStatus(StringUtils.isNotBlank(oTransaction.getStatus()) ? oTransaction.getStatus() : "");

		MultiTransactionBodyImpl.Builder oMultiTransactionBodyImpl = MultiTransactionBodyImpl.newBuilder();
		oMultiTransactionBodyImpl.setData(encApi.hexEnc(oMultiTransactionBody.getData().toByteArray()));
		oMultiTransactionBodyImpl.setType(oMultiTransactionBody.getType());
		for (ByteString delegate : oMultiTransactionBody.getDelegateList()) {
			oMultiTransactionBodyImpl.addDelegate(encApi.hexEnc(delegate.toByteArray()));
		}

		oMultiTransactionBodyImpl.setExdata(oMultiTransactionBody.getExdata().toStringUtf8());

		for (MultiTransactionInput input : oMultiTransactionBody.getInputsList()) {
			MultiTransactionInputImpl.Builder oMultiTransactionInputImpl = MultiTransactionInputImpl.newBuilder();
			oMultiTransactionInputImpl.setAddress(encApi.hexEnc(input.getAddress().toByteArray()));
			oMultiTransactionInputImpl
					.setAmount(ByteUtil.bytesToBigInteger(input.getAmount().toByteArray()).toString());
			oMultiTransactionInputImpl.setCryptoToken(encApi.hexEnc(input.getCryptoToken().toByteArray()));
			oMultiTransactionInputImpl.setFee(input.getFee());
			oMultiTransactionInputImpl.setNonce(input.getNonce());
			oMultiTransactionInputImpl.setSymbol(input.getSymbol());
			oMultiTransactionInputImpl.setToken(input.getToken());
			oMultiTransactionBodyImpl.addInputs(oMultiTransactionInputImpl);
		}
		for (MultiTransactionOutput output : oMultiTransactionBody.getOutputsList()) {
			MultiTransactionOutputImpl.Builder oMultiTransactionOutputImpl = MultiTransactionOutputImpl.newBuilder();
			oMultiTransactionOutputImpl.setAddress(encApi.hexEnc(output.getAddress().toByteArray()));
			oMultiTransactionOutputImpl
					.setAmount(ByteUtil.bytesToBigInteger(output.getAmount().toByteArray()).toString());
			oMultiTransactionOutputImpl.setCryptoToken(encApi.hexEnc(output.getCryptoToken().toByteArray()));
			oMultiTransactionOutputImpl.setSymbol(output.getSymbol());
			oMultiTransactionBodyImpl.addOutputs(oMultiTransactionOutputImpl);
		}
		// oMultiTransactionBodyImpl.setSignatures(index, value)
		for (MultiTransactionSignature signature : oMultiTransactionBody.getSignaturesList()) {
			MultiTransactionSignatureImpl.Builder oMultiTransactionSignatureImpl = MultiTransactionSignatureImpl
					.newBuilder();
			oMultiTransactionSignatureImpl.setSignature(encApi.hexEnc(signature.getSignature().toByteArray()));
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

		oMultiTransactionBody.setData(ByteString.copyFromUtf8(oMultiTransactionBodyImpl.getData()));

		for (String delegate : oMultiTransactionBodyImpl.getDelegateList()) {
			oMultiTransactionBody.addDelegate(ByteString.copyFrom(encApi.hexDec(delegate)));
		}
		oMultiTransactionBody.setExdata(ByteString.copyFromUtf8(oMultiTransactionBodyImpl.getExdata()));

		for (MultiTransactionInputImpl input : oMultiTransactionBodyImpl.getInputsList()) {
			MultiTransactionInput.Builder oMultiTransactionInput = MultiTransactionInput.newBuilder();
			oMultiTransactionInput.setAddress(ByteString.copyFrom(encApi.hexDec(input.getAddress())));
			oMultiTransactionInput.setAmount(ByteString.copyFrom(encApi.hexDec(input.getAmount())));
			oMultiTransactionInput.setCryptoToken(ByteString.copyFrom(encApi.hexDec(input.getCryptoToken())));
			oMultiTransactionInput.setFee(input.getFee());
			oMultiTransactionInput.setNonce(input.getNonce());
			oMultiTransactionInput.setSymbol(input.getSymbol());
			oMultiTransactionInput.setToken(input.getToken());
			oMultiTransactionBody.addInputs(oMultiTransactionInput);
		}
		for (MultiTransactionOutputImpl output : oMultiTransactionBodyImpl.getOutputsList()) {
			MultiTransactionOutput.Builder oMultiTransactionOutput = MultiTransactionOutput.newBuilder();
			oMultiTransactionOutput.setAddress(ByteString.copyFrom(encApi.hexDec(output.getAddress())));
			oMultiTransactionOutput.setAmount(ByteString.copyFrom(encApi.hexDec(output.getAmount())));
			oMultiTransactionOutput.setCryptoToken(ByteString.copyFrom(encApi.hexDec(output.getCryptoToken())));
			oMultiTransactionOutput.setSymbol(output.getSymbol());
			oMultiTransactionBody.addOutputs(oMultiTransactionOutput);
		}
		// oMultiTransactionBodyImpl.setSignatures(index, value)
		for (MultiTransactionSignatureImpl signature : oMultiTransactionBodyImpl.getSignaturesList()) {
			MultiTransactionSignature.Builder oMultiTransactionSignature = MultiTransactionSignature.newBuilder();
			oMultiTransactionSignature.setSignature(ByteString.copyFrom(encApi.hexDec(signature.getSignature())));
			oMultiTransactionBody.addSignatures(oMultiTransactionSignature);
		}
		oMultiTransactionBody.setTimestamp(oMultiTransactionBodyImpl.getTimestamp());
		oMultiTransaction.setTxBody(oMultiTransactionBody);
		return oMultiTransaction;
	}

	public Builder queryLastBlock() {
		RespBlockDetail.Builder ret = RespBlockDetail.newBuilder();
		CWVWltParameterExample parameterExample = new CWVWltParameterExample();
		parameterExample.createCriteria().andParamCodeEqualTo(QUERY_LASTBLOCK);
		Object parameterObj = daos.wltParameterDao.selectOneByExample(parameterExample);
		if (parameterObj != null) {
			CWVWltParameter parameter = (CWVWltParameter) parameterObj;
			FramePacket fposttx = PacketHelper.buildUrlForGet(parameter.getParamValue());
			val qryTxRet = sender.send(fposttx, 30000);
			if (qryTxRet.getBody() == null) {
				return ret.setRetCode(-2);
			}
			JsonNode retNode = null;
			try {
				retNode = mapper.readTree(qryTxRet.getBody());
			} catch (Exception e) {
				log.error("parse create transaction return error : " + e.getMessage());
				return ret.setRetCode(-2).setRetMsg("chain return data is null");
			}
			ret.setRetCode(retNode.has("retCode") ? retNode.get("retCode").asInt() : -1);
			ret.setRetMsg(retNode.has("retMsg") ? retNode.get("retMsg").asText() : "");

			if (retNode != null && retNode.get("retCode").asInt() == 1) {
				ret = parseJson2RespBlockDetail(retNode);
			}
		}
		return ret;

	}

	/**
	 * 
	 * @param retNode
	 * @return
	 */
	private Builder parseJson2RespBlockDetail(JsonNode retNode) {
		RespBlockDetail.Builder ret = RespBlockDetail.newBuilder();
		ret.setParentHash(retNode.has("parentHash") ? retNode.get("parentHash").asText() : "");
		ret.setTimestamp(retNode.has("timestamp") ? retNode.get("timestamp").asLong() : 0);
		ret.setBlockHash(retNode.has("blockHash") ? retNode.get("blockHash").asText() : "");
		ret.setSliceId(retNode.has("sliceId") ? retNode.get("sliceId").asInt() : 0);
		ret.setStateRoot(retNode.has("stateRoot") ? retNode.get("stateRoot").asText() : "");
		ret.setNumber(retNode.has("number") ? retNode.get("number").asLong() : 0);
		if (retNode.has("miner")) {
			BlockMinerImpl.Builder miner = BlockMinerImpl.newBuilder();
			JsonNode minerNode = retNode.get("miner");
			miner.setNode(minerNode.has("node") ? minerNode.get("node").asText() : "")
					.setReward(minerNode.has("reward") ? minerNode.get("reward").asText() : "")
					.setAddress(minerNode.has("address") ? minerNode.get("address").asText() : "")
					.setBcuid(minerNode.has("bcuid") ? minerNode.get("bcuid").asText() : "");
			ret.setMiner(miner);
		}
		// {
		// "retCode": 1,
		// "parentHash":
		// "7b197e65bdc0e35a2ec3b9b7161bee30fd8929da2b8c098c27e7f8527014f4fb",
		// "timestamp": 1534761971933,
		// "number": 168448,
		// "miner": {
		// "node": "F91n6Dm1ynhN3HcAg5Srz3mdct0",
		// "reward": "3170979198376459000",
		// "address": "33cc7aba10aed1c8b8224f9abe5ff10cfbd493b3",
		// "bcuid": "DF91n6Dm1ynhN3HcAg5Srz3mdct0"
		// },
		// "blockHash":
		// "7fde1c20a6a887becaab38ba09be3944ee95cfcc5940c999c51ea8df418889b2",
		// "sliceId": 0,
		// "stateRoot":
		// "4706de436b83ec823a59b087ca77eb4cb08c9e8942060e9b76346efc6198bc99"
		// }
		return ret.setRetCode(retNode.get("retCode").asInt());
	}
}
