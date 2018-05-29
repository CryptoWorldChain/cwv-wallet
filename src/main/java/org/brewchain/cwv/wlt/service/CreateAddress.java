package org.brewchain.cwv.wlt.service;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.helper.AddressHelper;
import org.brewchain.cwv.wlt.helper.TransactionHelper;
import org.brewchain.wallet.service.Wallet.PWLTCommand;
import org.brewchain.wallet.service.Wallet.PWLTModule;
import org.brewchain.wallet.service.Wallet.ReqNewAddress;
import org.brewchain.wallet.service.Wallet.RetNewAddress;

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
public class CreateAddress extends SessionModules<ReqNewAddress> {

	@ActorRequire(name = "addrHelper", scope = "global")
	AddressHelper addressHelper;
	
	@ActorRequire(name = "txHelper", scope = "global")
	TransactionHelper txHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PWLTCommand.NAD.name() };
	}

	@Override
	public String getModule() {
		return PWLTModule.WLT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqNewAddress pb, final CompleteHandler handler) {
		RetNewAddress.Builder ret = RetNewAddress.newBuilder();
		String address = "";
		if (pb != null && StringUtils.isNotBlank(pb.getType())) {
			try{
				address = addressHelper.registerAddress(pb.getType(), pb.getSeed());
				ret.setRetCode(1).setMsg("success").setAddress(address);
			} catch (Exception e){
				ret.setRetCode(-1).setMsg(e.getMessage());
				log.error("create addrss error");
			}
		} else {
			log.warn("no address type, create address failed");
			ret.setRetCode(-1).setMsg("create address error, no address type ");
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
