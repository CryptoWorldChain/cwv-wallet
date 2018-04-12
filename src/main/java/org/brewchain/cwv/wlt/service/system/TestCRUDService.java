package org.brewchain.cwv.wlt.service.system;

import java.util.Date;
import java.util.List;

import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUser;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltUserExample;
import org.fc.wlt.gens.System.PRetTestCUD;
import org.fc.wlt.gens.System.PSTestCUD;
import org.fc.wlt.gens.System.PSYSCommand;
import org.fc.wlt.gens.System.PSYSModule;

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


// http://localhost:8000/usr/pbreg.do?fh=REGUSR0000000J00&resp=bd&bd={"username":"aaa","userid":"1111"}

@NActorProvider
@Slf4j
@Data
public class TestCRUDService extends SessionModules<PSTestCUD> {

	
	@ActorRequire
	Daos daos;
	
	@Override
	public String[] getCmds() {		
		return new String[] { PSYSCommand.CUD.name() };
	}

	@Override
	public String getModule() {
		return PSYSModule.SYS.name();
	}
	public String toString(){
		return "PSSYSTestCRUD";
	}
	
	@Override
	public void onPBPacket(final FramePacket pack, PSTestCUD pbo, final CompleteHandler handler) {
		final PRetTestCUD.Builder ret = PRetTestCUD.newBuilder();
		try {
			//增加
			if(pbo != null) {
				if(pbo.getActionType() == 1) {
					CWVWltUser user = new CWVWltUser();
					user.setUserName(pbo.getUsername());
					user.setUserPasswd(pbo.getPasswd());
					user.setUserId(UUIDGenerator.generate());
					user.setUserCode(user.getUserId());
					user.setCheckPhone("");
					user.setCheckEmail("");
					user.setReserved1("");
					user.setReserved2("");
					user.setUserStatus("1");
					user.setCreatedTime(new Date());
					user.setUpdatedTime(user.getCreatedTime());
					int insertRet = daos.wltUserDao.insert(user);
					if(insertRet == 1) {
						System.out.println("add successfully ...");
					}
					
				}else if(pbo.getActionType() == 2) {
					//删除
					CWVWltUserExample delExample = new CWVWltUserExample();
					delExample.createCriteria().andUserNameEqualTo(pbo.getUsername()).andUserPasswdEqualTo(pbo.getPasswd());
					CWVWltUser rec = new CWVWltUser();
					rec.setUserStatus("0");//删除不做物理删除，修改状态做逻辑删除
					int delRet = daos.wltUserDao.updateByExampleSelective(rec, delExample);
					if(delRet == 1) {
						System.out.println("update successfully....");
					}
				}else if(pbo.getActionType() == 3) {
					//修改
					CWVWltUserExample updExample = new CWVWltUserExample();
					updExample.createCriteria().andUserNameEqualTo(pbo.getUsername()).andUserPasswdEqualTo(pbo.getPasswd());
					CWVWltUser rec = new CWVWltUser();
					rec.setUserPasswd("123123123");;
					int updRet = daos.wltUserDao.updateByExampleSelective(rec, updExample);
					
					if(updRet == 1) {
						System.out.println("update successfully....");
					}
				}else if(pbo.getActionType() == 4) {
					//查询
					
					CWVWltUserExample qryExample = new CWVWltUserExample();
					qryExample.createCriteria().andUserNameEqualTo(pbo.getUsername());
					List<Object> lists = daos.wltUserDao.selectByExample(qryExample);
					if(lists != null && !lists.isEmpty()) {
						for(Object obj : lists) {
							CWVWltUser user = (CWVWltUser)obj;
							System.out.println("user info is ::: username : " + user.getUserName() + "; password : " + user.getUserPasswd());
						}
					}
				}
			}
			
		} catch (NullPointerException e) {
			System.out.println("the error message is : " + e.getMessage());
		} catch (Exception e) {
			System.out.println("the error message is : " + e.getMessage());
			e.printStackTrace();
		}
		ret.setErrCode("000000");
		ret.setErrMsg("success");
		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}

}