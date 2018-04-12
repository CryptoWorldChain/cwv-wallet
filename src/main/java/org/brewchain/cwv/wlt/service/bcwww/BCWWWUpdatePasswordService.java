package org.brewchain.cwv.wlt.service.bcwww;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.bc.entity.CWVBcWwwUser;
import org.brewchain.cwv.wlt.dbgens.bc.entity.CWVBcWwwUserExample;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PBCWCommand;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PBCWModule;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PRetUPW;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PSUPW;

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
public class BCWWWUpdatePasswordService extends SessionModules<PSUPW>{
	@ActorRequire
	Daos daos;
	@Override
	public String[] getCmds() {		
		return new String[] { PBCWCommand.UPW.name() };
	}

	@Override
	public String getModule() {
		return PBCWModule.BCW.name();
	}
	public String toString(){
		return "updatepassword";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSUPW pbo, final CompleteHandler handler) {
		final PRetUPW.Builder ret = PRetUPW.newBuilder();
		if(pbo != null && StringUtils.isNotBlank(pbo.getUsername()) && StringUtils.isNotBlank(pbo.getOldpassword()) && StringUtils.isNotBlank(pbo.getNewpassword())){
			CWVBcWwwUserExample example = new CWVBcWwwUserExample();
			example.createCriteria().andUserNameEqualTo(pbo.getUsername()).andPasswdEqualTo(pbo.getOldpassword());
			List<Object> users = daos.bcWwwUserDao.selectByExample(example);
			if(users != null && !users.isEmpty() && users.size() == 1){
				CWVBcWwwUser user = (CWVBcWwwUser)users.get(0);
				user.setPasswd(pbo.getNewpassword());
				user.setUpdatedTime(new Date());
				int result = daos.bcWwwUserDao.updateByPrimaryKey(user);
				if(result == 1){
					ret.setErrorCode("000000");
					ret.setErrorDesc("修改成功...");
				}else{
					ret.setErrorCode("000001");
					ret.setErrorDesc("修改失败，数据库更新失败...");
				}
			}else{
				ret.setErrorCode("000001");
				ret.setErrorDesc("修改失败，根据用户名查询用户失败...");
			}
		}else{
			ret.setErrorCode("000001");
			ret.setErrorDesc("用户名或密码为空...");
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
