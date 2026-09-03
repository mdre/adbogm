package test;

import java.util.ArrayList;
import net.adbogm.annotations.Entity;
import net.adbogm.security.SObject;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
@Entity
public class SSimpleVertex extends SObject {

    private String s;
    private ArrayList<SimpleVertex> subsv = new ArrayList<>();
    
    public SSimpleVertex(String s) {
        this.s = s;
    }

    public SSimpleVertex(){
        this.s = "string";
    }
    
    public String getS(){
        return this.s;
    }

    public void setS(String s) {
        this.s = s;
    }
    
    @Override
    public String toString() {
        return this.s + " - State: "+this.getSecurityState() ;
    }
 
    public void addSV(SimpleVertex sv) {
        this.subsv.add(sv);
    }
    
    public int svSize() {
        return this.subsv.size();
    }
    
    public void setSAndAddSub(String s, SimpleVertex sv) {
        this.s = s;
        this.subsv.add(sv);
    }
}
