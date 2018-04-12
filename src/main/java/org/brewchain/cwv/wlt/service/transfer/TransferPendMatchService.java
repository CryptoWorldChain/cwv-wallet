package org.brewchain.cwv.wlt.service.transfer;
//package org.fc.bc.wlt.service.transfer;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import org.fc.wlt.gens.System.PRetTestPB;
//import org.fc.wlt.gens.System.PSYSCommand;
//import org.fc.wlt.gens.System.PSYSModule;
//import org.fc.wlt.gens.Transfer.PSBuySellMake;
//
//import lombok.Data;
//import lombok.val;
//import lombok.extern.slf4j.Slf4j;
//import onight.oapi.scala.commons.SessionModules;
//import onight.osgi.annotation.NActorProvider;
//import onight.tfw.async.CompleteHandler;
//import onight.tfw.ntrans.api.annotation.ActorRequire;
//import onight.tfw.otransio.api.IPacketSender;
//import onight.tfw.otransio.api.PacketHelper;
//import onight.tfw.otransio.api.beans.FramePacket;
//import onight.tfw.outils.conf.PropHelper;
//import onight.tfw.outils.serialize.JsonSerializer;
//
//
//// http://localhost:8000/usr/pbreg.do?fh=REGUSR0000000J00&resp=bd&bd={"username":"aaa","userid":"1111"}
//
//@NActorProvider
//@Slf4j
//@Data
//public class TransferPendMatchService extends SessionModules<PSBuySellMake> {
//
//	@ActorRequire(name = "http", scope = "global")
//	IPacketSender sender;
//	
//	private PropHelper props = new PropHelper(null);
//	
//	@Override
//	public String[] getCmds() {		
//		return new String[] { PSYSCommand.TES.name() };
//	}
//
//	@Override
//	public String getModule() {
//		return PSYSModule.SYS.name();
//	}
//	public String toString(){
//		return "PSSYSTest";
//	}
//	
//	@Override
//	public void onPBPacket(final FramePacket pack, PSBuySellMake pbo, final CompleteHandler handler) {
//		final PRetTestPB.Builder ret = PRetTestPB.newBuilder();
//		
//		//请求参数开始构造
//		Map<String, Object> params = new HashMap<>();
//		params.put("node_cluster_guid", "4254996ea74da7e7ce7fe114684d5d29");
//		params.put("domain", "jd");
//		String sendJson = JsonSerializer.formatToString(params);
//		String postUrl = "http://172.16.21.22:6001/fbs/ctl/pbget.do";
//		
//        FramePacket fp = PacketHelper.buildUrlFromJson(sendJson, "POST", postUrl);
//		val retReg = sender.send(fp, 30000);
//		System.out.println(new String(retReg.genBodyBytes()));
//		ret.setErrCode("000000");
//		ret.setErrMsg("success");
//		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
//	}
//
//}