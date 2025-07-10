package test;

import net.adbogm.annotations.Entity;
import net.adbogm.annotations.Sequence;

/**
 *
 * @author jbertinetti
 */
@Entity
public class Serial {
    
    @Sequence(sequenceName = "test_sequence")
    public Long s1;
    
    @Sequence(sequenceName = "test_sequence")
    public Long s2;


    public Serial() {
    }
    
}
