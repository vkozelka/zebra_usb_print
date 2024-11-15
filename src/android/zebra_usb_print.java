package cz.vaclavkozelka.zebra_usb_print;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.Thread;
import java.lang.Runnable;
import java.lang.Exception;
import java.util.List;
import java.util.LinkedList;

import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.app.PendingIntent;
import android.util.Log;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterUsb;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;

import com.google.gson.Gson;

/**
 * This class echoes a string called from JavaScript.
 */
public class zebra_usb_print extends CordovaPlugin {

    private static final String TAG = "TabletApp";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("getDevices")) {
            String zpl = args.getString(0);
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Log.d(TAG, "starting discovery");
                    try {
                        mUsbManager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
                        mPermissionIntent = PendingIntent.getBroadcast(cordova.getActivity().getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);

                        UsbDiscoveryHandler handler = new UsbDiscoveryHandler();
                        UsbDiscoverer.findPrinters(cordova.getActivity().getApplicationContext(), handler);

                        Log.d(TAG, "Finding printers");
                        while (!handler.discoveryComplete) {
                            Thread.sleep(100);
                        }
                        Log.d(TAG, "Finding complete");
                        if (handler.printers != null && handler.printers.size() > 0) {
                            Log.d(TAG, "Found pritners: " + handler.printers.size());
                            discoveredPrinterUsb = handler.printers.get(0);

                            if (discoveredPrinterUsb.device != null) {
                                Log.d(TAG, discoveredPrinterUsb.device.getDeviceName());
                                if (!mUsbManager.hasPermission(discoveredPrinterUsb.device)) {
                                    mUsbManager.requestPermission(discoveredPrinterUsb.device, mPermissionIntent);
                                } else {
                                    Connection conn = discoveredPrinterUsb.getConnection();
                                    Log.d(TAG, "Device connection: " + conn.getSimpleConnectionName());
                                    try {
                                        Log.d(TAG, "Open connection");
                                        conn.open();
                                        Log.d(TAG, "Sending data: " + zpl);
                                        conn.write(zpl.getBytes());
                                        Log.d(TAG, "Closing connection");
                                        conn.close();
                                        callbackContext.success("Print success");
                                    } catch (ConnectionException ce) {
                                        Log.d(TAG, "Connection error: " + ce.getMessage());
                                        callbackContext.error(ce.getMessage());
                                    } finally {
                                        if (conn != null) {
                                            try {
                                                conn.close();
                                            } catch (ConnectionException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            } else {
                                callbackContext.error("Cannot get permission without device");
                            }
                        } else {
                            callbackContext.error("No printer found");
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Exception: " + e.getMessage());
                        callbackContext.error(e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
            return true;
        } else if (action.equals("printZpl")) {
            String zpl = args.getString(0);
            this.printZpl(zpl, callbackContext);
            return true;
        }
        return false;
    }

    /* USBDiscovery Part - start */
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    private PendingIntent mPermissionIntent;
    private boolean hasPermissionToCommunicate = false;
    private UsbManager mUsbManager;
    private DiscoveredPrinterUsb discoveredPrinterUsb;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            hasPermissionToCommunicate = true;
                        }
                    }
                }
            }
        }
    };
    class UsbDiscoveryHandler implements DiscoveryHandler {
        public List<DiscoveredPrinterUsb> printers;
        public boolean discoveryComplete = false;
        public UsbDiscoveryHandler() {
            printers = new LinkedList<DiscoveredPrinterUsb>();
        }
        public void foundPrinter(final DiscoveredPrinter printer) {
            printers.add((DiscoveredPrinterUsb) printer);
        }
        public void discoveryFinished() {
            discoveryComplete = true;
        }
        public void discoveryError(String message) {
            discoveryComplete = true;
        }
    }
    /* USBDiscovery Part - end */

    private void printZpl(String zplCode, CallbackContext callbackContext) {
        callbackContext.success("printZpl called: " + zplCode);
    }
}
