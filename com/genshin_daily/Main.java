package com.genshin_daily;

import org.junit.runner.JUnitCore;

public class Main {
	
	public static void main(String[] args) {
		DriverUpdateCheck checkup = new DriverUpdateCheck();
		if(checkup.check()) {
			JUnitCore.main(new String[]{"com.genshin_daily.ClicksTest"});			
		}
	}
}
