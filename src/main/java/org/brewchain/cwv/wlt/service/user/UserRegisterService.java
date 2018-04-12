package org.brewchain.cwv.wlt.service.user;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.entity.AddressEntity;
import org.brewchain.cwv.wlt.entity.AssetEntity;
import org.brewchain.cwv.wlt.entity.RegisterRetEntity;
import org.brewchain.cwv.wlt.enums.AddrStatusEnum;
import org.brewchain.cwv.wlt.enums.AddrTypeEnum;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.enums.UserStatusEnum;
import org.brewchain.cwv.wlt.util.BackUpUtil;
import org.brewchain.cwv.wlt.util.StringUtil;
import org.fc.wlt.gens.Asset.PMFullAddress;
import org.fc.wlt.gens.User.PRetUsrReg;
import org.fc.wlt.gens.User.PSUsrReg;
import org.fc.wlt.gens.User.PUSRCommand;
import org.fc.wlt.gens.User.PUSRModule;

import lombok.Data;
import lombok.val;
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
import onight.tfw.outils.serialize.UUIDGenerator;

@NActorProvider
@Slf4j
@Data
public class UserRegisterService extends SessionModules<PSUsrReg> {

	@Override
	public String getModule() {
		return PUSRModule.USR.name();
	}

	@Override
	public String[] getCmds() {
		return new String[] { PUSRCommand.REG.name() };
	}

	public String toString() {
		return "PSUsrReg";
	}

	@ActorRequire
	Daos daos;
	
	private PropHelper props = new PropHelper(null);
	
	@ActorRequire(name="http",scope="global")
	IPacketSender sender;

