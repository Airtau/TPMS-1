package com.naruto.tpms.app;

import java.util.Arrays;
import java.util.List;

import com.naruto.tpms.app.bean.TireData;
import com.naruto.tpms.app.comm.Commend;
import com.naruto.tpms.app.comm.Constants;
import com.naruto.tpms.app.comm.util.CommUtil;
import com.naruto.tpms.app.comm.util.MusicUtil;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * ��̨���ķ���
 * 
 * @author Thinkman
 * 
 */
@SuppressLint("HandlerLeak")
public class CoreBtService extends Service {

	public static final String TAG = "CoreBtService";
	public static final int MSG_AUTO_REFRESH_VIEW = 123;
	// �㲥����
	public static final String BROADCAST_CONNECTED = "com.naruto.tpms.app.CONNECTED";
	public static final String BROADCAST_CONNECTING = "com.naruto.tpms.app.CONNECTING";
	public static final String BROADCAST_CONNECTFAIL = "com.naruto.tpms.app.CONNECTFAIL";
	public static final String BROADCAST_CONNECTLOST = "com.naruto.tpms.app.CONNECTLOST";
	public static final String BROADCAST_READ = "com.naruto.tpms.app.READ";
	public static final String BROADCAST_WRITE = "com.naruto.tpms.app.WRITE";
	public static final String BROADCAST_UPDATE_SETTINGS = "com.naruto.tpms.app.UPDATESETTINGS";
	public static final String BROADCAST_TOAST = "com.naruto.tpms.app.TOAST";
	// ������serviceӦ�ý��յĹ㲥
	public static final String BROADCAST_RECONNECT = "com.naruto.tpms.app.RECONNECT";
	public static final String BROADCAST_DISCONNECT = "com.naruto.tpms.app.DISCONNECT";
	private static final int FOREGROUND_ID = R.drawable.icon;// ֪ͨid

	// ������Ϣ
	private BluetoothAdapter bAdapter;
	private BluetoothDevice device;// λ�ð󶨵��豸
	private SharedPreferences pref;
	private BTService mService;
	private final int MAX_RETRY_COUNT = 3;// ������Դ���
	private int retryCount = 0;// ����ʧ�������˵Ĵ���
	private Vibrator vibrator;
	private TireData[] tireDataArr = new TireData[4];// 4����̥(����Ϊ��ǰ����ǰ������Һ�)
	public double highPressure, lowPressure, highTemprature;
	private PendingIntent contentIntent;
	private RemoteViews contentView;// ֪ͨ���Զ�����ͼ
	private Notification mNotification;
	private String[] tireNames;
	private String resRunning;
	private PowerManager.WakeLock mWakeLock;

	/**
	 * �澯�Ƿ���
	 */
	private boolean isAlarmOn;

