package org.brewchain.cwv.wlt.util;

import java.text.DecimalFormat;

public class DoubleUtil {

	public static double formatMoney(String amount) {
		String money = "";
		if (Double.valueOf(amount) >= 1)
			money = new DecimalFormat("#.00").format(Double.valueOf(amount));
		else {
			money = "0" + new DecimalFormat("#.00").format(Double.valueOf(amount));
		}

		return Double.parseDouble(money);
	}
	
	public static double formatMoney(double amount) {
		String money = "";
		double bmount = 0.00;
		DecimalFormat decimalFormat = new DecimalFormat("#0.000000");//格式化设置
		try {
			money = decimalFormat.format(amount);
			bmount = Double.parseDouble(money);
		} catch (Exception e) {
			bmount = amount;
		}
		return bmount;
	}
	
//	public static String 
	
	public static double getTotalFee(double sourceAmount) {
		return sourceAmount * 0.001;//千分之一
	}
}
