//package org.brewchain.cwv.wlt.service.query;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//
//import org.apache.commons.lang3.StringUtils;
//import org.brewchain.cwv.wlt.dao.Daos;
//import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddr;
//import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAddrExample;
//import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAsset;
//import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltAssetExample;
//import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFund;
//import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltFundKey;
//import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltMonitor;
//import org.brewchain.cwv.wlt.dbgens.wlt.entity.CWVWltMonitorExample;
//import org.brewchain.cwv.wlt.enums.AssetReturnEnum;
//import org.brewchain.cwv.wlt.enums.TransferReturnEnum;
//import org.brewchain.cwv.wlt.enums.UserReturnEnum;
//import org.brewchain.cwv.wlt.helper.EthereumJHelper;
//import org.brewchain.cwv.wlt.util.DoubleUtil;
//import org.fc.wlt.gens.Asset.PMAssetInfo;
//import org.fc.wlt.gens.Asset.PMFullAddress;
//import org.fc.wlt.gens.Asset.PMFundInfo;
//import org.fc.wlt.gens.Query.PQRYCommand;
//import org.fc.wlt.gens.Query.PQRYModule;
//import org.fc.wlt.gens.Query.PRetGetAsset;
//import org.fc.wlt.gens.Query.PSGetAsset;
//import org.fc.wlt.gens.Transfer.PRetAssetTransfer;
//import org.fc.wlt.gens.Transfer.PSAssetTransfer;
//
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import onight.oapi.scala.commons.SessionModules;
//import onight.osgi.annotation.NActorProvider;
//import onight.tfw.async.CompleteHandler;
//import onight.tfw.ntrans.api.annotation.ActorRequire;
//import onight.tfw.otransio.api.PacketHelper;
//import onight.tfw.otransio.api.beans.FramePacket;
//import onight.tfw.outils.serialize.JsonSerializer;
//
//@NActorProvider
//@Slf4j
//@Data
//public class MonitorAssetService extends SessionModules<PSGetAsset> {
//
//	@ActorRequire
//	Daos daos;
//	
//	@ActorRequire
//	EthereumJHelper ethHelper;
//	
//	@Override
//	public String[] getCmds() {		
//		return new String[] { PQRYCommand.GOA.name() };
//	}
//
//	@Override
//	public String getModule() {
//		return PQRYModule.QRY.name();
//	}
//	public String toString(){
//		return "MonitorAssetService";
//	}
//	ThreadPoolExecutor exec = new ThreadPoolExecutor(20, 100, 60, TimeUnit.SECONDS,
//			new LinkedBlockingQueue<Runnable>());
//	@Override
//	public void onPBPacket(final FramePacket pack, PSGetAsset pbo, final CompleteHandler handler) {
//		final PRetGetAsset.Builder ret = PRetGetAsset.newBuilder();
//		
//		//查询cwv地址监听表，开启多线程，自动化操作充值
//		CWVWltMonitorExample example = new CWVWltMonitorExample();
//		//TODO 时间上应该有限制，创建监听后固定时间为充值即为失效
//		example.createCriteria().andStatusEqualTo("0");
//		List<Object> addrsMonitor = daos.wltMonitorDao.selectByExample(example);
//		
//		for(Object obj : addrsMonitor){
//			final CWVWltMonitor wltMonitor = (CWVWltMonitor) obj;
//			final BigDecimal baseBalance = wltMonitor.getBaseBalance();
//			final String addrs = wltMonitor.getAddress();
//			exec.execute(new Runnable() {
//				@Override
//				public void run() {
//					try {
//						//TODO 调用eth查询账户余额接口
//						Map<String,Object> ethData = ethHelper.checkWalletETH(addrs);
//						//使用查询结果中的余额与监听中的基础余额比较，超出即为已充值
//						BigDecimal balance = baseBalance.add(new BigDecimal("1"));
//						
//						//如果监听地址余额大于基础余额，则满足充值规则
//						if(balance.compareTo(baseBalance)>0){
//							//移除监听
//							wltMonitor.setStatus("1");
//							daos.wltMonitorDao.updateByPrimaryKey(wltMonitor);
//							//TODO 暂时不将用户cwv账户余额转移到大钱包，如果转入，需提前告知用户，充值后，需扣除一部分管理费，用作转到大钱包使用
//							
////							PSAssetTransfer.Builder assetTransfer = PSAssetTransfer.newBuilder();
////							PRetAssetTransfer.Builder ret = PRetAssetTransfer.newBuilder();
//							
//						}
//						
//					} catch (Exception e) {
//						// unlockMrd(mrdRecord);
//						String msg = "";
//						e.printStackTrace();
//						log.error("异常", e);
//					} finally {
//
//					}
//				}
//			});
//		}
//
//		pack.setFbody(JsonSerializer.formatToString(ret.toString()));
//		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
//	}
//
//}