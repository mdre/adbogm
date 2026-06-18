/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.adbogm.proxy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.adbogm.utils.ThreadHelper;
import net.dirtydetector.agent.ITransparentDirtyDetector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ArrayListEmbeddedProxy extends ArrayList implements IEmbeddedCalls {

    private static final long serialVersionUID = 3136116168236143774L;
    private final static Logger LOGGER = LogManager.getLogger(ArrayListEmbeddedProxy.class.getName());

//    static {
//        Configurator.setLevel(ArrayListEmbeddedProxy.class.getName(), LogginProperties.ArrayListEmbeddedProxy);
//    }
    // referencia debil al objeto padre. Se usa para notificar al padre que la colección ha cambiado.
    private WeakReference<IObjectProxy> parent;
    private String field; // the field bound to this AL
    
    /**
     * Crea un ArrayList embebido.
     *
     * @param parent Vínculo al objeto que contiene la colección
     * @param field field bound to this collections.
     */
    @Override
    public synchronized void init(IObjectProxy parent, String field) {
        try {
            this.parent = new WeakReference<>(parent);
            this.field = field;
        } catch (Exception ex) {
            LOGGER.log(Level.ERROR, "ERROR",ex);
        }
    }

    
    private synchronized void setDirty() {
        LOGGER.log(Level.DEBUG, "Colección marcada como Dirty. Avisar al padre.");
        LOGGER.log(Level.DEBUG, "weak:"+this.parent.get());
        // si el padre no está marcado como garbage, notificarle el cambio de la colección.
        if (this.parent.get()!=null) {
            this.parent.get().___setDirty();
            ((ITransparentDirtyDetector)this.parent.get().___getProxiedObject()).___tdd___addModifiedField(field);
            LOGGER.log(Level.DEBUG, ThreadHelper.getCurrentStackTrace());
        }
    }
    
    //====================================================================================

    public ArrayListEmbeddedProxy() {
        super();
    }
    
    public ArrayListEmbeddedProxy(IObjectProxy parent, String field) {
        super();
        this.init(parent, field);
    }
    
    
    public ArrayListEmbeddedProxy(IObjectProxy parent, String field, List l) {
        super(l);
        this.init(parent, field);
    }


    @Override
    public void replaceAll(UnaryOperator operator) {
        this.setDirty();
        super.replaceAll(operator);
    }

    @Override
    public boolean removeIf(Predicate filter) {
        boolean removed = super.removeIf(filter);
        if (removed)
            this.setDirty();
        return removed;
    }



    @Override
    public boolean retainAll(Collection c) {
        boolean changeDetected = super.retainAll(c);
        if (changeDetected)
            this.setDirty();
        return changeDetected;
    }

    @Override
    public boolean removeAll(Collection c) {
        boolean changeDetected = super.removeAll(c);
        if (changeDetected)
            this.setDirty();
        return changeDetected;
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        this.setDirty();
        super.removeRange(fromIndex, toIndex);
    }

    @Override
    public boolean addAll(int index, Collection c) {
        this.setDirty();
        return super.addAll(index, c);
    }

    @Override
    public boolean addAll(Collection c) {
        this.setDirty();
        return super.addAll(c);
    }

    @Override
    public void clear() {
        this.setDirty();
        super.clear();
    }

    @Override
    public boolean remove(Object o) {
        
        boolean changeDetected = super.remove(o);
        if (changeDetected)
            this.setDirty();
        return changeDetected;
    }

    @Override
    public Object remove(int index) {
        this.setDirty();
        return super.remove(index);
    }

    @Override
    public void add(int index, Object element) {
        this.setDirty();
        super.add(index, element);
    }

    @Override
    public boolean add(Object e) {
        LOGGER.log(Level.DEBUG, "DIRTY: Elemento nuevo agregado: "+e.toString());
        this.setDirty();
        return super.add(e);
    }

    @Override
    public Object set(int index, Object element) {
        this.setDirty();
        return super.set(index, element);
    }


    @Override
    public void trimToSize() {
        this.setDirty();
        super.trimToSize();
    }
    
}
