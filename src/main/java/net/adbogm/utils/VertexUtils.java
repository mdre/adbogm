/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.adbogm.utils;

import com.arcadedb.database.Record;
import com.arcadedb.graph.Edge;
import com.arcadedb.graph.Vertex;
import java.util.Map;
import net.adbogm.LogginProperties;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class VertexUtils {

    private final static Logger LOGGER = LogManager.getLogger(VertexUtils.class.getName());

    static {
        Configurator.setLevel(VertexUtils.class.getName(), LogginProperties.VertexUtils);
    }

    /**
     * Check if two vertex are conected
     *
     * @param v1 first vertex
     * @param v2 second vertex
     * @param edgeLabel etiqueta del edge
     * @return true if any conection exist
     */
    public static boolean areConected(Vertex v1, Vertex v2, String edgeLabel) {
        boolean connected = false;
        if ((v1 != null)&&(v2 != null)) {
            Iterable<Vertex> result = v1.getVertices(Vertex.DIRECTION.BOTH, edgeLabel==null?"E":edgeLabel);
            for (Vertex ov : result) {
                if (ov.getIdentity().toString().equals(v2.getIdentity().toString())) {
                    connected = true;
                    break;
                }
            }
        }
        return connected;
    }
    
    
    /**
     * Check if v1 is conected to (out) v2
     *
     * @param v1 source vertex
     * @param v2 target vertex
     * @param edgeLabel edge label to test. Null to test any label.
     * @return true if a conection exist
     */
    public static boolean isConectedTo(Vertex v1, Vertex v2, String edgeLabel) {
        boolean connected = false;
        LOGGER.log(Level.DEBUG, "Verificando edges entre {} y {} a través de la realción {}", new Object[]{v1.getIdentity(), v2.getIdentity(), edgeLabel});
        Iterable<Edge> result = v1.getEdges(Vertex.DIRECTION.OUT, edgeLabel==null?"E":edgeLabel);
        return v1.isConnectedTo(v2.getIdentity(), Vertex.DIRECTION.OUT, edgeLabel);
//        for (Edge oe : result) {
//            if (oe.getOut().getIdentity().toString().equals(v2.getIdentity().toString())) {
//                LOGGER.log(Level.DEBUG, "Conectados por el edge: {}", oe.getIdentity());
//                connected = true;
//                break;
//            }
//        }
//        return connected;
    }
    
    /**
     * Helper class to fill elements with properties throught a Map
     * MMAPI does has not have that funcionality as provided by Tinkerpop
     * Helper para asignación masiva de campos a través de un hashmap. 
     * MMAPI no tiene esa funcionalidad como la proveía Tinkerpop
     * @param oRecord     vértice a completar con los datos
     * @param omap  pares de valores con el nombre de los campos y los valores a asignar.
     */
    public static void fillElement(Record oRecord, Map<String,Object> omap) {
        omap.entrySet().stream().forEach(entry->{
            oRecord.asDocument().modify().set(entry.getKey(), entry.getValue());
        });
    }
}
