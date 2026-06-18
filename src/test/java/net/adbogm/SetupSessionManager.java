package net.adbogm;


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

        
//        sm
//          .setClassLevelLog(TransparentDirtyDetectorInstrumentator.class, Level.FINER)
//            .setClassLevelLog(ObjectProxy.class, Level.FINEST)
//            .setClassLevelLog(ClassCache.class, Level.FINER)
//            .setClassLevelLog(Transaction.class, Level.TRACE)
//            .setClassLevelLog(ObjectProxy.class, Level.TRACE)
//            .setClassLevelLog(SimpleCache.class, Level.FINER)
//            .setClassLevelLog(ArrayListLazyProxy.class, Level.TRACE)
//            .setClassLevelLog(ObjectMapper.class, Level.TRACE)
//            .setClassLevelLog(VertexUtils.class, Level.TRACE)
//            .setClassLevelLog(SObject.class, Level.FINER)
//            .setClassLevelLog(TransparentDirtyDetectorInstrumentator.class, Level.INFO)
//        ;

        return sm;
    }
}
