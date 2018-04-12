package org.brewchain.cwv.wlt.service.user;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertOrg;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertPer;
import org.brewchain.cwv.wlt.enums.UserCertTypeEnum;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.fc.wlt.gens.User.PRetCertifcationUpdate;
import org.fc.wlt.gens.User.PSCertifcationUpdate;
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
public class UserCertificationUPDService extends SessionModules<PSCertifcationUpdate> {

	@Override
	public String getModule() {
		return PUSRModule.USR.name();
	}

	@Override
	public String[] getCmds() {
		return new String[] {PUSRCommand.CUP.name()};
	}

	public String toString() {
		return "PSUsrCheckCeritificationUpdate";
	}
	
	@ActorRequire
	Daos daos;

	@Override
	public void onPBPacket(FramePacket pack, PSCertifcationUpdate pbo, CompleteHandler handler) {
		final PRetCertifcationUpdate.Builder ret = PRetCertifcationUpdate.newBuilder();
		if(pbo != null && StringUtils.isNotBlank(pbo.getStatus())) {
			
			if(pbo.getType().equals(UserCertTypeEnum.ORGANATION.getValue()) || pbo.getType().equals(UserCertTypeEnum.PERSONAL.getValue())){
				if(pbo.getType().equals(UserCertTypeEnum.ORGANATION.getValue())) {
					CWVWltCertOrg org = new CWVWltCertOrg();
//					org.setUserId(pbo.getUserId());
					org.setOrgId(pbo.getOrgId());
					org.setOrgStatus(pbo.getStatus());
					org.setUpdatedTime(new Date());
					
					int updOrgRet = daos.wltCertOrgDao.updateByPrimaryKeySelective(org);
					if(updOrgRet == 1) {
						ret.setErrCode(UserReturnEnum.OK.getValue());
						ret.setMsg(UserReturnEnum.OK.getName());
					}else {
						ret.setErrCode(UserReturnEnum.FAIL_ERROR_ORGID_OR_PERID.getValue());
						ret.setMsg(UserReturnEnum.FAIL_ERROR_ORGID_OR_PERID.getName());
					}
				}else {
					CWVWltCertPer person = new CWVWltCertPer();
					person.setPerId(pbo.getPerId());
					person.setPerStatus(pbo.getStatus());
					person.setUpdatedTimd(new Date());
					
					int updPerRet = daos.wltCertPerDao.updateByPrimaryKeySelective(person);
					if(updPerRet == 1) {
						ret.setErrCode(UserReturnEnum.OK.getValue());
						ret.setMsg(UserReturnEnum.OK.getName());
					}else {
						ret.setErrCode(UserReturnEnum.FAIL_ERROR_ORGID_OR_PERID.getValue());
						ret.setMsg(UserReturnEnum.FAIL_ERROR_ORGID_OR_PERID.getName());
					}
				}
			}else {
				ret.setErrCode(UserReturnEnum.FAIL_ERROR_CERTIFICATION_TYPE.getValue());
				ret.setMsg(UserReturnEnum.FAIL_ERROR_CERTIFICATION_TYPE.getName());
			}
		}else {
			ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
			ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_PARAM.getName());
		}
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}

}
