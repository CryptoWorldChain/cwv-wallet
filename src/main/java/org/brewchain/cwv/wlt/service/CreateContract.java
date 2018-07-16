package org.brewchain.cwv.wlt.service;


import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltParameter;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltParameterExample;
import org.brewchain.cwv.wlt.helper.TransactionHelper;
import org.brewchain.cwv.wlt.utils.DESedeCoder;
import org.brewchain.wallet.service.Wallet.BaseData;
import org.brewchain.wallet.service.Wallet.PWLTCommand;
import org.brewchain.wallet.service.Wallet.PWLTModule;
import org.brewchain.wallet.service.Wallet.ReqCreateContractTransaction;
import org.brewchain.wallet.service.Wallet.RespCreateContractTransaction;
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
import onight.tfw.outils.bean.JsonPBFormat;

@NActorProvider
@Slf4j
@Data
public class CreateContract extends SessionModules<BaseData>{

	@ActorRequire(name = "txHelper", scope = "global")
	TransactionHelper txHelper;
	
	@ActorRequire(name = "daos", scope = "global")
	Daos daos;
	
	@Override
	public String[] getCmds() {
		return new String[] { PWLTCommand.NCR.name() };
	}

	@Override
	public String getModule() {
		return PWLTModule.WLT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final BaseData pb, final CompleteHandler handler) {
		RespCreateContractTransaction.Builder ret = null;
		ReqCreateContractTransaction.Builder req = ReqCreateContractTransaction.newBuilder();
		if(pb != null && StringUtils.isNoneBlank(pb.getBusi(), pb.getData())){
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
						InputStream inputStream = new ByteArrayInputStream(node.toString().getBytes()); 
						new JsonPBFormat().merge(inputStream, req);
						ret = txHelper.createContract(req.build());
					}else {
						ret = RespCreateContractTransaction.newBuilder();
						ret.setRetCode(-1).setRetMsg("create contract error, no decryptodata");
					}
				} catch (Exception e) {
					ret = RespCreateContractTransaction.newBuilder();
					ret.setRetCode(-1).setRetMsg("create contract error : " + e.getMessage());
				}
			}else {
				ret = RespCreateContractTransaction.newBuilder();
				ret.setRetCode(-1).setRetMsg("create contract error, no key");
			}
		}else{
			log.warn("no contract ,create contract error");
			ret = RespCreateContractTransaction.newBuilder();
			ret.setRetCode(-1);
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
