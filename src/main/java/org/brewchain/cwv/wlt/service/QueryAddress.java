package org.brewchain.cwv.wlt.service;


import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.helper.AddressHelper;
import org.brewchain.wallet.service.Wallet.PWLTCommand;
import org.brewchain.wallet.service.Wallet.PWLTModule;
import org.brewchain.wallet.service.Wallet.ReqGetAccount;
import org.brewchain.wallet.service.Wallet.RespGetAccount;

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
public class QueryAddress extends SessionModules<ReqGetAccount>{

	@ActorRequire(name = "addrHelper", scope = "global")
	AddressHelper addressHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PWLTCommand.QAD.name() };
	}

	@Override
	public String getModule() {
		return PWLTModule.WLT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetAccount pb, final CompleteHandler handler) {
		RespGetAccount.Builder ret = null;
		if(pb != null && StringUtils.isNotBlank(pb.getAddress())){
			try{
				ret = addressHelper.queryAddressInfo(pb.getAddress());
			} catch (Exception e){
				log.error("query address error : " + e.getMessage());
			}
			if(ret == null){
				ret = RespGetAccount.newBuilder();
				ret.setRetCode(-1);
			}
		}else{
			log.warn("no address ");
			ret = RespGetAccount.newBuilder();
			ret.setRetCode(-1);
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