	@Override
	public void onPBPacket(FramePacket pack, PSUsrReg pbo, CompleteHandler handler) {
		final PRetUsrReg.Builder ret = PRetUsrReg.newBuilder();
		if(pbo != null) {
			String isCreator = pbo.getIsCreator();
			if(StringUtils.isBlank(isCreator)) {
				isCreator = "0";
			}
			
			RegisterRetEntity regRetEntity = null;
			try {
				//调用链创建账户
//				regRetEntity = register(pbo.getUserCode());
				//TODO 暂时使用假数据，保证接口继续运行
				regRetEntity = new RegisterRetEntity();
				regRetEntity.setErr_code("0");
				regRetEntity.setMsg("");
				
				AddressEntity addressEntity = new AddressEntity();
				addressEntity.setAlias("A");
				addressEntity.setHex_addr("B");
				addressEntity.setPki("C");
				addressEntity.setPub("D");
				addressEntity.setRpmd_hash("E");
				List<AddressEntity> addrs = new ArrayList<>();
				addrs.add(addressEntity);
				
				regRetEntity.setAddrs(addrs);
				
				
				
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage());
				throw new IllegalArgumentException("connect to block chain error...");
			}
			if(regRetEntity != null) {
				if(regRetEntity.getErr_code().equals("0")) {
					Date current = new Date();
					String userId = UUIDGenerator.generate();
					
					AddressEntity address = regRetEntity.getAddrs().get(0);
//					AssetEntity asset = regRetEntity.getFbc_asset();
					
					CWVWltUser user = new CWVWltUser();
					user.setCheckEmail(StringUtils.isBlank(pbo.getCheckEMail()) ? "" : pbo.getCheckEMail());
					user.setCheckPhone(StringUtils.isBlank(pbo.getCheckPhone()) ? "" : pbo.getCheckPhone());
					user.setCreatedTime(current);
					user.setIsCreator(isCreator);
					user.setReserved1("");
					user.setReserved2("");
					user.setUpdatedTime(current);
					user.setUserCode(pbo.getUserCode());
					user.setUserId(userId);
					user.setUserName(StringUtils.isNotBlank(pbo.getUserName()) ? pbo.getUserName() : "");
					user.setUserPasswd(StringUtils.isNotBlank(pbo.getUserPasswd()) ? StringUtil.getMD5(pbo.getUserPasswd()) : "");
					user.setUserStatus(UserStatusEnum.OK.getValue());

					daos.wltUserDao.insert(user);
					
					CWVWltAddr addr = new CWVWltAddr();
					addr.setAddrId(UUIDGenerator.generate());
					addr.setAddrStatus(AddrStatusEnum.EMPTY.getValue());
					addr.setAddrAlias(StringUtils.isBlank(address.getAlias()) ? "" : address.getAlias());
					addr.setCreatedTime(current);
					addr.setHexAddr(StringUtils.isBlank(address.getHex_addr()) ? "" : address.getHex_addr());
					addr.setPrivateKey(StringUtils.isBlank(address.getPki()) ? "" : address.getPki());
					addr.setPublicKey(StringUtils.isBlank(address.getPub()) ? "" : address.getPub());
					addr.setPublicKeyHash(StringUtils.isBlank(address.getRpmd_hash()) ? "" : address.getRpmd_hash());
					addr.setReserved1("");
					addr.setReserved2("");
					addr.setUpdatedTime(current);
					addr.setUserId(userId);
					addr.setAddrType(AddrTypeEnum.WALLET_ADDR.getValue());
					
					daos.wltAddrDao.insert(addr);
					
					String chainapi = props.get("chainapi", "http://127.0.0.1:8000");
					chainapi += "/fbs/pbnew.do";
					try {
						//TODO 暂时不进行数据入链保存
//						BackUpUtil.backUpUser(user, sender, chainapi, addr.getHexAddr(), addr.getPublicKeyHash());
//						BackUpUtil.backUpAddr(addr, sender, chainapi, addr.getHexAddr(), addr.getPublicKeyHash());
					}catch(Exception e) {
						
					}
					
					ret.setErrCode(UserReturnEnum.OK.getValue());
					ret.setMsg(UserReturnEnum.OK.getName());
					ret.setRequestNo(pbo.getRequestNo());

					PMFullAddress.Builder addrInfo = PMFullAddress.newBuilder();
					addrInfo.setAlias(StringUtils.isNotBlank(address.getAlias()) ? address.getAlias() : "");
					addrInfo.setHexAddr(StringUtils.isNotBlank(address.getHex_addr()) ? address.getHex_addr() : "");
					addrInfo.setPki(StringUtils.isNotBlank(address.getPub()) ? address.getPub() : "");
					addrInfo.setRpmdHash(StringUtils.isNotBlank(address.getRpmd_hash()) ? address.getRpmd_hash() : "");
					
					ret.setAddrs(addrInfo);
				}else {
					ret.setErrCode(UserReturnEnum.FAIL_ERROR_REGISTER_TO_CHAIN_ERROR.getValue());
					ret.setMsg(regRetEntity.getMsg());
				}
			}else {
				ret.setErrCode(UserReturnEnum.FAIL_ERROR_REGISTER_TO_CHAIN.getValue());
				ret.setMsg(UserReturnEnum.FAIL_ERROR_REGISTER_TO_CHAIN.getName());
			}
		}else {
			ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
			ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_PARAM.getName());
		}
		ret.setRequestNo(pbo.getRequestNo());
		
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
	
	private RegisterRetEntity register(String userCode) {
		String chainapi = props.get("chainapi", "http://127.0.0.1:8000");
		chainapi += "/fbs/pbreg.do";
		log.debug("chainapi is : " + chainapi);
		Map<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put("user_id", userCode);
		dataMap.put("org", "hrwy");
		dataMap.put("address_count", 0);
		dataMap.put("metadata", "");
		String sendJson = JsonSerializer.formatToString(dataMap);
		FramePacket fp = PacketHelper.buildUrlFromJson(sendJson, "POST", chainapi);
		val regRet = sender.send(fp, 30000);
		
		RegisterRetEntity regRetEntity = JsonSerializer.getInstance().deserialize(regRet.getBody(), RegisterRetEntity.class);
		
		return regRetEntity;
	}

}
