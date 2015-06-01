package com.openvehicles.OVMS.ui;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.openvehicles.OVMS.R;
import com.openvehicles.OVMS.api.OnResultCommandListener;
import com.openvehicles.OVMS.entities.CarData;

import com.openvehicles.OVMS.ui.utils.Ui;
import com.openvehicles.OVMS.utils.CarsStorage;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by julien on 06/02/15.
 */
public class JoystickFragment extends BaseFragment
        implements OnResultCommandListener, AdapterView.OnClickListener {
    View joystickView = null;
    ImageView disc = null;

    int startMargin = 100;

    int newStickValue;
    int sentStickValue;
    boolean brakeToggle;


    //user recently
    int actionDuration;
    int speedLimit;
    int speedLimitEnabled;
    int spookyEnabled;
    int noThrottleEnabled;


    int sequenceNumber;
    int brakeCommand;
    int remoteOn;
    int autoBrake;
    int brakeMode;
    int timeout;
    int scenario;
    long last_updated;

    private final Handler controlMessageHandler = new Handler();

    private SimpleAdapter listAdapter;

    private final int CONTROL_COMMAND = 106;
    private final int ENABLED = 1;
    private final int DISABLE = 0;
    private final int HARD_SPEED_LIMIT = 10;

    private final int CMD_CONTROL = 106;

    private final int SUB_CMD_RESET = 0;
    private final int SUB_CMD_SPOOKY = 2;
    private final int SUB_CMD_NO_THROTTLE = 1;
    private final int SUB_CMD_FORWARD = 3;
    private final int SUB_CMD_REVERSE = 4;
    private final int SUB_CMD_SPEED_LIMIT = 5;
    private final int SUB_CMD_GET_STATE = 6;

    private final int DEFAULT_THROTTLE = 15;

    private final int NETWORK_TIMEOUT = 15;

    Date lastUpdatedLocalTime = null;


    private int mInterval = 3000; // 5 seconds by default, can be changed later

    private Handler mUiHandler;
    private Handler mMsgHandler;

    private ImageButton button_stop;

    private ImageButton btnGoForward;
    private ImageButton btnGoReverse;

    private Button btnLimitSpeedEnable;
    private Button btnLimitSpeedDisable;

    private Button btnSpookyModeOn;
    private Button btnSpookyModeOff;

    private ImageButton btnResetCar;

    private Button btnThrottleEnable;
    private Button btnThrottleDisable;

    private NumberPicker pickerSpeedLimit;
    private NumberPicker pickerDuration;

    private ImageView imgConnection;

    private TextView tv;
    private TextView last_cmd;


    private CarData mCarData;

    private String TAG = "JOYSTICK";
    private long counter = 0;

    private String commandString = "";
    private String commandName = "";
    int pendingCommand = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // init car data:
        mCarData = CarsStorage.get().getSelectedCarData();

        actionDuration = 1;
        speedLimit = HARD_SPEED_LIMIT;
        speedLimitEnabled = DISABLE;
        spookyEnabled = DISABLE;
        noThrottleEnabled = DISABLE;

        remoteOn = 0;
        autoBrake = 1;
        timeout = 100;
        last_updated = -30000;

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+1:00"));
        lastUpdatedLocalTime = cal.getTime();


        //scenario = NO_SCENARIO;

//        getActivity().setRequestedOrientation(
//                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);


        joystickView = inflater.inflate(R.layout.joystick_view, container, false);

        // Assign listener to widgets



        //Get reference to buttons
        btnGoForward = (ImageButton)joystickView.findViewById(R.id.btn_forward);
        btnGoReverse = (ImageButton)joystickView.findViewById(R.id.btn_reverse);

        btnLimitSpeedDisable = (Button)joystickView.findViewById(R.id.btn_speed_limit_disable);
        btnLimitSpeedEnable = (Button)joystickView.findViewById(R.id.btn_speed_limit_enable);

        btnSpookyModeOff = (Button) joystickView.findViewById(R.id.btn_spooky_off);
        btnSpookyModeOn = (Button) joystickView.findViewById(R.id.btn_spooky_on);

        btnResetCar = (ImageButton) joystickView.findViewById(R.id.btn_reset);

        btnThrottleDisable = (Button) joystickView.findViewById(R.id.btn_throttle_disable);
        btnThrottleEnable = (Button) joystickView.findViewById(R.id.btn_throttle_enable);


        imgConnection = (ImageView)joystickView.findViewById(R.id.image_connection);
        tv = (TextView)joystickView.findViewById(R.id.txt_last_updated);

        last_cmd = (TextView)joystickView.findViewById(R.id.txt_last_cmd);


        //Set onclick listeners for buttons
        btnGoForward.setOnClickListener(this);
        btnGoReverse.setOnClickListener(this);
        btnLimitSpeedDisable.setOnClickListener(this);
        btnLimitSpeedEnable.setOnClickListener(this);
        btnSpookyModeOff.setOnClickListener(this);
        btnSpookyModeOn.setOnClickListener(this);
        btnResetCar.setOnClickListener(this);
        btnThrottleDisable.setOnClickListener(this);
        btnThrottleEnable.setOnClickListener(this);


        //button_on_off.setOnCheckedChangeListener(onOffToggleListener);
        //button_stop.setOnClickListener(this);

        //Get reference to pickers
        pickerSpeedLimit = (NumberPicker) joystickView.findViewById(R.id.picker_speed);
        pickerDuration = (NumberPicker) joystickView.findViewById(R.id.picker_duration);

        //Set picker listeners
        pickerDuration.setOnValueChangedListener(numberPickerChangeListener);
        pickerSpeedLimit.setOnValueChangedListener(numberPickerChangeListener);




        // Populate the numberpicker
        pickerSpeedLimit.setMaxValue(80);
        pickerSpeedLimit.setMinValue(0);
        pickerSpeedLimit.setValue(HARD_SPEED_LIMIT);
        pickerSpeedLimit.setWrapSelectorWheel(false);



        Double minValue = 0.0;
        Double maxValue = 5.0;
        Double step = 0.1;

        Double steps = (maxValue - minValue)/step;

        String[] valueSet = new String[steps.intValue()];

        NumberFormat formatter = new DecimalFormat("#0.0");

        for (int i = 0; i < steps.intValue(); i++) {
            valueSet[i] = formatter.format(i*step);
        }

        pickerDuration.setDisplayedValues(valueSet);

        pickerDuration.setMaxValue(steps.intValue()-1);
        pickerDuration.setMinValue(0);
        pickerDuration.setValue(20);
        pickerDuration.setWrapSelectorWheel(false);

        //Set states
        pickerSpeedLimit.setEnabled(true);
        pickerDuration.setEnabled(true);

        //disc = (ImageView) joystickView.findViewById(R.id.imageView);
        //disc.setOnTouchListener(joystickMotionListener);

        // Center the joystick

        /*
        ViewTreeObserver vto = joystickView.getViewTreeObserver();

        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) disc.getLayoutParams();
                params.topMargin = joystickView.getHeight()/2 - disc.getHeight()/2;
                disc.setLayoutParams(params);

                params = (FrameLayout.LayoutParams) joystickView.findViewById(R.id.imageView2).getLayoutParams();
                params.height = (int)(joystickView.getHeight() * 0.8);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    joystickView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    joystickView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
*/
        // Populate the spinner with scenarios
        /*
        Spinner spinner = (Spinner) joystickView.findViewById(R.id.spinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.scenarios_array));
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(scenarioSelectedHandler);
        */

        //controlMessageHandler.postDelayed(controlMessageRunnable,1000);

        // Populate listview with initial values

        ArrayList<Map<String, String>> list = buildData();
        String[] from = { "name", "value" };
        int[] to = { android.R.id.text1, android.R.id.text2 };

        listAdapter = new SimpleAdapter(getActivity(), list,
                android.R.layout.simple_list_item_2, from, to);
        ((ListView)joystickView.findViewById(R.id.listView)).setAdapter(listAdapter);





        mMsgHandler = new Handler();

        startRepeatingTask();
        // Inflate the layout for this fragment
        return joystickView;
    }






    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            counter++;


            Log.d(TAG, "ticking 1s");
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+1:00"));
            Date currentLocalTime = cal.getTime();
            long diffInMs = currentLocalTime.getTime() - lastUpdatedLocalTime.getTime();
            long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);
            tv.setText(String.format("+%d",diffInSec));
            if(diffInSec > NETWORK_TIMEOUT)
            {
                applyTimeoutView();
            }
            last_cmd.setVisibility(View.INVISIBLE);

            if((counter % 2 == 0) && (pendingCommand == 1)){
                sendCommand(commandName, commandString);
                last_cmd.setText(commandName);
                last_cmd.setVisibility(View.VISIBLE);
            } else if(counter % 5 == 0){
                setStatsOutdated();
                //updateStatus(); //this function can change value of mInterval.
                long now;
                Log.d(TAG, "ticking 3s");
                now = System.currentTimeMillis();
                long delay = now - last_updated;
                if((delay > 4000)  && (spookyEnabled == DISABLE) ){
                    askForStatusUpdate();
                    //Log.d(TAG, "ask for update in loop");
                }
            }



            mMsgHandler.postDelayed(mStatusChecker, 1000);
        }
    };



    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mMsgHandler.removeCallbacks(mStatusChecker);
    }


    /**
     * Called when car data is updated
     * @param carData Car data structure
     */
    @Override
    public void update(CarData carData) {

        mCarData = carData;
        updateLastUpdatedView(carData);
    }

    private void setListViewValue(int pos, String value) {
        ((Map<String, String>) listAdapter.getItem(pos)).put("value", value);
    }


    /**
     * This method disables Throttle pedal
     */
    protected void setNoThrottle(int noThrottle){


        String msg = String.format("%d,%d,%d",
                CMD_CONTROL,
                SUB_CMD_NO_THROTTLE,
                noThrottle);

        queueControlMessage("No throttle", msg);
    }

    /**
     * This method stops the remote agent and resets everything to normal
     */
    protected void stopRemoteAgent(){


        String msg = String.format("%d,%d",
                CMD_CONTROL,
                SUB_CMD_RESET);

        queueControlMessage("Reset", msg);
    }

    /**
      * @param enabled enable or disable
     * @param interval_duration spooky interval in each direction in seconds
     *
     */
    protected void setSpookyMode(int enabled, float interval_duration){


        String msg = String.format("%d,%d,%d,%d",
                CMD_CONTROL,
                SUB_CMD_SPOOKY,
                enabled,
                correctTimeInterval(interval_duration));

        queueControlMessage("Spooky", msg);

    }

    /**
     * This method enable or disables spooky mode
     * @param speed speed limit
     * @param intervalDuration spooky interval in each direction in seconds
     *
     */
    protected void goForward(int speed, float intervalDuration){

        String msg = String.format("%d,%d,%d,%d,%d",
                CMD_CONTROL,
                SUB_CMD_FORWARD,
                validateSpeed(speed),
                DEFAULT_THROTTLE,
                correctTimeInterval(intervalDuration));

        queueControlMessage("Forward", msg);
    }


    /**
     * This method enable or disables spooky mode
     * @param speed limit
     * @param intervalDuration spooky interval in each direction in seconds
     *
     */
    protected void goReverse(int speed, float intervalDuration){

        String msg = String.format("%d,%d,%d,%d,%d",
                CMD_CONTROL,
                SUB_CMD_REVERSE,
                validateSpeed(speed),
                DEFAULT_THROTTLE,
                correctTimeInterval(intervalDuration));

        queueControlMessage("Reverse", msg);
    }

    /**
     * This method enable or disables spooky mode
     * @param enabled limit
     * @param speedLimit spooky interval in each direction in seconds
     *
     */
    protected void setSpeedLimit(int enabled, int speedLimit){

        String msg = String.format("%d,%d,%d,%d",
                CMD_CONTROL,
                SUB_CMD_SPEED_LIMIT,
                enabled,
                validateSpeed(speedLimit));


        queueControlMessage("Speed limit", msg);
    }


    /**
     * This method asks for car status
     */
    protected void askForStatusUpdate(){

        String msg = String.format("%d,%d",
                CMD_CONTROL,
                SUB_CMD_GET_STATE);

        queueControlMessage("Get stats", msg);
    }

    /**
     * Formats and sends a message containing throttle and brake commands with a sequence number
     * The brakeCommand indicator values are 1: set brakes, 2: disable brakes, 0: no action
     */
    protected void queueControlMessage(String name, String str) {

        commandName = name;
        commandString = str;
        pendingCommand = 1;
        /*
        sentStickValue = newStickValue;

        long delta = (System.currentTimeMillis()-last_updated)/1000;
        if (delta > 20) {
            ((ImageView)joystickView.findViewById(R.id.image_connection)).setImageResource(R.drawable.connection_unknown);
        }
        else if (delta > 5) {
            ((ImageView)joystickView.findViewById(R.id.image_connection)).setImageResource(R.drawable.connection_bad);
        }
        else {
            ((ImageView)joystickView.findViewById(R.id.image_connection)).setImageResource(R.drawable.connection_good);
        }
        */

    }


    protected void sendCommand(String commandName, String commandString){
        sendCommand(commandName, commandString, this);
        pendingCommand = 0;
    }

    /**
     * Formats and sends a message containing throttle and brake commands with a sequence number
     * The brakeCommand indicator values are 1: set brakes, 2: disable brakes, 0: no action
     */
    protected int correctTimeInterval(float intervalSeconds) {

        return (int)(intervalSeconds*10);

    }


    /**
     * Set max speed to hard speed limit or speed if it's lower
     */
    protected int validateSpeed(int speed) {

        if(speed>=HARD_SPEED_LIMIT)
            return HARD_SPEED_LIMIT;
        else
            return speed;
    }

    /**
     * Called when a command message acknowledgment is received
     * @param result An string array built from the comma separated values in the ack message
     */
    @Override
    public void onResultCommand(String[] result) {


        /*"MP-0 c101,
        brake_state,
        car_speed,
        motor_speed,
        State of charge,
        config_on,
        seq_number,
        ControlOk"
         */

        Log.d(TAG, "got something");

        long now = System.currentTimeMillis();
        long delay = last_updated - now;
        last_updated = now;
        TextView tv = (TextView)findViewById(R.id.txt_last_updated);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+1:00"));
        lastUpdatedLocalTime = cal.getTime();

        tv.setText("+0");


        if(delay < 5000){
            imgConnection.setImageDrawable(getResources().getDrawable(R.drawable.connection_good));
        } else if(delay < 20000) {
            imgConnection.setImageDrawable(getResources().getDrawable(R.drawable.connection_bad));
        } else {
            imgConnection.setImageDrawable(getResources().getDrawable(R.drawable.connection_unknown));
        }




        if (result.length <= 1)
            return;

        int command = Integer.parseInt(result[0]);
        int resCode = Integer.parseInt(result[1]);

        String cmdMessage = getSentCommandMessage(result[0]);

        if (command == CONTROL_COMMAND && result.length > 3 ) {
            /*
            else if (result[1].equals("2")) { // car is emergency braking
                ((ImageButton)joystickView.findViewById(R.id.imageButtonStop)).setImageResource(R.drawable.ic_restart);
                brakeToggle = true;
                brakeCommand = ((brakeCommand == 1) ? 0 : brakeCommand);
            }
            */

            setListViewValue(0,result[2] + " km/h");
            setListViewValue(1,result[3] + " rpm");
            setListViewValue(2,result[4] + " %");

            listAdapter.notifyDataSetChanged(); // notify the value change to the list

            //Config on
            /*
            if (result[5].equals("1")){
                ((ToggleButton)joystickView.findViewById(R.id.toggleButton))
                        .setTextColor(Color.GREEN);
            }
            else {
                ((ToggleButton)joystickView.findViewById(R.id.toggleButton))
                        .setTextColor(Color.RED);
            }
*/

            // TODO : use result
        }

        else {

            switch (resCode) {
                case 0: // ok
                    Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.msg_ok),
                            Toast.LENGTH_SHORT).show();
                    break;
                case 1: // failed
                    Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.err_failed, result[2]),
                            Toast.LENGTH_SHORT).show();
                    break;
                case 2: // unsupported
                    Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.err_unsupported_operation),
                            Toast.LENGTH_SHORT).show();
                    break;
                case 3: // unimplemented
                    Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.err_unimplemented_operation),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }

