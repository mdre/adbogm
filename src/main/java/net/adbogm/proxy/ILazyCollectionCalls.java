/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.adbogm.proxy;

import java.util.Map;

import com.arcadedb.graph.Vertex;
import com.arcadedb.graph.Vertex.DIRECTION;

import net.adbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface ILazyCollectionCalls extends ILazyCalls {
    /**
     * Inicializa la colección y la establece como lazy.
     * @param sm        Transacción sobre la cual realizar los pedidos 
     * @param relatedTo vértice sobre el cual se encuentran las realaciones
     * @param parent    Objeto relacionado al que notificar los cambios
     * @param field     campo a procesar
     * @param c         clase asociada al campo
     * @param d         Si es OUT, se notifican los cambios. Si es IN, se toma como una colección 
     *                  indirecta y SE IGNORAN LOS CAMBIOS.
     */
    public void init(Transaction sm, Vertex relatedTo, IObjectProxy parent, String field, Class<?> c, DIRECTION d);
    public Map<Object,ObjectCollectionState> collectionState();
    
}
