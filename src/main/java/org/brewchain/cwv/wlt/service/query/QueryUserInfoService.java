package org.brewchain.cwv.wlt.service.query;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUserExample;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.enums.UserStatusEnum;
import org.fc.wlt.gens.Query.PQRYCommand;
import org.fc.wlt.gens.Query.PQRYModule;
import org.fc.wlt.gens.Query.PRetGetUserInfo;
import org.fc.wlt.gens.Query.PSGetUserInfo;
import org.fc.wlt.gens.User.PMUserInfo;

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
public class QueryUserInfoService extends SessionModules<PSGetUserInfo> {
	@ActorRequire
	Daos daos;
	
	@Override
	public String[] getCmds() {		
		return new String[] {PQRYCommand.GUI.name()};
	}

	@Override
	public String getModule() {
		return PQRYModule.QRY.name();
	}
	public String toString(){
		return "QueryUserInfoService";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSGetUserInfo pbo, final CompleteHandler handler) {
		final PRetGetUserInfo.Builder ret = PRetGetUserInfo.newBuilder();
		if(pbo != null) {
			ret.setRequestNo(pbo.getRequestNo());
			String userCode = pbo.getUserCode();
			if(StringUtils.isNotBlank(userCode)) {
				CWVWltUserExample userExample = new CWVWltUserExample();
				userExample.createCriteria().andUserCodeEqualTo(userCode).andUserStatusEqualTo(UserStatusEnum.OK.getValue());
				Object userObj = daos.wltUserDao.selectOneByExample(userExample);
				if(userObj != null) {
					CWVWltUser user  = (CWVWltUser)userObj;
					PMUserInfo.Builder userInfo = PMUserInfo.newBuilder();
					userInfo.setCheckEMail(user.getCheckEmail());
					userInfo.setCheckPhone(user.getCheckPhone());
					userInfo.setDateTime(user.getCreatedTime().getTime());
					userInfo.setUserCode(user.getUserCode());
					userInfo.setUserId(user.getUserId());
					
					ret.setUserInfo(userInfo);
					ret.setErrCode(UserReturnEnum.OK.getValue());
					ret.setMsg(UserReturnEnum.OK.getName());
				}else {
					ret.setErrCode(UserReturnEnum.FAIL_ERROR_USER_CODE.getValue());
					ret.setMsg(UserReturnEnum.FAIL_ERROR_USER_CODE.getName());
				}
			}
			ret.setRequestNo(pbo.getRequestNo());
		}else {
			ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
			ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_PARAM.getName());
		}
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));

	}
}