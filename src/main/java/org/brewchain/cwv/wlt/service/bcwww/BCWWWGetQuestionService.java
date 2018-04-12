	package org.brewchain.cwv.wlt.service.bcwww;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.bc.entity.CWVBcWwwUser;
import org.brewchain.cwv.wlt.dbgens.bc.entity.CWVBcWwwUserExample;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PBCWCommand;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PBCWModule;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PRetQUS;
import org.fc.bc.wlt.gens.bcwww.Bcwww.PSQUS;
import org.fc.bc.wlt.gens.bcwww.Bcwww.Question;

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
public class BCWWWGetQuestionService extends SessionModules<PSQUS>{
	@ActorRequire
	Daos daos;
	@Override
	public String[] getCmds() {		
		return new String[] { PBCWCommand.QUS.name() };
	}

	@Override
	public String getModule() {
		return PBCWModule.BCW.name();
	}
	public String toString(){
		return "getQustion";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSQUS pbo, final CompleteHandler handler) {
		final PRetQUS.Builder ret = PRetQUS.newBuilder();
		if(pbo != null && StringUtils.isNotBlank(pbo.getUsername())){
			CWVBcWwwUserExample example = new CWVBcWwwUserExample();
			example.createCriteria().andUserNameEqualTo(pbo.getUsername());
			List<Object> users = daos.bcWwwUserDao.selectByExample(example);
			if(users != null && !users.isEmpty()){
				ret.setErrorCode("000000");
				ret.setErrorDesc("查询成功...");
				for(Object obj : users){
					CWVBcWwwUser user = (CWVBcWwwUser)obj;
					if(StringUtils.isNoneBlank(user.getQustion1(), user.getAnswer1())){
						Question.Builder value = Question.newBuilder();
						value.setQuestion(user.getQustion1());
						value.setAnswer(user.getAnswer1());
						ret.addQuestion(value);
					}
				}
			}else{
				ret.setErrorCode("000001");
				ret.setErrorDesc("查询用户失败，请重试...");
			}
		}else{
			ret.setErrorCode("000001");
			ret.setErrorDesc("用户名为空...");
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
