package net.adbogm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotating a String field with this annotation, automatically it will contain
 * the RID of the associated vertex.
 * 
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RID {
}
