package net.adbogm.exceptions;

import com.arcadedb.graph.Vertex;
import net.adbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ReferentialIntegrityViolation extends OGMException {
    
    public ReferentialIntegrityViolation(Vertex referencedVertex, Transaction transaction) {
        super(String.format("El vértice %s aún tiene referencias entrantes.",
                referencedVertex.getIdentity()), transaction);
    }


    public ReferentialIntegrityViolation(String message, Transaction transaction) {
        super(message, transaction);
    }
    
}
