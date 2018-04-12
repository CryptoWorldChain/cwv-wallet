package org.brewchain.cwv.wlt.service.query;

import org.brewchain.cwv.wlt.dao.Daos;
import org.fc.wlt.gens.Query.PQRYCommand;
import org.fc.wlt.gens.Query.PQRYModule;
import org.fc.wlt.gens.Query.PSGetBuySellMake;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.JsonSerializer;


// http://localhost:8000/usr/pbreg.do?fh=REGUSR0000000J00&resp=bd&bd={"username":"aaa","userid":"1111"}

@NActorProvider
@Slf4j
@Data
public class QueryPendMatchService extends SessionModules<PSGetBuySellMake> {

	@ActorRequire
	Daos daos;
	@ActorRequire(name = "http", scope = "global")
	IPacketSender sender;
	
	private PropHelper props = new PropHelper(null);
	
	@Override
	public String[] getCmds() {		
		return new String[] { PQRYCommand.GTM.name() };
	}

	@Override
	public String getModule() {
		return PQRYModule.QRY.name();
	}
	public String toString(){
		return "QueryPendMatchService";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSGetBuySellMake pbo, final CompleteHandler handler) {
		final PSGetBuySellMake.Builder ret = PSGetBuySellMake.newBuilder();
		
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}

}