package com.foxconn.bannerview;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
	
	private BannerView1 banner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		banner = (BannerView1) findViewById(R.id.banner);
		ArrayList<String> imageUrlList=new ArrayList<String>();
		imageUrlList.add("adf");
		imageUrlList.add("adf");
		imageUrlList.add("adf");
		banner.setImageUrls(imageUrlList);
		banner.setAutoStart(true);
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
//		banner.stop();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
//		banner.start();
	}
}
