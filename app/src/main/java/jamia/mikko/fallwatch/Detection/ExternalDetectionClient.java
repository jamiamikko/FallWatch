package jamia.mikko.fallwatch.Detection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.DataToken;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Led;

import java.util.ArrayList;

import bolts.Continuation;
import bolts.Task;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by jamiamikko on 25/09/2017.
 */

public class ExternalDetectionClient implements Runnable, ServiceConnection {

    private static final String USER_PREFERENCES = "UserPreferences";
    private final String mwMacAddress = "E6:F3:22:B3:2C:4E";
    private MetaWearBoard mwBoard;
    private Accelerometer accelerometer;
    private Context context;
    private BluetoothManager btManager;
    private Handler uiHandler;
    private BluetoothDevice btDevice;
    private SharedPreferences prefs;

    public ExternalDetectionClient(Context context, BluetoothManager btManager, Handler uiHandler) {

        //Initialize
        this.context = context;
        this.btManager = btManager;
        this.uiHandler = uiHandler;
        prefs = context.getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
    }

    @Override
    public void run() {
        try {
            Thread.sleep(500);
            //Start client
            start();
        } catch (Exception e) {
        }
    }

    public void start() {
        Intent bindIntent = new Intent(context, BtleService.class);
        context.bindService(bindIntent, this, Context.BIND_AUTO_CREATE);
    }

    public void stop() {
        try {

            //Stop client
            context.unbindService(this);
            if (accelerometer != null) {
                accelerometer.acceleration().stop();
            }

        } catch (Exception e) {
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        //Initialize external sensor device
        BtleService.LocalBinder serviceBinder = (BtleService.LocalBinder) service;
        btDevice = btManager.getAdapter().getRemoteDevice(mwMacAddress);
        mwBoard = serviceBinder.getMetaWearBoard(btDevice);

        mwBoard.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {

            @Override
            public Task<Route> then(Task<Void> task) throws Exception {

                //Configure accelerometer
                accelerometer = mwBoard.getModule(Accelerometer.class);
                accelerometer.configure()
                        .odr(5f)
                        .commit();

                //Data route
                return accelerometer.acceleration().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(final RouteComponent source) {
                        source.map(Function1.RSS).average((byte) 5).filter(ThresholdOutput.BINARY, 0.5f)
                                .multicast()
                                .to().filter(Comparison.EQ, -1).react(new RouteComponent.Action() {
                            @Override
                            public void execute(DataToken token) {
                                //Play led when the acceleration gets great enough.
                                Led led = mwBoard.getModule(Led.class);
                                led.editPattern(Led.Color.RED, Led.PatternPreset.SOLID).commit();
                                led.play();
                            }
                        }).stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                Message msg = uiHandler.obtainMessage();

                                //Build data message
                                ArrayList<String> messages = new ArrayList<>();

                                messages.add(prefs.getString("contact1", null));
                                messages.add(prefs.getString("contact2", null));
                                messages.add(prefs.getString("username", null));
                                messages.add(ApplicationClass.getGoogleApiHelper().getLocation());


                                msg.what = 0;
                                msg.obj = messages;

                                //Send message to service
                                uiHandler.sendMessage(msg);

                            }
                        }).to().filter(Comparison.EQ, 1).react(new RouteComponent.Action() {
                            @Override
                            public void execute(DataToken token) {
                                //Stop led.
                                Led led = mwBoard.getModule(Led.class);
                                led.stop(true);


                            }
                        }).end();
                    }
                });

            }
        }).continueWith(new Continuation<Route, Void>() {

            @Override
            public Void then(Task<Route> task) throws Exception {

                //If defined task is ok, start accelerometer
                if (task.isFaulted()) {
                    Log.i("Freefall", String.valueOf(task.getError()));
                    mwBoard.getModule(Debug.class).resetAsync();
                } else {
                    accelerometer.acceleration().start();
                    accelerometer.start();
                }

                return null;
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

}
