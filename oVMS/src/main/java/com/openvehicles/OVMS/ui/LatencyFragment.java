package com.openvehicles.OVMS.ui;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


import com.openvehicles.OVMS.R;
import com.openvehicles.OVMS.api.OnResultCommandListener;
import com.openvehicles.OVMS.entities.CarData;
import com.openvehicles.OVMS.entities.PingData;
import com.openvehicles.OVMS.utils.CarsStorage;

public class LatencyFragment extends BaseFragment implements OnClickListener, OnResultCommandListener {
	private static final String TAG = "LatencyFragment";

	private CarData mCarData;

    private enum OnGoingTest {APP2CAR, APP2SERVER};

    private OnGoingTest onGoingTest = OnGoingTest.APP2CAR;

    private ArrayList pingLog;
    private int NUMBER_PINGS = 10+2;

    private String CAR_PING_CMD = "105";
    private String SERVER_PING_CMD = "MP-0 A";
    private int pingsRemained = 0;

    private long overallStartTime;
    private long pingStartTime;

    private EditText console;

    private StringBuilder sb;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// init car data:
		mCarData = CarsStorage.get().getSelectedCarData();

		// inflate layout:
		View rootView = inflater.inflate(R.layout.fragment_latency, null);

		if (mCarData.car_type.equals("RT")) {
			// layout changes for Renault Twizy:

			// exchange "Homelink" by "Profile":
			ImageView icon = (ImageView) rootView.findViewById(R.id.tabCarImageHomelink);
			if (icon != null)
				icon.setImageResource(R.drawable.ic_drive_profile);
			TextView label = (TextView) rootView.findViewById(R.id.txt_homelink);
			if (label != null)
				label.setText(R.string.textPROFILE);
		}

        this.console = (EditText)rootView.findViewById(R.id.txt_console);

		return rootView;
	}
	
	@Override
	public void update(CarData pCarData) {
		mCarData = pCarData;

	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		findViewById(R.id.btn_measure_latency_car).setOnClickListener(this);
		findViewById(R.id.btn_measure_latency_server).setOnClickListener(this);

        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	
	@Override
	public void onClick(View v) {
		if (mCarData == null) return;

		switch (v.getId()) {

			case R.id.btn_measure_latency_car:
                Log.d(TAG, "measure latency to car");
                //sendCommand(CAR_PING_CMD, this);
                measureLatencyToCar();
				break;

			case R.id.btn_measure_latency_server:
                Log.d(TAG, "measure latency to server");
                measureLatencyToServer();
				break;

			default:
				v.performLongClick();
		}
	}


    private void measureLatencyToCar(){
        this.pingLog = new ArrayList();
        pingsRemained = NUMBER_PINGS;
        onGoingTest = OnGoingTest.APP2CAR;
        overallStartTime = System.nanoTime();
        ping();
        sb = new StringBuilder();
        sb.append("Measure latency to car...\n");
        console.setText(sb.toString());
    }

    private void measureLatencyToServer(){
        this.pingLog = new ArrayList();
        pingsRemained = NUMBER_PINGS;
        onGoingTest = OnGoingTest.APP2SERVER;
        overallStartTime = System.nanoTime();
        ping();
        sb = new StringBuilder();
        sb.append("Measure latency to server...\n");
        console.setText(sb.toString());
    }

    private void ping(){
        if(pingsRemained>0) {
            if (onGoingTest == OnGoingTest.APP2CAR)
                sendCommand(CAR_PING_CMD, this);
            else
                sendCommand(SERVER_PING_CMD, this);

            pingStartTime = System.nanoTime();
            pingsRemained--;
        }
        else
            finalizeLatencyTest();
    }

    private void finalizeLatencyTest(){
        sb.append("Average round trip time is ");

        sb.append(PingData.nanoToMilliString(PingData.computeAverageDuration(pingLog)));
        sb.append("\nFinished measurements.");
        console.setText(sb.toString());
    }

	@Override
	public void onResultCommand(String[] result) {
		if (result.length <= 1)
			return;


		int command = Integer.parseInt(result[0]);
		int resCode = Integer.parseInt(result[1]);

        if (command == 105 && resCode == 0){
            PingData tmpPingData = new PingData();
            tmpPingData.setSendTime(pingStartTime);
            tmpPingData.setArrivalTime(System.nanoTime());
            pingLog.add(tmpPingData);
            sb.append(PingData.nanoToMilliString(tmpPingData.getDuration()));
            sb.append("\n");
            console.setText(sb.toString());

            ping();
            return;
        }

		String cmdMessage = getSentCommandMessage(result[0]);

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

		cancelCommand();
	}

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "Latency onResume");

        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "Latency onPause");
        cancelCommand();

        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onDestroyView() {
        cancelCommand();
        super.onDestroyView();
    }
}