//        cancelCommand();
    }

    /**
     * Thread sending a control message periodically
     */
    /*
    private final Runnable controlMessageRunnable = new Runnable() {
        public void run() {
            queueControlMessage();
            controlMessageHandler.postDelayed(this,2000); // periodicity in ms
        }
    };
    */

    @Override
    public void onPause() {
        super.onPause();
        stopRemoteAgent();

        stopRepeatingTask();
        // TODO: STAHP EVERYTHING
        // Another activity is taking focus (this activity is about to be "paused").

        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        cancelCommand();
    }

    @Override
    public void onStop() {
        super.onStop();
        // TODO: STAHP EVERYTHING
        // The activity is no longer visible (it is now "stopped")
        stopRemoteAgent();
    }


    @Override
    public void onResume() {
        super.onResume();

        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

    }

    /**
     * Creates an ArrayList of various car values to display on two rows
     * @return ArrayList
     */
    private ArrayList<Map<String, String>> buildData() {
        ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();
        list.add(putData("Speed", "?"));
        list.add(putData("Rot. speed", "?"));
        list.add(putData("Battery", "?"));
        return list;
    }

    /**
     * Create a HashMap object to insert in an ArrayList
     * @param name First row of the list object
     * @param purpose Second row of the list object
     * @return The list object created
     */
    private HashMap<String, String> putData(String name, String purpose) {
        HashMap<String, String> item = new HashMap<String, String>();
        item.put("name", name);
        item.put("value", purpose);
        return item;
    }


    /**
     * Listener for the speed limit number picker
     */
    private NumberPicker.OnValueChangeListener numberPickerChangeListener =
            new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    switch (picker.getId()) {
                        case R.id.picker_speed:
                            speedLimit = newVal;
                            break;
                        case R.id.picker_duration:
                            actionDuration = newVal;
                            break;
                    }
                }
            };

    /**
     * Listener for the checkboxes
     */
    /*
    private CompoundButton.OnCheckedChangeListener checkboxChangeLister =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    switch (buttonView.getId()) {
                        case R.id.checkBoxAutoBrake:
                            autoBrake = isChecked ? 1 : 0;
                            break;
                        case R.id.checkBoxSpeedLimit:
                            speedLimitEnabled = isChecked ? 1 : 0;
                            break;
                    }
                }
            };
    */

    /**
     * Listener for the scenario dropdown list
     */
    /*
    private AdapterView.OnItemSelectedListener scenarioSelectedHandler =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    // TODO: Do the thing
                    switch (i) {
                        case NO_SCENARIO: // Joystick
                            disc.setEnabled(true);
                            scenario = NO_SCENARIO;
                            break;
                        case SPOOKY_SCENARIO: // Spooky
                            disc.setEnabled(false);
                            scenario = SPOOKY_SCENARIO;
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    // TODO: STAHP TEH CAR OMG
                }
            };

    */

    /**
     * Called when a view has been clicked
     * @param v The clicked view
     */
    @Override
    public void onClick(View v) {
        actionDuration = pickerDuration.getValue();
        speedLimit = pickerSpeedLimit.getValue();

        switch (v.getId()) {
            case R.id.btn_forward:
                goForward(pickerSpeedLimit.getValue(), actionDuration);
                break;
            case R.id.btn_reverse:
                goReverse(pickerSpeedLimit.getValue(), actionDuration);
                break;
            case R.id.btn_speed_limit_disable:
                setSpeedLimit(DISABLE, 0);
                speedLimitEnabled = DISABLE;
                break;
            case R.id.btn_speed_limit_enable:
                setSpeedLimit(ENABLED, speedLimit);
                speedLimitEnabled = ENABLED;
                break;
            case R.id.btn_spooky_off:
                setSpookyMode(DISABLE, actionDuration);
                spookyEnabled = DISABLE;
                break;
            case R.id.btn_spooky_on:
                setSpookyMode(ENABLED, actionDuration);
                spookyEnabled = ENABLED;
                break;
            case R.id.btn_reset:
                stopRemoteAgent();
                spookyEnabled = DISABLE;
                speedLimitEnabled = DISABLE;
                break;
            case R.id.btn_throttle_disable:
                setNoThrottle(ENABLED);
                noThrottleEnabled = ENABLED;
                break;
            case R.id.btn_throttle_enable:
                setNoThrottle(DISABLE);
                noThrottleEnabled = DISABLE;
                break;
        }
    }


    //
    private CompoundButton.OnCheckedChangeListener onOffToggleListener =
            new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        remoteOn = ENABLED;
                        // The toggle is enabled
                    } else {
                        remoteOn = DISABLE;
                        // The toggle is disabled
                    }
                    //queueControlMessage();
                }
            };



    // This updates the part of the view with times shown.
    // It is called by a periodic timer so it gets updated every few seconds.
    public void updateLastUpdatedView(CarData pCarData) {
        // Quick exit if the car data is not there yet...
        if ((pCarData == null) || (pCarData.car_lastupdated == null))
            return;


        // The signal strength indicator
        ImageView iv = (ImageView) findViewById(R.id.img_signal_rssi);
        iv.setImageResource(Ui.getDrawableIdentifier(getActivity(),
                "signal_strength_" + pCarData.car_gsm_bars));
    }


    private void applyTimeoutView(){
        imgConnection.setImageDrawable(getResources().getDrawable(R.drawable.connection_unknown));
        // The signal strength indicator
        ImageView iv = (ImageView) findViewById(R.id.img_signal_rssi);
        iv.setImageResource(Ui.getDrawableIdentifier(getActivity(),
                "signal_strength_" + 0));
    }

    private void setStatsOutdated(){
        setListViewValue(0,"-" + " km/h");
        setListViewValue(1,"-" + " rpm");
        setListViewValue(2,"-" + " %");
        listAdapter.notifyDataSetChanged(); // notify the value change to the list
    }

}