package org.brewchain.cwv.wlt.service;


import org.brewchain.cwv.wlt.helper.TransactionHelper;
import org.brewchain.wallet.service.Wallet.PWLTCommand;
import org.brewchain.wallet.service.Wallet.PWLTModule;
import org.brewchain.wallet.service.Wallet.ReqDoContractTransaction;
import org.brewchain.wallet.service.Wallet.RespCreateTransaction;

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
public class DoContract extends SessionModules<ReqDoContractTransaction>{

	@ActorRequire(name = "txHelper", scope = "global")
	TransactionHelper txHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PWLTCommand.DCR.name() };
	}

	@Override
	public String getModule() {
		return PWLTModule.WLT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqDoContractTransaction pb, final CompleteHandler handler) {
		RespCreateTransaction.Builder ret = null;
		if(pb != null){
			try{
				ret = txHelper.doContract(pb);
			} catch (Exception e){
				log.error("do contract error + " + e.getMessage());
			}
			if(ret == null){
				log.error("do contract error");
				ret = RespCreateTransaction.newBuilder();
				ret.setRetCode(-1);
			}
		}else{
			log.warn("no contract, do contract failed");
			ret = RespCreateTransaction.newBuilder();
			ret.setRetCode(-1);
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
