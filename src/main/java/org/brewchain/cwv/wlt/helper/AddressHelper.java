package org.brewchain.cwv.wlt.helper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddress;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltParameter;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltParameterExample;
import org.brewchain.wallet.service.Wallet.AccountCryptoTokenImpl;
import org.brewchain.wallet.service.Wallet.AccountCryptoValueImpl;
import org.brewchain.wallet.service.Wallet.AccountTokenValueImpl;
import org.brewchain.wallet.service.Wallet.AccountValueImpl;
import org.brewchain.wallet.service.Wallet.RespGetAccount;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

import lombok.Data;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;
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
@Instantiate(name = "addrHelper")
@Slf4j
@Data
public class AddressHelper implements ActorService {

	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@ActorRequire(name = "http", scope = "global")
	IPacketSender sender;

	@ActorRequire(name = "daos", scope = "global")
	Daos daos;
	
	BigDecimal ws = new BigDecimal("1000000000000000000");

	private final static String QUERY_ADDRESS = "queryAddressURL";

	private static PropHelper props = new PropHelper(null);
//	private static String QUERY_ADDRESS = "http://127.0.0.1:8000/fbs/act/pbgac.do";
	static {
//		QUERY_ADDRESS = props.get("query_address", "http://127.0.0.1:8000/fbs/act/pbgac.do");
	}

	/**
	 * 生成地址并入库
	 * 
	 * @param type
	 * @param seed
	 * @return
	 */
	public String registerAddress(String seed) {
		KeyPairs key = encApi.genKeys();
		if (StringUtils.isNotBlank(seed)) {
			key = encApi.genKeys(seed);
		}

		//写库操作
		Date now = new Date();
		CWVWltAddress addressEntity = new CWVWltAddress();
		addressEntity.setAddressId(UUIDGenerator.generate());
		addressEntity.setAddress(key.getAddress());
		addressEntity.setBalance(0l);
		addressEntity.setBcuid(key.getBcuid());
		addressEntity.setCreatedTime(now);
		addressEntity.setNonce(0);
		addressEntity.setPrivateKey(key.getPrikey());
		addressEntity.setPublicKey(key.getPubkey());
		addressEntity.setReserved1("");
		addressEntity.setReserved2("");
		addressEntity.setSeed(StringUtils.isNotBlank(seed) ? seed : "");
		addressEntity.setType("");
		addressEntity.setUpdatedTime(now);

		daos.wltAddressDao.insert(addressEntity);

		return key.getAddress();
	}

	/**
	 * query address
	 * 
	 * @param address
	 * @param p 
	 * @param s 
	 * @return
	 */
	public RespGetAccount.Builder queryAddressInfo(String address, int s, int p) {
		RespGetAccount.Builder account = null;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", address);
		params.put("s", s);
		params.put("p", p);
		
		String sendJson = JsonSerializer.formatToString(params);

		CWVWltParameterExample parameterExample = new CWVWltParameterExample();
		parameterExample.createCriteria().andParamCodeEqualTo(QUERY_ADDRESS);
		Object parameterObj = daos.wltParameterDao.selectOneByExample(parameterExample);
		if(parameterObj == null){
			return null;
		}
		
		CWVWltParameter parameter = (CWVWltParameter)parameterObj;
		
		String[] nodes = this.getNodeList();
		for(String node : nodes) {
			FramePacket fposttx = PacketHelper.buildUrlFromJson(sendJson, "POST", node + parameter.getParamValue());
			val txretReg = sender.send(fposttx, 30000);
			
			if(txretReg.getBody() == null) {
				log.warn("chain return data is null " );
				continue ;
			}
			JsonNode retNode = null;
			ObjectMapper mapper = new ObjectMapper();
			try{
				retNode = mapper.readTree(txretReg.getBody());
			} catch (Exception e){
				log.error("parse query address error : " + e.getMessage());
				continue ;
			}
			
			if(retNode != null && retNode.has("retCode") && retNode.get("retCode").asInt() == 1){
				account = parseJson2AccountValueImpl(retNode);
			}
			break ;
		}
		
		return account;
	}
	
