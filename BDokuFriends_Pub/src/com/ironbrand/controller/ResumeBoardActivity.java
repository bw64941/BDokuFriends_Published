/**
 * 
 */
package com.ironbrand.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.ironbrand.bdoku.friends.BoardOpen;
import com.ironbrand.bdoku.friends.R;

/**
 * @author bwinters
 * 
 */
public class ResumeBoardActivity extends Activity implements OnClickListener {

    /*
     * Creates the resume board Alert dialog for game.
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	requestWindowFeature(Window.FEATURE_NO_TITLE);

	LayoutInflater layout = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	View alertDialog = layout.inflate(R.layout.resumeboardalert, null);

	Button noButton = (Button) alertDialog.findViewById(R.id.resumeNo);
	noButton.setOnClickListener(this);

	Button yesButton = (Button) alertDialog.findViewById(R.id.resumeYes);
	yesButton.setOnClickListener(this);

	Button deleteButton = (Button) alertDialog.findViewById(R.id.delete);
	deleteButton.setOnClickListener(this);

	setContentView(alertDialog);
    }

    @Override
    public void onClick(View v) {
	if (v.getId() == R.id.resumeYes) {
	    Intent boardActivity = new Intent(getApplicationContext(), BoardActivity.class);
	    boardActivity.putExtra(BoardActivity.RESUME, true);
	    startActivity(boardActivity);
	    finish();
	} else if (v.getId() == R.id.resumeNo) {

	    Intent boardChooser = new Intent(getApplicationContext(), DifficultyChooserActivity.class);
	    startActivity(boardChooser);
	    finish();
	} else if (v.getId() == R.id.delete) {

	    BoardOpen opener = new BoardOpen(this);
	    opener.deleteExistingFile();

	    Intent difficultyChooser = new Intent(getApplicationContext(), DifficultyChooserActivity.class);
	    startActivity(difficultyChooser);
	    finish();
	} else {

	}
    }
}
