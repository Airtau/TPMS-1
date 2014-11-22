package com.naruto.tpms.app;

import com.naruto.tpms.app.comm.Constants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
	Handler mHandler = new Handler(){
		public void dispatchMessage(android.os.Message msg) {
			switch(msg.what){
			case 1:
				Context  context = (Context) msg.obj;
				context.startService(new Intent(context, CoreBtService.class));
				Intent it = new Intent(CoreBtService.BROADCAST_RECONNECT);// ֪ͨ��̨Service�������ӣ���������ӣ�����������������
				context.sendBroadcast(it);
				break;
			}
		};
	};

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREF_NAME_SETTING, Context.MODE_PRIVATE);
		boolean isAutoBoot = sharedPreferences.getBoolean(Constants.SETTING_IS_AUTO_BOOT, true);
//		Log.d("test","test:"+isAutoBoot);
//		Toast.makeText(context, "������:"+isAutoBoot, Toast.LENGTH_LONG).show();
		if(isAutoBoot){
			Message msg = new Message();
			msg.what = 1;
			msg.obj = context;
			mHandler.sendMessageDelayed(msg, 3000);
		}
	}
 

}
