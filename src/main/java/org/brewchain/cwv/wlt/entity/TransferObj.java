package org.brewchain.cwv.wlt.entity;

import org.fc.wlt.gens.Asset.PMFundInfo;

import lombok.Data;

@Data
public class TransferObj {

	String userCode;//发起的用户代码
	String toUserCode;
	PMFundInfo target;
	Double targetAmount;
	
//	string toUserCode = 2;//接收的用户代码
//	PMFundInfo target = 3;//目标资产
//	double targetAmount = 4;//目标资产的数量
//	PMAssetInfo oldAsset = 5;//转移前资产
//	PMAssetInfo newAsset = 6;//转移后的资产
//	int64 dateTime = 7;//时间戳
}
