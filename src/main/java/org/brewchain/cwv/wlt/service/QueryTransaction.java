package org.brewchain.cwv.wlt.service;


import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.helper.TransactionHelper;
import org.brewchain.wallet.service.Wallet.PWLTCommand;
import org.brewchain.wallet.service.Wallet.PWLTModule;
import org.brewchain.wallet.service.Wallet.ReqGetTxByHash;
import org.brewchain.wallet.service.Wallet.RespGetTxByHash;

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
public class QueryTransaction extends SessionModules<ReqGetTxByHash>{

	@ActorRequire(name = "txHelper", scope = "global")
	TransactionHelper txHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PWLTCommand.QTS.name() };
	}

	@Override
	public String getModule() {
		return PWLTModule.WLT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetTxByHash pb, final CompleteHandler handler) {
		RespGetTxByHash.Builder ret = null;
		if(pb != null && StringUtils.isNotBlank(pb.getHexTxHash())){
			try{
				ret = txHelper.queryTransactionByTxHash(pb.getHexTxHash());
			} catch (Exception e){
				log.error("query transaction error : " + e.getMessage());
			}
			if(ret == null){
				ret = RespGetTxByHash.newBuilder();
				log.warn("query transaction error");
				ret.setRetCode(-1);
			}
		}else{
			ret = RespGetTxByHash.newBuilder();
			log.warn("no txhash");
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
