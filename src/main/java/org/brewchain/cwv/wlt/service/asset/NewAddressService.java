package org.brewchain.cwv.wlt.service.asset;
//package org.fc.bc.wlt.service.asset;
//
//import java.io.IOException;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.apache.commons.lang3.StringUtils;
//import org.fc.bc.wlt.dao.Daos;
//import org.fc.bc.wlt.entity.AddressEntity;
//import org.fc.bc.wlt.entity.RegisterRetEntity;
//import org.fc.bc.wlt.enums.AddrStatusEnum;
//import org.fc.bc.wlt.enums.AssetReturnEnum;
//import org.fc.bc.wlt.enums.UserStatusEnum;
//import org.fc.wlt.gens.Asset.PASTCommand;
//import org.fc.wlt.gens.Asset.PASTModule;
//import org.fc.wlt.gens.Asset.PMUserAddress;
//import org.fc.wlt.gens.Asset.PRetNewUserAddress;
//import org.fc.wlt.gens.Asset.PSNewUserAddress;
//import org.fc.wlt.ordbgens.wlt.entity.WLTAddr;
//import org.fc.wlt.ordbgens.wlt.entity.WLTUser;
//import org.fc.wlt.ordbgens.wlt.entity.WLTUserExample;
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
//import onight.tfw.outils.serialize.UUIDGenerator;
//
//// http://localhost:8000/usr/pbreg.do?fh=REGUSR0000000J00&resp=bd&bd={"username":"aaa","userid":"1111"}
//
//@NActorProvider
//@Slf4j
//@Data
//public class NewAddressService extends SessionModules<PSNewUserAddress> {
//	
//	@ActorRequire
//	Daos daos;
//	
//	private PropHelper props = new PropHelper(null);
//	
//	@ActorRequire(name="http",scope="global")
//	IPacketSender sender;
//	
//	@Override
//	public String[] getCmds() {
//		return new String[] { PASTCommand.NUA.name() };
//	}
//
//	@Override
//	public String getModule() {
//		return PASTModule.AST.name();
//	}
//	
//	public String toString(){
//		return "newaddr";
//	}
//
//	@Override
//	public void onPBPacket(final FramePacket pack, PSNewUserAddress pbo, final CompleteHandler handler) {
//		final PRetNewUserAddress.Builder ret = PRetNewUserAddress.newBuilder();
//
//		if(pbo != null) {
//			if(StringUtils.isNotBlank(pbo.getUserCode())) {
//				WLTUserExample userExample = new WLTUserExample();
//				userExample.createCriteria().andUserCodeEqualTo(pbo.getUserCode()).andUserStatusEqualTo(UserStatusEnum.OK.getValue());
//				Object userObj = daos.wltUserDao.selectOneByExample(userExample);
//				if(userObj != null) {
//					WLTUser user = (WLTUser)userObj;
//					WLTAddr addr = new WLTAddr();
//					addr.setAddrId(UUIDGenerator.generate());
//					addr.setAddrStatus(AddrStatusEnum.INIT.getValue());
//					addr.setCreatedTime(new Date());
//					addr.setHexAddr("");
//					addr.setPrivateKey("");
//					addr.setPublicKey("");
//					addr.setPublicKeyHash("");
//					addr.setReserved1("");
//					addr.setReserved2("");
//					addr.setUpdatedTime(addr.getCreatedTime());
//					addr.setUserId(user.getUserId());
//					daos.wltAddrDao.insert(addr);
//					
//					String chainapi = props.get("chainapi", "http://127.0.0.1:8000");
//					chainapi += "/fbs/fbs/pbreg.do";
//					Map<String, Object> dataMap = new HashMap<String, Object>();
//					dataMap.put("user_id", user.getUserCode());
//					dataMap.put("org", "hrwy");
//					dataMap.put("metadata", "mm");
//					dataMap.put("address_count", 0);
//					String sendJson = JsonSerializer.formatToString(dataMap);
//					FramePacket fp = PacketHelper.buildUrlFromJson(sendJson, "POST", chainapi);
//					val regRet = sender.send(fp, 30000);
//					
//					RegisterRetEntity regRetEntity = JsonSerializer.getInstance().deserialize(regRet.getBody(), RegisterRetEntity.class);
//					if(regRetEntity == null) {
//						throw new NullPointerException("connect to blockchain error...");
//					}
//					if(regRetEntity.getErr_code().equals("0")) {
//						AddressEntity address = regRetEntity.getAddrs().get(0);
//						
//						addr.setHexAddr(address.getHex_addr());
//						addr.setPrivateKey(address.getPki());
//						addr.setPublicKey(address.getPub());
//						addr.setPublicKeyHash(address.getRpmd_hash());
//						addr.setUpdatedTime(new Date());
//						addr.setAddrStatus(AddrStatusEnum.EMPTY.getValue());
//						
//						daos.wltAddrDao.updateByPrimaryKeySelective(addr);
//						
//						ret.setErrCode(AssetReturnEnum.OK.getValue());
//						ret.setMsg(AssetReturnEnum.OK.getName());
//						ret.setRequestNo(pbo.getRequestNo());
//						PMUserAddress.Builder addrInfo = PMUserAddress.newBuilder();
//						addrInfo.setDateTime(addr.getCreatedTime().getTime());
//						addrInfo.setHexAddr(addr.getHexAddr());
//						addrInfo.setPub(addr.getPublicKey());
//						addrInfo.setPubHash(addr.getPublicKeyHash());
//						addrInfo.setUserCode(pbo.getUserCode());
//						addrInfo.setAddrId(addr.getAddrId());
//						
//						ret.setUa(addrInfo);
//					}else {
//						ret.setErrCode(AssetReturnEnum.FAIL_ERROR_CHAIN.getValue());
//						ret.setMsg(regRetEntity.getMsg());
//						ret.setRequestNo(pbo.getRequestNo());
//					}
//					
//					
//				}else {
//					ret.setRequestNo(pbo.getRequestNo());
//					ret.setErrCode(AssetReturnEnum.FAIL_ERROR_USER_CODE.getValue());
//					ret.setMsg(AssetReturnEnum.FAIL_ERROR_USER_CODE.getName());
//				}
//			}
//		}else {
//			ret.setErrCode(AssetReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
//			ret.setMsg(AssetReturnEnum.FAIL_ERROR_NO_PARAM.getName());
//		}
//		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
//		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
//	}
//}