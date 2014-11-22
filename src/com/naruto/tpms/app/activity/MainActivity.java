package com.naruto.tpms.app.activity;

import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;

import com.naruto.tpms.app.BTService;
import com.naruto.tpms.app.CoreBtService;
import com.naruto.tpms.app.R;
import com.naruto.tpms.app.adapter.TireWatchAdapter;
import com.naruto.tpms.app.bean.TireData;
import com.naruto.tpms.app.comm.AppManager;
import com.naruto.tpms.app.comm.Commend;
import com.naruto.tpms.app.comm.Constants;
import com.naruto.tpms.app.comm.util.CommUtil;
import com.naruto.tpms.app.weight.LoadingDialog;

@SuppressLint("HandlerLeak")
public class MainActivity extends BaseActivity implements OnItemClickListener, OnClickListener {

	public static final int MSG_AUTO_REFRESH_VIEW = 123;

	private TextView[] tv_alarm_arr = new TextView[4];// ��ʾ�澯
	// private RelativeLayout[] rl_alarm_arr = new RelativeLayout[4];
	private LoadingDialog mLoadingDialog;// ������Dialog...
	private GridView gv_tireData;// ��ʾ����
	private DialogInterface menuDialog;// ѡ��˵�
	private ImageView iv_title_left;// �������󰲽�
	private Button btn_title_left;// �������󰴼�
	private ImageButton ib_mute;// ��������

	// ������Ϣ
	private SharedPreferences sharedPreferences;
	private String defaultTitle;// ��������
	private TireData[] tireDataArr = new TireData[4];// 4����̥(����Ϊ��ǰ����ǰ������Һ�)
	private TireWatchAdapter mGridAdapter;// ����������
	private int pressureUnitIndex, temperatureUnitIndex;// ѹ����λ���¶ȵ�λ���
	private boolean isAlarmOn;// �澯�Ƿ��ѿ���

