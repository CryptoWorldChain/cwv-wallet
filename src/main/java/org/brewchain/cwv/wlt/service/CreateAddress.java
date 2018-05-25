package org.brewchain.cwv.wlt.dao.service;


import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddressKey;
import org.brewchain.wallet.service.Test.PTSTCommand;
import org.brewchain.wallet.service.Test.PTSTModule;
import org.brewchain.wallet.service.Test.ReqCrt;
import org.brewchain.wallet.service.Test.RetCrt;
import org.fc.brewchain.bcapi.EncAPI;

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
public class AddBalance extends SessionModules<ReqCrt>{

	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	
	@ActorRequire(name = "daos", scope = "global")
	Daos daos;
	
	@Override
	public String[] getCmds() {
		return new String[] { PTSTCommand.CRT.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCrt pb, final CompleteHandler handler) {
		RetCrt.Builder ret = RetCrt.newBuilder();
		daos.wltAddressDao.selectByPrimaryKey(new CWVWltAddressKey("1"));
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
