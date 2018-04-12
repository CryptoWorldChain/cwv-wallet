package org.brewchain.cwv.wlt.service.user;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUserExample;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.enums.UserStatusEnum;
import org.brewchain.cwv.wlt.util.StringUtil;
import org.fc.wlt.gens.User.PMUserInfo;
import org.fc.wlt.gens.User.PRetUserLogin;
import org.fc.wlt.gens.User.PSUserLogin;
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
public class UserLoginService extends SessionModules<PSUserLogin> {

	@Override
	public String getModule() {
		return PUSRModule.USR.name();
	}

	@Override
	public String[] getCmds() {
		return new String[] {PUSRCommand.LIN.name()};
	}

	public String toString() {
		return "PSUsrLogin";
	}
	
	@ActorRequire
	Daos daos;
	
	@Override
	public void onPBPacket(FramePacket pack, PSUserLogin pbo, CompleteHandler handler) {
		final PRetUserLogin.Builder ret = PRetUserLogin.newBuilder();
		if(pbo != null && StringUtils.isNoneBlank(pbo.getUserName(), pbo.getUserPasswd())) {
			ret.setRequestNo(pbo.getRequestNo());
			String userName = pbo.getUserName();
			String userPasswd = pbo.getUserPasswd();
			CWVWltUserExample userExample = new CWVWltUserExample();;
			userExample.createCriteria()
				.andUserNameEqualTo(userName)
				.andUserPasswdEqualTo(StringUtil.getMD5(userPasswd))
				.andUserStatusEqualTo(UserStatusEnum.OK.getValue());
			Object userObj = daos.wltUserDao.selectOneByExample(userExample);
			if(userObj != null) {
				CWVWltUser user = (CWVWltUser)userObj;
				PMUserInfo.Builder userInfo = PMUserInfo.newBuilder();
				userInfo.setCheckEMail(user.getCheckEmail());
				userInfo.setCheckPhone(user.getCheckPhone());
				userInfo.setDateTime(user.getCreatedTime().getTime());
				userInfo.setUserCode(user.getUserCode());
				userInfo.setUserId(user.getUserId());

				ret.setUserInfo(userInfo);
				ret.setRequestNo(pbo.getRequestNo());
				ret.setErrCode(UserReturnEnum.OK.getValue());
				ret.setMsg(UserReturnEnum.OK.getName());
			}else {
				ret.setRequestNo(pbo.getRequestNo());
				ret.setErrCode(UserReturnEnum.FAIL_ERROR_USER_CODE_OR_PASSWD.getValue());
				ret.setMsg(UserReturnEnum.FAIL_ERROR_USER_CODE_OR_PASSWD.getName());
			}
			
		}else {
			ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
			ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_PARAM.getName());
		}
		
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));	
	}

}
