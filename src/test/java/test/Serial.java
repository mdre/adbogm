package test;

import net.adbogm.annotations.Entity;

/**
 *
 * @author jbertinetti
 */
@Entity
public class Serial {
    
    //@Sequence(sequenceName = "test_sequence")
    public Long s1;
    
    //@Sequence(sequenceName = "test_sequence")
    public Long s2;

    public Long autoinc;
    
    public Serial() {
    }
    
}
