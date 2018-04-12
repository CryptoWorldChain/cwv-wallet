package org.brewchain.cwv.wlt.service.user;

import java.util.List;

import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddrExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAssetExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertOrg;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertOrgKey;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertPer;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltCertPerKey;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFund;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFundExample;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUserExample;
import org.brewchain.cwv.wlt.enums.CertStatusEnum;
import org.brewchain.cwv.wlt.enums.UserCertTypeEnum;
import org.brewchain.cwv.wlt.enums.UserReturnEnum;
import org.brewchain.cwv.wlt.enums.UserStatusEnum;
import org.fc.wlt.gens.Asset.PMAssetInfo;
import org.fc.wlt.gens.Asset.PMFundInfo;
import org.fc.wlt.gens.Asset.PMUserAddress;
import org.fc.wlt.gens.User.PRetCertifcationVerify;
import org.fc.wlt.gens.User.PSCertifcationVerify;
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
public class UserCertificationCheckService extends SessionModules<PSCertifcationVerify> {

	@Override
	public String getModule() {
		return PUSRModule.USR.name();
	}

	@Override
	public String[] getCmds() {
		return new String[] {PUSRCommand.CEV.name()};
	}

	public String toString() {
		return "PSUsrCheckCeritification";
	}
	
	@ActorRequire
	Daos daos;