	private Handler mHandler = new Handler() {
		public void dispatchMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_AUTO_REFRESH_VIEW:
				mHandler.removeMessages(MSG_AUTO_REFRESH_VIEW);
				// refreshView();
				parseData();
				mHandler.sendEmptyMessageDelayed(MSG_AUTO_REFRESH_VIEW, 1000);
				break;
			case BTService.MSG_TOAST:
				CommUtil.showTips(R.drawable.tips_warning, String.valueOf(msg.obj));
				break;
			case BTService.MSG_CONNECTED: {
				// Log.d(TAG, "���ӳɹ�");
				retryCount = 0;
				device = (BluetoothDevice) msg.obj;
				notifyConnected(device);
				notification(resRunning, false);
				// ���Ͳ�ѯ��̥id��ָ��
				mService.sendMsg(Commend.TX.QUERY_TIRE_ID);
			}
				break;
			case BTService.MSG_CONNECTING: {
				// Log.d(TAG, "������...");
				notifyConnecting();
			}
				break;
			case BTService.MSG_CONNECTED_FAIL: {
//				 Log.d(TAG, "����ʧ��...");
				if (retryCount++ < MAX_RETRY_COUNT) {
					// Log.d(TAG, "����ʧ��,����" + retryCount);
					doConnect();
				} else {
					retryCount = 0;
					CommUtil.showTips(R.drawable.tips_error, R.string.tips_connect_fail);
					mHandler.removeMessages(MSG_AUTO_REFRESH_VIEW);
					parseData();
					disConnect();
					notifyConnectFail();
				}
			}
				break;
			case BTService.MSG_CONNECTED_LOST: {
				 Log.d(TAG, "���Ӷ�ʧ");
				retryCount = 0;
				mHandler.removeMessages(MSG_AUTO_REFRESH_VIEW);
				// refreshView();
				disConnect();
				notifyConnectLost();
//				CommUtil.showTips(R.drawable.tips_error, R.string.tips_connect_lost);
			}
				break;
			case BTService.MSG_WRITE:
				break;
			case BTService.MSG_READ:
				byte[] report = (byte[]) msg.obj;
				Log.d(TAG, "���յ�����Ϣ:" + Arrays.toString(report) + "ʮ������:" + CommUtil.byte2HexStr(report));

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
						// Log.w(TAG, "У��λ����ȷ,��ǰУ��λ=" + report[report.length -
						// 1] + ",��ȷУ��λӦ=" + checkByte);
						return;
					}
				}
				notifyRead(report);

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
						// Log.d(TAG, "��ʼIDѧϰ:" + report[4]);
						break;
					case 0x18:
						// Log.d(TAG, "IDѧϰ�ɹ�:" + report[4]);
						int index = CommUtil.getTireIndexByTireNum(report[4]);
						if (index > -1) {
							String data = getResources().getString(R.string.msg_tire_study_success);
							CommUtil.showTips(R.drawable.tips_smile, String.format(data, Constants.TIRE_NAMES[index]));
						}
						break;
					case 0x30:
						// Log.d(TAG, "��̥�����ɹ�:" + report[4]);
						// CommUtil.showTips(R.drawable.tips_smile,
						// R.string.msg_tire_switch_success);
						break;
					case 0x40:
						break;
					}
					// ��ѯ�ĸ���������ID���
				} else if (report[2] == 0x07) {
					byte[] idByte = new byte[] { report[4], report[5], report[6] };
					// Log.d(TAG, "ID��ѯ���:" + report[3] + "'id=" +
					// CommUtil.byte2HexStr(idByte));
					int index = CommUtil.getTireIndexByTireNum(report[3]);
					if (index != -1) {
						tireDataArr[index].tireId = idByte;
					}
					mHandler.sendEmptyMessage(MSG_AUTO_REFRESH_VIEW);
				}
				parseData();
				break;
			}
		};
	};

	private BroadcastReceiver myReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// Log.d(TAG, "action=" + action);
			if (BROADCAST_WRITE.equals(action)) {
				byte[] data = intent.getByteArrayExtra("data");
				mService.sendMsg(data);
			} else if (BROADCAST_UPDATE_SETTINGS.equals(action)) {
				updateSettings();
			} else if (BROADCAST_RECONNECT.equals(action)) {
				doConnect();
			} else if (BROADCAST_DISCONNECT.equals(action)) {
				disConnect();
			}
		}
	};

	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		super.onCreate();
		resRunning = getString(R.string.msg_tpms_running);
		pref = getSharedPreferences(Constants.PREF_NAME_SETTING, Context.MODE_PRIVATE);
		bAdapter = BluetoothAdapter.getDefaultAdapter();// ������
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);// �𶯷���

		mService = new BTService(mHandler);
		
		tireNames = getResources().getStringArray(com.naruto.tpms.app.R.array.tires_names);

		// ����ǰ̨����
		mNotification = new Notification();
		mNotification.tickerText = resRunning;
		mNotification.icon = R.drawable.notify_icon;
		mNotification.defaults = Notification.DEFAULT_SOUND;
		contentView = new RemoteViews(getPackageName(), R.layout.notification_view);
		contentView.setImageViewResource(R.id.image, mNotification.icon);
		contentView.setTextColor(R.id.text, mNotification.icon == R.drawable.notify_icon ? Color.GREEN : Color.RED);
		contentView.setTextViewText(R.id.text, resRunning);
		mNotification.contentView = contentView;
		// if (Build.VERSION.SDK_INT >= 11) {
		// Bitmap bm = BitmapFactory.decodeResource(getResources(),
		// mNotification.icon);
		// mNotification = new
		// Notification.Builder(this).setContentTitle(mNotification.tickerText).setLargeIcon(bm).build();
		// }
		// mNotification.vibrate = new long[] { 0, 100, 300, 400 };
		// PendingIntent pendingIntent =
		// PendingIntent.getActivity(getApplicationContext(), 0, new
		// Intent(getApplicationContext(),
		// MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
		PackageManager packageManager = this.getPackageManager();
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setPackage(this.getPackageName());
		List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);
		ResolveInfo ri = apps.iterator().next();
		Intent intent1 = null;
		if (ri != null) {
			String className = ri.activityInfo.name;
			intent1 = new Intent(Intent.ACTION_MAIN);
			intent1.addCategory(Intent.CATEGORY_LAUNCHER);
			ComponentName cn = new ComponentName(this.getPackageName(), className);
			intent1.setComponent(cn);
		}
		contentIntent = PendingIntent.getActivity(this, 600, intent1, PendingIntent.FLAG_CANCEL_CURRENT);

		mNotification.contentIntent = contentIntent;
		// mNotification.setLatestEventInfo(getApplicationContext(),
		// getString(R.string.app_name), getString(R.string.msg_running),
		// contentIntent);
		// mNotificationManager.notify(FOREGROUND_ID, mNotification);
		startForeground(FOREGROUND_ID, mNotification);

		updateSettings();
		IntentFilter inf = new IntentFilter();
		inf.addAction(BROADCAST_WRITE);
		inf.addAction(BROADCAST_UPDATE_SETTINGS);
		inf.addAction(BROADCAST_RECONNECT);
		inf.addAction(BROADCAST_DISCONNECT);
		registerReceiver(myReceiver, inf);
		// ��ʼ����̥����
		for (int i = 0; i < tireDataArr.length; i++) {
			tireDataArr[i] = new TireData();
			tireDataArr[i].tireNum = Constants.TIRE_NUMS[i];
		}
		doConnect();
		acquireWakeLock();
	}

	/**
	 * onCreateʱ,�����豸��Դ�������ָ÷�������ĻϨ��ʱ��Ȼ��ȡCPUʱ����������
	 */
	private void acquireWakeLock() {
		if (null == mWakeLock) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "myService");
			if (null != mWakeLock) {
				mWakeLock.acquire();
			}
		}
	}

	/**
	 * onDestroyʱ���ͷ��豸��Դ��
	 */
	private void releaseWakeLock() {
		if (null != mWakeLock) {
			mWakeLock.release();
			mWakeLock = null;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG,"onDestroy");
		disConnect();
		unregisterReceiver(myReceiver);
		mHandler.removeMessages(MSG_AUTO_REFRESH_VIEW);
		MusicUtil.stop(this);
		vibrator.cancel();
//		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//		mNotificationManager.cancel(FOREGROUND_ID);
		releaseWakeLock();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private synchronized void doConnect() {
//		Toast.makeText(this, "������...", Toast.LENGTH_LONG).show();
		String mac = pref.getString(Constants.SETTING_MAC, null);
		if (mac == null) {
			notifyConnectFail();
			// Toast.makeText(this, "no bind", Toast.LENGTH_SHORT).show();
			notifyToast(getString(R.string.msg_device_not_bind));
		} else if (!bAdapter.isEnabled()) {
			notifyConnectFail();
			// CommUtil.showTips(R.drawable.tips_error, "����δ��");
			notifyToast(getString(R.string.msg_bluetooth_closed));
			// Toast.makeText(this, "����δ��", Toast.LENGTH_SHORT).show();
		} else {
			// ��������
			if (mService != null) {
				switch (mService.getState()) {
				case BTService.STATE_CONNECTING:
					notifyConnecting();
					return;
				case BTService.STATE_CONNECTED:
					notifyConnected(device);
					return;
				default:
					mService.stop();
//					mService = null;
					break;
				}
			}
			device = bAdapter.getRemoteDevice(mac);
			mService.connect(device);
		}
	}

	private void disConnect() {
		if (mService != null) {
			mService.stop();
//			mService = null;
		}
	}

	private void updateSettings() {
		isAlarmOn = pref.getBoolean(Constants.SETTING_IS_ALARM_ON, true);

		highPressure = pref.getInt(Constants.SETTING_HIGH_PRESSURE, 5) * Constants.HIGH_PRESSURE_STEUP + Constants.HIGH_PRESSURE_MIN;
		lowPressure = pref.getInt(Constants.SETTING_LOW_PRESSURE, 0) * Constants.LOW_PRESSURE_STEUP + Constants.LOW_PRESSURE_MIN;
		highTemprature = pref.getInt(Constants.SETTING_HIGH_TEMPRATURE, 5) * Constants.HIGH_TEMPRATURE_STEUP
				+ Constants.HIGH_TEMPRATURE_MIN;
		// parseData();
	}

	/**
	 * �������ݣ������澯
	 */
	protected void parseData() {
//		 Log.d("debug", "ˢ������");
		// �澯����
		boolean isAnyAlarm = false;// �Ƿ��и澯��Ϣ
		int alarmCount = 0;
		StringBuilder alarmSb = new StringBuilder();
		alarmSb.append("(");
		for (int i = 0; i < tireDataArr.length; i++) {
			TireData td = tireDataArr[i];
			String alarmStr = null;
			if (td == null) {
				continue;
			}
			// �ϱ�ʱ�䳬��5���򲻴���
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
				if (td.pressure > highPressure) {
					sb.append(getString(R.string.alarm_high_pressure));
					sb.append(",");
				}
				if (td.pressure < lowPressure) {
					sb.append(getString(R.string.alarm_low_pressure));
					sb.append(",");
				}
				if (td.temperature > highTemprature) {
					sb.append(getString(R.string.alarm_high_temprature));
					sb.append(",");
				}
				if (sb.length() > 0) {
					alarmStr = tireNames[i] + ":" + sb.substring(0, sb.length() - 1);
				}
			}
			td.haveAlarm = (alarmStr != null);
			isAnyAlarm = isAnyAlarm || td.haveAlarm;
			alarmCount += td.haveAlarm ? 1 : 0;
			if (td.haveAlarm) {
				alarmSb.append(alarmStr);
				alarmSb.append(";");
			}
		}
		alarmSb.append(")");
		if (alarmCount != 0) {
			alarmSb.insert(0, alarmCount + " " + getString(R.string.msg_n_tire_alarming));
		}
		if (alarmCount == 0) {
			// Log.d(TAG, " �澯����:" + alarmSb.toString());
		} else {
			// Log.d(TAG, "�޸澯");
		}
		if (isAnyAlarm) {
			notification(alarmSb.toString(), true);
		} else {
			notification(resRunning, false);
		}
		// sendWarnNotification(isAnyAlarm ? alarmSb.toString() : resRunning);
		// ������˾�����Ϣ�ҿ����˾���
		if (isAnyAlarm && isAlarmOn) {
			if (!MusicUtil.isPlaying()) {
//				 Log.d(TAG, "��ʼ�澯");
				vibrator.vibrate(new long[] { 500, 1000 }, 0);
				MusicUtil.play(this, R.raw.alarm1);
			}
		} else {
			// ֹͣ�澯
			// Log.d(TAG, "ֹͣ�澯");
			MusicUtil.stop(this);
			vibrator.cancel();
		}
	}

	private void notification(String text, boolean isWarn) {
		contentView.setImageViewResource(R.id.image, isWarn ? R.drawable.notify_icon_warn : R.drawable.notify_icon);
		contentView.setTextColor(R.id.text, isWarn ? Color.RED : Color.GREEN);
		contentView.setTextViewText(R.id.text, text);
		mNotification.icon = isWarn ? R.drawable.notify_icon_warn : R.drawable.notify_icon;

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (!mNotification.tickerText.equals(text)) {
			mNotification.tickerText = text;
			mNotificationManager.notify(FOREGROUND_ID, mNotification);
		}
	}

	/**
	 * ֪ͨ������
	 */
	protected void notifyConnected(BluetoothDevice device) {
		Intent intent = new Intent(BROADCAST_CONNECTED);
		intent.putExtra("data", device);
		sendBroadcast(intent);
		// Log.d(TAG,"֪ͨ������");
	}

	protected void notifyConnecting() {
		Intent intent = new Intent(BROADCAST_CONNECTING);
		sendBroadcast(intent);
	}

	protected void notifyConnectFail() {
//		Log.d(TAG, "����ʧ��");
		notification(getString(R.string.msg_connect_fail), true);

		Intent intent = new Intent(BROADCAST_CONNECTFAIL);
		sendBroadcast(intent);
		// !resRunning.equals(alarmText)
	}

	protected void notifyConnectLost() {
//		Log.d(TAG, "���Ӷ�ʧ");
		notification(getString(R.string.msg_connect_lost), true);

		Intent intent = new Intent(BROADCAST_CONNECTLOST);
		sendBroadcast(intent);
	}

	protected void notifyRead(byte[] bytes) {
		Intent intent = new Intent(BROADCAST_READ);
		intent.putExtra("data", bytes);
		sendBroadcast(intent);
	}

	protected void notifyToast(String data) {
		notification(data, !resRunning.equals(data));

		Intent intent = new Intent(BROADCAST_TOAST);
		intent.putExtra("data", data);
		sendBroadcast(intent);
	}

}
