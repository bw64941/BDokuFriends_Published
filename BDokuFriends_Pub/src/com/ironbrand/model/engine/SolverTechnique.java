/**
 * 
 */
package com.ironbrand.model.engine;

import java.util.ArrayList;

import com.ironbrand.bdoku.friends.Cell;
import com.ironbrand.bdoku.friends.ValuesArray;

/**
 * @author bwinters
 *
 */
public interface SolverTechnique {
    
    public enum CellModStatus { 
	SET_VALUE, REMOVE_POSSIBILITY;
    };
    public void executeTechnique(ValuesArray values);
    public void processCellGrouping(ArrayList<Cell> cells);
}
