package org.brewchain.cwv.wlt.service;


import org.brewchain.cwv.wlt.helper.TransactionHelper;
import org.brewchain.wallet.service.Wallet.MultiTransactionInputImpl;
import org.brewchain.wallet.service.Wallet.PWLTCommand;
import org.brewchain.wallet.service.Wallet.PWLTModule;
import org.brewchain.wallet.service.Wallet.ReqCreateContractTransaction;
import org.brewchain.wallet.service.Wallet.RespCreateContractTransaction;

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
public class CreateContract extends SessionModules<ReqCreateContractTransaction>{

	@ActorRequire(name = "txHelper", scope = "global")
	TransactionHelper txHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PWLTCommand.NCR.name() };
	}

	@Override
	public String getModule() {
		return PWLTModule.WLT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateContractTransaction pb, final CompleteHandler handler) {
		RespCreateContractTransaction.Builder ret = null;
		
		if(pb != null){
			if(ret == null){
				log.warn("create contract error");
				try{
					ret = txHelper.createContract(pb);
				} catch (Exception e){
					log.error("create contract error : " + e.getMessage());
				}
				if(ret == null){
					log.error("create contract error");
					ret = RespCreateContractTransaction.newBuilder().setRetCode(-1).setRetMsg("create error");
				}
			}
		}else{
			log.warn("no contract ,create contract error");
			ret = RespCreateContractTransaction.newBuilder();
			ret.setRetCode(-1);
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
