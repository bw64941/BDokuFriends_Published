/**
 * 
 */
package com.ironbrand.view;

import java.util.ArrayList;

import android.view.View;
import android.widget.PopupWindow.OnDismissListener;

import com.ironbrand.bdoku.friends.Board;
import com.ironbrand.bdoku.friends.R;

/**
 * @author bwinters
 * 
 */
public class ValueQuickActionBar extends QuickAction implements OnDismissListener {

    private ArrayList<ActionItem> valuesArray = new ArrayList<ActionItem>();
    private ActionItem clearActionItem = new ActionItem();
    private int selectedValue = -99;

    public ValueQuickActionBar(View anchor, ArrayList<Integer> userPencilValues) {
	super(anchor);
	obtainPossibilityButtons();
	this.setOnDismissListener(this);
    }

    /*
     * Obtain buttons from view xml.
     */
    private void obtainPossibilityButtons() {
	for (int i = 1; i <= Board.ROWS; i++) {
	    final ActionItem item = new ActionItem();
	    item.setTitle(String.valueOf(i));
	    valuesArray.add(item);
	}
	clearActionItem.setTitle(anchor.getContext().getResources().getString(R.string.clearButtonText));
	populateValueBarListeners();
    }

    /**
     * Populate action bar with 1-9 action items.
     */
    private void populateValueBarListeners() {
	for (ActionItem possibility : valuesArray) {
	    final int possibilityChosen = Integer.valueOf(possibility.getTitle());
	    possibility.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    selectedValue = possibilityChosen;
		    dismiss();
		}
	    });
	    this.addActionItem(possibility);
	}
	clearActionItem.setOnClickListener(new View.OnClickListener() {
	    @Override
	    public void onClick(View v) {
		selectedValue = 0;
		dismiss();
	    }
	});
	this.addActionItem(clearActionItem);
    }

    @Override
    public void onDismiss() {
	super.dismiss();
	if (selectedValue >= 0) {
	    BoardView boardView = (BoardView) anchor;
	    boardView.setValueInSelectedCell(selectedValue);
	}
    }
}
