package net.adbogm;


import net.adbogm.proxy.ObjectProxy;
import org.apache.logging.log4j.Level;
import test.TestConfig;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author mdre
 */
public class SetupSessionManager {
    public static SessionManager getSessionManager() {
        boolean grpc = true;
        SessionManager sm = null;
        if (grpc) {
            sm =new SessionManager(TestConfig.TESTSERVER,TestConfig.TESTGRPCDBPORT, TestConfig.TESTDBPORT,TestConfig.TESTDB, TestConfig.TESTDBUSER, TestConfig.TESTDBPASS, true);
        } else {
            sm  = new SessionManager(TestConfig.TESTSERVER, TestConfig.TESTDBPORT,TestConfig.TESTDB, TestConfig.TESTDBUSER, TestConfig.TESTDBPASS);
        }

        
        sm
//          .setClassLevelLog(TransparentDirtyDetectorInstrumentator.class, Level.FINER)
            .setClassLevelLog(Transaction.class, org.apache.logging.log4j.Level.TRACE)
            .setClassLevelLog(ObjectProxy.class, Level.TRACE)
        ;

        return sm;
    }
}
