package cn.navior.tool.sumsungrssirec;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.samsung.android.sdk.bt.gatt.BluetoothGatt;
import com.samsung.android.sdk.bt.gatt.BluetoothGattAdapter;
import com.samsung.android.sdk.bt.gatt.BluetoothGattCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class SearchingActivity extends Activity {

    /* constants defined by wangxiayang */
    private static final int REQUEST_ENABLE_BT = 36287;

    private ArrayAdapter< String > devicesArrayAdapter;
    private TextView searchingStatus;
    private ArrayList< RecordItem > tempRecords = new ArrayList< RecordItem >();	// temporary record storage
    private PrintWriter writer;
    private Handler handler = new Handler();

    /* fields about local Bluetooth device model */
    private BluetoothAdapter mBluetoothAdapter; // local device model
    private BluetoothGatt mBluetoothGatt = null;    // local ble searching device model
    private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {
        @Override
        public void onScanResult( final android.bluetooth.BluetoothDevice device, final int rssi, byte[] scanRecord) {
            // add the record into the Arraylist
            RecordItem item = new RecordItem( device.getAddress() );
            item.setRssi( rssi + 0 );	// replace the short value into an integer
            item.setName( device.getName() );
            SimpleDateFormat tempDate = new SimpleDateFormat( "kk-mm-ss-SS", Locale.ENGLISH );
            String datetime = tempDate.format(new java.util.Date());
            item.setDatetime( datetime );
            tempRecords.add( item );

            handler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    devicesArrayAdapter.add( device.getName() + " " + rssi );
                    devicesArrayAdapter.notifyDataSetChanged();
                }
            });
        }
    };  // message handler
    private BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothGattAdapter.GATT) {
                mBluetoothGatt = (BluetoothGatt) proxy;
                mBluetoothGatt.registerApp(mGattCallbacks);
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothGattAdapter.GATT) {
                if (mBluetoothGatt != null)
                    mBluetoothGatt.unregisterApp();

                mBluetoothGatt = null;
            }
        }
    };  // device model builder

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_searching);

		/* initialize the fields */
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();	// get the Bluetooth adapter for this device
        devicesArrayAdapter = new ArrayAdapter< String >( this, android.R.layout.simple_expandable_list_item_1 );
        searchingStatus = ( TextView )findViewById( R.id.searching_status );

        /* build up local Bluetooth device model */
        if ( mBluetoothGatt == null ) {
            BluetoothGattAdapter.getProfileProxy( this, mProfileServiceListener, BluetoothGattAdapter.GATT );
        }

		/* bind the action listeners */
        final Button stopButton = ( Button )findViewById( R.id.searching_stop );
        final Button startButton = ( Button ) findViewById( R.id.searching_start );
        final Button quitButton = ( Button )findViewById( R.id.searching_quit );
        // stop-searching button
        stopButton.setOnClickListener( new View.OnClickListener(){
            public void onClick( View v ){
                mBluetoothGatt.stopScan();
                searchingStatus.setText( "Discovery has finished." );
                // write record
                saveRecords();
                // write stop info
                SimpleDateFormat tempDate = new SimpleDateFormat( "kk-mm-ss-SS", Locale.ENGLISH );
                String datetime = tempDate.format(new java.util.Date());
                writer.write( "stop," + datetime );
                writer.close();
                stopButton.setEnabled( false );
                startButton.setEnabled( true );
            }
        });
        stopButton.setEnabled( false );	// disable the stop button until discovery starts
        // start-searching button
        startButton.setOnClickListener( new View.OnClickListener(){
            public void onClick( View v ){
                devicesArrayAdapter.clear();
                devicesArrayAdapter.notifyDataSetChanged();
                createPrintWriter();
                mBluetoothGatt.startScan();
                searchingStatus.setText( "Discovery is on." );
                stopButton.setEnabled( true );
                startButton.setEnabled( false );
            }

            private void createPrintWriter() {
                // create directory
                File directory = new File( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "sumsung_rssirec" );
                if( !directory.exists() ) {
                    directory.mkdir();
                }

                // create the time string
                SimpleDateFormat tempDate = new SimpleDateFormat( "yyyy-MM-dd-kk-mm-ss", Locale.ENGLISH );
                String datetime = tempDate.format(new java.util.Date());

                // create the file
                File recordFile = new File( directory.getAbsolutePath() + "/" + datetime + ".txt" );
                if( recordFile.exists() ) {
                    recordFile.delete();
                }

                // open writer
                try {
                    writer = new PrintWriter( recordFile );
                    writer.write( "Device name," + mBluetoothAdapter.getName() + "\n" );
                    writer.write( "Device address," + mBluetoothAdapter.getAddress() + "\n" );
                    writer.write( "start," + datetime + "\n" );
                    writer.write( "mac,name,rssi,time\n" );
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        // quit-searching button
        quitButton.setOnClickListener( new View.OnClickListener(){
            public void onClick( View v ){
                SearchingActivity.this.finish();
            }
        });

		/* set list adapter */
        ListView lv = ( ListView )findViewById( R.id.searching_device_list );
        lv.setAdapter( devicesArrayAdapter );
	}

    @Override
    protected void onResume() {
        super.onResume();
		/* check Bluetooth status, notify user to turn it on if it's not */
        // request for Bluetooth if it's not on.
        // repeat requesting if user refused to open Bluetooth
        while (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * Not handle NULLPOINTER exception for that writer is null
     */
    private void saveRecords() {
        for( int i = 0; i < tempRecords.size(); i++ ) {
            RecordItem item = tempRecords.get( i );
            writer.write( item.getMac() + "," + item.getName() + "," + item.getRssi() + "," + item.getDatetime() + "\n" );
        }
        tempRecords.clear();
    }

}
