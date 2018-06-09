package org.brewchain.cwv.wlt.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltParameter;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltParameterExample;
import org.brewchain.cwv.wlt.helper.AddressHelper;
import org.brewchain.cwv.wlt.helper.TransactionHelper;
import org.brewchain.cwv.wlt.utils.DESedeCoder;
import org.brewchain.wallet.service.Wallet.BaseData;
import org.brewchain.wallet.service.Wallet.PWLTCommand;
import org.brewchain.wallet.service.Wallet.PWLTModule;
import org.brewchain.wallet.service.Wallet.ReqNewAddress;
import org.brewchain.wallet.service.Wallet.RetNewAddress;
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
public class CreateAddress extends SessionModules<BaseData> {

	@ActorRequire(name = "addrHelper", scope = "global")
	AddressHelper addressHelper;
	
	@ActorRequire(name = "txHelper", scope = "global")
	TransactionHelper txHelper;
	
	@ActorRequire(name = "daos", scope = "global")
	Daos daos;

	@Override
	public String[] getCmds() {
		return new String[] { PWLTCommand.NAD.name() };
	}

	@Override
	public String getModule() {
		return PWLTModule.WLT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final BaseData pb, final CompleteHandler handler) {
		RetNewAddress.Builder ret = RetNewAddress.newBuilder();
		String address = "";
		String seed = "";
		if(pb != null && StringUtils.isNoneBlank(pb.getData(), pb.getBusi())){
			String data = pb.getData();
			CWVWltParameterExample example = new CWVWltParameterExample();
			example.createCriteria().andParamCodeEqualTo(pb.getBusi());
			Object parameterObj = daos.wltParameterDao.selectOneByExample(example);
			if(parameterObj != null){
				CWVWltParameter parameter = (CWVWltParameter)parameterObj;
				String decryptData = null;
				ReqNewAddress.Builder req = ReqNewAddress.newBuilder();
				try {
					decryptData = DESedeCoder.decrypt(data, parameter.getParamValue());
					if(decryptData != null){
						JsonNode node = new ObjectMapper().readTree(decryptData);
						InputStream inputStream = new ByteArrayInputStream(node.toString().getBytes()); 
						new JsonPBFormat().merge(inputStream, req);
						
						if(StringUtils.isNotBlank(req.getSeed())){
							seed = req.getSeed();
						}
						address = addressHelper.registerAddress(seed);
						if(StringUtils.isNotBlank(address)){
							ret.setRetCode(1).setMsg("success").setAddress(address);
						}else{
							ret.setRetCode(-1).setMsg("create address error");
						}
					}else {
						ret.setRetCode(-1).setMsg(" no data");
					}
				} catch (Exception e) {
					log.error("create address error " + e.getMessage());
					ret.setRetCode(-1).setMsg(" create address error");
				}
			}else {
				ret.setRetCode(-1).setMsg("no crypto key");
			}
		}else{
			ret.setRetCode(-1).setMsg("no params");
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