	private Handler mHandler = new Handler() {
		@Override
		public void dispatchMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_AUTO_REFRESH_VIEW:
				mHandler.removeMessages(MSG_AUTO_REFRESH_VIEW);
				refreshView();
				mHandler.sendEmptyMessageDelayed(MSG_AUTO_REFRESH_VIEW, 1000);
				break;
			case BTService.MSG_TOAST:
				CommUtil.showTips(R.drawable.tips_warning, String.valueOf(msg.obj));
				break;
			case BTService.MSG_CONNECTED: {
				setTitle(defaultTitle + getResources().getString(R.string.msg_state_connected));
				mLoadingDialog.dismiss();
				btn_title_left.setVisibility(View.VISIBLE);

				// ��ʼ����̥����
				for (int i = 0; i < tireDataArr.length; i++) {
					tireDataArr[i] = new TireData();
					tireDataArr[i].tireNum = Constants.TIRE_NUMS[i];
				}
				// ���Ͳ�ѯ��̥id��ָ��
				queryTireId();
			}
				break;
			case BTService.MSG_CONNECTING: {
				mLoadingDialog.show();
				setTitle(defaultTitle + getResources().getString(R.string.msg_state_connecting));
				mLoadingDialog.setText(getResources().getString(R.string.msg_connecting));
			}
				break;
			case BTService.MSG_CONNECTED_FAIL: {
				setTitle(defaultTitle + getResources().getString(R.string.msg_state_connect_fail));
				CommUtil.showTips(R.drawable.tips_error, R.string.tips_connect_fail);
				mHandler.sendEmptyMessage(MSG_AUTO_REFRESH_VIEW);
//				btn_title_left.setVisibility(View.INVISIBLE);
				mLoadingDialog.dismiss();
			}
				break;
			case BTService.MSG_CONNECTED_LOST: {
				setTitle(defaultTitle + getResources().getString(R.string.msg_state_connect_lost));
//				btn_title_left.setVisibility(View.INVISIBLE);
				tireDataArr[0] = tireDataArr[1] = tireDataArr[2] = tireDataArr[3] = null;
				mHandler.removeMessages(MSG_AUTO_REFRESH_VIEW);
				refreshView();
				CommUtil.showTips(R.drawable.tips_error, R.string.tips_connect_lost);
				mLoadingDialog.dismiss();
			}
				break;
			case BTService.MSG_WRITE:
				break;
			case BTService.MSG_READ:
				byte[] report = (byte[]) msg.obj;
//				 Log.d("tag", "���յ�����Ϣ:" + Arrays.toString(report) + "ʮ������:" +
//				 CommUtil.byte2HexStr(report));

				// ��֤��Ϣ����
				if (report.length < 3) {
					return;
				}
				// ��֤��Ϣ�̶�λ
				if (report[0] != (byte) 0x55 || report[1] != (byte) 0xAA) {
					return;
				}
				// ����У��λ(��̥id��ѯ�޼���λ)
				if (report[2] != 0x07) {
					byte checkByte = CommUtil.getCheckByte(report, 0, report.length - 2);// ���У��λ
					if (checkByte != report[report.length - 1]) {
						// Log.w("tag", "У��λ����ȷ,��ǰУ��λ=" + report[report.length -
						// 1] + ",��ȷУ��λӦ=" + checkByte);
						return;
					}
				}

				// ������Ϣ
				// ״̬�ϱ�
				// 1.08��ʾ״̬�ϱ�
				if (report[2] == 0x08) {
					int index = CommUtil.getTireIndexByTireNum(report[3]);
					if (index != -1 && tireDataArr[index] != null) {
						tireDataArr[index].lastUpdateTime = System.currentTimeMillis();
						tireDataArr[index].pressure = Integer.parseInt(Integer.toHexString(report[4] & 0XFF), 16) * 3.44;
						tireDataArr[index].temperature = Integer.parseInt(Integer.toHexString(report[5] & 0XFF), 16) - 50;
						String status = CommUtil.byteToBinaryString(report[6]);
						tireDataArr[index].isSignalError = '1' == status.charAt(2);
						tireDataArr[index].isLowBattery = '1' == status.charAt(3);
						tireDataArr[index].isLeak = '1' == status.charAt(4);
					}
					mHandler.sendEmptyMessage(MSG_AUTO_REFRESH_VIEW);
					// 2.IDѧϰ����
				} else if (report[2] == 0x06) {
					switch (report[3]) {
					case 0x10:
						// Log.d("tag", "��ʼIDѧϰ:" + report[4]);
						break;
					case 0x18:
						// Log.d("tag", "IDѧϰ�ɹ�:" + report[4]);
						queryTireId();
						int index = CommUtil.getTireIndexByTireNum(report[4]);
						if (index > -1) {
							String data = getResources().getString(R.string.msg_tire_study_success);
							CommUtil.showTips(R.drawable.tips_smile, String.format(data, Constants.TIRE_NAMES[index]));
						}
						break;
					case 0x30:
						// Log.d("tag", "��̥�����ɹ�:" + report[4]);
						queryTireId();
						// CommUtil.showTips(R.drawable.tips_smile,
						// R.string.msg_tire_switch_success);
						break;
					case 0x40:
						break;
					}
					// ��ѯ�ĸ���������ID���
				} else if (report[2] == 0x07) {
					byte[] idByte = new byte[] { report[4], report[5], report[6] };
					// Log.d("tag", "ID��ѯ���:" + report[3] + "'id=" +
					// CommUtil.byte2HexStr(idByte));
					int index = CommUtil.getTireIndexByTireNum(report[3]);
					if (index != -1 &&index<tireDataArr.length&& tireDataArr[index] != null) {
						tireDataArr[index].tireId = idByte;
					}
					
					mHandler.sendEmptyMessage(MSG_AUTO_REFRESH_VIEW);
				}
				break;
			}

		};

	};

	private BroadcastReceiver myReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// Ϊ�˷��㣬���﷢����Ϣֱ��ʹ����BTService��msg what
			if (CoreBtService.BROADCAST_CONNECTING.equals(action)) {
				Message msg = mHandler.obtainMessage(BTService.MSG_CONNECTING);
				mHandler.sendMessage(msg);
			} else if (CoreBtService.BROADCAST_CONNECTED.equals(action)) {
				Message msg = mHandler.obtainMessage(BTService.MSG_CONNECTED);
				BluetoothDevice device = (BluetoothDevice) msg.obj;
				msg.obj = device;
				mHandler.sendMessage(msg);
			} else if (CoreBtService.BROADCAST_CONNECTFAIL.equals(action)) {
				Message msg = mHandler.obtainMessage(BTService.MSG_CONNECTED_FAIL);
				mHandler.sendMessage(msg);
			} else if (CoreBtService.BROADCAST_CONNECTLOST.equals(action)) {
				Message msg = mHandler.obtainMessage(BTService.MSG_CONNECTED_LOST);
				mHandler.sendMessage(msg);
			} else if (CoreBtService.BROADCAST_READ.equals(action)) {
//				Log.d("MainActivity","�յ���ȡ���ݵĹ㲥");
				byte[] data = intent.getByteArrayExtra("data");
				Message msg = mHandler.obtainMessage(BTService.MSG_READ);
				msg.obj = data;
				mHandler.sendMessage(msg);
			} else if (CoreBtService.BROADCAST_TOAST.equals(action)) {
				String data = intent.getStringExtra("data");
				Message msg = mHandler.obtainMessage(BTService.MSG_TOAST);
				msg.obj = data;
				mHandler.sendMessage(msg);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Log.d("tag", "onCreate");
		setContentView(R.layout.activity_main);
		initView();
		initData();
	}

	/**
	 * ����������ʾ��ͼ
	 */
	protected void refreshView() {
//		 Log.d("debug", "ˢ������");
		// �澯����
		boolean isAnyAlarm = false;// �Ƿ��и澯��Ϣ
		for (int i = 0; i < tireDataArr.length; i++) {
			TireData td = tireDataArr[i];
			String alarmStr = null;
			if (td == null) {
				// Log.d("debug", i + "  �����ı�:��");
				tv_alarm_arr[i].setText("");
				continue;
			}
			// �ϱ�ʱ�䳬��10���򲻴���
			if (System.currentTimeMillis() - td.lastUpdateTime <= 10000) {
				StringBuilder sb = new StringBuilder();
				if (td.isLeak) {
					sb.append(getString(R.string.alarm_leak));
					sb.append(",");
				}
				if (td.isLowBattery) {
					sb.append(getString(R.string.alarm_low_battery));
					sb.append(",");
				}
				if (td.isSignalError) {
					sb.append(getString(R.string.alarm_signal_error));
					sb.append(",");
				}
				if (td.pressure > mGridAdapter.highPressure) {
					sb.append(getString(R.string.alarm_high_pressure));
					sb.append(",");
				}
				if (td.pressure < mGridAdapter.lowPressure) {
					sb.append(getString(R.string.alarm_low_pressure));
					sb.append(",");
				}
				if (td.temperature > mGridAdapter.highTemprature) {
					sb.append(getString(R.string.alarm_high_temprature));
					sb.append(",");
				}
				if (sb.length() > 0) {
					alarmStr = sb.substring(0, sb.length() - 1);
				}
			}
			td.haveAlarm = (alarmStr != null);
			isAnyAlarm = isAnyAlarm || td.haveAlarm;
			String curAlarmStr = tv_alarm_arr[i].getText().toString();
			// Log.d("debug", i + "��ǰ�ı�:" + curAlarmStr);
			if (alarmStr != null) {
				if (!alarmStr.equals(curAlarmStr)) {
					// Log.d("debug", i + "  �����ı�:" + alarmStr);
					tv_alarm_arr[i].setText(alarmStr);
				}
			} else if (!TextUtils.isEmpty(curAlarmStr)) {
				// Log.d("debug", i + "  �����ı�:��");
				tv_alarm_arr[i].setText(alarmStr);
			}
			// rl_alarm_arr[i].setBackgroundDrawable(td.haveAlarm?tire_alarm_bg:tire_normal_bg);
		}
		mGridAdapter.notifyDataSetChanged();
	}

	@SuppressLint("UseSparseArrays")
	@Override
	protected void onResume() {
		super.onResume();
		// Log.d("tag", "onResume");
		pressureUnitIndex = sharedPreferences.getInt(Constants.SETTING_PRESSURE_UNIT, 1);
		temperatureUnitIndex = sharedPreferences.getInt(Constants.SETTING_TEMPERATURE_UNIT, 0);
		isAlarmOn = sharedPreferences.getBoolean(Constants.SETTING_IS_ALARM_ON, true);
		ib_mute.setImageResource(isAlarmOn ? R.drawable.mute_on : R.drawable.mute_off);
		mGridAdapter.pressureUnitIndex = pressureUnitIndex;
		mGridAdapter.temperatureUnitIndex = temperatureUnitIndex;

		mGridAdapter.highPressure = sharedPreferences.getInt(Constants.SETTING_HIGH_PRESSURE, 5) * Constants.HIGH_PRESSURE_STEUP
				+ Constants.HIGH_PRESSURE_MIN;
		mGridAdapter.lowPressure = sharedPreferences.getInt(Constants.SETTING_LOW_PRESSURE, 0) * Constants.LOW_PRESSURE_STEUP
				+ Constants.LOW_PRESSURE_MIN;
		mGridAdapter.highTemprature = sharedPreferences.getInt(Constants.SETTING_HIGH_TEMPRATURE, 5) * Constants.HIGH_TEMPRATURE_STEUP
				+ Constants.HIGH_TEMPRATURE_MIN;
		boolean keepScreenOn = sharedPreferences.getBoolean(Constants.SETTING_IS_SCREEN_KEEP, true);
		if (keepScreenOn) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			// this.wakeLock.acquire();
		}
		mHandler.sendEmptyMessage(MSG_AUTO_REFRESH_VIEW);
		doConnect();
	}

	@Override
	protected void onStart() {
		// Log.d("tag", "onStart");
		super.onStart();
	}

	@Override
	protected void onPause() {
		// Log.d("tag", "onPause");
		super.onPause();
		// this.wakeLock.release();
		mHandler.removeMessages(MSG_AUTO_REFRESH_VIEW);
		refreshView();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		 Log.d("tag", "onDestroy");
		unregisterReceiver(myReceiver);
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		CommUtil.showTips(R.drawable.tips_warning, R.string.msg_run_in_background);
		super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.addSubMenu(R.string.exit);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		exit();
		return true;
	}

	private void exit() {
//		sendBroadcast(new Intent(CoreBtService.BROADCAST_DISCONNECT));
		AppManager.getAppManager().AppExit(this);
		stopService(new Intent(this, CoreBtService.class));
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// Log.d("tag", "onConfigurationChanged");
		mGridAdapter.setOrientation(getResources().getConfiguration().orientation);
		mHandler.sendEmptyMessage(MSG_AUTO_REFRESH_VIEW);
		super.onConfigurationChanged(newConfig);
	}

	@SuppressLint("UseSparseArrays")
	private void initData() {
		defaultTitle = getResources().getString(R.string.mode_watch);
		Constants.TIRE_NAMES = getResources().getStringArray(com.naruto.tpms.app.R.array.tires_names);
		setVolumeControlStream(AudioManager.STREAM_MUSIC); // ע����Ƶͨ��,�澯��������������
		sharedPreferences = getSharedPreferences(Constants.PREF_NAME_SETTING, Context.MODE_PRIVATE);
		// this.powerManager = (PowerManager)
		// this.getSystemService(Context.POWER_SERVICE);
		// this.wakeLock =
		// this.powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK,
		// "My Lock");
		IntentFilter inf = new IntentFilter();
		inf.addAction(CoreBtService.BROADCAST_CONNECTED);
		inf.addAction(CoreBtService.BROADCAST_CONNECTING);
		inf.addAction(CoreBtService.BROADCAST_CONNECTFAIL);
		inf.addAction(CoreBtService.BROADCAST_CONNECTLOST);
		inf.addAction(CoreBtService.BROADCAST_READ);
		registerReceiver(myReceiver, inf);
	}

	private void initView() {
		setTitle(R.string.app_summary);
		iv_title_left = (ImageView) getTitleBar().findViewById(R.id.iv_title_left);
		iv_title_left.setVisibility(View.GONE);

		ib_mute = (ImageButton) findViewById(R.id.ib_mute);
		ImageView iv_title_right = (ImageView) getTitleBar().findViewById(R.id.iv_title_right);
		iv_title_right.setVisibility(View.GONE);

		getTitleBar().findViewById(R.id.ib_mute).setVisibility(View.VISIBLE);// ������ť�ɼ�

		btn_title_left = (Button) findViewById(R.id.btn_title_left);
		btn_title_left.setText(getString(R.string.menu));// �˵���ť
		btn_title_left.setVisibility(View.VISIBLE);

		Button btn_title_right = (Button) findViewById(R.id.btn_title_right);// ���ð�ť
		btn_title_right.setText(getString(R.string.action_settings));
		btn_title_right.setVisibility(View.VISIBLE);

		// �澯��Ϣ��ʾTextView
		tv_alarm_arr[0] = (TextView) findViewById(R.id.tv_alarm_left_front);
		tv_alarm_arr[1] = (TextView) findViewById(R.id.tv_alarm_right_front);
		tv_alarm_arr[2] = (TextView) findViewById(R.id.tv_alarm_left_rear);
		tv_alarm_arr[3] = (TextView) findViewById(R.id.tv_alarm_right_rear);
		// rl_alarm_arr[0] = (RelativeLayout)
		// findViewById(R.id.rl_alarm_left_front);
		// rl_alarm_arr[1] = (RelativeLayout)
		// findViewById(R.id.rl_alarm_right_front);
		// rl_alarm_arr[2] = (RelativeLayout)
		// findViewById(R.id.rl_alarm_left_rear);
		// rl_alarm_arr[3] = (RelativeLayout)
		// findViewById(R.id.rl_alarm_right_rear);

		gv_tireData = (GridView) findViewById(R.id.gv_tireData);// ��̥������ʾ�ؼ�
		mGridAdapter = new TireWatchAdapter(this, gv_tireData, tireDataArr);
		mGridAdapter.setOrientation(getResources().getConfiguration().orientation);
		gv_tireData.setAdapter(mGridAdapter);
		TransitionDrawable tdrawable = new TransitionDrawable(new Drawable[] { new ColorDrawable(android.R.color.transparent),
				new ColorDrawable(R.color.red) });
		tdrawable.startTransition(1000);

		mLoadingDialog = new LoadingDialog(this);// �����Ľ��ȿ�
		mLoadingDialog.setCancelable(true);
		// ��ʼ�Զ�ˢ��
		mHandler.sendEmptyMessage(MSG_AUTO_REFRESH_VIEW);

	}

	private void doConnect() {
		startService(new Intent(this, CoreBtService.class));
		Intent intent = new Intent(CoreBtService.BROADCAST_RECONNECT);// ֪ͨ��̨Service�������ӣ���������ӣ�����������������
		sendBroadcast(intent);
	}

	private void queryTireId() {
		Intent in = new Intent(CoreBtService.BROADCAST_WRITE);
		in.putExtra("data", Commend.TX.QUERY_TIRE_ID);
		sendBroadcast(in);
	}

	private void updateSettings() {
		Intent intent = new Intent(CoreBtService.BROADCAST_UPDATE_SETTINGS);
		sendBroadcast(intent);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_bar:
			doConnect();
			break;
		case R.id.iv_title_left:
		case R.id.btn_title_left: {
			menuDialog = new AlertDialog.Builder(MainActivity.this)
					.setTitle(R.string.msg_choose_mode)
					.setItems(
							new String[] { getResources().getString(R.string.item_study), getResources().getString(R.string.item_switch) },
							MainActivity.this).setNegativeButton(R.string.comm_cancel, null).show();
		}
			break;
		case R.id.btn_title_right:
		case R.id.iv_title_right:
			Intent intent = new Intent(this, SettingActivity.class);
			startActivity(intent);
			break;
		case R.id.ib_mute:
			isAlarmOn = !isAlarmOn;
			ib_mute.setImageResource(isAlarmOn ? R.drawable.mute_on : R.drawable.mute_off);
			sharedPreferences.edit().putBoolean(Constants.SETTING_IS_ALARM_ON, isAlarmOn).commit();
			CommUtil.showTips(R.drawable.tips_warning, isAlarmOn ? R.string.tip_alarm_on : R.string.tip_alarm_off);
			mHandler.sendEmptyMessage(MSG_AUTO_REFRESH_VIEW);
			updateSettings();
			break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (dialog == menuDialog) {
			switch (which) {
			case 0: {
				Intent intent = new Intent(this, EditActivity.class);
				intent.putExtra(EditActivity.EXTRA_MODE, EditActivity.MODE_STUDY);
				startActivity(intent);
			}
				break;
			case 1: {
				Intent intent = new Intent(this, EditActivity.class);
				intent.putExtra(EditActivity.EXTRA_MODE, EditActivity.MODE_SWITCH);
				startActivity(intent);
			}
				break;
			}
		}
	}
}
