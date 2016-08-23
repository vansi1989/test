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

	public static CH34xUARTDriver driver;// ��Ҫ��CH34x��������д��APP�����棬ʹ�ð��������������������Ӧ�ó����������������ͬ��
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
		if (!driver.UsbFeatureSupported()) {// �ж�ϵͳ�Ƿ�֧��USB HOST
			Dialog dialog = new AlertDialog.Builder(MainActivity.this)
			.setTitle("��ʾ")
			.setMessage("�����ֻ���֧��USB HOST������������ֻ����ԣ�")
			.setPositiveButton("ȷ��",
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

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// ���ֳ�������Ļ��״̬
		InitUI();
		open_button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!isOpen) { //�豸δ����
					if (driver.ResumeUsbList()) { //ö�ٵ�ǰ�����豸
						if (driver.UartInit()) { //��ʼ���豸
							Toast.makeText(MainActivity.this, "�豸�򿪳ɹ�", Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(MainActivity.this, "�豸��ʧ��", Toast.LENGTH_SHORT).show();
							return;
						}
						
						//���ô��ڲ����ʣ�����˵���ɲ��ձ���ֲ�
						if (driver.SetConfig(baud_rate, data_bit, stop_bit, parity, flow_ctrl)) {
							Toast.makeText(MainActivity.this, "�豸���óɹ�", Toast.LENGTH_SHORT).show();
							open_button.setText("Close");
							isOpen = true;
							new readThread().start();//�������̶߳�ȡ���ڽ��յ�����
						} else {
							Toast.makeText(MainActivity.this, "�豸����ʧ��", Toast.LENGTH_SHORT).show();
						}
					} else {
						Toast.makeText(MainActivity.this, "���豸ʧ��!",	Toast.LENGTH_SHORT).show();
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
				int retval = driver.WriteData(to_send, to_send.length);//д���ݣ���һ������Ϊ��Ҫ���͵��ֽ����飬�ڶ�������Ϊ��Ҫ���͵��ֽڳ��ȣ�����ʵ�ʷ��͵��ֽڳ���
				if (retval < 0)
					Toast.makeText(MainActivity.this, "дʧ��!",
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
				int retval = driver.WriteData(to_send, to_send.length);//д���ݣ���һ������Ϊ��Ҫ���͵��ֽ����飬�ڶ�������Ϊ��Ҫ���͵��ֽڳ��ȣ�����ʵ�ʷ��͵��ֽڳ���
				if (retval < 0)
					Toast.makeText(MainActivity.this, "дʧ��!",
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
	 * ��byte[]����ת��ΪString����
	 * @param arg
	 *            ��Ҫת����byte[]����
	 * @param length
	 *            ��Ҫת�������鳤��
	 * @return ת�����String����
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
	 * ��Stringת��Ϊbyte[]����
	 * @param arg
	 *            ��Ҫת����String����
	 * @return ת�����byte[]����
	 */
	private byte[] toByteArray(String arg) {
		if (arg != null) {
			/* 1.��ȥ��String�е�' '��Ȼ��Stringת��Ϊchar���� */
			char[] NewArray = new char[1000];
			char[] array = arg.toCharArray();
			int length = 0;
			for (int i = 0; i < array.length; i++) {
				if (array[i] != ' ') {
					NewArray[length] = array[i];
					length++;
				}
			}
			/* ��char�����е�ֵת��һ��ʵ�ʵ�ʮ�������� */
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
				/* �� ÿ��char��ֵÿ�������һ��16�������� */
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







