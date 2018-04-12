package org.brewchain.cwv.wlt.service.bcwww;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.bc.entity.CWVBcWwwUser;
import org.brewchain.cwv.wlt.dbgens.bc.entity.CWVBcWwwUserExample;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PBCWCommand;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PBCWModule;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PRetREG;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PSREG;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.UUIDGenerator;

@NActorProvider
@Slf4j
@Data
public class BCWWWRegisterService extends SessionModules<PSREG>{
	@ActorRequire
	Daos daos;
	@Override
	public String[] getCmds() {		
		return new String[] { PBCWCommand.REG.name() };
	}

	@Override
	public String getModule() {
		return PBCWModule.BCW.name();
	}
	public String toString(){
		return "register user";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSREG pbo, final CompleteHandler handler) {
		final PRetREG.Builder ret = PRetREG.newBuilder();
		if(pbo != null && StringUtils.isNotBlank(pbo.getUsername()) && StringUtils.isNotBlank(pbo.getPassword())){
			CWVBcWwwUserExample example = new CWVBcWwwUserExample();
			example.createCriteria().andUserNameEqualTo(pbo.getUsername());
			List<Object> users = daos.bcWwwUserDao.selectByExample(example);
			if(users != null && !users.isEmpty()){
				ret.setErrorCode("000001");
				ret.setErrorCode("该用户名已被注册，请更换用户名重试...");
			}else{
				CWVBcWwwUser user = new CWVBcWwwUser();
				user.setUserId(UUIDGenerator.generate());
				user.setUserName(pbo.getUsername());
				user.setPasswd(pbo.getPassword());
				if(StringUtils.isNoneBlank(pbo.getQuestion1(), pbo.getAnswer1())){
					user.setQustion1(pbo.getQuestion1().trim());
					user.setAnswer1(pbo.getAnswer1().trim());
				}
				if(StringUtils.isNoneBlank(pbo.getQuestion2(), pbo.getAnswer2())){
					user.setQustion2(pbo.getQuestion2().trim());
					user.setAnswer2(pbo.getAnswer2().trim());
				}
				if(StringUtils.isNoneBlank(pbo.getQuestion3(), pbo.getAnswer3())){
					user.setQustion3(pbo.getQuestion3().trim());
					user.setAnswer3(pbo.getAnswer3().trim());
				}
				
				user.setCreatedTime(new Date());
				daos.bcWwwUserDao.insert(user);
				ret.setErrorCode("000000");
				ret.setErrorDesc("注册成功");
			}
		}else{
			ret.setErrorCode("000001");
			ret.setErrorDesc("用户名或密码为空...");
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
