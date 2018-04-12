package org.brewchain.cwv.wlt.filter;

import java.util.Date;

import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.cwv.wlt.dao.Daos;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAccessLog;
import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAccessLogKey;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.fc.zippo.filter.FilterConfig;
import org.fc.zippo.filter.exception.FilterException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.ActWrapper;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketFilter;
import onight.tfw.otransio.api.SimplePacketFilter;
import onight.tfw.otransio.api.beans.FramePacket;

@iPojoBean
@Provides(specifications = { PacketFilter.class, ActorService.class }, strategy = "SINGLETON")
@Slf4j
@Data
public class SessionFilter extends SimplePacketFilter implements PacketFilter, ActorService {
	private final static String REGISTER_URL = "/cwv/usr/pbreg.do";
	private final static String CREATE_ASSET_URL = "/fbs/ast/pbona.do";
	private final static String TRANSFER_2_ANOTHER_URL = "/cwv/trs/pbuat.do";
	private final static String QUERY_ASSET_URL = "/cwv/qry/pbgoa.do";
	private final static String QUERY_TRANSFER_URL = "/fbs/qry/pbgat.do";
	private final static String BCWWW_LOGIN = "/fbs/bcw/pblin.do";
	private final static String QUERY_ADDRESS_URL = "/fbs/qry/pbgua.do";
	
	@ActorRequire
	Daos daos;

	ObjectMapper mapper = new ObjectMapper();

	@Override
	public void destroy(final FilterConfig filterConfig) throws FilterException {
		super.destroy(filterConfig);
	}

	@Override
	public int getPriority() {
		return 10;
	}

	@Override
	public void init(final FilterConfig filterConfig) throws FilterException {
		super.init(filterConfig);
	}

	@Override
	public String getSimpleName() {
		return "session_filter";
	}

	@Override
	public boolean postRoute(final ActWrapper actor, final FramePacket pack, final CompleteHandler handler)
			throws FilterException {
		String pathUrl = pack.getHttpServerletRequest().getRequestURI();
		if(!pathUrl.equals(BCWWW_LOGIN)) {
			ObjectNode body = null;
			String respStr = (String) pack.getFbody();
			if (respStr.length() > 255) {
				respStr = respStr.substring(0, 255);
			}
			try {
				body = (ObjectNode) mapper.readTree(new String(pack.getBody()));
			} catch (Exception e) {
				e.printStackTrace();
				throw new UnknownError("unknown error ...");
			}
			String requestNo = body.get("requestNo").toString();
			requestNo = requestNo.substring(1, requestNo.length() - 1);
			CWVWltAccessLogKey accessKey = new CWVWltAccessLogKey(requestNo);
			CWVWltAccessLog oldAccess = daos.wltAccessLogDao.selectByPrimaryKey(accessKey);
			if (oldAccess != null) {
				Date current = new Date();
				long start = oldAccess.getCreatedTime().getTime();
				long end = current.getTime();
				int cost = (int) (end - start);
				oldAccess.setCostMs(cost);
				oldAccess.setUpdatedTime(current);
				oldAccess.setRespStr(respStr);
				daos.wltAccessLogDao.updateByPrimaryKey(oldAccess);
				
			}
		}
		return super.postRoute(actor, pack, handler);
	}

	@Override
	public boolean preRoute(final ActWrapper actor, final FramePacket pack, final CompleteHandler handler)
			throws FilterException {
		String pathUrl = pack.getHttpServerletRequest().getRequestURI();
		String requestNo = "";

		if (pack != null && pack.getBody() != null) {
//			System.out.println("the pathUrl is ::: " + pathUrl);
			boolean open_the_door = openUrl(pathUrl);
			if(open_the_door == false) {
				return false;
			}
			ObjectNode node = null;
			try {
				node = (ObjectNode) mapper.readTree(new String(pack.getBody()));
			} catch (Exception e) {
				throw new UnknownError("unknown error ...");
			}

			if(!pathUrl.equals(BCWWW_LOGIN)) {
				if (!node.has("requestNo")) {
					throw new NullPointerException("requestNo is null, please specify an unique requestNo while you asking the api");
				}
				
				requestNo = node.get("requestNo").toString();
				requestNo = requestNo.substring(1, requestNo.length() - 1);
				
				CWVWltAccessLogKey logKey = new CWVWltAccessLogKey(requestNo);
				CWVWltAccessLog accessLog = daos.wltAccessLogDao.selectByPrimaryKey(logKey);
				if(accessLog != null) {
					throw new IllegalArgumentException("requestNo duplicate!! please use a new requestNo and try again...");
				}
				
				// insert into access_log
				CWVWltAccessLog access = new CWVWltAccessLog();
				access.setAccessId(requestNo);
				access.setCostMs(0);
				access.setCreatedTime(new Date());
				access.setPbAction(pathUrl);
				access.setReqStr(node.toString());
				access.setRequestNo(requestNo);
				access.setReserved1("");
				access.setReserved2("");
				access.setRespStr("");
				access.setUpdatedTime(access.getCreatedTime());
				access.setUserId("");
				try {
					daos.wltAccessLogDao.insert(access);
				} catch (Exception e) {
					log.warn("insert access log error");
					log.info(e.getMessage());
				}
			}
		} else {
			throw new NullPointerException("no params...");
		}

		return true;
	}
	
	private boolean openUrl(String pathUrl) {
		if(pathUrl.equals(REGISTER_URL)) {
			return true;
		}
		if(pathUrl.equals(CREATE_ASSET_URL)) {
			return true;
		}
		if(pathUrl.equals(TRANSFER_2_ANOTHER_URL)) {
			return true;
		}
		if(pathUrl.equals(QUERY_ASSET_URL)) {
			return true;
		}
		if(pathUrl.equals(QUERY_TRANSFER_URL)) {
			return true;
		}
		if(pathUrl.equals(BCWWW_LOGIN)) {
			return true;
		}
		if(pathUrl.equals(QUERY_ADDRESS_URL)) {
			return true;
		}
		return false;
	}
}