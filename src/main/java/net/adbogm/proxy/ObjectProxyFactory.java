package net.adbogm.proxy;

import asm.proxy.EasyProxy;
import asm.proxy.TypesCache;
import com.arcadedb.database.Document;

import java.lang.reflect.InvocationTargetException;

import net.adbogm.LogginProperties;
import net.adbogm.Transaction;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;


/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class ObjectProxyFactory {

    private final static Logger LOGGER = LogManager.getLogger(ObjectProxyFactory.class.getName());

    static {
        Configurator.setLevel(ObjectProxyFactory.class.getName(), LogginProperties.ObjectProxyFactory);
    }
    private static TypesCache typeCache = new TypesCache();

    public ObjectProxyFactory() {
    }
  

    public static <T> T create(T o, Document md, Transaction transaction) {
        // return cglibcreate((Class<T>)o.getClass(), oe, transaction);
        return epcreate((Class<T>) o.getClass(), md, transaction);
    }

    public static <T> T create(Class<T> c, Document md, Transaction transaction) {
        // return cglibcreate(c, ov, transaction);
        return epcreate(c, md, transaction);
    }


    /**
     * Devuelve un proxy a partir de una definición de clase.
     * @param <T>
     * @param c
     * @param md
     * @return T instance
     */
    public static <T> T epcreate(Class<T> c, Document md, Transaction transaction ) {
        LOGGER.log(Level.TRACE, "create proxy for class: "+c+(c!=null?c.getName().toString():"NULL CLASS!!!!"));
        T po = null;
        try { 
            
            // crear el proxy al que delegar las llamadas
            ObjectProxy bbi = new ObjectProxy(c,md.modify(),transaction);
            
            // crear una instancia
            po = typeCache.findOrInsert(c, IObjectProxy.class, ()->{
                    Class clazz = new EasyProxy().getProxyClass(c, IObjectProxy.class);
                    return clazz;
                }).newInstance(bbi);

            bbi.___setProxiedObject(po);
            
            // clean possible dirtiness (because of actions in default constructor)
            bbi.___removeDirtyMark();
            
        
//            System.out.println("//Object Proxy =====================================================");
//            
//            for (Method declaredMethod : c.getDeclaredMethods()) {
//                System.out.println(": "+declaredMethod.getName()+" : "+declaredMethod.isSynthetic()+" : "+Arrays.toString(declaredMethod.getParameters()));
//            }
//            System.out.println("//-----------------------------------------------------");
//            for (Method declaredMethod : po.getClass().getDeclaredMethods()) {
//                System.out.println(": "+declaredMethod.getName()+" : "+declaredMethod.isSynthetic()+" : "+Arrays.toString(declaredMethod.getParameters()));
//            }
//            System.out.println("//=====================================================");
        
            
        } catch (InstantiationException | IllegalAccessException | SecurityException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException ex) {
            LOGGER.log(Level.ERROR, "ERROR",ex);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return po;
    }
    
   
}
