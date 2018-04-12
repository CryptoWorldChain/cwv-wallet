package org.brewchain.cwv.wlt.service.query;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddrExample;
import org.brewchain.cwv.wlt.enums.AssetReturnEnum;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.fc.wlt.gens.Asset.PMSignAddress;
import org.fc.wlt.gens.Query.PQRYCommand;
import org.fc.wlt.gens.Query.PQRYModule;
import org.fc.wlt.gens.Query.PRetGetUserAddress;
import org.fc.wlt.gens.Query.PSGetUserAddress;

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
public class QueryUserAddressService extends SessionModules<PSGetUserAddress> {

	@ActorRequire
	Daos daos;
	
	@Override
	public String[] getCmds() {		
		return new String[] { PQRYCommand.GUA.name() };
	}

	@Override
	public String getModule() {
		return PQRYModule.QRY.name();
	}
	public String toString(){
		return "QueryUserAddressService";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSGetUserAddress pbo, final CompleteHandler handler) {
		final PRetGetUserAddress.Builder ret = PRetGetUserAddress.newBuilder();
		if(pbo != null) {
			ret.setRequestNo(pbo.getRequestNo());
			String hexAddr = pbo.getHexAddr();
			if(StringUtils.isNotBlank(hexAddr)) {
				CWVWltAddrExample addrExample = new CWVWltAddrExample();
				addrExample.createCriteria().andHexAddrEqualTo(hexAddr);
				Object addrObj = daos.wltAddrDao.selectOneByExample(addrExample);
				if(addrObj != null) {
					CWVWltAddr addr = (CWVWltAddr)addrObj;
					
					PMSignAddress.Builder addrInfo = PMSignAddress.newBuilder();
					addrInfo.setAlias(addr.getAddrAlias());
					addrInfo.setHexAddr(addr.getHexAddr());
					addrInfo.setRpmdHash(addr.getPublicKeyHash());
					ret.setUa(addrInfo);
					ret.setErrCode(AssetReturnEnum.OK.getValue());
					ret.setMsg(AssetReturnEnum.OK.getName());
				}else {
					ret.setErrCode(AssetReturnEnum.FAIL_ERROR_HEX_ADDR_ERROR.getValue());
					ret.setMsg(AssetReturnEnum.FAIL_ERROR_HEX_ADDR_ERROR.getName());
				}
			}else {
				ret.setErrCode(AssetReturnEnum.FAIL_ERROR_HEX_ADDR_IS_NULL.getValue());
				ret.setMsg(AssetReturnEnum.FAIL_ERROR_HEX_ADDR_IS_NULL.getName());
			}
		}else {
			ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
			ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_PARAM.getName());
		}
		
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}

}