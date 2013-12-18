package com.droidplanner.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.droidplanner.DroidPlannerApp;
import com.droidplanner.R;
import com.droidplanner.MAVLink.MavLinkStreamRates;
import com.droidplanner.calibration.RC_CalParameters;
import com.droidplanner.drone.Drone;
import com.droidplanner.drone.DroneInterfaces.DroneEventsType;
import com.droidplanner.drone.DroneInterfaces.OnDroneListner;
import com.droidplanner.fragments.calibration.FragmentSetupRCCompleted;
import com.droidplanner.fragments.calibration.FragmentSetupRCMenu;
import com.droidplanner.fragments.calibration.FragmentSetupRCMiddle;
import com.droidplanner.fragments.calibration.FragmentSetupRCMinMax;
import com.droidplanner.fragments.calibration.FragmentSetupRCOptions;
import com.droidplanner.widgets.FillBar.FillBarWithText;
import com.droidplanner.widgets.RcStick.RcStick;

public class RcSetupFragment extends Fragment implements OnDroneListner,
		OnClickListener {
	private static final int RC_MIN = 1000;
	private static final int RC_MAX = 2000;

	// Extreme RC update rate in this screen
	private static final int RC_MSG_RATE = 50;

	private Drone drone;
	private FragmentManager fragmentManager;
	private RC_CalParameters rcParameters;
	private TextView textViewThrottle, textViewYaw, textViewRoll,
			textViewPitch;

	private FillBarWithText bar5;
	private FillBarWithText bar6;
	private FillBarWithText bar7;
	private FillBarWithText bar8;

	private RcStick stickLeft;

	private RcStick stickRight;

	private Button btnCalibrate;

	private Fragment setupPanel;
	private int calibStep = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fragmentManager = getFragmentManager();
		setupPanel = fragmentManager.findFragmentById(R.id.fragment_setup_rc);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		drone = ((DroidPlannerApp) getActivity().getApplication()).drone;
		View view = inflater.inflate(R.layout.fragment_setup_rc, container,
				false);

		Fragment defPanel = fragmentManager
				.findFragmentById(R.id.fragment_setup_rc);
		if (defPanel == null) {
			defPanel = new FragmentSetupRCMenu();
			((FragmentSetupRCMenu) defPanel).rcSetupFragment = this;

			fragmentManager.beginTransaction()
					.add(R.id.fragment_setup_rc, defPanel).commit();

		}
		setupLocalViews(view);
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		drone.events.addDroneListener(this);
		rcParameters = new RC_CalParameters(drone);
		setupDataStreamingForRcSetup();
	}

	@Override
	public void onStop() {
		super.onStop();
		drone.events.removeDroneListener(this);
		MavLinkStreamRates
				.setupStreamRatesFromPref((DroidPlannerApp) getActivity()
						.getApplication());
	}

	private void setupLocalViews(View view) {
		stickLeft = (RcStick) view.findViewById(R.id.stickLeft);
		stickRight = (RcStick) view.findViewById(R.id.stickRight);
		bar5 = (FillBarWithText) view.findViewById(R.id.fillBar5);
		bar6 = (FillBarWithText) view.findViewById(R.id.fillBar6);
		bar7 = (FillBarWithText) view.findViewById(R.id.fillBar7);
		bar8 = (FillBarWithText) view.findViewById(R.id.fillBar8);

		bar5.setup("CH 5", RC_MAX, RC_MIN);
		bar6.setup("CH 6", RC_MAX, RC_MIN);
		bar7.setup("CH 7", RC_MAX, RC_MIN);
		bar8.setup("CH 8", RC_MAX, RC_MIN);

		textViewRoll = (TextView) view.findViewById(R.id.RCRollPWM);
		textViewPitch = (TextView) view.findViewById(R.id.RCPitchPWM);
		textViewThrottle = (TextView) view.findViewById(R.id.RCThrottlePWM);
		textViewYaw = (TextView) view.findViewById(R.id.RCYawPWM);

		btnCalibrate = (Button) view.findViewById(R.id.buttonRCCalibrate);
		btnCalibrate.setOnClickListener(this);
	}

	private void setupDataStreamingForRcSetup() {
		MavLinkStreamRates.setupStreamRates(drone.MavClient, 1, 0, 1, 1, 1,
				RC_MSG_RATE, 0, 0);
	}

	@Override
	public void onDroneEvent(DroneEventsType event, Drone drone) {
		switch (event) {
		case RC_IN:
			onNewInputRcData();
			break;
		case RC_OUT:
			break;
		default:
			break;
		}

	}

	public void onNewInputRcData() {
		int[] data = drone.RC.in;
		bar5.setValue(data[4]);
		bar6.setValue(data[5]);
		bar7.setValue(data[6]);
		bar8.setValue(data[7]);

		float x, y;
		x = (data[3] - RC_MIN) / ((float) (RC_MAX - RC_MIN)) * 2 - 1;
		y = (data[2] - RC_MIN) / ((float) (RC_MAX - RC_MIN)) * 2 - 1;
		stickLeft.setPosition(x, y);

		x = (data[0] - RC_MIN) / ((float) (RC_MAX - RC_MIN)) * 2 - 1;
		y = (data[1] - RC_MIN) / ((float) (RC_MAX - RC_MIN)) * 2 - 1;
		stickRight.setPosition(x, -y);

		textViewRoll.setText(Integer.toString(data[0]));
		textViewPitch.setText(Integer.toString(data[1]));
		textViewThrottle.setText(Integer.toString(data[2]));
		textViewYaw.setText(Integer.toString(data[3]));
	}

	@Override
	public void onClick(View arg0) {
		if (arg0.equals(btnCalibrate)) {
			if (calibStep > 0)
				cancel();
			else
				changeSetupPanel(1);
		}
	}

	public void changeSetupPanel(int step) {
		calibStep = step;
		switch (step) {
		case 0:
			setupPanel = new FragmentSetupRCMenu();
			((FragmentSetupRCMenu) setupPanel).rcSetupFragment = this;
			break;
		case 1:
			setupPanel = new FragmentSetupRCMinMax();
			((FragmentSetupRCMinMax) setupPanel).rcSetupFragment = this;
			break;
		case 2:
			setupPanel = new FragmentSetupRCMiddle();
			((FragmentSetupRCMiddle) setupPanel).rcSetupFragment = this;
			break;
		case 3:
			setupPanel = new FragmentSetupRCCompleted();
			((FragmentSetupRCCompleted) setupPanel).rcSetupFragment = this;
			break;
		case 5:
			setupPanel = new FragmentSetupRCOptions();
			((FragmentSetupRCOptions) setupPanel).rcSetupFragment = this;
			break;
		}
		fragmentManager.beginTransaction()
				.replace(R.id.fragment_setup_rc, setupPanel).commit();
		if (btnCalibrate != null) {
			if (step > 0) {
				btnCalibrate.setText(R.string.rc_btn_cancel);
				btnCalibrate.setVisibility(View.VISIBLE);
			} else {
				btnCalibrate.setVisibility(View.GONE);
			}
		}
	}

	public void cancel() {
		// TODO Auto-generated method stub
		calibStep = 0;
		changeSetupPanel(0);
	}

	public void updateCalibrationData() {
		// TODO Auto-generated method stub
		calibStep = 0;
		changeSetupPanel(0);		
	}

	public void updateFailsafaData() {
		// TODO Auto-generated method stub
		calibStep = 0;
		changeSetupPanel(0);		
	}

}
