package org.brewchain.cwv.wlt.service.transfer;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.helper.WltHelper;
import org.fc.wlt.gens.Transfer.PRetTransfer;
import org.fc.wlt.gens.Transfer.PSTransfer;
import org.fc.wlt.gens.Transfer.PTRSCommand;
import org.fc.wlt.gens.Transfer.PTRSModule;

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


@NActorProvider
@Slf4j
@Data
public class TransferService extends SessionModules<PSTransfer> {

	@Override
	public String[] getCmds() {		
		return new String[] { PTRSCommand.TXS.name() };
	}

	@Override
	public String getModule() {
		return PTRSModule.TRS.name();
	}
	public String toString(){
		return "PTRStransfer";
	}
	
	@ActorRequire
	WltHelper wltHelper;

	private PropHelper props = new PropHelper(null);
	
	@ActorRequire(name="http",scope="global")
	IPacketSender sender;
	
	@ActorRequire
	Daos daos;
	
	@Override
	public void onPBPacket(final FramePacket pack, PSTransfer pb, final CompleteHandler handler) {
		final PRetTransfer.Builder ret = PRetTransfer.newBuilder();
		try {
			checkParam(pb, ret);
			Map<String, Object> result = wltHelper.transfer(pb.getSign(), pb.getCoin());
			if(result.get("errCode").equals("1")){
				ret.setRetCode("0000");
				ret.setRetMsg(result.get("msg").toString());
			}else{
				ret.setRetCode("2000");
				ret.setRetMsg(result.get("msg").toString());
			}
		} catch (Exception e) {
		}
		
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
	
	public void checkParam(PSTransfer pb,PRetTransfer.Builder ret){
		if(pb == null){
			ret.setRetCode("1000");
			ret.setRetMsg("请求参数异常");
			throw new IllegalArgumentException("请求参数异常");
		}
		
		if(StringUtils.isBlank(pb.getCoin())){
			ret.setRetCode("1001");
			ret.setRetMsg("请求参数未设定交易币种");
			throw new IllegalArgumentException("请求参数未设定交易币种");
		}
		
		if(StringUtils.isBlank(pb.getSign())){
			ret.setRetCode("1001");
			ret.setRetMsg("请求参数没有交易签名");
			throw new IllegalArgumentException("请求参数没有交易签名");
		}
	}
}