	/**
	 * @param retNode
	 * @return
	 */
	public RespGetAccount.Builder parseJson2AccountValueImpl(JsonNode retNode){
		RespGetAccount.Builder ret = RespGetAccount.newBuilder();
		ret.setRetCode(retNode.get("retCode").asInt());
		if(retNode.has("address")){
			ret.setAddress(retNode.get("address").asText());
		}
		
		if(retNode.has("account")){
			JsonNode accountNode = retNode.get("account");
			AccountValueImpl.Builder account = AccountValueImpl.newBuilder();
			account.setNonce(accountNode.has("nonce") ? accountNode.get("nonce").asInt() : 0);
			account.setBalance(accountNode.has("balance") ? new BigDecimal(accountNode.get("balance").asText()).divide(ws, 18,BigDecimal.ROUND_HALF_UP).toString() : "0");
			account.setPubKey(accountNode.has("pubKey") ? accountNode.get("pubKey").asText() : "");
			account.setMax(accountNode.has("max") ? accountNode.get("max").asLong() : 0L);
			account.setAcceptMax(accountNode.has("acceptMax") ? accountNode.get("acceptMax").asLong() : 0L);
			account.setAcceptLimit(accountNode.has("acceptLimit") ? accountNode.get("acceptLimit").asInt() : 0);
			account.setCode(accountNode.has("code") ? accountNode.get("code").asText() : "");
			account.setStorage(accountNode.has("storage") ? accountNode.get("storage").asText() : "");
			if(accountNode.has("address")){
				ArrayNode addrs = (ArrayNode) accountNode.get("address");
				if(addrs != null && addrs.size() > 0){
					for (JsonNode addrObj : addrs){
						account.addAddress(addrObj.asText());
					}
				}
			}
			
			if(accountNode.has("tokens")){
				ArrayNode tokens = (ArrayNode) accountNode.get("tokens");
				if(tokens != null && tokens.size() > 0){
					for(JsonNode token : tokens){
						AccountTokenValueImpl.Builder tokenValue = AccountTokenValueImpl.newBuilder();
						tokenValue.setBalance(token.has("balance") ? new BigDecimal(token.get("balance").asText()).divide(ws, 18,BigDecimal.ROUND_HALF_UP).toString() : "0");
						tokenValue.setToken(token.has("token") ? token.get("token").asText() : "");
						
						account.addTokens(tokenValue);
					}
				}
			}
			
			if(accountNode.has("cryptos")){
				ArrayNode cryptos = (ArrayNode) accountNode.get("cryptos");
				if(cryptos != null && cryptos.size() > 0){
					for(JsonNode crypto : cryptos){
						AccountCryptoValueImpl.Builder cryptoValue = AccountCryptoValueImpl.newBuilder();
						cryptoValue.setSymbol(crypto.has("symbol") ? crypto.get("symbol").asText() : "");
						if(crypto.has("tokens")){
							ArrayNode tokens = (ArrayNode) crypto.get("tokens");
							for(JsonNode token : tokens){
								AccountCryptoTokenImpl.Builder cryptoToken = AccountCryptoTokenImpl.newBuilder();
								cryptoToken.setHash(token.has("hash") ? token.get("hash").asText() : "");
								cryptoToken.setTimestamp(token.has("timestamp") ? token.get("timestamp").asLong() : 0L);
								cryptoToken.setIndex(token.has("index") ? token.get("index").asInt() : 0);
								cryptoToken.setTotal(token.has("total") ? token.get("total").asInt() : 0);
								cryptoToken.setCode(token.has("code") ? token.get("code").asText() : "");
								cryptoToken.setName(token.has("name") ? token.get("name").asText() : "");
								cryptoToken.setNonce(token.has("nonce") ? token.get("nonce").asInt() : 0);
								cryptoToken.setOwnertime(token.has("ownertime") ? token.get("ownertime").asLong() : 0L);
								
								cryptoValue.addTokens(cryptoToken);
							}
						}
						
						account.addCryptos(cryptoValue);
					}
				}
			}
			
			ret.setAccount(account);
		}
		
		return ret;
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
}
