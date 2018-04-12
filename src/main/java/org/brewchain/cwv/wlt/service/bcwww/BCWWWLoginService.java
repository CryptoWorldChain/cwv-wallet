package org.brewchain.cwv.wlt.service.bcwww;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.bc.entity.CWVBcWwwUserExample;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PBCWCommand;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PBCWModule;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PRetLIN;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PSLIN;

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
public class BCWWWLoginService extends SessionModules<PSLIN>{
	@ActorRequire
	Daos daos;
	@Override
	public String[] getCmds() {		
		return new String[] { PBCWCommand.LIN.name() };
	}

	@Override
	public String getModule() {
		return PBCWModule.BCW.name();
	}
	public String toString(){
		return "userlogin";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSLIN pbo, final CompleteHandler handler) {
		final PRetLIN.Builder ret = PRetLIN.newBuilder();
		if(pbo != null && StringUtils.isNotBlank(pbo.getUsername()) && StringUtils.isNotBlank(pbo.getPassword())){
			CWVBcWwwUserExample example = new CWVBcWwwUserExample();
			example.createCriteria().andUserNameEqualTo(pbo.getUsername()).andPasswdEqualTo(pbo.getPassword());
			List<Object> users = daos.bcWwwUserDao.selectByExample(example);
			if(users != null && !users.isEmpty()){
				ret.setErrorCode("000000");
				ret.setErrorDesc("登录成功");
			}else{
				ret.setErrorCode("000001");
				ret.setErrorDesc("登录失败，用户名或密码错误...");
			}
		}else{
			ret.setErrorCode("000001");
			ret.setErrorDesc("用户名或密码为空...");
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
