package org.brewchain.cwv.wlt.filter;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.entity.TestCudEntity;
import org.fc.zippo.filter.exception.FilterException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.outils.serialize.JsonSerializer;
@iPojoBean
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Slf4j
@Data
public class FilterHelper implements  ActorService{
	
	public final static int SES_TIMTOUT = 60*20;//session超时时间
	
	public final static String API_SYS_CUD = "/fbs/sys/pbcud.do";   //测试增删改查
	public final static String API_REGISTER = "/fbs/usr/pbreg.do";   //注册接口
	
	
	
	@ActorRequire
	Daos daos;
	
	//防止相互引用死循环
	@Override
	public String toString() {
		return "filterservice:";
	}
	
	public Boolean checkNull(byte[] body, String apiType) {
		if(apiType.equals(API_SYS_CUD)) {
			apiSysCudCheckNull(body);
		}
		
		return true;
	}
	
	public Boolean apiSysCudCheckNull(byte[] body) {
		TestCudEntity cudEntity = JsonSerializer.getInstance().deserialize(body, TestCudEntity.class);
		if(StringUtils.isBlank(cudEntity.getRequestNo())) {
			throw new NullPointerException("please specify a unique requestNo like : {\"requestNo\":\"29201929392932\"}");
		}
		
		return true;
	}
	
	
	
	
	public int indexOfByNumber(String str,int number){
		int index = 0;
		for(int i = 0; i < number; i++){
		  index = str.indexOf("/");
		  str = str.replaceFirst("/", "a");
		 }
		 return  index;
	}
	
	public String getCostMs(Date date) {
		return new Date().getTime() - date.getTime() + "";
	}
	public void checkTimeout(Date temtime){
		Date nowtime =  new Date();
		int time = (int)(nowtime.getTime() - temtime.getTime())/1000;
		if(time>SES_TIMTOUT){
			throw new FilterException("filter: [session timeout...]");
		};
	}
}
