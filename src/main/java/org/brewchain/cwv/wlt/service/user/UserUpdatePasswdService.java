package org.brewchain.cwv.wlt.service.user;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUserExample;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.enums.UserStatusEnum;
import org.brewchain.cwv.wlt.util.StringUtil;
import org.fc.wlt.gens.User.PRetUpdateUserPasswd;
import org.fc.wlt.gens.User.PSUpdateUserPasswd;
import org.fc.wlt.gens.User.PUSRCommand;
import org.fc.wlt.gens.User.PUSRModule;

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
public class UserUpdatePasswdService extends SessionModules<PSUpdateUserPasswd> {

	@Override
	public String getModule() {
		return PUSRModule.USR.name();
	}

	@Override
	public String[] getCmds() {
		return new String[] {PUSRCommand.UPW.name()};
	}

	public String toString() {
		return "PSUsrLogin";
	}
	
	@ActorRequire
	Daos daos;
	
	@Override
	public void onPBPacket(FramePacket pack, PSUpdateUserPasswd pbo, CompleteHandler handler) {
		final PRetUpdateUserPasswd.Builder ret = PRetUpdateUserPasswd.newBuilder();
		if(pbo != null) {
			if(StringUtils.isNotBlank(pbo.getUserName()) || StringUtils.isNotBlank(pbo.getPasswd())) {
				ret.setRequestNo(pbo.getRequestNo());
				CWVWltUserExample userExample = new CWVWltUserExample();
				userExample.createCriteria().andUserCodeEqualTo(pbo.getUserCode()).andUserStatusEqualTo(UserStatusEnum.OK.getValue());
				Object userObj = daos.wltUserDao.selectOneByExample(userExample);
				if(userObj != null) {
					CWVWltUser user = (CWVWltUser)userObj;
					if(StringUtils.isNotBlank(pbo.getUserName())) {
						user.setUserName(pbo.getUserName());
					}
					if(StringUtils.isNotBlank(pbo.getPasswd())) {
						user.setUserPasswd(StringUtil.getMD5(pbo.getPasswd()));
					}
					user.setUpdatedTime(new Date());
					daos.wltUserDao.updateByPrimaryKey(user);
					
					ret.setErrCode(UserReturnEnum.OK.getValue());
					ret.setMsg(UserReturnEnum.OK.getName());
				}else {
					ret.setErrCode(UserReturnEnum.FAIL_ERROR_USER_CODE.getValue());
					ret.setMsg(UserReturnEnum.FAIL_ERROR_USER_CODE.getName());
				}
			}else {
				ret.setErrCode(UserReturnEnum.OK.getValue());
				ret.setMsg(UserReturnEnum.OK.getName());
			}
			
		}else {
			ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
			ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_PARAM.getName());
		}
		
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));	
	}

}
