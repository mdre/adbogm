package net.adbogm.proxy;

import com.arcadedb.database.Document;
import com.arcadedb.graph.Edge;
import com.arcadedb.graph.Vertex;


/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public interface IObjectProxy {
    
    // used to detect new vertex
    public boolean ___isNew();
    public void ___setAsNew(boolean b);
    
    public void ___setDeletedMark();
    public boolean ___isDeleted();
    
    public Document ___getElement();
    public Vertex ___getVertex();
    public String ___getRid();
    public void ___setVertex(Vertex v);
    public void ___injectRid();
//    public void ___uptadeVersion();
    
    public Edge ___getEdge();
    public void ___setEdge(Edge v);
    
    public Class<?> ___getBaseClass();
    public Object ___getProxiedObject();
    
    public boolean ___isValid();
    
    public void ___setDirty();
    public boolean ___isDirty() ;
    public void ___removeDirtyMark();
    
    public void ___commit();
    public void ___reload();
    public void ___rollback();
    public void ___loadLazyLinks();
    public void ___eagerLoad();
    public void ___fullLoad();
    public void ___commitSuccessful();
    
    public void ___setAuditLogLabel(String label);
    public String ___getAuditLogLabel();
}
