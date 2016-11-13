package engineeringwork.pl.kinzil.activity;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import com.movisens.smartgattlib.Characteristic;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import engineeringwork.pl.kinzil.R;
import engineeringwork.pl.kinzil.bluetooth.BluetoothFragment;
import engineeringwork.pl.kinzil.counter.BluetoothLeService;
import engineeringwork.pl.kinzil.counter.CounterFragment;
import engineeringwork.pl.kinzil.history.HistoryFragment;
import engineeringwork.pl.kinzil.map.MapFragment;
import engineeringwork.pl.kinzil.setting.SettingsFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public interface Callbacks {
        public void onBackPressedCallBack();
    }
    private Callbacks mCallbacks;
    private HistoryFragment historyFragment;

    private String login;
    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private String[] tabs = {"Counter", "Map"};
    private TabLayout tabLayout;
    private FrameLayout frameLayout;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private boolean mConnected = false;
    private double distance;

    public String getLogin() {
        return login;
    }
    public BluetoothLeService getmBluetoothLeService() { return mBluetoothLeService; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SharedPreferences shared = getSharedPreferences("data",MODE_PRIVATE);
        login = shared.getString("login", null);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        frameLayout = (FrameLayout) findViewById(R.id.fragment_layout);
        //fragments
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        //fragments
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if(historyFragment != null && historyFragment.isVisible()) {
            mCallbacks.onBackPressedCallBack();
        }else if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if(mViewPager.getVisibility() == View.GONE && tabLayout.getVisibility() == View.GONE) {
            tabLayout.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.GONE);
        }
        else {
            super.onBackPressed();
        }
    }

    private void bindUpdateService (Intent intent) {
        //mDeviceName = intent.getStringExtra("DEVICE_NAME");
        mDeviceAddress = intent.getStringExtra("DEVICE_ADDRESS");

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        boolean isConnected = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.d("Is connected = ", String.valueOf(isConnected));
    }

    //może jest jakaś wbudowana funkcja getCharacteristic ????
    public BluetoothGattCharacteristic getCharacteristic(UUID characteristicUUID){
        List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();
        String expected = Characteristic.lookup(characteristicUUID, "");
        //mBluetoothLeService.getSupportedGattServices().indexOf()
        //lamda z javy 8 sie nada ? -----> list.stream().anyMatch(dto -> dto.getId() == id);

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                String charName = Characteristic.lookup(gattCharacteristic.getUuid(),"");
                if(expected == charName)
                    return  gattCharacteristic;
            }
        }
        return null;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                mSectionsPagerAdapter.counterFragment.setButtonStatus(true, "START");
                //updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mSectionsPagerAdapter.counterFragment.setButtonStatus(false, "Counter disconnected");
                //updateConnectionState(R.string.disconnected);
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                // displayGattServices(mBluetoothLeService.getSupportedGattServices());

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String speed = intent.getStringExtra("EXTRA_SPEED");
                String wheelTime = intent.getStringExtra("WHEEL_TIME");
                Double newDistance = intent.getDoubleExtra("NEW_DISTANCE", 0.0);
                distance += newDistance;

                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA), speed, wheelTime, distance);
            }
        }
    };

    public void readCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        final int charaProperties = characteristic.getProperties();

        if ((charaProperties | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristic != null) {
                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            mBluetoothLeService.readCharacteristic(characteristic);
        }

        if ((charaProperties | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = characteristic;
            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Intent intent = getIntent();

        //Check if BluetoothActivity send intent with Bluetooth DEVICE_NAME and DEVICE_ADDRESS
        if(intent != null && intent.getStringExtra("DEVICE_NAME") != null) {
            bindUpdateService(intent);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void displayData(String cscData, String speed, String wheelTime, Double distance) {
        if (cscData != null) {
            mSectionsPagerAdapter.counterFragment.changeText(cscData, speed, wheelTime, distance);
            Log.d("CSC Measurement data", cscData);
        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        tabLayout.setVisibility(View.GONE);
        mViewPager.setVisibility(View.GONE);
        frameLayout.setVisibility(View.VISIBLE);

        FragmentManager fragmentManager = getSupportFragmentManager();

        if (id == R.id.nav_connection) {
            fragmentManager.beginTransaction().replace(R.id.fragment_layout, new BluetoothFragment()).commit();
        } else if (id == R.id.nav_settings) {
            fragmentManager.beginTransaction().replace(R.id.fragment_layout, new SettingsFragment()).commit();
        } else if (id == R.id.nav_history) {
            historyFragment = new HistoryFragment();
            fragmentManager.beginTransaction().replace(R.id.fragment_layout, historyFragment).commit();
            mCallbacks = (Callbacks) historyFragment;
        } else if (id == R.id.nav_aboutus) {

        } else if (id == R.id.nav_logout) {
            SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("autorun","false");
            editor.apply();
            Intent intent = new Intent(this, UserAddActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
            finish();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public CounterFragment counterFragment;
        public MapFragment mapFragment;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            counterFragment = new CounterFragment();
            mapFragment = new MapFragment();
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if (position == 0) {
                return counterFragment;
            } else if (position == 1) {
                return mapFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return tabs.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabs[position];
        }
    }
}
