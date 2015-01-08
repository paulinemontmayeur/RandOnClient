package com.insa.randon.controller;

import static com.insa.randon.services.Constants.JSON_OBJECT;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.insa.randon.R;
import com.insa.randon.model.Hike;
import com.insa.randon.services.HikeServices;
import com.insa.randon.utilities.ErrorCode;
import com.insa.randon.utilities.TaskListener;

public class FinishHikeActivity extends BaseActivity {
	private static final String DISTANCE_UNIT = " m";

	Context context;
	private Hike hike;
	private Boolean nameAlreadyExists;

	private TextView distanceTextView, durationTextView;
	private EditText nameEditText;
	private Button saveButton;
	private CheckBox shareCheck;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_finish_hike);
		getActionBar().setTitle(R.string.app_name);

		context = this;
		nameAlreadyExists=false;

		Intent intent = getIntent();
		this.hike = (Hike)intent.getParcelableExtra(MapActivity.EXTRA_HIKE);

		distanceTextView = (TextView) findViewById(R.id.distance_done_finish_textView);
		nameEditText = (EditText) findViewById(R.id.editText_hike_name);
		durationTextView = (TextView) findViewById(R.id.duration_finish_textView);
		saveButton = (Button) findViewById(R.id.button_share_hike);
		shareCheck = (CheckBox) findViewById(R.id.check_share_hike);


		//Verify if hike name already exists
		nameEditText.addTextChangedListener(new TextWatcher(){
			TaskListener nameExistsListener = new TaskListener() {
				@Override
				public void onSuccess(String content) {
					try {
						System.out.println(content);
						JSONObject alreadyExistJSON = new JSONObject(content);
						nameAlreadyExists=alreadyExistJSON.getBoolean(JSON_OBJECT);
						if(nameAlreadyExists){
							Toast.makeText(context,R.string.name_already_exists, Toast.LENGTH_SHORT).show();
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onFailure(ErrorCode errCode) {
					if (errCode == ErrorCode.REQUEST_FAILED){
						Toast.makeText(context,R.string.request_failed, Toast.LENGTH_SHORT).show();
					} else if (errCode == ErrorCode.FAILED){
						Toast.makeText(context, R.string.request_failed, Toast.LENGTH_SHORT).show();
					}
				}
			};

			public void afterTextChanged(Editable s) {
				HikeServices.hikeNameExist(nameEditText.getText().toString(), nameExistsListener);
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){
				
			}
		});


		distanceTextView.setText(this.hike.getDistance() + DISTANCE_UNIT);    
		durationTextView.setText(this.hike.getDuration());
	}

	private void backToHistory()
	{
		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra(HomeActivity.FRAGMENT_EXTRA, HikeListFragment.class.getName());
		startActivity(intent);
		finish();
	}

	public void onButtonClick(View view) {
		if(!nameEditText.getText().toString().isEmpty()){
			TaskListener createHikeListener = new TaskListener() {
				@Override
				public void onSuccess(String content) {
					Toast.makeText(context, R.string.share_hike_succeded, Toast.LENGTH_SHORT).show();			
					setResult(RESULT_OK);
					finish();
				}

				@Override
				public void onFailure(ErrorCode errCode) {
					if (errCode == ErrorCode.REQUEST_FAILED){
						Toast.makeText(context,R.string.request_failed, Toast.LENGTH_SHORT).show();
					} else if (errCode == ErrorCode.FAILED){
						Toast.makeText(context, R.string.request_failed, Toast.LENGTH_SHORT).show();
					}
				}
			};

			if(view == saveButton && !nameAlreadyExists) //We save the hike and share it
			{
				boolean isPrivate = !shareCheck.isChecked();
				String name = nameEditText.getText().toString();
				hike.setName(name);
				HikeServices.createHike(hike, isPrivate, createHikeListener);
			} else {
				Toast.makeText(context,R.string.name_already_exists, Toast.LENGTH_SHORT).show();
			}
		} else {
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setMessage(R.string.name_your_hike);
			alertDialogBuilder.setNeutralButton(R.string.neutral_button, null);
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}
	}
}
