/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.adbogm;

import com.arcadedb.schema.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import net.adbogm.proxy.ArrayListLazyProxy;
import net.adbogm.proxy.HashMapLazyProxy;
import net.adbogm.proxy.LinkedListLazyProxy;
import net.adbogm.proxy.VectorLazyProxy;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class Primitives {
    public enum PRIMITIVE {

        BOOLEAN,
        BYTE,
        CHAR,
        STRING,
        DOUBLE,
        FLOAT,
        INT,
        LONG,
        SHORT,
        BIGDECIMAL,
        DATE,
        LOCALDATETIME,
        ZONEDDATETIME,
        LOCALDATE
    }
    public static final IdentityHashMap<Class<?>, Type> PRIMITIVE_MAP = new IdentityHashMap<>();
    public static final IdentityHashMap<Class<?>, Class<?>> LAZY_COLLECTION = new IdentityHashMap<>();

    static {
        PRIMITIVE_MAP.put(Boolean.class, Type.BOOLEAN);
        PRIMITIVE_MAP.put(Byte.class, Type.BYTE);
        PRIMITIVE_MAP.put(Double.class, Type.DOUBLE);
        PRIMITIVE_MAP.put(Float.class, Type.FLOAT);
        PRIMITIVE_MAP.put(Integer.class, Type.INTEGER);
        PRIMITIVE_MAP.put(Long.class, Type.LONG);
        PRIMITIVE_MAP.put(Short.class, Type.SHORT);
        
        PRIMITIVE_MAP.put(BigDecimal.class, Type.DECIMAL);

        PRIMITIVE_MAP.put(String.class, Type.STRING);
        
        PRIMITIVE_MAP.put(Date.class, Type.DATETIME);
        PRIMITIVE_MAP.put(LocalDate.class, Type.DATETIME);
        PRIMITIVE_MAP.put(LocalDateTime.class, Type.DATETIME);
        PRIMITIVE_MAP.put(ZonedDateTime.class, Type.DATETIME);

        PRIMITIVE_MAP.put(Boolean.TYPE, Type.BOOLEAN);
        PRIMITIVE_MAP.put(Byte.TYPE, Type.BYTE);
        PRIMITIVE_MAP.put(Double.TYPE, Type.DOUBLE);
        PRIMITIVE_MAP.put(Float.TYPE, Type.FLOAT);
        PRIMITIVE_MAP.put(Integer.TYPE, Type.INTEGER);
        PRIMITIVE_MAP.put(Long.TYPE, Type.LONG);
        PRIMITIVE_MAP.put(Short.TYPE, Type.SHORT);
        
        
        LAZY_COLLECTION.put(List.class,ArrayListLazyProxy.class);
        LAZY_COLLECTION.put(ArrayList.class,ArrayListLazyProxy.class);
        LAZY_COLLECTION.put(LinkedList.class,LinkedListLazyProxy.class);
        LAZY_COLLECTION.put(Vector.class,VectorLazyProxy.class);
        
        LAZY_COLLECTION.put(HashMap.class,HashMapLazyProxy.class);
        LAZY_COLLECTION.put(Map.class,HashMapLazyProxy.class);
        
        
    }

    
}
