package org.brewchain.cwv.wlt.service.query;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertOrg;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertOrgExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertPer;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertPerExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUserExample;
import org.fc.wlt.gens.Query.PQRYCommand;
import org.fc.wlt.gens.Query.PQRYModule;
import org.fc.wlt.gens.Query.PRetGetUserCertifcation;
import org.fc.wlt.gens.Query.PSGetUserCertifcation;
import org.fc.wlt.gens.User.PMOrganizationInfo;
import org.fc.wlt.gens.User.PMPersonalInfo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.JsonSerializer;

// http://localhost:8000/usr/pbreg.do?fh=REGUSR0000000J00&resp=bd&bd={"username":"aaa","userid":"1111"}

@NActorProvider
@Slf4j
@Data
public class QueryUserCertificationService extends SessionModules<PSGetUserCertifcation> {

	@ActorRequire
	Daos daos;

	@Override
	public String[] getCmds() {
		return new String[] { PQRYCommand.GUC.name() };
	}

	@Override
	public String getModule() {
		return PQRYModule.QRY.name();
	}

	public String toString() {
		return "QueryUserCertificationService";
	}

	@Override
	public void onPBPacket(final FramePacket pack, PSGetUserCertifcation pbo, final CompleteHandler handler) {
		final PRetGetUserCertifcation.Builder ret = PRetGetUserCertifcation.newBuilder();
		String userid= null;
		ret.setRequestNo(pbo.getRequestNo());
		ret.setPageNo(pbo.getPageNo());
		ret.setPageSize(pbo.getPageSize());
		if(StringUtils.isNotBlank(pbo.getUserCode())) {
			CWVWltUserExample userExample = new CWVWltUserExample();
			userExample.createCriteria().andUserCodeEqualTo(pbo.getUserCode());
			
			List<Object> userObjList = daos.wltUserDao.selectByExample(userExample);
			
			if(userObjList == null || userObjList.size() == 0) {
				ret.setErrCode("01").setMsg("未找到用户");
				handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
				return;
			}
			if(userObjList.size() > 1) {
				ret.setErrCode("02").setMsg("找到多个用户");
				handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
				return;
			}
			if(userObjList.get(0) == null) {
				ret.setErrCode("01").setMsg("未找到用户");
				handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
				return;
			}
			CWVWltUser user = (CWVWltUser)userObjList.get(0);
			if(user == null || StringUtils.isBlank(user.getUserId())) {
				ret.setErrCode("01").setMsg("未找到用户");
				handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
				return;
			}
			userid = user.getUserId();
		}
		
		CWVWltCertOrgExample wltCertOrgExample = new CWVWltCertOrgExample();
		CWVWltCertPerExample wltCertPerxample = new CWVWltCertPerExample();
		
		CWVWltCertOrgExample.Criteria wltCertOrgCriteria = wltCertOrgExample.createCriteria();
		CWVWltCertPerExample.Criteria wltCertPerCriteria = wltCertPerxample.createCriteria();
		if(StringUtils.isNotBlank(userid)) {
			wltCertOrgCriteria.andUserIdEqualTo(userid);
			wltCertPerCriteria.andUserIdEqualTo(userid);
		}
		if(StringUtils.isNotBlank(pbo.getType())) {
			wltCertOrgCriteria.andOrgTypeEqualTo(pbo.getType());
			wltCertPerCriteria.andIdCardTypeEqualTo(pbo.getType());
		}
		
		//wltCertOrgExample.createCriteria().andUserIdEqualTo(pbo.getUserCode());
		wltCertOrgExample.setLimit(pbo.getPageSize());
		wltCertPerxample.setOffset((pbo.getPageNo()-1)*pbo.getPageSize());
		
		wltCertOrgExample.setLimit(pbo.getPageSize());
		wltCertPerxample.setOffset((pbo.getPageNo()-1)*pbo.getPageSize());
		
		List<Object> certOrgs = daos.wltCertOrgDao.selectByExample(wltCertOrgExample);
		List<Object> certPers = daos.wltCertPerDao.selectByExample(wltCertPerxample);
		
		
		if (certOrgs != null && certOrgs.size() != 0 ) {
			//个人
			
		
			for (Object obj : certOrgs) {
				PMOrganizationInfo.Builder orgInfo = PMOrganizationInfo.newBuilder();
				CWVWltCertOrg certOrg = (CWVWltCertOrg) obj;
				orgInfo.setCity(certOrg.getCity());
				orgInfo.setFax(certOrg.getFax());
				orgInfo.setNational(certOrg.getNational());
				orgInfo.setProvince(certOrg.getProvince());
				orgInfo.setLicenseCode(certOrg.getLicenseCode());
				orgInfo.setLicensePhotoId(certOrg.getLicensePhotoId());
				orgInfo.setStreets(certOrg.getStreet());
				orgInfo.setTel(certOrg.getTel());
				orgInfo.setWebRecordNumber(certOrg.getWebRecordNum());
				
				ret.addOrgInfo(orgInfo);
			}
		}
		if(certPers != null && certPers.size() != 0) {
			
			for(Object obj2 : certPers) {
				if(obj2 == null) {
					continue;
				}
				
				CWVWltCertPer certPer = (CWVWltCertPer) obj2;
				PMPersonalInfo.Builder perInfo = PMPersonalInfo.newBuilder();
				
				perInfo.setDateTime(certPer.getCreatedTime().getTime());
				perInfo.setIdCardCode(certPer.getIdCardCode());
				perInfo.setRealName(certPer.getRealName());
				perInfo.setIdCardPhotoId(certPer.getIdCardPhotoId());
				perInfo.setPerId(certPer.getPerId());
				perInfo.setStatus(certPer.getPerStatus());
				
				ret.addPerInfo(perInfo);
				
			}
			
		}
		
		
		ret.setErrCode("0").setMsg("success");
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}

}