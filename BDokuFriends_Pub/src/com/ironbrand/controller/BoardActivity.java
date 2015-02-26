package com.ironbrand.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.facebook.model.GraphUser;
import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.ironbrand.bdoku.friends.Board;
import com.ironbrand.bdoku.friends.BoardOpen;
import com.ironbrand.bdoku.friends.BoardSaver;
import com.ironbrand.bdoku.friends.Cell;
import com.ironbrand.bdoku.friends.PredefinedBoardList;
import com.ironbrand.bdoku.friends.R;
import com.ironbrand.bdoku.friends.SavedBoard;
import com.ironbrand.model.engine.SolverStep;
import com.ironbrand.view.BoardView;

public class BoardActivity extends Activity implements OnClickListener, OnCheckedChangeListener {

    // public static final String TAG = BoardActivity.class.getName();
    public static final String RESUME = "resume";
    public static final String DIFFICULTY_CHOSEN = "difficulty";
    public static final String BOARD_DIFFICULTY_EASY = "Easy";
    public static final String BOARD_DIFFICULTY_MEDIUM = "Medium";
    public static final String BOARD_DIFFICULTY_HARD = "Hard";
    public static final String TIMER_VALUE = "gameTime";
    public static final int SOLVED_TIME_DISPLAY_POST = 100;
    public static final int POST_GAME_TIME = 200;
    public static final int ROWS = Board.ROWS;
    public static final int COLUMNS = Board.COLUMNS;
    private static Board board = null;
    private BoardView boardView = null;
    private Animation openFadeAnimation = null;
    private boolean solved = false;
    private ViewFlipper viewFlipper = null;
    private TextView timeText = null;
    private TextView fbTextArea = null;

    private static final String APP_ID = "235388389885329";
    private Facebook facebook = null;
    private String[] PERMISSIONS = new String[] { "publish_stream", "read_stream", "photo_upload" };
    private AsyncFacebookRunner asyncFBRunner = null;
    private static final String TOKEN = "access_token";
    private static final String EXPIRES = "expires_in";
    private static final String KEY = "facebook-credentials";
    private static String picturePostId = "";
    private static String commentDataFromPost = "";
    private static Handler fbHandler = new Handler();
    private String facebookMessage = null;
    private byte[] imageCaptured = null;
    private boolean responseLoopStarted = false;

