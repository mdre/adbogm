package net.adbogm.exceptions;

import net.adbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class UnknownRID extends OGMException {

    public UnknownRID(Transaction transaction) {
        super(transaction);
    }

    public UnknownRID(String message, Transaction transaction) {
        super(message, transaction);
    }
}
