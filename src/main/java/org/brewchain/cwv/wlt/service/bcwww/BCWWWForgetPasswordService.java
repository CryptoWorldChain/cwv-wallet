package org.brewchain.cwv.wlt.service.bcwww;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.bc.entity.CWVBcWwwUser;
import org.brewchain.cwv.wlt.dbgens.bc.entity.CWVBcWwwUserExample;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PBCWCommand;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PBCWModule;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PRetLIN;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PSFPW;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class BCWWWForgetPasswordService extends SessionModules<PSFPW>{
	@ActorRequire
	Daos daos;
	@Override
	public String[] getCmds() {		
		return new String[] { PBCWCommand.FPW.name() };
	}

	@Override
	public String getModule() {
		return PBCWModule.BCW.name();
	}
	public String toString(){
		return "forgetpassword";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSFPW pbo, final CompleteHandler handler) {
		final PRetLIN.Builder ret = PRetLIN.newBuilder();
		if(pbo != null && StringUtils.isNotBlank(pbo.getUsername()) && StringUtils.isNotBlank(pbo.getQuestion()) && StringUtils.isNotBlank(pbo.getAnswer())){
			CWVBcWwwUserExample example = new CWVBcWwwUserExample();
			example.createCriteria().andUserNameEqualTo(pbo.getUsername().trim()).andQustion1EqualTo(pbo.getQuestion().trim()).andAnswer1EqualTo(pbo.getAnswer().trim());
			List<Object> users = daos.bcWwwUserDao.selectByExample(example);
			if(users != null && !users.isEmpty() && users.size() == 1){
				CWVBcWwwUser user = (CWVBcWwwUser) users.get(0);
				user.setPasswd("123456");
				user.setUpdatedTime(new Date());
				int result = daos.bcWwwUserDao.updateByPrimaryKey(user);
				if(result == 1){
					ret.setErrorCode("000000");
					ret.setErrorDesc("密码重置为：123456，请谨记密码或修改成新的密码...");
				}else{
					ret.setErrorCode("000001");
					ret.setErrorDesc("密码重置失败,请重试...");
				}
				
			}else{
				ret.setErrorCode("000001");
				ret.setErrorDesc("问题答案错误，查询用户失败，请重试...");
			}
		}else{
			ret.setErrorCode("000001");
			ret.setErrorDesc("用户名或问题答案为空...");
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
