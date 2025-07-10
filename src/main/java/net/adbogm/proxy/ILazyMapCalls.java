package net.adbogm.proxy;

import java.util.Map;

import com.arcadedb.graph.Edge;
import com.arcadedb.graph.Vertex;
import com.arcadedb.graph.Vertex.DIRECTION;

import net.adbogm.Transaction;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface ILazyMapCalls extends ILazyCalls {
    public void init(Transaction t, Vertex relatedTo, IObjectProxy parent, String field, Class<?> keyClass, Class<?> valueClass, DIRECTION d);
    public Map<Object,ObjectCollectionState> collectionState();
    public Map<Object, ObjectCollectionState> getEntitiesState();
    public Map<Object, ObjectCollectionState> getKeyState();
    public Map<Object, Edge> getKeyToEdge();
    public void updateKey(Object originalKey, Edge edge);
}
