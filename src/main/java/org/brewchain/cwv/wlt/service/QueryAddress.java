package org.brewchain.cwv.wlt.service;


import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltParameter;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltParameterExample;
import org.brewchain.cwv.wlt.helper.AddressHelper;
import org.brewchain.cwv.wlt.utils.DESedeCoder;
import org.brewchain.wallet.service.Wallet.BaseData;
import org.brewchain.wallet.service.Wallet.PWLTCommand;
import org.brewchain.wallet.service.Wallet.PWLTModule;
import org.brewchain.wallet.service.Wallet.ReqGetAccount;
import org.brewchain.wallet.service.Wallet.RespGetAccount;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class QueryAddress extends SessionModules<BaseData>{

	@ActorRequire(name = "addrHelper", scope = "global")
	AddressHelper addressHelper;
	
	@ActorRequire(name = "daos", scope = "global")
	Daos daos;
	
	@Override
	public String[] getCmds() {
		return new String[] { PWLTCommand.QAD.name() };
	}

	@Override
	public String getModule() {
		return PWLTModule.WLT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final BaseData pb, final CompleteHandler handler) {
		RespGetAccount.Builder ret = null;
		if(pb != null && StringUtils.isNoneBlank(pb.getData(), pb.getBusi())){
			String data = pb.getData();
			CWVWltParameterExample example = new CWVWltParameterExample();
			example.createCriteria().andParamCodeEqualTo(pb.getBusi());
			Object parameterObj = daos.wltParameterDao.selectOneByExample(example);
			if(parameterObj != null){
				CWVWltParameter parameter = (CWVWltParameter)parameterObj;
				String decryptData = null;
				try {
					decryptData = DESedeCoder.decrypt(data, parameter.getParamValue());
					if(decryptData != null){
						JsonNode node = new ObjectMapper().readTree(decryptData);
						String address = node.get("address").asText();
						int s = node.get("s").asInt();
						int p = node.get("p").asInt();
						if(StringUtils.isNotBlank(address)){
							ret = addressHelper.queryAddressInfo(address,s,p);
							
							if(ret == null){
								ret = RespGetAccount.newBuilder();
								ret.setRetCode(-1);
							}
						}else {
							log.warn("no address ");
							ret = RespGetAccount.newBuilder();
							ret.setRetCode(-1);
						}
					}else {
						ret = RespGetAccount.newBuilder();
						ret.setRetCode(-1);
					}
				} catch (Exception e) {
					log.error("query address error : " + e.getMessage());
					ret = RespGetAccount.newBuilder();
					ret.setRetCode(-1);
				}
			}else {
				ret = RespGetAccount.newBuilder();
				ret.setRetCode(-1);
			}
		}else {
			ret = RespGetAccount.newBuilder();
			ret.setRetCode(-1);
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
