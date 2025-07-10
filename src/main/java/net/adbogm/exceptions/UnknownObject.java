package net.adbogm.exceptions;

import net.adbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class UnknownObject extends OGMException {

    public UnknownObject(Transaction transaction) {
        super(transaction);
    }
    
}