    /*
     * Creates main board view and creates background board entity.
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	requestWindowFeature(Window.FEATURE_NO_TITLE);
	setContentView(R.layout.board);

	// Look up the AdView as a resource and load a request.
	AdView adView = (AdView) this.findViewById(R.id.adView);
	adView.loadAd(new AdRequest());

	boardView = (BoardView) findViewById(R.id.boardView);
	boardView.setDrawingCacheEnabled(true);

	openFadeAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
	openFadeAnimation.reset();

	viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
	viewFlipper.setInAnimation(this, R.anim.rail);

	Button back = (Button) findViewById(R.id.backButton);
	back.setOnClickListener(this);

	Button post = (Button) findViewById(R.id.postButton);
	post.setOnClickListener(this);

	RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.boardLayout);
	relativeLayout.setAnimation(openFadeAnimation);

	Button undo = (Button) findViewById(R.id.undo);
	undo.setOnClickListener(this);

	Button checkWork = (Button) findViewById(R.id.validate);
	checkWork.setOnClickListener(this);

	ToggleButton pencilToggle = (ToggleButton) findViewById(R.id.pencilToggle);
	pencilToggle.setOnCheckedChangeListener(this);

	Button shareButton = (Button) findViewById(R.id.share);
	shareButton.setOnClickListener(this);

	Button save = (Button) findViewById(R.id.save);
	save.setOnClickListener(this);

	fbTextArea = (TextView) findViewById(R.id.facebookFeedView);
	fbTextArea.setMovementMethod(new ScrollingMovementMethod());

	Board board = (Board) getLastNonConfigurationInstance();
	if (board == null) {
	    solved = false;
	    /*
	     * Initialize the board chosen from parent activity and solve
	     * background board.
	     */
	    int rc = showBoard();
	    if (rc == 0) {
		boardView.requestFocus();
	    }
	}

	facebook = new Facebook(APP_ID);
	asyncFBRunner = new AsyncFacebookRunner(facebook);
	commentDataFromPost = getResources().getString(R.string.waitingForFBResponseText);

	// start Facebook Login
	Session.openActiveSession(this, true, new Session.StatusCallback() {

	    // callback when session changes state
	    @Override
	    public void call(Session session, SessionState state, Exception exception) {
		if (session.isOpened()) {
		    // make request to the /me API
		    Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {

			// callback after Graph API response with user object
			@Override
			public void onCompleted(GraphUser user, Response response) {
			    if (user != null) {
				Toast.makeText(getApplicationContext(), "Hello " + user.getName() + "!", Toast.LENGTH_SHORT).show();
			    }
			}
		    });
		}
	    }
	});
    }

    /*
     * Update facebook response text
     */
    public void updateFbResponseText(String text) {
	fbTextArea = (TextView) findViewById(R.id.facebookFeedView);
	fbTextArea.setText(text);
    }

    /*
     * Update timer view from game timer.
     */
    public void updateTimer(String time) {
	timeText = (TextView) findViewById(R.id.timerText);
	timeText.setText(time);
    }

    /*
     * Returns the predefined board to play given chosen board from dialog box
     * on splash screen.
     */
    private int showBoard() {
	int rc = 0;
	int[][] predefinedBoardValuesArray = null;
	BoardOpen opener = new BoardOpen(this);

	boolean resumeInProgressGame = getIntent().getBooleanExtra(BoardActivity.RESUME, false);

	if (resumeInProgressGame == true) {
	    ArrayList<ArrayList<Integer>> savedPencilValues = null;
	    opener.open();
	    SavedBoard savedInProgressBoard = opener.getSavedBoard();
	    predefinedBoardValuesArray = savedInProgressBoard.getArrayValues();
	    savedPencilValues = savedInProgressBoard.getUserPlacedPencilValuesIndex();
	    board = Board.getBoard(predefinedBoardValuesArray);
	    ArrayList<Integer> userPlacedValuesIndexes = savedInProgressBoard.getUserPlacedValues();
	    if (userPlacedValuesIndexes.size() > 0) {
		for (Integer userPlacedValueIndex : userPlacedValuesIndexes) {
		    board.getValues().get(userPlacedValueIndex).setUserPlaced(true);
		}
	    }

	    if (savedPencilValues != null && !savedPencilValues.isEmpty()) {
		int cellIndex = 0;
		for (Cell cell : board.getValues()) {
		    cell.setUserPencilValues(savedPencilValues.get(cellIndex));
		    cellIndex++;
		}
	    }

	    board.solveBackGroundBoard();
	} else {
	    PredefinedBoardList listOfPredefinedBoards = PredefinedBoardList.getSavedBoardList(this);
	    String difficultyChosen = getIntent().getStringExtra(BoardActivity.DIFFICULTY_CHOSEN);
	    SavedBoard predefinedBoard = listOfPredefinedBoards.getPredefinedBoardWithDifficulty(difficultyChosen);
	    if (predefinedBoard != null) {
		predefinedBoardValuesArray = predefinedBoard.getArrayValues();
		board = Board.getBoard(predefinedBoardValuesArray);
		board.solveBackGroundBoard();
	    } else {
		rc = -1;
	    }
	}

	return rc;
    }

    /**
     * Checks all of the user placed values on the board.
     */
    private void checkWorkDoneOnBoard() {
	if (solved == false) {
	    if (checkBoardValidity() == false) {
		showToast(getResources().getString(R.string.mistakeText));
	    } else {
		showToast(getResources().getString(R.string.doingGreatText));
	    }
	}
    }

    /**
     * Save Board
     * 
     * @return
     */
    private void saveBoard() {
	if (solved == false) {
	    if (checkBoardValidity() == true) {
		new BoardSaver(this).execute(board);
	    } else {
		showToast(getResources().getString(R.string.correctMistakesText));
	    }
	}
    }

    /*
     * Checks for mistakes on the board and highlights erroneous cell.
     * 
     * @return
     */
    private boolean checkBoardValidity() {
	boolean validBoard = true;
	for (int cellIndex = 0; cellIndex < board.getSolvedValuesArray().size(); cellIndex++) {
	    if (!board.getValues().get(cellIndex).isEmpty() && board.getSolvedValuesArray().get(cellIndex).getValue() != board.getValues().get(cellIndex).getValue()) {
		validBoard = false;
		boardView.highlightSelectedArea(board.getValues().get(cellIndex).getCol(), board.getValues().get(cellIndex).getRow(), BoardView.HIGHLIGHT_ERROR);
	    }
	}

	return validBoard;
    }

    /*
     * Get next Solved Value not figure out by player for Hint.
     */
    private void getRandomHint() {
	if (solved == false) {
	    Cell hintCell = null;

	    /*
	     * Pick a random cell on the board that is either empty or been
	     * incorrectly solved by user. Give correct hint for that cell.
	     */
	    while (hintCell == null) {
		Random randon = new Random();
		int randomInex = randon.nextInt(board.getValues().size());
		if ((board.getValues().get(randomInex).isUserPlaced() || board.getValues().get(randomInex).isEmpty()) && board.getSolvedValuesArray().get(randomInex).getValue() != board.getValues().get(randomInex).getValue()) {
		    hintCell = board.getSolvedValuesArray().get(randomInex);
		}
	    }

	    boardView.highlightSelectedArea(hintCell.getCol(), hintCell.getRow(), BoardView.HIGHLIGHT_HINT);
	}
    }

    /*
     * Clears a cell on the board after clear button has been pressed.
     */
    private void undoLastMove() {
	if (board.getValues().getHistory().size() > 0 && solved == false) {
	    SolverStep setValueStep = null;

	    if (board.getValues().getHistory().peek().getAlgorithm().equals(SolverStep.USER_PLACED)) {
		setValueStep = board.getValues().getHistory().pop();
		setValueStep.getCell().clear();
		boardView.highlightSelectedArea(setValueStep.getCell().getCol(), setValueStep.getCell().getRow(), BoardView.HIGHLIGHT);
		board.getValues().evaluateInitialValues();
	    }

	    while (!board.getValues().getHistory().isEmpty() && board.getValues().getHistory().peek().getAlgorithm().equals(SolverStep.SYSTEM_POSSIBILITY_REMOVED)) {
		SolverStep removePossibilitiesStep = board.getValues().getHistory().pop();
		removePossibilitiesStep.getCell().getRemainingPossibilities().add(removePossibilitiesStep.getValue());
		if (board.getValues().getHistory().empty()) {
		    break;
		}
	    }
	}
    }

    /*
     * Toggle Pencil Mode for game.
     */
    public void togglePencilMode(boolean value) {
	if (solved == false) {
	    boardView.setPencilModeOn(value);
	}
    }

    /**
     * @param showSolved
     *            the solved to set
     */
    public void setSolvedOn() {
	Intent boardActivity = new Intent(getApplicationContext(), RestartActivity.class);
	boardActivity.putExtra("gameTime", timeText.getText());
	startActivityForResult(boardActivity, BoardActivity.SOLVED_TIME_DISPLAY_POST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	super.onActivityResult(requestCode, resultCode, data);
	if (requestCode == BoardActivity.SOLVED_TIME_DISPLAY_POST) {
	    // Log.d(TAG, "SOLVED_TIME_DISPLAY_POST returned");
	    if (resultCode == BoardActivity.POST_GAME_TIME) {
		// Log.d(TAG, "POST_GAME_TIME returned");
		String postMessage = getResources().getString(R.string.solvedPostString);
		facebookMessage = postMessage + "\nSolve Time: " + timeText.getText();
		imageCaptured = boardView.getOriginalStateOfBoard();
		if (facebook.isSessionValid()) {
		    postImageToWall(facebookMessage, imageCaptured);
		} else {
		    facebook.authorize(this, PERMISSIONS, new LoginDialogListener());
		}
	    } else {
		finish();
	    }
	}
	facebook.authorizeCallback(requestCode, resultCode, data);
	Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    private Runnable fbResponseTask = new Runnable() {
	public void run() {
	    requestCommentsFromPost();
	    updateFbResponseText(commentDataFromPost);
	    fbHandler.postDelayed(this, 3000);
	}
    };

    /*
     * Saves the Facebook token and set the expire time for the token.
     */
    public boolean saveCredentials(Facebook facebook) {
	Editor editor = getApplicationContext().getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
	editor.putString(TOKEN, facebook.getAccessToken());
	editor.putLong(EXPIRES, facebook.getAccessExpires());
	return editor.commit();
    }

    /*
     * Restores the token from shared prefs when application is re-entered.
     */
    public boolean restoreCredentials(Facebook facebook) {
	SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(KEY, Context.MODE_PRIVATE);
	facebook.setAccessToken(sharedPreferences.getString(TOKEN, null));
	facebook.setAccessExpires(sharedPreferences.getLong(EXPIRES, 0));
	return facebook.isSessionValid();
    }

    /*
     * Clears the shared prefs for facebook token.
     */
    public static void clear(Context context) {
	Editor editor = context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
	editor.clear();
	editor.commit();
    }

    class LoginDialogListener implements DialogListener {
	public void onComplete(Bundle values) {
	    saveCredentials(facebook);
	    // Log.d(TAG, "Connected to Facebook with token " +
	    // facebook.getAccessToken() + " APP ID " + facebook.getAppId());
	    showToast("Connected to Facebook with token " + facebook.getAccessToken() + " APP ID " + facebook.getAppId());

	    postImageToWall(facebookMessage, imageCaptured);
	}

	public void onFacebookError(FacebookError error) {
	    // Log.d(TAG, "Authentication with Facebook failed!");
	    showToast("Facebook Error");
	}

	public void onError(DialogError error) {
	    // Log.d(TAG, "Authentication with Facebook failed!");
	    showToast("Facebook Error");
	}

	public void onCancel() {
	    // Log.d(TAG, "Authentication with Facebook cancelled!");
	    showToast("Facebook Cancelled");
	}
    }

    public void postImageToWall(String message, byte[] image) {
	Bundle params = new Bundle();
	params.putByteArray("photo", image);
	params.putString("caption", message);

	asyncFBRunner.request("me/photos", params, "POST", new BoardImageUploadListener(), null);
    }

    public void requestCommentsFromPost() {
	Bundle params = new Bundle();
	params.putString("id", picturePostId);

	asyncFBRunner.request("comments", params, "GET", new CommentsListener(), null);
    }

    public class BoardImageUploadListener implements RequestListener {

	@Override
	public void onComplete(final String response, final Object state) {
	    try {
		// process the response here: (executed in background thread)
		// Log.d(TAG, "Response: " + response.toString());
		JSONObject json = Util.parseJson(response);
		picturePostId = json.getString("id");
		showToast("Game Board Posted to Facebook!");
		if (boardView.isSolved() == true) {
		    runOnUiThread(new Runnable() {

			@Override
			public void run() {
			    finish();
			}
		    });
		}
		startResponseTask();
	    } catch (JSONException e) {
		// Log.w(TAG, "JSON Error in response");
	    } catch (FacebookError e) {
		// Log.w(TAG, "Facebook Error: " + e.getMessage());
	    }
	}

	@Override
	public void onIOException(IOException e, Object state) {
	    Log.e("Facebook", e.getMessage());
	    showToast("Facebook Error");
	}

	@Override
	public void onFileNotFoundException(FileNotFoundException e, Object state) {
	    Log.e("Facebook", e.getMessage());
	    showToast("Facebook Error");
	}

	@Override
	public void onMalformedURLException(MalformedURLException e, Object state) {
	    Log.e("Facebook", e.getMessage());
	    showToast("Facebook Error");
	}

	@Override
	public void onFacebookError(FacebookError e, Object state) {
	    Log.e("Facebook", e.getMessage());
	    showToast("Facebook Error");
	}
    }

    public class CommentsListener implements RequestListener {

	@Override
	public void onComplete(final String response, final Object state) {
	    try {
		// Get the data (see above)
		JSONObject json = Util.parseJson(response);
		// Get the element that holds the earthquakes ( JSONArray )
		JSONArray comments = json.getJSONArray("data");

		HashMap<String, String> map = new HashMap<String, String>();
		// Loop the Array
		for (int i = 0; i < comments.length(); i++) {
		    JSONObject e = comments.getJSONObject(i);
		    map.put("id", String.valueOf(i));
		    JSONObject from = e.getJSONObject("from");
		    map.put("from", from.getString("name"));
		    map.put("message", e.getString("message"));
		    map.put("time", e.getString("created_time"));
		}

		if (map.get("from") != null) {
		    if (!commentDataFromPost.equals(getResources().getString(R.string.waitingForFBResponseText)) && !commentDataFromPost.contains(map.get("time"))) {
			commentDataFromPost = commentDataFromPost + "\n" + map.get("from") + " says:\n" + map.get("message") + "\nPosted at " + map.get("time");

			runOnUiThread(new Runnable() {

			    @Override
			    public void run() {
				ViewFlipper viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
				viewFlipper.showNext();
			    }
			});

		    } else {
			commentDataFromPost = map.get("from") + " says:\n" + map.get("message") + "\nPosted at " + map.get("time");
		    }
		}
	    } catch (JSONException e) {
		showToast("JSON Error");
	    } catch (FacebookError e) {
		showToast("Facebook Error");
	    }

	}

	@Override
	public void onIOException(IOException e, Object state) {
	    Log.e("Facebook", e.getMessage());
	    showToast("Facebook Error");
	}

	@Override
	public void onFileNotFoundException(FileNotFoundException e, Object state) {
	    Log.e("Facebook", e.getMessage());
	    showToast("Facebook Error");
	}

	@Override
	public void onMalformedURLException(MalformedURLException e, Object state) {
	    Log.e("Facebook", e.getMessage());
	    showToast("Facebook Error");
	}

	@Override
	public void onFacebookError(FacebookError e, Object state) {
	    Log.e("Facebook", e.getMessage());
	    showToast("Facebook Error");
	}
    }

    private void showToast(final String message) {
	runOnUiThread(new Runnable() {
	    @Override
	    public void run() {
		Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	    }
	});
    }

    /**
     * Start the response thread listener after connecting to facebook
     */
    public void startResponseTask() {
	if (!responseLoopStarted) {
	    fbHandler.removeCallbacks(fbResponseTask);
	    fbHandler.postDelayed(fbResponseTask, 100);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
	super.onPause();
	// Log.d(TAG, "Pause called");
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
	solved = board.isSolved();
	facebook.extendAccessTokenIfNeeded(this, null);
	super.onResume();
	// Log.d(TAG, "Resume called");
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onRetainNonConfigurationInstance()
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
	return Board.getBoard();
    }

    @Override
    public void onClick(View v) {
	if (v.getId() == R.id.undo) {
	    undoLastMove();
	} else if (v.getId() == R.id.validate) {
	    checkWorkDoneOnBoard();
	} else if (v.getId() == R.id.share) {
	    viewFlipper.showNext();
	} else if (v.getId() == R.id.postButton) {
	    restoreCredentials(facebook);
	    if (APP_ID == null) {
		showToast("Facebook Applicaton ID must be " + "specified before running this example: see FbAPIs.java");
		return;
	    }

	    facebookMessage = getResources().getString(R.string.helpPostString);
	    imageCaptured = boardView.captureCanvas();

	    if (facebook.isSessionValid()) {
		postImageToWall(facebookMessage, imageCaptured);
	    } else {
		facebook.authorize(this, PERMISSIONS, new LoginDialogListener());
	    }
	} else if (v.getId() == R.id.save) {

	    saveBoard();
	} else if (v.getId() == R.id.backButton) {

	    viewFlipper.showPrevious();
	} else {

	}
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	if (buttonView.getId() == R.id.pencilToggle) {

	    togglePencilMode(isChecked);
	} else {
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
	super.onDestroy();
	// Log.d(TAG, "Destroy called");
	if (boardView.getThread().isAlive()) {
	    boardView.getThread().setRunning(false);
	    boardView.getmHandler().removeCallbacks(boardView.getmUpdateTimeTask());
	}
	fbHandler.removeCallbacks(fbResponseTask);
	responseLoopStarted = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	if (item.getItemId() == R.id.solve) {

	    this.solved = true;
	    boardView.setSolved(true);
	    setSolvedOn();
	} else if (item.getItemId() == R.id.hint) {

	    getRandomHint();
	} else {
	}
	return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.board_menu, menu);
	return true;
    }
}
