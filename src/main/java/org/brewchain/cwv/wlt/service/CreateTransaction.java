package org.brewchain.cwv.wlt.service;


import org.brewchain.cwv.wlt.helper.TransactionHelper;
import org.brewchain.wallet.service.Wallet.PWLTCommand;
import org.brewchain.wallet.service.Wallet.PWLTModule;
import org.brewchain.wallet.service.Wallet.ReqCreateMultiTransaction;
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
public class CreateTransaction extends SessionModules<ReqCreateMultiTransaction>{

	@ActorRequire(name = "txHelper", scope = "global")
	TransactionHelper txHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PWLTCommand.NTS.name() };
	}

	@Override
	public String getModule() {
		return PWLTModule.WLT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateMultiTransaction pb, final CompleteHandler handler) {
		RespCreateTransaction.Builder ret = null;
		if(pb != null){
			try{
				ret = txHelper.createTransaction(pb.getTransaction());
			} catch (Exception e){
				log.error("create transaction error + " + e.getMessage());
			}
			if(ret == null){
				log.error("create transaction error");
				ret = RespCreateTransaction.newBuilder();
				ret.setRetCode(-1);
			}
		}else{
			log.warn("no transaction, create transaction failed");
			ret = RespCreateTransaction.newBuilder();
			ret.setRetCode(-1);
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
