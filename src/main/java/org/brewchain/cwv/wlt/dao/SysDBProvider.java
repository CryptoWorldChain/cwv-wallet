package org.brewchain.cwv.wlt.dao;

import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.fc.hzq.orcl.impl.HZQOrclStoreProvider;
import org.osgi.framework.BundleContext;

import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.ojpa.ordb.SubDBProvider;

@iPojoBean
@Provides(specifications = {  StoreServiceProvider.class,ActorService.class }, strategy = "SINGLETON")
public class SysDBProvider extends SubDBProvider<HZQOrclStoreProvider>  {

	public SysDBProvider(BundleContext bundleContext) {
		super(bundleContext);
	}

	@Override
	public String[] getContextConfigs() {
		return new String[] { "/SpringContext-daoConfig-wlt.xml" };
	}
	@Validate
	public void startup() {
		 super.startup();
	}
}
