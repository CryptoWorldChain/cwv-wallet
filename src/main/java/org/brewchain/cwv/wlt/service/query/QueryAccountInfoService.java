package org.brewchain.cwv.wlt.service.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddrExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAssetExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFund;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFundExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUserExample;
import org.brewchain.cwv.wlt.enums.AssetReturnEnum;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.enums.UserStatusEnum;
import org.brewchain.cwv.wlt.helper.WltHelper;
import org.fc.wlt.gens.Acc.PACCCommand;
import org.fc.wlt.gens.Acc.PACCModule;
import org.fc.wlt.gens.Acc.PRetAccountInfo;
import org.fc.wlt.gens.Acc.PSAccountInfo;
import org.fc.wlt.gens.Asset.PMFundInfo;
import org.fc.wlt.gens.Query.PQRYCommand;
import org.fc.wlt.gens.Query.PQRYModule;
import org.fc.wlt.gens.Query.PRetGetAssetFundInfo;
import org.fc.wlt.gens.Query.PSGetAssetFundInfo;
import org.fc.wlt.gens.Transfer.PRetTransfer;
import org.fc.wlt.gens.Transfer.PSTransfer;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.JsonSerializer;



@NActorProvider
@Slf4j
@Data
public class QueryAccountInfoService extends SessionModules<PSAccountInfo> {

	@ActorRequire
	Daos daos;
	
	@ActorRequire
	WltHelper wltHelper;
	
	@Override
	public String[] getCmds() {		
		return new String[] { PACCCommand.QAI.name() };
	}

	@Override
	public String getModule() {
		return PACCModule.ACC.name();
	}
	public String toString(){
		return "QueryAccountInfoService";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSAccountInfo pb, final CompleteHandler handler) {
		final PRetAccountInfo.Builder ret = PRetAccountInfo.newBuilder();
		
		try {
			checkParam(pb, ret);
			Map<String, Object> result = wltHelper.queryAccount(pb.getCoin(), pb.getAddress());
			if(result.get("errCode").equals("0000")){
				ret.setAddress(result.get("address").toString());
				ret.setBalance(Double.parseDouble(result.get("balance").toString()));
				ret.setNonce(Integer.parseInt(result.get("nonce").toString()));
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
	
	public void checkParam(PSAccountInfo pb,PRetAccountInfo.Builder ret){
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
		
		if(StringUtils.isBlank(pb.getAddress())){
			ret.setRetCode("1001");
			ret.setRetMsg("请求参数中没有所要查询的地址");
			throw new IllegalArgumentException("请求参数中没有所要查询的地址");
		}
	}

}