	@Override
	public void onPBPacket(FramePacket pack, PSCertifcationVerify pbo, CompleteHandler handler) {
		final PRetCertifcationVerify.Builder ret = PRetCertifcationVerify.newBuilder();
		if(pbo != null) {
			ret.setRequestNo(pbo.getRequestNo());
			String userCode = pbo.getUserCode();
			String type = pbo.getType();
			CWVWltUserExample userExample = new CWVWltUserExample();
			userExample.createCriteria().andUserCodeEqualTo(userCode).andUserStatusEqualTo(UserStatusEnum.OK.getValue());
			Object userObj = daos.wltUserDao.selectOneByExample(userExample);
			if(userObj != null) {
				CWVWltUser user = (CWVWltUser) userObj;
				if(type.equals(UserCertTypeEnum.ORGANATION.getValue()) || type.equals(UserCertTypeEnum.PERSONAL.getValue())) {
					if(type.equals(UserCertTypeEnum.ORGANATION.getValue())) {
						//机构认证
						CWVWltCertOrgKey orgKey = new CWVWltCertOrgKey(pbo.getOrgId());
						CWVWltCertOrg org = daos.wltCertOrgDao.selectByPrimaryKey(orgKey);
						if(org != null) {
							if(org.getOrgStatus().equals(CertStatusEnum.OK.getValue())) {
								CWVWltAddrExample addrExample = new CWVWltAddrExample();
								addrExample.createCriteria().andUserIdEqualTo(user.getUserId());//TODO .andAddrStatusEqualTo("");地址的状态需要约定
								List<Object> addrObjs = daos.wltAddrDao.selectByExample(addrExample);
								if(addrObjs != null) {
									for(Object obj : addrObjs) {
										CWVWltAddr addr = (CWVWltAddr)obj;
										PMUserAddress.Builder addrInfo = PMUserAddress.newBuilder();
										CWVWltAssetExample assetExample = new CWVWltAssetExample();
										assetExample.createCriteria()
										.andUserIdEqualTo(user.getUserId())
										.andAddrIdEqualTo(addr.getAddrId());
										//TODO .andAssetStatusEqualTo(""); asset 状态需要约定
										Object assetObj = daos.wltAssetDao.selectOneByExample(assetExample);
										PMAssetInfo.Builder assetInfo = null;
										if(assetObj != null) {
											CWVWltAsset asset = (CWVWltAsset)assetObj;
											assetInfo = PMAssetInfo.newBuilder();
											assetInfo.setAlias(asset.getAssetAlias());
											assetInfo.setDataTable(asset.getAssetKeywords());
											assetInfo.setDmtCname(asset.getDmtCname());
											assetInfo.setDmtEname(asset.getDmtEname());
											assetInfo.setMetadata(asset.getMetadata());
											
											CWVWltFundExample fundExample = new CWVWltFundExample();
											fundExample.createCriteria()
											.andFundIdEqualTo(asset.getFundId());//TODO .andFundStatusEqualTo("") fund 状态需要约定
											
											Object fundObj = daos.wltFundDao.selectOneByExample(fundExample);
											if(fundObj != null) {
												CWVWltFund fund = (CWVWltFund)fundObj;
												PMFundInfo.Builder fundInfo = PMFundInfo.newBuilder();
												fundInfo.setDateTime(fund.getCreatedTime().getTime());
												fundInfo.setDmtCname(fund.getDmtCname());
												fundInfo.setDmtEname(fund.getDmtEname());
												fundInfo.setGenisDeposit(fund.getGenisDeposit());
												fundInfo.setTotalCount(fund.getTotalCount());
												fundInfo.setTurnoverCount(fund.getTurnoverCount());
												
												assetInfo.setFund(fundInfo);
											}
										}
										addrInfo.setAsset(assetInfo);
										ret.addUas(addrInfo);
									}
								}
							}
							ret.setUserCode(pbo.getUserCode());
							ret.setOrgId(org.getOrgId());
							ret.setStatus(org.getOrgStatus());
							ret.setType(type);
							ret.setRequestNo(pbo.getRequestNo());
							ret.setErrCode(UserReturnEnum.OK.getValue());
							ret.setMsg(UserReturnEnum.OK.getName());
						}else {
							
							ret.setRequestNo(pbo.getRequestNo());
							ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_CERTIFICATE.getValue());
							ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_CERTIFICATE.getName());
						}
					}else {
						//个人认证
						pbo.getPerId();
						CWVWltCertPerKey perKey = new CWVWltCertPerKey(pbo.getPerId());
						CWVWltCertPer person = daos.wltCertPerDao.selectByPrimaryKey(perKey);
						if(person != null) {
							if(person.getPerStatus().equals(CertStatusEnum.OK.getValue())) {
								CWVWltAddrExample addrExample = new CWVWltAddrExample();
								addrExample.createCriteria().andUserIdEqualTo(user.getUserId());//TODO .andAddrStatusEqualTo("");地址的状态需要约定
								List<Object> addrObjs = daos.wltAddrDao.selectByExample(addrExample);
								if(addrObjs != null) {
									for(Object obj : addrObjs) {
										CWVWltAddr addr = (CWVWltAddr)obj;
										PMUserAddress.Builder addrInfo = PMUserAddress.newBuilder();
										CWVWltAssetExample assetExample = new CWVWltAssetExample();
										assetExample.createCriteria()
										.andUserIdEqualTo(user.getUserId())
										.andAddrIdEqualTo(addr.getAddrId());
										//TODO .andAssetStatusEqualTo(""); asset 状态需要约定
										Object assetObj = daos.wltAssetDao.selectOneByExample(assetExample);
										PMAssetInfo.Builder assetInfo = null;
										if(assetObj != null) {
											CWVWltAsset asset = (CWVWltAsset)assetObj;
											assetInfo = PMAssetInfo.newBuilder();
											assetInfo.setAlias(asset.getAssetAlias());
											assetInfo.setDataTable(asset.getAssetKeywords());
											assetInfo.setDmtCname(asset.getDmtCname());
											assetInfo.setDmtEname(asset.getDmtEname());
											assetInfo.setMetadata(asset.getMetadata());
											
											CWVWltFundExample fundExample = new CWVWltFundExample();
											fundExample.createCriteria()
											.andFundIdEqualTo(asset.getFundId());//TODO .andFundStatusEqualTo("") fund 状态需要约定
											
											Object fundObj = daos.wltFundDao.selectOneByExample(fundExample);
											if(fundObj != null) {
												CWVWltFund fund = (CWVWltFund)fundObj;
												PMFundInfo.Builder fundInfo = PMFundInfo.newBuilder();
												fundInfo.setDateTime(fund.getCreatedTime().getTime());
												fundInfo.setDmtCname(fund.getDmtCname());
												fundInfo.setDmtEname(fund.getDmtEname());
												fundInfo.setGenisDeposit(fund.getGenisDeposit());
												fundInfo.setTotalCount(fund.getTotalCount());
												fundInfo.setTurnoverCount(fund.getTurnoverCount());
												
												assetInfo.setFund(fundInfo);
											}
										}
										addrInfo.setAsset(assetInfo);
										ret.addUas(addrInfo);
									}
								}
							}
							ret.setUserCode(user.getUserCode());
							ret.setType(type);
							ret.setStatus(person.getPerStatus());
							ret.setPerId(person.getPerId());
							ret.setRequestNo(pbo.getRequestNo());
							ret.setErrCode(UserReturnEnum.OK.getValue());
							ret.setMsg(UserReturnEnum.OK.getName());
						}else {
							ret.setRequestNo(pbo.getRequestNo());
							ret.setErrCode(UserReturnEnum.FAIL_ERROR_NO_CERTIFICATE.getValue());
							ret.setMsg(UserReturnEnum.FAIL_ERROR_NO_CERTIFICATE.getName());
						}
					}
				}else {
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
