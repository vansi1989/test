package com.fan.usejar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;
//import cn.wch.ch34xuartdemo.R;
import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class MainActivity extends Activity {

	public static CH34xUARTDriver driver;// 需要将CH34x的驱动类写在APP类下面，使得帮助类的生命周期与整个应用程序的生命周期是相同的
	private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";	
	
	private Handler handler;

	private boolean isOpen;
	public int baud_rate;
	public byte data_bit, stop_bit, parity, flow_ctrl;
	public byte power;
	
	private Button open_button,clearButton, SetButton, PowerButton;
	private NumberPicker pick_temp, pick_time;
	private EditText read_text;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		driver = new CH34xUARTDriver(
				(UsbManager) getSystemService(Context.USB_SERVICE), this,
				ACTION_USB_PERMISSION);
		if (!driver.UsbFeatureSupported()) {// 判断系统是否支持USB HOST
			Dialog dialog = new AlertDialog.Builder(MainActivity.this)
			.setTitle("提示")
			.setMessage("您的手机不支持USB HOST，请更换其他手机再试！")
			.setPositiveButton("确认",
					new DialogInterface.OnClickListener() {
			
						@Override
						public void onClick(DialogInterface arg0,
								int arg1) {
							System.exit(0);
						}
					}).create();
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();
		}

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// 保持常亮的屏幕的状态
		InitUI();
		open_button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!isOpen) { //设备未启用
					if (driver.ResumeUsbList()) { //枚举当前连接设备
						if (driver.UartInit()) { //初始化设备
							Toast.makeText(MainActivity.this, "设备打开成功", Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(MainActivity.this, "设备打开失败", Toast.LENGTH_SHORT).show();
							return;
						}
						
						//配置串口波特率，函数说明可参照编程手册
						if (driver.SetConfig(baud_rate, data_bit, stop_bit, parity, flow_ctrl)) {
							Toast.makeText(MainActivity.this, "设备配置成功", Toast.LENGTH_SHORT).show();
							open_button.setText("Close");
							isOpen = true;
							new readThread().start();//开启读线程读取串口接收的数据
						} else {
							Toast.makeText(MainActivity.this, "设备配置失败", Toast.LENGTH_SHORT).show();
						}
					} else {
						Toast.makeText(MainActivity.this, "打开设备失败!",	Toast.LENGTH_SHORT).show();
						driver.CloseDevice();
					}
				} else {
					isOpen = false;
					open_button.setText("Open");
					driver.CloseDevice();
				}
			}
		});
		
		clearButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				read_text.setText("");
			}
		});

		handler = new Handler() {
			public void handleMessage(Message msg) {
				read_text.append((String) msg.obj);
			}
		};
		
		PowerButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				byte[] to_send = new byte[1];
				
				if (0 == power) {
					power = 1;
				} else {
					power = 0;
				}
				
				to_send[0] = power;
				int retval = driver.WriteData(to_send, to_send.length);//写数据，第一个参数为需要发送的字节数组，第二个参数为需要发送的字节长度，返回实际发送的字节长度
				if (retval < 0)
					Toast.makeText(MainActivity.this, "写失败!",
							Toast.LENGTH_SHORT).show();
			}
		});
		
		SetButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
//				byte[] to_send = toByteArray(readText.getText().toString());
				byte to_send[] = new byte[2];
				to_send[0] = (byte) pick_temp.getValue();
				to_send[1] = (byte) pick_time.getValue();
				int retval = driver.WriteData(to_send, to_send.length);//写数据，第一个参数为需要发送的字节数组，第二个参数为需要发送的字节长度，返回实际发送的字节长度
				if (retval < 0)
					Toast.makeText(MainActivity.this, "写失败!",
							Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	private void InitUI() {
		open_button = (Button)findViewById(R.id.open_device);
		read_text = (EditText)findViewById(R.id.ReadValues);
		clearButton = (Button)findViewById(R.id.clearButton);
		PowerButton = (Button)findViewById(R.id.PowerButton);
		SetButton = (Button)findViewById(R.id.SetButton);
		pick_temp = (NumberPicker) findViewById(R.id.pick_temp);
		pick_time = (NumberPicker) findViewById(R.id.pick_time);
		
		baud_rate = 9600;
		data_bit = 8;
		stop_bit = 1;
		parity = 0;
		flow_ctrl = 0;
		pick_temp.setMaxValue(65);
		pick_temp.setMinValue(18);
		pick_temp.setValue(40);
//		pick_temp.setOnValueChangedListener(new OnValueChangeListener() {
//			@Override
//			public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
//				temp = newVal;
//			}
//		});
		pick_time.setMaxValue(99);
		pick_time.setMinValue(1);
		pick_time.setValue(60);
//		pick_time.setOnValueChangedListener(new OnValueChangeListener() {
//			
//			@Override
//			public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
//				time = newVal;
//			}
//		});
	}
	
	private class readThread extends Thread {

		public void run() {

			byte[] buffer = new byte[64];

			while (true) {
				Message msg = Message.obtain();
				if (!isOpen) {
					break;
				}
				int length = driver.ReadData(buffer, 64);
				if (length > 0) {
					String recv = toHexString(buffer, length);
					msg.obj = recv;
					handler.sendMessage(msg);
				}
			}
		}
	}

	/**
	 * 将byte[]数组转化为String类型
	 * @param arg
	 *            需要转换的byte[]数组
	 * @param length
	 *            需要转换的数组长度
	 * @return 转换后的String队形
	 */
	private String toHexString(byte[] arg, int length) {
		String result = new String();
		if (arg != null) {
			for (int i = 0; i < length; i++) {
				result = result
						+ (Integer.toHexString(
								arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
								+ Integer.toHexString(arg[i] < 0 ? arg[i] + 256
										: arg[i])
								: Integer.toHexString(arg[i] < 0 ? arg[i] + 256
										: arg[i])) + " ";
			}
			return result;
		}
		return "";
	}

	/**
	 * 将String转化为byte[]数组
	 * @param arg
	 *            需要转换的String对象
	 * @return 转换后的byte[]数组
	 */
	private byte[] toByteArray(String arg) {
		if (arg != null) {
			/* 1.先去除String中的' '，然后将String转换为char数组 */
			char[] NewArray = new char[1000];
			char[] array = arg.toCharArray();
			int length = 0;
			for (int i = 0; i < array.length; i++) {
				if (array[i] != ' ') {
					NewArray[length] = array[i];
					length++;
				}
			}
			/* 将char数组中的值转成一个实际的十进制数组 */
			int EvenLength = (length % 2 == 0) ? length : length + 1;
			if (EvenLength != 0) {
				int[] data = new int[EvenLength];
				data[EvenLength - 1] = 0;
				for (int i = 0; i < length; i++) {
					if (NewArray[i] >= '0' && NewArray[i] <= '9') {
						data[i] = NewArray[i] - '0';
					} else if (NewArray[i] >= 'a' && NewArray[i] <= 'f') {
						data[i] = NewArray[i] - 'a' + 10;
					} else if (NewArray[i] >= 'A' && NewArray[i] <= 'F') {
						data[i] = NewArray[i] - 'A' + 10;
					}
				}
				/* 将 每个char的值每两个组成一个16进制数据 */
				byte[] byteArray = new byte[EvenLength / 2];
				for (int i = 0; i < EvenLength / 2; i++) {
					byteArray[i] = (byte) (data[i * 2] * 16 + data[i * 2 + 1]);
				}
				return byteArray;
			}
		}
		return new byte[] {};
	}
}







