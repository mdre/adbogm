package net.adbogm.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.arcadedb.database.MutableDocument;
import com.arcadedb.graph.MutableEdge;
import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex.DIRECTION;

import asm.proxy.IEasyProxyInterceptor;
import com.arcadedb.database.Document;
import com.arcadedb.graph.Edge;
import com.arcadedb.graph.Vertex;
import net.adbogm.LogginProperties;
import net.adbogm.ObjectMapper;
import net.adbogm.ObjectStruct;
import net.adbogm.SessionManager;
import net.adbogm.Transaction;
import net.adbogm.annotations.Audit.AuditType;
import net.adbogm.annotations.DontLoadLinks;
import net.adbogm.annotations.Eager;
import net.adbogm.annotations.Indirect;
import net.adbogm.annotations.RemoveOrphan;
import net.adbogm.cache.ClassDef;
import net.adbogm.exceptions.CollectionNotSupported;
import net.adbogm.exceptions.DuplicateLink;
import net.adbogm.exceptions.InvalidObjectReference;
import net.adbogm.exceptions.ObjectMarkedAsDeleted;
import net.adbogm.utils.ReflectionUtils;
import net.adbogm.utils.ThreadHelper;
import net.adbogm.utils.VertexUtils;
import net.dirtydetector.agent.ITransparentDirtyDetector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ObjectProxy implements IObjectProxy, IEasyProxyInterceptor{

    private final static Logger LOGGER = LogManager.getLogger(ObjectProxy.class.getName());

    static {
        Configurator.setLevel(ObjectProxy.class.getName(), LogginProperties.ObjectProxy);
    }
    // the real object (as a IObjectProxy, ie EnhancedByCGLib)
    private Object ___proxiedObject;

    private final Class<?> ___baseClass;

    // Vértice desde el que se obtiene el objeto.
    // private MutableVertex baseVertex;
    private Document ___baseElement;
    
    // dado que Arcade no tiene isNew como Orient, agrego esta propiedad que 
    // debería actulizarse después del primer commit. El único método que la establecería
    // en true sería store()
    private boolean ___isNew = false;
    
    // permite marcar el objeto como inválida en caso que se haga un rollback 
    // sobre un objeto que nunca se persistió.
    private boolean ___isValidObject = true;

    private final Transaction ___transaction;

    private boolean ___dirty = false;

    // determina si ya se han cargado los links o no
    private boolean ___loadLazyLinks = true;

    // determina si el objeto ya ha sido completamente inicializado.
    // sirve para impedir que se invoquen a los métodos durante el setup inicial del construtor.
    private boolean ___objectReady = false;

    // si esta marca está activa indica que el objeto ha sido eliminado de la base de datos 
    // y toda comunicación con el mismo debe ser abortada
    private boolean ___deletedMark = false;

    // etiqueta utilizada en combinación con el Auditor para guardar la referencia que se desee
    private String ___auditLogLabel;

    public ObjectProxy(Class c, MutableDocument e, Transaction t) {
        this.___baseClass = c;
        this.___baseElement = e;
        this.___transaction = t;
    }
    
    
    // EasyProxy interceptor 
    @Override
    public Object intercept(Object target, Method method, Method superMethod, Object... args
                    ) throws Throwable {   
        
        // response object
        Object res = null;
        //this.___proxiedObject = self;
        // el estado del objeto se debe poder consultar siempre
        //=====================================================
        LOGGER.log(Level.TRACE, ">=====================================================");
        LOGGER.log(Level.TRACE, target.getClass().getName() + " : "+ this.___baseElement.getRecord().getIdentity()+ " > method: "+method.getName()+"  superMethod: "+(superMethod!=null?superMethod.getName():"NULL"));
        LOGGER.log(Level.TRACE, "--->"+args.length);
        LOGGER.log(Level.TRACE, "param: " + (args==null?"NULL":Arrays.toString(args)));
        LOGGER.log(Level.TRACE, "<<<<<param");
        
        LOGGER.log(Level.TRACE, "=====================================================");
        
        String debugLabel = this.___baseElement.getRecord().getIdentity()+ " > method: "+method.getName();
        
        if (method.getName().equals("___isValid")) {
            return this.___isValid();
        }
        
        if (method.getName().equals("___isDeleted")) {
            return this.___isDeleted();
        }

        if (method.getName().equals("___getVertex")) {
            return this.___getVertex();
        }
        if (method.getName().equals("___getEdge")) {
            return this.___getEdge();
        }
        if (method.getName().equals("___getElement")) {
            return this.___getElement();
        }

        if (method.getName().equals("___getRid")) {
            return this.___getRid();
        }
        
        if (method.getName().equals("___getBaseClass")) {
            return this.___getBaseClass();
        }

        if (!this.___isValidObject) {
            LOGGER.log(Level.DEBUG, "El objeto está marcado como inválido!!!");
            // throw new InvalidObjectReference(this.___transaction);
            throw new InvalidObjectReference();
        }

        if (method.getName().equals("___rollback")) {
            if (this.___objectReady) {
                this.___rollback();
                return true;
            }
        }

        if (this.___isNew) {
            LOGGER.log(Level.DEBUG, "RID nuevo. No procesar porque el store preparó todo y no hay nada que recuperar de la base.");
            this.___loadLazyLinks = false;
        }
        
        //if object was deleted:
        if (this.___deletedMark) {
            switch (method.getName()) {
                case "equals":
                case "hashCode":
                case "toString":
                    if (!this.___transaction.getSessionManager().getConfig().
                            isEqualsAndHashCodeOnDeletedThrowsException()) {
                        return superMethod.invoke(target, args);
                    }
                default:
                    throw new ObjectMarkedAsDeleted("The object " + this.___baseElement.getIdentity().toString() + 
                            " was deleted from the database. Trying to call to " + method.getName());
            }
        }
        
        // modificar el llamado
        switch (method.getName()) {
//            case "___uptadeVersion":
//                if (this.___objectReady) {
//                    this.___uptadeVersion();
//                }
//                break;
            case "___setAsNew":
                this.___setAsNew((boolean)args[0]);
                break;
            case "___isNew":
                if (this.___objectReady) {
                    res = this.___isNew();
                }
                break;
            case "___injectRid":
                if (this.___objectReady) {
                    this.___injectRid();
                }
                break;
            case "___getProxiedObject":
                if (this.___objectReady) {
                    res = this.___getProxiedObject();
                }
                break;
            case "___loadLazyLinks":
                if (this.___objectReady) {
                    this.___loadLazyLinks();
                }
                break;
            case "___eagerLoad":
                if (this.___objectReady) {
                    this.___eagerLoad();
                }
                break;
            case "___fullLoad":
                if (this.___objectReady) {
                    this.___fullLoad();
                }
                break;
            case "___isDirty":
                if (this.___objectReady) {
                    res = this.___isDirty();
                }
                break;
            case "___setDirty":
                if (this.___objectReady) {
                    this.___setDirty();
                }
                break;
            case "___removeDirtyMark":
                if (this.___objectReady) {
                    this.___removeDirtyMark();
                }
                break;
            case "___commit":
                /**
                 * FIXME: se podría evitar si se controlara si los links se
                 * han cargado o no al momento de hacer el commit para
                 * evitar realizar el load sin necesidad.
                 */
                if (this.___objectReady) {
                    if (this.___loadLazyLinks) {
                        this.___loadLazyLinks();
                    }
                    this.___commit();
                }
                break;
            case "___reload":
                if (this.___objectReady) {
                    this.___reload();
                }
                break;
            case "___commitSuccessful":
                if (this.___objectReady) {
                    this.___commitSuccessful();
                }
                break;
            case "___setDeletedMark":
                this.___setDeletedMark();
                break;
//            case "___updateElement":
//                this.___updateElement();
//                break;
            
            case "___setEdge":
                this.___setEdge((MutableEdge)args[0]);
                break;
            case "___setVertex":
                this.___setVertex((MutableVertex)args[0]);
                break;

            case "___setAuditLogLabel":
                this.___setAuditLogLabel((String)args[0]);
                break;
            case "___getAuditLogLabel":
                res = this.___getAuditLogLabel();
                break;
                
            
            case "___tdd___setDirty":
                res = superMethod.invoke(target, args);
                break;
            case "___tdd___isDirty":
                res = superMethod.invoke(target, args);
                break;
            case "___tdd___clearDirty":
                res = superMethod.invoke(target, args);
                break;

            default:
                // invoke the method on the real object with the given params:

                if (method.getName().equals("toString")) {
                    try {
                        //if object doesn't have toString defined, we implement one on the fly
                        ReflectionUtils.findMethod(this.___baseClass, "toString", (Class<?>[]) null);
                    } catch (NoSuchMethodException nsme) {
                        res = this.___baseElement.getIdentity().toString(); //returns rid
                        break;
                    }
                }

                if (this.___loadLazyLinks) {
                    boolean methodTriggersLoadLazyLink = true;
                    if (method.getName().equals("equals") || method.getName().equals("hashCode")) {
                        methodTriggersLoadLazyLink &= this.___transaction.getSessionManager().
                                getConfig().isEqualsAndHashCodeTriggerLoadLazyLinks();
                    }
                    methodTriggersLoadLazyLink &= !method.isAnnotationPresent(DontLoadLinks.class);
                    if (this.___objectReady && methodTriggersLoadLazyLink) {
                        this.___loadLazyLinks();
                    }
                }
                
                try {
                    if (superMethod == null) {
                        String bbMName = Arrays.asList(target.getClass().getDeclaredMethods()).stream().filter(m->m.getName().startsWith(method.getName()+"$ac")).findFirst().get().getName();
                        LOGGER.log(Level.TRACE, "SuperMethod NULL!!! ---> redefine superMethod> "+bbMName);
                        
                        Class[] p = new Class[args.length];
                        for (int i = 0; i < args.length; i++) {
                            p[i] = args[i].getClass().getName().contains("EasyProxy")?args[i].getClass().getSuperclass():args[i].getClass();
                        }
                        
                        LOGGER.log(Level.TRACE, "Method parameterTypes: " + Arrays.asList(method.getParameterTypes()));
                        LOGGER.log(Level.TRACE, "Args parameterTypes: " + Arrays.asList(p));
                        Method m = target.getClass().getMethod(bbMName, p);
                        res = m.invoke(target, args);
                    } else {
                            res = superMethod.invoke(target, args);
                    }
                } catch (InvocationTargetException ex) {
                        throw ex.getCause();
                }
                // verificar si hay diferencias entre los objetos dependiendo de la estrategia seleccionada.
                if (this.___objectReady) {
                    switch (this.___transaction.getSessionManager().getActivationStrategy()) {
                        case CLASS_INSTRUMENTATION:
                            // si se está usando la instrumentación de clase, directamente verificar en el objeto
                            // cual es su estado.
                            LOGGER.log(Level.TRACE, "o: {} ITrans: {}", new Object[]{target.getClass().getName(), target instanceof ITransparentDirtyDetector});
                            if (((ITransparentDirtyDetector) target).___tdd___isDirty()) {
                                LOGGER.log(Level.TRACE, "objeto {} marcado como dirty por ASM. Agregarlo a la lista de pendientes.", target.getClass().getName());
                                this.___setDirty();
                            }
                    }
                }
                break;
        }
        // return the result
        LOGGER.log(Level.TRACE, "<<<<<<<<<<<<<<<<<<<<< INTERCEPT END: "+debugLabel);
        return res;
    }

    /**
     * Establece el objecto como nuevo. Solo Store debería llamar a este método
     */
    @Override
    public void ___setAsNew(boolean b){
        this.___isNew = b;
    }
    
    @Override
    public boolean ___isNew() {
        return this.___isNew;
    }
    
    /**
     * Si se establece el AuditLogLabel todos los objetos que sean cargados a partir 
     * de el objeto actual llevaran por defecto el mismo label.
     * 
     * @param label  a utilizar en los logs por el Auditor
     */
    
    public void ___setAuditLogLabel(String label) {
        this.___auditLogLabel = label;
    }
    
    
    public String ___getAuditLogLabel() {
        return this.___auditLogLabel;
    }
    
    
    /**
     * Establece el objeto base sobre el que trabaja el proxy.
     *
     * @param po objeto de referencia
     */
    
    public void ___setProxiedObject(Object po) {
        this.___proxiedObject = po;
        this.___injectRid();
        this.___objectReady = true;
    }
    
    
    @Override
    public void ___injectRid() {
        //inject RID if the field is defined
        ClassDef classdef = getClassDef();
        if (classdef.ridField != null) {
            LOGGER.log(Level.TRACE, classdef.ridField+" = "+___baseElement.getIdentity().toString());
            objectMapper().setFieldValue(___proxiedObject, classdef.ridField, ___baseElement.getIdentity().toString());
        }
    }
    
    
//    @Override
//    public void ___uptadeVersion() {
//        ClassDef classdef = getClassDef();
//        if (classdef.versionField != null) {
//            objectMapper().setFieldValue(___proxiedObject, classdef.versionField, ___baseElement.get("@version"));
//        }
//    }


    /**
     * retorna el MutableDocument asociado a este proxi.
     *
     * @return referencia al Document
     */
    
    @Override
    public Document ___getElement() {
        return this.___baseElement;
    }

    /**
     * retorna el vértice asociado a este proxi o null en caso que no exista
     * uno.
     *
     * @return referencia al MutableVertex
     */
    @Override
    public Vertex ___getVertex() {
        if (this.___baseElement instanceof Vertex) {
            return this.___baseElement.asVertex();
        } else {
            return null;
        }
    }


    /**
     * Returns the Record ID of the associated element.
     *
     * @return RID of element.
     */
    @Override
    public String ___getRid() {
        if (this.___baseElement != null) {
            return this.___baseElement.getIdentity().toString();
        } else {
            return null;
        }
    }


    /**
     *
     * establece el elemento base como un vértice.
     *
     * @param v vétice de referencia
     */
    @Override
    public void ___setVertex(Vertex v) {
        this.___baseElement = v;
    }


    /**
     * retorna el vértice asociado a este proxi o null en caso que no exista
     * uno.
     *
     * @return la referencia al Edge
     */
    
    @Override
    public Edge ___getEdge() {
        if (this.___baseElement instanceof Edge) {
            return this.___baseElement.asEdge();
        } else {
            return null;
        }
    }


    /**
     *
     * establece el elemento base como un vértice.
     *
     * @param e Edge de referencia
     */
    
    @Override
    public void ___setEdge(Edge e) {
        this.___baseElement = e;
    }

    
    @Override
    public Object ___getProxiedObject() {
        return this.___proxiedObject;
    }

    
    @Override
    public Class<?> ___getBaseClass() {
        return this.___baseClass;
    }

    
    @Override
    public void ___setDeletedMark() {
        this.___deletedMark = true;
    }

    
    @Override
    public boolean ___isDeleted() {
        return this.___deletedMark;
    }

    

    /**
     * Creates a new temporary valid vertex and associates it with the proxy.
     */
//    
//    @Override
//    public void ___updateElement() {
//        if (this.___baseElement.getInternalStatus() == ORecordElement.STATUS.NOT_LOADED) {
//            this.___baseElement = this.___transaction.getCurrentGraphDb().newVertex(
//                    getClassDef().entityName);
//        }
//    }
    
    
    /**
     * Load all links of object.
     */
    
    @Override
    public synchronized void ___loadLazyLinks() {
        if (this.___loadLazyLinks) {
//            this.___transaction.initInternalTx();
            
            LOGGER.log(Level.DEBUG, "Base class: {}", this.___baseClass.getSimpleName());
            LOGGER.log(Level.DEBUG, "iniciando loadLazyLinks...");
            boolean currentDirtyState = this.___isDirty();
            // marcar que ya se han incorporado todo los links
            this.___loadLazyLinks = false;

            if (this.___baseElement instanceof MutableVertex) {
                // hydrate links attributes: process all links and indirectLinks
                this.updateLinks();
                this.updateIndirectLinks();
            }
            // resetear dirty si corresponde.
            this.___dirty = currentDirtyState;

//            this.___transaction.closeInternalTx();
        }
    }
    
    
    private void updateLinks() {
        ClassDef classdef = getClassDef();
        Vertex ov = this.___baseElement.asVertex();
        LOGGER.log(Level.DEBUG, "Processing {} links ", classdef.links.size());
        this.loadLinks(ov, classdef, classdef.links, false, false);
    }
    
    
    /**
     * Load the links of the given vertex.
     * 
     * @param ov The vertex.
     * @param linksFields Fields to hydrate.
     * @param onlyEager If true, only fields marked as Eager, else all fields.
     * @param indirect If must load indirect links instead of direct.
     */
    
    private void loadLinks(Vertex ov, ClassDef classdef, HashMap<String, Class<?>> linksFields, boolean onlyEager, boolean indirect) {
        for (Map.Entry<String, Class<?>> entry : linksFields.entrySet()) {
            try {
                String field = entry.getKey();
                Class<?> fc = entry.getValue();
                Field fLink = classdef.fieldsObject.get(field);
                
                if (onlyEager && !fLink.isAnnotationPresent(Eager.class)) {
                    continue; //only eager, discard field. Go to next
                }
                
                String graphRelationName;
                DIRECTION direction;
                
                if (indirect) {
                    //the name configured in annotation must be used
                    Indirect in = fLink.getAnnotation(Indirect.class);
                    graphRelationName = in.linkName();
                    direction = DIRECTION.IN;
                    LOGGER.log(Level.DEBUG, "Se ha detectado un indirect. Linkname = {}", new Object[]{in.linkName()});
                } else {
                    graphRelationName = classdef.entityName + "_" + field;
                    direction = DIRECTION.OUT;
                }
                LOGGER.log(Level.DEBUG, "Field: {}.{}   Class: {}  RelationName: {}",
                        new String[]{this.___baseClass.getSimpleName(), field,
                            fc.getSimpleName(), graphRelationName});
                
                boolean duplicatedLinkGuard = false;
                
                // retrieve vertex from database 
                for (Vertex vertice : ov.getVertices(direction, graphRelationName)) {
                    LOGGER.log(Level.DEBUG, "hydrate innerO: {}", vertice.getIdentity());
                    
                    if (!duplicatedLinkGuard) {
                        /*
                         * FIXME: esto genera una dependencia cruzada.
                         * Habría que revisar
                         * como solucionarlo. Esta llamada se hace para
                         * que quede el objeto
                         * mapeado
                         */
                        this.___transaction.addToTransactionCache(this.___getRid(), ___proxiedObject);

                        // si es una interface llamar a get solo con el RID.
                        Object innerO = fc.isInterface() ? this.___transaction.get(vertice.getIdentity().toString()) :
                                this.___transaction.get(fc, vertice.getIdentity().toString());
                        
                        LOGGER.log(Level.DEBUG, "Inner object {}: {}  FC: {}   innerO.class: {} hashCode: {}", new Object[]{
                            field, vertice, fc.getSimpleName(), innerO.getClass().getSimpleName(), System.identityHashCode(innerO)});
                        fLink.set(this.___proxiedObject, fc.cast(innerO));
                        duplicatedLinkGuard = true;

                        // replicate the AuditLogLabel to inner objects
                        if (this.___auditLogLabel != null && innerO instanceof IObjectProxy) {
                            ((IObjectProxy)innerO).___setAuditLogLabel(this.___auditLogLabel);
                        }
                        
                        ___transaction.decreseTransactionCache();
                    } else if (false) {
                        throw new DuplicateLink();
                    }
                    LOGGER.log(Level.DEBUG, "FIN hydrate innerO: {}^^^^^^^^^^^^^^^^^^^^^^^^^^^^^", vertice.getIdentity());
                }
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                LOGGER.log(Level.ERROR, "ERROR",ex);
            }
        }
    }

    
    private void updateIndirectLinks() {
        boolean preservDirtyState = this.___dirty;

        MutableVertex ov = (MutableVertex) this.___baseElement;
        ClassDef classdef = getClassDef();

        LOGGER.log(Level.DEBUG, "procesando {} indirected links", new Object[]{classdef.indirectLinks.size()});
        this.loadLinks(ov, classdef, classdef.indirectLinks, false, true);

        
        //ANALYZE WELL IF THIS CODE IS VALID OR UNNECESSARY AND REMOVE IT
        
//        // forzar la recarga de las colecciones.
//        LOGGER.log(Level.DEBUG, "Refrescando las colecciones indirectas...");
        // ********************************************************************************************
        // hidratar las colecciones indirectas
        // procesar todos los indirectLinkslist
        // ********************************************************************************************
//            for (Map.Entry<String, Class<?>> entry : classdef.indirectLinkLists.entrySet()) {
//                try {
//                    // FIXME: se debería considerar agregar una annotation EAGER!
//                    String field = entry.getKey();
//                    Class<?> fc = entry.getValue();
//                    LOGGER.log(Level.DEBUG, "Field: {}   Class: {}", new String[]{field, fc.getName()});
//
//                    Field fLink = classdef.fieldsObject.get(field);
//                    Direction relationDirection = Direction.IN;
//
//                    Indirect in = fLink.getAnnotation(Indirect.class);
//                    String graphRelationName = in.linkName();
//
//                    // si hay Vértices conectados o si el constructor del objeto ha inicializado los vectores, convertirlos
//                    if ((ov.countEdges(relationDirection, graphRelationName) > 0) || (fLink.get(___proxiedObject) != null)) {
//                        this.___transaction.getObjectMapper().collectionToLazy(___proxiedObject, field, fc, ov, ___transaction);
//                    }
//
//                } catch (IllegalAccessException | IllegalArgumentException ex) {
//                    Logger.getLogger(ObjectProxy.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }


        //----------------------------------------------------------------------
        //restart indirect collections to reload them
//        classdef.indirectLinks.keySet().forEach(field -> {
//            Field f = classdef.fieldsObject.get(field);
//                try {
//                    ILazyCalls coll = (ILazyCalls)f.get(this.___proxiedObject);
//                    coll.rollback();
//                } catch (IllegalArgumentException | IllegalAccessException ex) {
//                    LOGGER.log(Level.SEVERE, "Error eager loading link list! " + f, ex);
//            }
//        });
        //----------------------------------------------------------------------

        // volver a establecer el estado de Dirty.
        this.___dirty = preservDirtyState;
    }
    
    
    /**
     * Loads the vertex links marked as eager load.
     */
    
    @Override
    public void ___eagerLoad() {
        ClassDef classdef = getClassDef();
        if (classdef.isEager && this.___baseElement instanceof MutableVertex) {
//            this.___transaction.initInternalTx();
            LOGGER.log(Level.DEBUG, "Eager loading of {}", this.___baseElement);
            
            if (this.___baseClass.isAnnotationPresent(Eager.class)) {
                //eager load of whole class
                this.fullLoad();
            } else {
                MutableVertex ov = (MutableVertex) this.___baseElement;
                this.loadLinks(ov, classdef, classdef.links, true, false); //eager load of links
                this.eagerLoadLinkLists(classdef, classdef.linkLists, true); //eager load of link lists
                this.loadLinks(ov, classdef, classdef.indirectLinks, true, true); //eager load of indirect links
                this.eagerLoadLinkLists(classdef, classdef.indirectLinkLists, true); //eager load of indirect link lists
            }
//            this.___transaction.closeInternalTx();
        }
    }
    
    
    /**
     * Loads all the vertex links.
     */
    
    @Override
    public void ___fullLoad() {
        this.fullLoad();
    }
    
    
    private void fullLoad() {
        ClassDef classdef = getClassDef();
        this.___loadLazyLinks();
        this.eagerLoadLinkLists(classdef, classdef.linkLists, false); //force load of link lists
        this.eagerLoadLinkLists(classdef, classdef.indirectLinkLists, false); //force load of indirect link lists
    }
    
    
    /**
     * Forces the load of collections of links.
     */
    
    private void eagerLoadLinkLists(ClassDef classdef, HashMap<String, Class<?>> linksFields, boolean onlyEager) {
        linksFields.keySet().forEach(field -> {
            Field f = classdef.fieldsObject.get(field);
            if (!onlyEager || f.isAnnotationPresent(Eager.class)) {
                ILazyCalls coll = (ILazyCalls)objectMapper().getFieldValue(this.___proxiedObject, f);
                if (coll != null) coll.forceLoad();
            }
        });
    }

    
    @Override
    public boolean ___isValid() {
        return ___isValidObject;
    }

    
    @Override
    public boolean ___isDirty() {
        return ___dirty;
    }


    /**
     * Marca el objeto como dirty para que sea considerado en el próximo commit
     *
     */
    
    @Override
    public void ___setDirty() {
        if (!this.___dirty) {
            this.___dirty = true;
            // agregarlo a la lista de dirty para procesarlo luego
            LOGGER.log(Level.DEBUG, "Dirty: " + this.___proxiedObject);
            this.___transaction.setAsDirty(this.___proxiedObject);
            LOGGER.log(Level.DEBUG, "Objeto marcado como dirty! <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            LOGGER.log(Level.TRACE, ThreadHelper.getCurrentStackTrace());
        }
    }

    
    @Override
    public void ___removeDirtyMark() {
        this.___dirty = false;
        // verificar la estrategia de activación.
        // si la estrategia es ONCOMMIT se debe validar primero que existan cambios en los objetos
        // antes de proceder.
        if (this.___transaction.getSessionManager().getActivationStrategy() == SessionManager.ActivationStrategy.CLASS_INSTRUMENTATION) {
            LOGGER.log(Level.DEBUG, "CLASS_INSTRUMENTATION Strategy.");
            ((ITransparentDirtyDetector) this.___proxiedObject).___tdd___clearDirty();
        }
    }

    
    @Override
    public synchronized void ___commit() {
        LOGGER.log(Level.DEBUG, "Iniciando ___commit() ....");
        LOGGER.log(Level.DEBUG, "valid: {}", this.___isValidObject);
        LOGGER.log(Level.DEBUG, "dirty: {}", this.___dirty);
        LOGGER.log(Level.DEBUG, "rid: {}", this.___baseElement.getIdentity());
        LOGGER.log(Level.DEBUG, "modified fields: {}", ((ITransparentDirtyDetector)this.___proxiedObject).___tdd___getModifiedFields());
        
        if (this.___dirty ) {            

            // obtener la definición de la clase
            ClassDef cDef = this.___transaction.getObjectMapper().getClassDef(this.___proxiedObject);

            // obtener un mapa actualizado del objeto contenido
            ObjectStruct oStruct = this.___transaction.getObjectMapper().objectStruct(this.___proxiedObject);
            Map<String, Object> omap = oStruct.fields;

            // bajar todo al vértice
            VertexUtils.fillElement(this.___baseElement, omap);
            
            oStruct.removedProperties.forEach(prop -> this.___baseElement.modify().remove(prop));

            // guardar log de auditoría si corresponde.
            if (this.___transaction.isAuditing() ) {
                this.___transaction.auditLog(this, AuditType.WRITE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"UPDATE", omap);
            }
            
            // si se trata de un Vértice
            if (this.___baseElement instanceof Vertex) {
                MutableVertex ov = this.___baseElement.asVertex().modify();
                // Analizar si cambiaron los vértices
                /*
                 * procesar los objetos internos. Primero se deber determinar
                 * si los objetos ya existían en el contexto actual. Si no
                 * existen deben ser creados.
                 */
                for (Map.Entry<String, Class<?>> link : cDef.links.entrySet()) {
                    String field = link.getKey();
                    String graphRelationName = cDef.entityName + "_" + field;
                    // determinar el estado del campo
                    if (oStruct.links.get(field) == null) {
                        // si está en null, es posible que se haya eliminado el objeto
                        // por lo cual se debería eliminar el vértice correspondiente
                        // si es que existe
                        if (ov.getEdges(DIRECTION.OUT, graphRelationName).iterator().hasNext()) {
                            // se ha eliminado el objeto y debe ser removido el Vértice o el Edge correspondiente
                            LOGGER.log(Level.TRACE, "se ha eliminado el objeto y debe ser removido el Vértice o el Edge correspondiente");
                            MutableEdge removeEdge = null;
                            for (Edge edge : ov.getEdges(DIRECTION.OUT, graphRelationName)) {
                                removeEdge = edge.modify();
                                if (this.___transaction.isAuditing()) {
                                    this.___transaction.auditLog(this, AuditType.WRITE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"REMOVE LINK: " + graphRelationName, removeEdge);
                                }
                                this.removeEdge(removeEdge, field);
                            }
                        }
                    } else {
                        Object innerO = oStruct.links.get(field);
                        // verificar si ya está en el contexto. Si fue creado en forma 
                        // separada y asociado con el objeto principal, se puede dar el caso
                        // de que el objeto principal tiene RID y el agregado no.
                        if (innerO instanceof IObjectProxy) {
                            LOGGER.log(Level.TRACE, "Se encontró una relación a un objeto ADMINISTRADO");
                            // el objeto existía.
                            // se debe verificar si el eje entre los dos objetos ya existía.
                            if (!VertexUtils.isConectedTo(ov, ((IObjectProxy) innerO).___getVertex(), graphRelationName)) {
                                // No existe un eje. Se debe crear
                                LOGGER.log(Level.DEBUG, "Los objetos no están conectados. ({} |-{}-|{}",
                                        new Object[]{ov.getIdentity(), graphRelationName, ((IObjectProxy) innerO).___getVertex().getIdentity()});

                                // primero verificar si no existía una relación previa con otro objeto para removerla.
                                Iterator<Edge> toRemove = ov.getEdges(DIRECTION.OUT, graphRelationName).iterator();
                                if (toRemove.hasNext()) {
                                    LOGGER.log(Level.DEBUG, "Existía una relación previa. Se debe eliminar.");
                                    // existé una relación. Elimnarla antes de proceder a establecer la nueva.
                                    //MutableEdge removeEdge = null;
                                    while (toRemove.hasNext()) {
                                        MutableEdge removeEdge = toRemove.next().modify();
                                        //removeEdge = (MutableEdge) edge;
                                        LOGGER.log(Level.DEBUG, "Eliminar relación previa a " + removeEdge.getOut());

                                        if (this.___transaction.isAuditing()) {
                                            this.___transaction.auditLog(this, AuditType.WRITE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"REMOVE LINK: " + graphRelationName, removeEdge);
                                        }
                                        
                                        this.removeEdge(removeEdge, field);
                                        
                                    }
                                }
                                LOGGER.log(Level.TRACE, "vertex out({}: {}",new Object[]{graphRelationName,ov.getEdges(DIRECTION.OUT, graphRelationName)});
                                
                                
                                LOGGER.log(Level.DEBUG, "Agregar un link entre dos objetos existentes.");
                                MutableEdge oe = ov.newEdge(graphRelationName, ((IObjectProxy) innerO).___getVertex());
                                
                                if (this.___transaction.isAuditing()) {
                                    this.___transaction.auditLog(this, AuditType.WRITE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"ADD LINK: " + graphRelationName, oe);
                                }
                            }
                        } else {
                            // el objeto es nuevo
                            LOGGER.log(Level.TRACE, "Se encontró una relación a un objeto NUEVO");
                            // primero verificar si no existía una relación previa con otro objeto para removerla.
                            if (ov.getEdges(DIRECTION.OUT, graphRelationName).iterator().hasNext()) {
                                LOGGER.log(Level.DEBUG, "Existía una relación previa. Se debe eliminar.");
                                // existé una relación. Elimnarla antes de proceder a establecer la nueva.
                                //MutableEdge removeEdge = null;
                                while (ov.getEdges(DIRECTION.OUT, graphRelationName).iterator().hasNext()) {
                                    MutableEdge removeEdge = ov.getEdges(DIRECTION.OUT, graphRelationName).iterator().next().modify();
                                    
                                    LOGGER.log(Level.DEBUG, "Eliminar relación previa a " + removeEdge.getOut());
                                    if (this.___transaction.isAuditing()) {
                                        this.___transaction.auditLog(this, AuditType.WRITE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"REMOVE LINK: " + graphRelationName, removeEdge);
                                    }
                                    this.removeEdge(removeEdge, field);
                                }
                            }

                            // crear la nueva relación
                            LOGGER.log(Level.DEBUG, "innerO nuevo. Crear un vértice y un link");
                            innerO = this.___transaction.store(innerO);
                            this.___transaction.getObjectMapper().setFieldValue(this.___proxiedObject, field, innerO);

                            // si está activa la instrumentación de clases, desmarcar el objeto como dirty
                            if (innerO instanceof ITransparentDirtyDetector) {
                                ((ITransparentDirtyDetector) innerO).___tdd___clearDirty();
                            }

                            //MutableEdge oe = this.___transaction.getCurrentGraphDb().addEdge("class:" + graphRelationName, ov, ((IObjectProxy) innerO).___getVertex(), graphRelationName);
                            MutableEdge oe = ov.newEdge(graphRelationName, ((IObjectProxy) innerO).___getVertex());
                            if (this.___transaction.isAuditing()) {
                                this.___transaction.auditLog(this, AuditType.WRITE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"ADD LINK: " + graphRelationName, oe);
                            }
                        }
                    }
                }

                /**
                 * Procesar los linklists.
                 */
                Field f;
                for (Map.Entry<String, Class<?>> entry : cDef.linkLists.entrySet()) {
                    try {
                        String field = entry.getKey();
                        LOGGER.log(Level.DEBUG, "procesando campo: {} clase: {}",
                                new Object[]{field, this.___proxiedObject.getClass()});

                        f = cDef.fieldsObject.get(field);

                        // preparar el nombre de la relación
                        final String graphRelationName = cDef.entityName + "_" + field;

                        Object collectionFieldValue = f.get(this.___proxiedObject);

                        // verificar si existe algún cambio en la coleccion
                        // ingresa si la colección es distinta de null y
                        // collectionFieldValue es instancia de ILazyCalls y está marcado como dirty
                        // o collectionFieldValue no es instancia de ILazyCalls, lo que 
                        // significa que es una colección nueva y debe ser procesada completamente.
                        if ((collectionFieldValue != null)
                                && ((ILazyCalls.class.isAssignableFrom(collectionFieldValue.getClass()) && ((ILazyCalls) collectionFieldValue).isDirty())
                                || (!ILazyCalls.class.isAssignableFrom(collectionFieldValue.getClass())))) {
                            LOGGER.log(Level.DEBUG, (!ILazyCalls.class.isAssignableFrom(collectionFieldValue.getClass()))
                                    ? "No es instancia de ILazyCalls"
                                    : "Es instancia de Lazy y está marcado como DIRTY");

                            if (collectionFieldValue instanceof List) {
                                ILazyCollectionCalls lazyCollectionCalls;
                                // procesar la colección
                                
                                if (ILazyCollectionCalls.class.isAssignableFrom(collectionFieldValue.getClass())) {
                                    LOGGER.log(Level.DEBUG, "ya implementa ILazyCollectionCalls");
                                    lazyCollectionCalls = (ILazyCollectionCalls) collectionFieldValue;
                                } else {
                                    LOGGER.log(Level.DEBUG, "Colección nueva! Convertir.");
                                    // se ha asignado una colección original y se debe exportar todo
                                    this.___transaction.getObjectMapper().collectionToLazy(this.___proxiedObject, 
                                                                                           field,
                                                                                           this.___baseElement.asVertex(),
                                                                                           this.___transaction);

                                    //recuperar la nueva colección
                                    Collection inter = (Collection) f.get(this.___proxiedObject);

                                    //agregar todos los valores que existían
                                    inter.addAll((Collection) collectionFieldValue);
                                    //preparar la interface para que se continúe con el acceso.
                                    lazyCollectionCalls = (ILazyCollectionCalls) inter;
                                    // reasignar el objeto oCol
                                    collectionFieldValue = f.get(this.___proxiedObject);
                                }

                                List listFieldValue = (List) collectionFieldValue;
                                Map<Object, ObjectCollectionState> colState = lazyCollectionCalls.collectionState();

                                // procesar los elementos presentes en la colección
                                for (int i = 0; i < listFieldValue.size(); i++) {
                                    Object colObject = listFieldValue.get(i);
                                    // verificar el estado del objeto en la colección
                                    if (colState.get(colObject) == ObjectCollectionState.ADDED) {
                                        // si se agregó uno, determinar si era o no manejado por el SM
                                        LOGGER.log(Level.DEBUG, "se ha agregado un objeto a la colección.");
                                        if (!(colObject instanceof IObjectProxy)) {
                                            LOGGER.log(Level.DEBUG, "Objeto nuevo. Insertando en la base y reemplazando el original...");
                                            // no es un objeto que se haya almacenado.
                                            colObject = this.___transaction.store(colObject);
                                            // reemplazar en la colección el objeto por uno administrado
                                            listFieldValue.set(i, colObject);

                                            // si está activa la instrumentación de clases, desmarcar el objeto como dirty
                                            if (colObject instanceof ITransparentDirtyDetector) {
                                                ((ITransparentDirtyDetector) colObject).___tdd___clearDirty();
                                            }

                                        }

                                        // vincular el nodo
                                        //MutableEdge oe = this.___transaction.getCurrentGraphDb().addEdge("class:" + graphRelationName, this.___getVertex(), ((IObjectProxy) colObject).___getVertex(), graphRelationName);
                                        LOGGER.log(Level.TRACE, "creando nueve edge {} --> {}",new String[]{this.___baseElement.getIdentity().toString(),
                                                                                                                        ((IObjectProxy) colObject).___getVertex().getIdentity().toString()
                                                                                                                        });
                                        MutableEdge oe = this.___getVertex().newEdge(graphRelationName, ((IObjectProxy) colObject).___getVertex());
                                        oe.save();
                                        if (this.___transaction.isAuditing()) {
                                            this.___transaction.auditLog(this, AuditType.WRITE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"LINKLIST ADD: " + graphRelationName, oe);
                                        }
                                    }
                                }

                                // procesar los removidos solo si está el anotation en el campo
                                for (Map.Entry<Object, ObjectCollectionState> entry1 : colState.entrySet()) {
                                    Object colObject = entry1.getKey();
                                    ObjectCollectionState colObjState = entry1.getValue();
                                    LOGGER.log(Level.TRACE, "{} : {}", new String[]{
                                                                                            colObject.toString(),
                                                                                            colObjState.toString()
                                                                                            });
                                    if (colObjState == ObjectCollectionState.REMOVED) {
                                        LOGGER.log(Level.TRACE, "objeto borrado: {}", new String[]{((IObjectProxy) colObject).___getVertex().getIdentity().toString()});
                                        // remover el link
                                        // for (MutableEdge edge : ((MutableVertex) this.___baseElement)
                                        //        .getEdges(((IObjectProxy) colObject).___getVertex(),
                                        //                ODirection.OUT,
                                        //                graphRelationName))
                                        
                                        Iterator<Edge> edges = ((MutableVertex) this.___baseElement).getEdges(DIRECTION.OUT, graphRelationName).iterator(); 
                                        while (edges.hasNext()){
                                            MutableEdge edge = edges.next().modify();
                                            LOGGER.log(Level.TRACE, "edge in: {} -- out --> {}",new String[]{edge.getIn().toString(),edge.getOut().toString()});
                                            if (edge.getIn().toString().equals(((IObjectProxy) colObject).___getVertex().getIdentity().toString())) {
                                                if (this.___transaction.isAuditing()) {
                                                    this.___transaction.auditLog(this, AuditType.WRITE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"LINKLIST REMOVE: " + graphRelationName, edge);
                                                }
                                                edge.delete();
                                            }
                                        }
                                        
                                        // si existe la anotación, remover tambien el vertex
                                        if (f.isAnnotationPresent(RemoveOrphan.class)) {
                                            if (this.___transaction.isAuditing()) {
                                                this.___transaction.auditLog(this, AuditType.DELETE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"LINKLIST DELETE: " + graphRelationName, colObject);
                                            }
                                            this.___transaction.delete(colObject);
                                        }
                                    }
                                }

                            } else if (collectionFieldValue instanceof Map) {

                                Map mapFieldValue;
                                // procesar la colección

                                if (ILazyMapCalls.class.isAssignableFrom(collectionFieldValue.getClass())) {
                                    mapFieldValue = (Map) collectionFieldValue;
                                } else {
                                    // se ha asignado una colección original y se debe exportar todo
                                    this.___transaction.getObjectMapper().collectionToLazy(this.___proxiedObject, 
                                                                                           field, 
                                                                                           this.___baseElement.asVertex(), 
                                                                                           this.___transaction);
                                    //recuperar la nueva colección
                                    // Collection inter = (Collection) f.get(this.realObj);
                                    Map inter = (Map) f.get(this.___proxiedObject);
                                    //agregar todos los valores que existían
                                    inter.putAll((Map) collectionFieldValue);
                                    //preparar la interface para que se continúe con el acceso.
                                    mapFieldValue = (Map) inter;
                                }

                                // refrescar los estados
                                ILazyMapCalls lazyMap = (ILazyMapCalls) mapFieldValue;
                                final Map<Object, ObjectCollectionState> keysState = lazyMap.getKeyState();
                                final Map<Object, Edge> keysToEdges = lazyMap.getKeyToEdge();
                                final Map<Object, ObjectCollectionState> entitiesState = lazyMap.getEntitiesState();

                                // recorrer todas las claves del mapa
                                for (Map.Entry<Object, ObjectCollectionState> entry1 : keysState.entrySet()) {
                                    Object key = entry1.getKey();
                                    ObjectCollectionState keyState = entry1.getValue();

                                    LOGGER.log(Level.DEBUG, "imk: {} state: {}", new Object[]{key, keyState});
                                    // para cada entrada, verificar la existencia del objeto y crear un Edge.
                                    Object linkedO = mapFieldValue.get(key);
                                    
                                    if (keyState != ObjectCollectionState.REMOVED &&
                                            !(linkedO instanceof IObjectProxy)) {
                                        LOGGER.log(Level.DEBUG, "Link Map Object nuevo. Crear un vértice y un link");
                                        linkedO = this.___transaction.store(linkedO);
                                        mapFieldValue.replace(key, linkedO);
                                        if (linkedO instanceof ITransparentDirtyDetector) {
                                            ((ITransparentDirtyDetector) linkedO).___tdd___clearDirty();
                                        }
                                    }

                                    // verificar el estado del objeto en la colección.
                                    switch (keyState) {
                                        case ADDED:
                                            LOGGER.log(Level.DEBUG, "-----> agregando un LinkList al Map!");
                                            MutableVertex to = ((IObjectProxy) linkedO).___getVertex().modify();
                                            MutableEdge edge = null;
                                            boolean newEdge = true;
                                            
                                            // if key already a IObjectProxy, check the status of the edge to recreate it if invalid (previous failed commit)
//                                            if (key instanceof IObjectProxy) {
//                                                edge = ((IObjectProxy)key).___getEdge();
//                                                if (edge.getInternalStatus() == ORecordElement.STATUS.LOADED && Objects.equals(to, edge.getTo())) {
//                                                    newEdge = false; // valid edge
//                                                }
//                                            }
                                            
                                            if (newEdge) {
                                                // crear un link entre los dos objetos.
                                                edge = this.___baseElement.asVertex().modify().newEdge(graphRelationName, to);
                                                // actualizar el edge con los datos de la key.
                                                // FIXME: new edges
                                                //this.___transaction.getObjectMapper().fillSequenceFields(key, this.___transaction, null);
                                                VertexUtils.fillElement(edge, this.___transaction.getObjectMapper().simpleMap(key));
                                                edge.save();
                                            }

                                            if (this.___transaction.isAuditing()) {
                                                this.___transaction.auditLog(this, AuditType.WRITE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"LINKLIST ADD: " + graphRelationName, edge);
                                            }
                                            
                                            //update the map with the managed key
                                            lazyMap.updateKey(key, edge);
                                            break;

                                        case NOCHANGE:
                                            // el link no se ha modificado. 
                                            break;

                                        case REMOVED:
                                            // quitar el Edge
                                            Edge oeRemove = keysToEdges.get(key);
                                            if (oeRemove == null) {
                                                throw new IllegalStateException("The edge object couldn't be found. "
                                                        + "Make sure its hashCode is change-proof.");
                                            }
                                            
                                            // verificar si corresponde borrar el vértice en caso de estar marcado con @RemoveOrphan.
                                            boolean removeOrphan = false;
                                            if (f.isAnnotationPresent(RemoveOrphan.class)) {
                                                if (entitiesState.get(linkedO) == ObjectCollectionState.REMOVED) {
                                                    removeOrphan = true;
                                                }
                                            }
                                            removeEdge(graphRelationName, oeRemove.modify(), removeOrphan ?
                                                    (IObjectProxy)linkedO : null);
                                            break;
                                    }
                                }
                                
                                for (Map.Entry<Object, ObjectCollectionState> e : entitiesState.entrySet()) {
                                    Object value = e.getKey();
                                    ObjectCollectionState valueState = e.getValue();
                                    if (value instanceof IObjectProxy && valueState == ObjectCollectionState.REMOVED) {
                                        //we must remove the old edge
                                        IObjectProxy valueop = (IObjectProxy)value;
                                        boolean removeOrphan = f.isAnnotationPresent(RemoveOrphan.class);
                                        
                                        valueop.___getVertex().getEdges(DIRECTION.IN, graphRelationName).forEach(edge -> {
                                            if (Objects.equals(this.___baseElement, edge.asEdge().getInVertex())) {
                                                removeEdge(graphRelationName, edge.modify(), removeOrphan ? valueop : null);
                                            }
                                        });
                                    }
                                }
                                
                            } else {
                                LOGGER.log(Level.DEBUG, "********************************************");
                                LOGGER.log(Level.DEBUG, "field: {}", field);
                                LOGGER.log(Level.DEBUG, "********************************************");
                                throw new CollectionNotSupported(collectionFieldValue.getClass().getName());
                            }
                        }
                    } catch (IllegalArgumentException | IllegalAccessException ex) {
                        LOGGER.log(Level.ERROR, "ERROR",ex);
                    }
                }
            }
            
            //if we must update version field, enqueue in transaction
            if (cDef.versionField != null) {
                this.___transaction.processAfterDbCommit((IObjectProxy)this.___proxiedObject);
            }
            
            // grabar los cambios
            this.___baseElement.modify().save();
            
//            this.___transaction.closeInternalTx();
        }
        LOGGER.log(Level.DEBUG, "fin commit ----");
    }
    
    
    private void removeEdge(String graphRelationName, MutableEdge edge, IObjectProxy vertexToRemove) {
        if (this.___transaction.isAuditing()) {
            this.___transaction.auditLog(this, AuditType.WRITE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"LINKLIST REMOVE: " + graphRelationName, edge);
        }
        // FIXME: ojo que esto puede haber cambiado.
        edge.reload();
        //--- 
        
        edge.delete();
        if (vertexToRemove != null) {
            this.___transaction.delete(vertexToRemove);
            if (this.___transaction.isAuditing()) {
                this.___transaction.auditLog(this, AuditType.DELETE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"LINKLIST REMOVE: " + graphRelationName, vertexToRemove);
            }
        }
    }
    

    /**
     * Refresca el objeto base recuperándolo nuevamente desde la base de datos.
     */
    
    @Override
    public void ___reload() {
        //FIXME: reload() no funciona. Workaround para recargar los datos.
        this.___baseElement.reload();
    }


    /**
     * Función de uso interno para remover un eje
     *
     * @param edgeToRemove
     * @param field
     */
    
    private synchronized void removeEdge(MutableEdge edgeToRemove, String field) {
        try {
            ClassDef classdef = this.___transaction.getObjectMapper().getClassDef(___baseClass);
            Field f = classdef.fieldsObject.get(field);
//            Field f = ReflectionUtils.findField(this.___baseClass, field);

            String outTo = edgeToRemove.getIn().toString();
            LOGGER.log(Level.DEBUG, "El edge {} apunta from: {} ---(to)--> {}",
                    new Object[]{edgeToRemove,
                        edgeToRemove.getOutVertex().getIdentity().toString(),
                        edgeToRemove.getInVertex().getIdentity().toString()});
            // remover primero el eje
            edgeToRemove.delete();
//            edgeToRemove.save();
            // si corresponde
            if (f.isAnnotationPresent(RemoveOrphan.class)) {
                LOGGER.log(Level.DEBUG, "Remove orphan presente");
                //auditar
                if (this.___transaction.isAuditing()) {
                    this.___transaction.auditLog(this, AuditType.DELETE, (this.___auditLogLabel!=null?this.___auditLogLabel+" : ":"")+"LINKLIST DELETE: ", edgeToRemove + " : " + field + " : " + f.get(this.___proxiedObject));
                }
                // eliminar el objecto
                // this.sm.delete(f.get(realObj));
                if (f.get(this.___proxiedObject) != null) {
                    LOGGER.log(Level.DEBUG, "La referencia aún existe. Eliminar el objeto directamente");
                    this.___transaction.delete(f.get(this.___proxiedObject));
                } else {
                    LOGGER.log(Level.DEBUG, "la referencia estaba en null, recupear y eliminar el objeto.");
                    this.___transaction.delete(this.___transaction.get(outTo));
                }
            }

        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            LOGGER.log(Level.ERROR, "ERROR",ex);
        }
    }


    /**
     * Revierte el objeto al estado que tiene el Vertex original.
     */
    
    @Override
    public synchronized void ___rollback() {
        LOGGER.log(Level.DEBUG, "\n\n******************* ROLLBACK *******************\n\n");
        LOGGER.log(Level.TRACE, ThreadHelper.getCurrentStackTrace());

//        this.___transaction.initInternalTx();
        // si es un objeto nuevo
//        boolean isNew = this.___baseElement.getIdentity().isNew();
        LOGGER.log(Level.DEBUG, "RID: {} Nueva?: {}", new Object[]{this.___baseElement.getIdentity().toString(), ___isNew});
        if (___isNew) {
            // invalidar el objeto
            LOGGER.log(Level.DEBUG, "El objeto aún no se ha persistido en la base. Invalidar");
            this.___isValidObject = false;
            return;
        }
        // asegurarse que la marca de borrado sea eliminada.
        this.___deletedMark = false;

        // recargar todo.
        LOGGER.log(Level.DEBUG, "baseElement pre reload: {}", this.___baseElement.propertiesAsMap());
        this.___reload();
        LOGGER.log(Level.DEBUG, "baseElement post reload: {}", this.___baseElement.propertiesAsMap());

        //LOGGER.log(Level.DEBUG, "vmap: {}", this.___baseElement.getProperties());
        // restaurar los atributos al estado original.
        ObjectMapper objectMapper = objectMapper();
        ClassDef classdef = getClassDef();
        
        
        LOGGER.log(Level.DEBUG, "Reverting basic attributes.........");
        LOGGER.log(Level.DEBUG, "baseElement map: {}", this.___baseElement.propertiesAsMap());
        
        for (var entry : classdef.fields.entrySet()) {
            String prop = entry.getKey();
            if (!classdef.embeddedFields.containsKey(prop)) {
                Object value = this.___baseElement.get(prop);
                LOGGER.log(Level.DEBUG, "Rollingback field {} :- old value -> ", new Object[]{prop,value});
                objectMapper.setFieldValue(___proxiedObject, prop, value);
            }
        }
        

        LOGGER.log(Level.DEBUG, "Reverting embedded collections.........");
        this.___transaction.getObjectMapper().hydrateEmbeddedCollections(
                classdef, (IObjectProxy)this.___proxiedObject, this.___baseElement);
        

        // procesar los enum
        Field f;
        LOGGER.log(Level.DEBUG, "Reverting enums...");
        for (Map.Entry<String, Class<?>> entry : classdef.enumFields.entrySet()) {
            String prop = entry.getKey();
            LOGGER.log(Level.DEBUG, "Buscando campo {} ....", new String[]{prop});
            Object value = this.___baseElement.get(prop);
            try {
                f = classdef.fieldsObject.get(prop);
                if (value != null) {
                    f.set(this.___proxiedObject, Enum.valueOf(f.getType().asSubclass(Enum.class), value.toString()));
                } else {
                    f.set(this.___proxiedObject, null);
                }
                LOGGER.log(Level.DEBUG, "hidratado campo: {}={}", new Object[]{prop, value});
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                LOGGER.log(Level.ERROR, "ERROR",ex);
            }
        }


        
        LOGGER.log(Level.DEBUG, "Reverting enum collections.........");
        this.___transaction.getObjectMapper().hydrateEnumCollections(
                classdef, (IObjectProxy)this.___proxiedObject, this.___baseElement);
        

        
        LOGGER.log(Level.DEBUG, "Revirtiendo los Links......... ");
        // hidratar los atributos @links
        // procesar todos los links
        for (Map.Entry<String, Class<?>> entry : classdef.links.entrySet()) {
            try {
                String field = entry.getKey();
                Field fLink = classdef.fieldsObject.get(field);
                fLink.set(this.___proxiedObject, null);
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                LOGGER.log(Level.ERROR, "ERROR",ex);
            }
        }
        // volver a activar la carga de los links
        this.___loadLazyLinks = true;

        // revertir las colecciones
        // procesar todos los linkslist
        LOGGER.log(Level.DEBUG, "Revirtiendo las colecciones...");
        for (Map.Entry<String, Class<?>> entry : classdef.linkLists.entrySet()) {
            String field = entry.getKey();
            Class<?> fc = entry.getValue();
            LOGGER.log(Level.DEBUG, "Field: {}   Class: {}", new String[]{field, fc.getName()});

            Field fLink = classdef.fieldsObject.get(field);
            Object coll = objectMapper.getFieldValue(this.___proxiedObject, fLink);
            if (coll != null && coll instanceof ILazyCalls) {
                ((ILazyCalls)coll).rollback();
            } else {
                objectMapper.setFieldValue(this.___proxiedObject, fLink, null);
            }
        }
        
        this.___removeDirtyMark();
//        this.___transaction.closeInternalTx();
    }
    
    
    /**
     * Reset dirty status of object and collections after a successful commit.
     */
    
    @Override
    public synchronized void ___commitSuccessful() {
//        this.___transaction.initInternalTx();
        ObjectMapper objectMapper = objectMapper();
        ClassDef classdef = getClassDef();
        classdef.linkLists.keySet().stream().
                map(fieldName -> classdef.fieldsObject.get(fieldName)).
                map(field -> objectMapper.getFieldValue(this.___proxiedObject, field)).
                filter(coll -> (coll != null && coll instanceof ILazyCalls && ((ILazyCalls)coll).isDirty())).
                forEach(coll -> ((ILazyCalls)coll).clearState());
        this.___removeDirtyMark();
//        this.___transaction.closeInternalTx();
    }

    
    private ClassDef getClassDef() {
        return this.___transaction.getObjectMapper().getClassDef(this.___baseClass);
    }
    
    
    private ObjectMapper objectMapper() {
        return this.___transaction.getObjectMapper();
    }

    
}
