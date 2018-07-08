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
import org.brewchain.wallet.service.Wallet.ReqCreateMultiTransaction;
import org.brewchain.wallet.service.Wallet.RespCreateTransaction;
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
public class CreateTransaction extends SessionModules<BaseData>{

	@ActorRequire(name = "txHelper", scope = "global")
	TransactionHelper txHelper;
	
	@ActorRequire(name = "daos", scope = "global")
	Daos daos;
	
	@Override
	public String[] getCmds() {
		return new String[] { PWLTCommand.NTS.name() };
	}

	@Override
	public String getModule() {
		return PWLTModule.WLT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final BaseData pb, final CompleteHandler handler) {
		RespCreateTransaction.Builder ret = null;
		if(pb != null && StringUtils.isNoneBlank(pb.getData(), pb.getBusi())){
			ReqCreateMultiTransaction.Builder req = ReqCreateMultiTransaction.newBuilder();
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
						log.debug(new JsonPBFormat().printToString(req.getTransaction()));
						ret = txHelper.createTransaction(req.getTransaction());
					}else {
						ret = RespCreateTransaction.newBuilder();
						ret.setRetCode(-1).setRetMsg("no crypto data");
					}
				} catch (Exception e){
					e.printStackTrace();
					log.debug("create transaction exception,message:"+e.getMessage());
					ret = RespCreateTransaction.newBuilder();
					ret.setRetCode(-1).setRetMsg(e.getMessage());
				}
			} else {
				log.debug("get system param["+pb.getBusi()+"] error");
				ret = RespCreateTransaction.newBuilder();
				ret.setRetCode(-1).setRetMsg("create transaction error");
			}
		} else {
			ret = RespCreateTransaction.newBuilder();
			ret.setRetCode(-1).setRetMsg("request has illegal argument");
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
