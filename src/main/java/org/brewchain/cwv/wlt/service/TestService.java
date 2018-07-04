//package org.brewchain.cwv.wlt.service;
//
//import org.brewchain.wallet.service.Wallet.MultiTransactionBodyImpl;
//import org.brewchain.wallet.service.Wallet.MultiTransactionInputImpl;
//import org.brewchain.wallet.service.Wallet.MultiTransactionSignatureImpl;
//import org.brewchain.wallet.service.Wallet.PWLTCommand;
//import org.brewchain.wallet.service.Wallet.PWLTModule;
//import org.brewchain.wallet.service.Wallet.Test;
//import org.fc.brewchain.bcapi.EncAPI;
//
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import onight.oapi.scala.commons.SessionModules;
//import onight.osgi.annotation.NActorProvider;
//import onight.tfw.async.CompleteHandler;
//import onight.tfw.ntrans.api.annotation.ActorRequire;
//import onight.tfw.otransio.api.PacketHelper;
//import onight.tfw.otransio.api.beans.FramePacket;
//
//@NActorProvider
//@Slf4j
//@Data
//public class TestService extends SessionModules<Test> {
//
//	@ActorRequire(name = "bc_encoder", scope = "global")
//	EncAPI encApi;
//
//	@Override
//	public String[] getCmds() {
//		return new String[] { PWLTCommand.TST.name() };
//	}
//
//	@Override
//	public String getModule() {
//		return PWLTModule.WLT.name();
//	}
//
//	@Override
//	public void onPBPacket(final FramePacket pack, final Test pb, final CompleteHandler handler) {
//		Test.Builder ret = Test.newBuilder();
//		String pub = "04a00cdd9ebc21be186f788b32b5cdbb0524dd97b2469fba6ff1a31816473f7449b8d5b9f0f214b1e706779cb0462e5edc851bc6e502016668ce78d70668975c5c";
//		String pri = "92b96aca4b48a4e6fe27f5f52b0745f42400d9950825f97ecb863643fa686bb9";
//		String address = "307e1f0f9361a29f23a556237d18f2e894fa5acaf7";
//		MultiTransactionBodyImpl.Builder txBody = MultiTransactionBodyImpl.newBuilder();
//		MultiTransactionInputImpl.Builder input = MultiTransactionInputImpl.newBuilder();
//		input.setAddress(address);
//		input.setAmount(1);
//		input.setNonce(1);
//		input.setPubKey(pub);
//		txBody.addInputs(input);
//		txBody.setTimestamp(System.currentTimeMillis());
//		txBody.setData("");
//		txBody.setExdata("");
//		MultiTransactionSignatureImpl.Builder signature = MultiTransactionSignatureImpl.newBuilder();
//		signature.setPubKey(pub);
//		signature.setSignature(encApi.hexEnc(encApi.ecSign(pri, txBody.build().toByteArray())));
//		
//
//		boolean flag = encApi.ecVerify(pub, txBody.build().toByteArray(),encApi.hexDec(signature.getSignature()));
//		ret.setFlag(flag);
//		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
//	}
//}
