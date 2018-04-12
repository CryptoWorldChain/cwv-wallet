package org.brewchain.cwv.wlt.service.user;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertOrg;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertPer;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUserExample;
import org.brewchain.cwv.wlt.enums.CertStatusEnum;
import org.brewchain.cwv.wlt.enums.UserCertTypeEnum;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.enums.UserStatusEnum;
import org.fc.wlt.gens.User.PMOrganizationInfo;
import org.fc.wlt.gens.User.PMPersonalInfo;
import org.fc.wlt.gens.User.PRetCertifcation;
import org.fc.wlt.gens.User.PSCertifcation;
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
import onight.tfw.outils.serialize.UUIDGenerator;

@NActorProvider
@Slf4j
@Data
public class UserCertificationService extends SessionModules<PSCertifcation> {

	@Override
	public String getModule() {
		return PUSRModule.USR.name();
	}

	@Override
	public String[] getCmds() {
		return new String[] { PUSRCommand.CER.name() };
	}

	public String toString() {
		return "PSUsrcertification";
	}

	@ActorRequire
	Daos daos;

	@Override
	public void onPBPacket(FramePacket pack, PSCertifcation pbo, CompleteHandler handler) {
		final PRetCertifcation.Builder ret = PRetCertifcation.newBuilder();

		if(pbo != null) {
			ret.setRequestNo(pbo.getRequestNo());
			String userCode = pbo.getUserCode();
			
			CWVWltUserExample userExample = new CWVWltUserExample();
			userExample.createCriteria().andUserCodeEqualTo(userCode).andUserStatusEqualTo(UserStatusEnum.OK.getValue());
			Object userObj = daos.wltUserDao.selectOneByExample(userExample);
			if(userObj != null) {
				CWVWltUser user = (CWVWltUser) userObj;
				String cerType = pbo.getType();
				CWVWltCertOrg org = null;
				CWVWltCertPer person = null;
				PMOrganizationInfo.Builder orgInfo = null;
				PMPersonalInfo.Builder personInfo = null;
				if(cerType.equals(UserCertTypeEnum.ORGANATION.getValue()) || cerType.equals(UserCertTypeEnum.PERSONAL.getValue())) {
					if(cerType.equals(UserCertTypeEnum.ORGANATION.getValue())) {//企业认证
						org = new CWVWltCertOrg();
						org.setOrgName(pbo.getOrgName());
						org.setCity(StringUtils.isBlank(pbo.getCity()) ? "" : pbo.getCity());
						org.setCorporateName(pbo.getJuridicalName());
						org.setCreatedTime(new Date());
						org.setFax(StringUtils.isBlank(pbo.getFax()) ? "" : pbo.getFax());
						org.setLicenseCode(pbo.getLicenseCode());
						org.setLicensePhotoId(pbo.getLicensePhotoId());
						org.setNational(StringUtils.isBlank(pbo.getNational()) ? "" : pbo.getNational());
						org.setOrgCode(UUIDGenerator.generate());
						org.setOrgId(org.getOrgCode());
						org.setOrgStatus(CertStatusEnum.RECEIVE.getValue());
						org.setOrgType(pbo.getOrgType());
						org.setProvince(StringUtils.isBlank(pbo.getProvince()) ? "" : pbo.getProvince());
						org.setReserved1("");
						org.setReserved2("");
						org.setStreet(StringUtils.isBlank(pbo.getStreets()) ? "" : pbo.getStreets());
						org.setTel(StringUtils.isBlank(pbo.getTel()) ? "" : pbo.getTel());
						org.setUpdatedTime(org.getCreatedTime());
						org.setUserId(user.getUserId());
						org.setWebRecordNum(StringUtils.isBlank(pbo.getWebRecordNumber()) ? "" : pbo.getWebRecordNumber());
						
						daos.wltCertOrgDao.insert(org);
						

						orgInfo = PMOrganizationInfo.newBuilder();
						orgInfo.setCity(org.getCity());
						orgInfo.setDateTime(org.getCreatedTime().getTime());
						orgInfo.setFax(org.getFax());
						orgInfo.setJuridicalName(org.getCorporateName());
						orgInfo.setLicenseCode(org.getLicenseCode());
						orgInfo.setLicensePhotoId(org.getLicensePhotoId());
						orgInfo.setNational(org.getNational());
						orgInfo.setOrgId(org.getOrgId());
						orgInfo.setOrgName(org.getOrgName());
						orgInfo.setOrgType(org.getOrgType());
						orgInfo.setProvince(org.getProvince());
						orgInfo.setStatus(org.getOrgStatus());
						orgInfo.setStreets(org.getStreet());
						orgInfo.setTel(org.getStreet());
						orgInfo.setUserCode(user.getUserCode());
						orgInfo.setWebRecordNumber(org.getWebRecordNum());
						
						ret.setOrgInfo(orgInfo);
						
						
					}else {//个人认证
						person = new CWVWltCertPer();
						person.setCreatedTime(new Date());
						person.setIdCardCode(pbo.getIdCardCode());
						person.setIdCardType(pbo.getIdCardType());
						person.setIdCardPhotoId(pbo.getIdCardPhotoId());
						person.setPerCode(UUIDGenerator.generate());
						person.setPerId(person.getPerCode());
						person.setPerStatus(CertStatusEnum.RECEIVE.getValue());
						person.setRealName(pbo.getRealName());
						person.setReserved1("");
						person.setReserved2("");
						person.setUpdatedTimd(person.getCreatedTime());
						person.setUserId(user.getUserId());
						
						daos.wltCertPerDao.insert(person);
						
						personInfo = PMPersonalInfo.newBuilder();
						personInfo.setDateTime(person.getCreatedTime().getTime());
						personInfo.setIdCardCode(person.getIdCardCode());
						personInfo.setIdCardPhotoId(person.getIdCardPhotoId());
						personInfo.setPerId(person.getPerId());
						personInfo.setRealName(person.getRealName());
						personInfo.setStatus(person.getPerStatus());
						personInfo.setUserCode(user.getUserCode());
						ret.setPerInfo(personInfo);
					}
					
					ret.setRequestNo(pbo.getRequestNo());
					ret.setErrCode(UserReturnEnum.OK.getValue());
					ret.setMsg(UserReturnEnum.OK.getName());
					
				} else {//认证类型错误

					ret.setRequestNo(pbo.getRequestNo());
					ret.setErrCode(UserReturnEnum.FAIL_ERROR_CERTIFICATION_TYPE.getValue());
					ret.setMsg(UserReturnEnum.FAIL_ERROR_CERTIFICATION_TYPE.getName());
				}
				
				
			}else {
				ret.setRequestNo(pbo.getRequestNo());
				ret.setErrCode(UserReturnEnum.FAIL_ERROR_USER_CODE.getValue());
				ret.setMsg(UserReturnEnum.FAIL_ERROR_USER_CODE.getName());
			}
			
		}else {
			ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_PARAM.getValue());
			ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_PARAM.getName());
		}
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}

}
