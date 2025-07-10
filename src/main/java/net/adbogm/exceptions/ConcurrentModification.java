package net.adbogm.exceptions;

import com.arcadedb.exception.ConcurrentModificationException;
import net.adbogm.Transaction;


/**
 * Indica que un mismo elemento fue modificado por distintas transacciones a la
 * vez.
 * 
 * @author jbertinetti
 */
public class ConcurrentModification extends OGMException {
    
    public ConcurrentModification(ConcurrentModificationException cause,
            Transaction transaction) {
        super(cause, transaction);
    }
    
}
