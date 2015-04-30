package com.openvehicles.OVMS.ui;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.openvehicles.OVMS.R;
import com.openvehicles.OVMS.api.OnResultCommandListener;
import com.openvehicles.OVMS.entities.CarData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    int sequenceNumber;
    int brakeCommand;
    int remoteOn;
    int autoBrake;
    int speedLimit;
    int speedLimitEnabled;
    int timeout;
    int scenario;
    long last_updated;

    private final Handler controlMessageHandler = new Handler();

    private SimpleAdapter listAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        newStickValue = 0;
        sentStickValue = 0;
        brakeToggle = false;

        sequenceNumber = 0;
        brakeCommand = 0;
        remoteOn = 0;
        autoBrake = 1;
        speedLimit = 10;
        speedLimitEnabled = 0;
        timeout = 50;
        last_updated = -30000;

        scenario = 0;

        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);


        joystickView = inflater.inflate(R.layout.joystick_view, container, false);

        // Assign listener to widgets

        ToggleButton button_on_off = (ToggleButton) joystickView.findViewById(R.id.toggleButton);
        ImageButton button_stop = (ImageButton) joystickView.findViewById(R.id.imageButtonStop);
        CheckBox checkbox_autobrake = (CheckBox) joystickView.findViewById(R.id.checkBoxAutoBrake);
        CheckBox checkbox_speedlimit = (CheckBox) joystickView.findViewById(R.id.checkBoxSpeedLimit);
        NumberPicker numberPicker_speedlimit = (NumberPicker) joystickView.findViewById(R.id.numberPicker);

        button_on_off.setOnCheckedChangeListener(onOffToggleListener);
        button_stop.setOnClickListener(this);
        checkbox_autobrake.setOnCheckedChangeListener(checkboxChangeLister);
        checkbox_speedlimit.setOnCheckedChangeListener(checkboxChangeLister);
        numberPicker_speedlimit.setOnValueChangedListener(numberPickerChangeListener);

        // Populate the numberpicker
        numberPicker_speedlimit.setMaxValue(80);
        numberPicker_speedlimit.setMinValue(0);
        numberPicker_speedlimit.setValue(10);
        numberPicker_speedlimit.setWrapSelectorWheel(false);

        disc = (ImageView) joystickView.findViewById(R.id.imageView);
        disc.setOnTouchListener(joystickMotionListener);

        // Center the joystick

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

        // Populate the spinner with scenarios

        Spinner spinner = (Spinner) joystickView.findViewById(R.id.spinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.scenarios_array));
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(scenarioSelectedHandler);

        controlMessageHandler.postDelayed(controlMessageRunnable,1000);

        // Populate listview with initial values

        ArrayList<Map<String, String>> list = buildData();
        String[] from = { "name", "value" };
        int[] to = { android.R.id.text1, android.R.id.text2 };

        listAdapter = new SimpleAdapter(getActivity(), list,
                android.R.layout.simple_list_item_2, from, to);
        ((ListView)joystickView.findViewById(R.id.listView)).setAdapter(listAdapter);


        // Inflate the layout for this fragment
        return joystickView;
    }

    /**
     * Called when car data is updated
     * @param carData Car data structure
     */
    @Override
    public void update(CarData carData) {

    }

    private void setListViewValue(int pos, String value) {
        ((Map<String, String>)listAdapter.getItem(pos)).put("value",value);
    }

    /**
     * Defines the joystick value, and send the control message if it differs by
     * at least 80 points of percentage from the last value sent
     * @param value The new value
     */
    private void setStickValue(int value) {

        newStickValue = value;

        if (Math.abs(newStickValue - sentStickValue) > 80
                || (newStickValue == 0 && sentStickValue != 0) ) {
            sendControlMessage();
        }

    }

    /**
     * Formats and sends a message containing throttle and brake commands with a sequence number
     * The brakeCommand indicator values are 1: set brakes, 2: disable brakes, 0: no action
     */
    protected void sendControlMessage() {
        String msg = "101,0,0,0,0";

        msg = String.format("101,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d",
                newStickValue > 0 ? newStickValue : 0,
                newStickValue < 0 ? Math.abs(newStickValue) : 0,
                (brakeCommand == 1 ? 1 : 0),
                (brakeCommand == 2 ? 1 : 0),
                sequenceNumber,
                autoBrake,
                remoteOn,
                speedLimit,
                speedLimitEnabled,
                timeout,
                scenario);

        sequenceNumber = (sequenceNumber + 1) % 100;

        sendCommand("Control", msg, this);
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

    }

    /**
     * Called when a command message acknowledgment is received
     * @param result An string array built from the comma separated values in the ack message
     */
    @Override
    public void onResultCommand(String[] result) {

        last_updated = System.currentTimeMillis();

        if (result.length >= 0)
        {
            if (result[0].equals("101") ) {
                if (result[1].equals("0")) { // car is not braking
                    ((ImageButton)joystickView.findViewById(R.id.imageButtonStop)).setImageResource(R.drawable.ic_stop);
                    brakeToggle = false;
                    brakeCommand = ((brakeCommand == 2) ? 0 : brakeCommand);
                }
                else if (result[1].equals("2")) { // car is emergency braking
                    ((ImageButton)joystickView.findViewById(R.id.imageButtonStop)).setImageResource(R.drawable.ic_restart);
                    brakeToggle = true;
                    brakeCommand = ((brakeCommand == 1) ? 0 : brakeCommand);
                }

                setListViewValue(0,result[2] + " km/h");
                setListViewValue(1,result[3] + " rpm");
                setListViewValue(2,result[4] + " %");

                listAdapter.notifyDataSetChanged(); // notify the value change to the list

                if (result[5].equals("1")){
                    ((ToggleButton)joystickView.findViewById(R.id.toggleButton))
                            .setTextColor(Color.GREEN);
                }
                else {
                    ((ToggleButton)joystickView.findViewById(R.id.toggleButton))
                            .setTextColor(Color.RED);
                }

            }
            else if (result[0].equals("102"))
            {

            }

            // TODO : use result
        }
//        cancelCommand();
    }

    /**
     * Thread sending a control message periodically
     */
    private final Runnable controlMessageRunnable = new Runnable() {
        public void run() {
            sendControlMessage();
            controlMessageHandler.postDelayed(this,2000); // periodicity in ms
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        brakeCommand = 1;
        sendControlMessage();
        // TODO: STAHP EVERYTHING
        // Another activity is taking focus (this activity is about to be "paused").
    }

    @Override
    public void onStop() {
        super.onStop();
        brakeCommand = 1;
        sendControlMessage();
        // TODO: STAHP EVERYTHING
        // The activity is no longer visible (it is now "stopped")
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        brakeCommand = 1;
        sendControlMessage();
        // TODO: STAHP EVERYTHING
        // The activity is about to be destroyed.
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
                        case R.id.numberPicker:
                            speedLimit = newVal;
                            break;
                    }
                }
            };

    /**
     * Listener for the checkboxes
     */
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

    /**
     * Listener for the scenario dropdown list
     */
    private AdapterView.OnItemSelectedListener scenarioSelectedHandler =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    // TODO: Do the thing
                    switch (i) {
                        case 0: // Joystick
                            disc.setEnabled(true);
                            scenario = 0;
                            break;
                        case 1: // Spooky
                            disc.setEnabled(false);
                            scenario = 1;
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    // TODO: STAHP TEH CAR OMG
                }
            };



    /**
     * Called when a view has been clicked
     * @param v The clicked view
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageButtonStop:
                if (scenario != 0) {
                    scenario = 0;
                    brakeCommand = 1;
                    ((Spinner) joystickView.findViewById(R.id.spinner)).setSelection(0);
                    sendControlMessage(); // enable brakes
                }
                else {
                    if (brakeToggle) {
                        brakeCommand = 2;
                        sendControlMessage(); // disable brakes
                    } else {
                        brakeCommand = 1;
                        sendControlMessage(); // enable brakes
                    }
                }
                break;
        }
    }

    private CompoundButton.OnCheckedChangeListener onOffToggleListener =
            new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        remoteOn = 1;
                        // The toggle is enabled
                    } else {
                        remoteOn = 0;
                        // The toggle is disabled
                    }

                }
            };

    /**
     * Called when the joystick is touched
     */
    private View.OnTouchListener joystickMotionListener =
            new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
                    int[] coord = new int [2];
                    joystickView.getLocationOnScreen(coord);
                    int yOffset = coord[1];
                    int correctedY = 0, relativeY = 0;
                    int maxHeight = joystickView.getHeight();
                    int radius = disc.getHeight()/2;

                    switch (event.getAction())
                    {
                        case MotionEvent.ACTION_MOVE:
                            relativeY = (int) event.getRawY() - yOffset;
                            correctedY = (int) (relativeY + radius > maxHeight * 0.9 ?
                                    maxHeight * 0.9 - radius : (relativeY - radius < maxHeight * 0.1 ?
                                    maxHeight * 0.1 + radius : relativeY));

                            params.topMargin = correctedY - radius;
                            setStickValue((int)((correctedY-maxHeight*0.1-radius)*200/(maxHeight*0.8-2*radius)-100)*-1);

                            disc.setLayoutParams(params);
                            break;

                        case MotionEvent.ACTION_UP:
                            params.topMargin = maxHeight/2 - radius;
                            disc.setLayoutParams(params);
                            setStickValue(0);
                            break;
                    }

                    return true;
                }
            };

}
