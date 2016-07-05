package zzti.com.bluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    //该UUID表示串口服务
    static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    Button btnSearch, btnDis, btnExit,btnClose;
    ListView lvBTDevices;
    ArrayAdapter<String> adtDevices;
    List<String> lstDevices = new ArrayList<String>();
    BluetoothAdapter btAdapt;
    public static BluetoothSocket btSocket;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Button 设置
        btnSearch = (Button) this.findViewById(R.id.btnSearch);
        //搜索蓝牙
        btnSearch.setOnClickListener(new ClickEvent());
        //退出程序
        btnExit = (Button) this.findViewById(R.id.btnExit);
        btnExit.setOnClickListener(new ClickEvent());
        //打开蓝牙
        btnDis = (Button) this.findViewById(R.id.btnDis);
        btnDis.setOnClickListener(new ClickEvent());
        //关闭蓝牙
        btnClose = (Button) this.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(btAdapt!=null){
                    if(btAdapt.isEnabled()){
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("温馨提示")
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setMessage("确认关闭蓝牙吗？")
                                .setPositiveButton("确定",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog,
                                                                int which) {
                                                btAdapt.disable();
                                                adtDevices.clear();
                                                lvBTDevices.setAdapter(adtDevices);
                                            }
                                        })
                                .setNegativeButton("取消",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int which) {
                                                dialog.cancel();// 取消弹出框
                                            }
                                        }).create().show();
                    }
                }
            }

        });
        // ListView及其数据源 适配器
        lvBTDevices = (ListView) this.findViewById(R.id.lvDevices);
        adtDevices = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, lstDevices);
        lvBTDevices.setAdapter(adtDevices);
        lvBTDevices.setOnItemClickListener(new ItemClickEvent());

        btAdapt = BluetoothAdapter.getDefaultAdapter();// 初始化本机蓝牙功能

        // ============================================================
        // 注册Receiver来获取蓝牙设备相关的结果
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);// 用BroadcastReceiver来取得搜索结果
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(searchDevices, intent);
    }

    private BroadcastReceiver searchDevices = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = null;
            // 搜索设备时，取得设备的MAC地址
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    String str = "未配对|" + device.getName() + "|"
                            + device.getAddress();
                    if (lstDevices.indexOf(str) == -1)// 防止重复添加
                        lstDevices.add(str); // 获取设备名称和mac地址
                    adtDevices.notifyDataSetChanged();
                }
            }else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        connect(device);//连接设备
                        break;
                    case BluetoothDevice.BOND_NONE:
                    default:
                        break;
                }
            }

        }
    };

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(searchDevices);
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    class ItemClickEvent implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                long arg3) {
            if(btAdapt.isDiscovering())btAdapt.cancelDiscovery();
            String str = lstDevices.get(arg2);
            String[] values = str.split("\\|");
            String address = values[2];
            BluetoothDevice btDev = btAdapt.getRemoteDevice(address);
            try {
                Boolean returnValue = false;
                if (btDev.getBondState() == BluetoothDevice.BOND_NONE) {
                    //利用反射方法调用BluetoothDevice.createBond(BluetoothDevice remoteDevice);
                    Toast.makeText(MainActivity.this, "正在连接，请稍后...", 1000).show();
                    Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                    returnValue = (Boolean) createBondMethod.invoke(btDev);

                }else if(btDev.getBondState() == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(MainActivity.this, "正在连接，请稍后...", 1000).show();
                    connect(btDev);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private void connect(BluetoothDevice btDev) {
        UUID uuid = UUID.fromString(SPP_UUID);
        try {
            btSocket = btDev.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            // 搜索蓝牙设备，在BroadcastReceiver显示结果
            if (v == btnSearch){
                if (btAdapt.getState() == BluetoothAdapter.STATE_OFF) {// 如果蓝牙还没开启
                    Toast.makeText(MainActivity.this, "请先打开蓝牙", 1000).show();
                    return;
                }
                if (btAdapt.isDiscovering())
                    btAdapt.cancelDiscovery();
                lstDevices.clear();
                Object[] lstDevice = btAdapt.getBondedDevices().toArray();
                for (int i = 0; i < lstDevice.length; i++) {
                    BluetoothDevice device = (BluetoothDevice) lstDevice[i];
                    String str = "已配对|" + device.getName() + "|"
                            + device.getAddress();
                    lstDevices.add(str); // 获取设备名称和mac地址
                    adtDevices.notifyDataSetChanged();
                }
                setTitle("本机蓝牙地址：" + btAdapt.getAddress());
                btAdapt.startDiscovery();
            }else if (v == btnDis)// 本机可以被搜索
            {
                Intent discoverableIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(
                        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);
            } else if (v == btnExit) {
                try {
                    if (btSocket != null)
                        btSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                MainActivity.this.finish();
            }
        }

    }

}

