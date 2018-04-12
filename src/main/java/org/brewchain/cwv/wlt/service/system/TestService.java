package org.brewchain.cwv.wlt.service.system;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.proxy.IActor;

@Component
@Instantiate
@Provides(specifications = { IActor.class, ActorService.class })
@Data
@Slf4j
public class TestService implements ActorService, IActor {

	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "UNSPPORT");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "UNSPPORT");
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/octet-stream");
		response.getOutputStream().write("yes".getBytes("utf-8"));
		
	}

	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "UNSPPORT");
	}

	@Override
	public String[] getWebPaths() {
		return new String[] { "/sys/test.do" };
	}

}
