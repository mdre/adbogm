package net.adbogm.exceptions;

import com.arcadedb.exception.NeedRetryException;
import net.adbogm.Transaction;

/**
 *
 * @author jbertinetti
 */
public class OGMException extends RuntimeException {
    
    private boolean canRetry = false;
    
    public OGMException(Transaction transaction) {
    }
    
    public OGMException(String message, Transaction transaction) {
        super(message);
    }

    public OGMException(Throwable cause, Transaction transaction) {
        super(cause.getMessage(), cause);
        canRetry = cause instanceof NeedRetryException;
    }
        
    /**
     * @return Indica si se puede reintentar la acción que se estaba ejecutando
     * para completarla.
     */
    public boolean canRetry() {
        return canRetry;
    }
}
