/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.arcadedb.database.Database;
import com.arcadedb.remote.RemoteDatabase;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class TestEx {
    private final static Logger LOGGER = Logger.getLogger(TestEx.class .getName());

    public TestEx() {
        
        RemoteDatabase db = new RemoteDatabase("localhost",2480,"ogm-test","root","rootroot");
        try {
            db.begin();


            db.commit();
        } catch (Exception e) {
            db.rollback();
        } finally {
            db.close();
        }
    }
    
    public static void main(String[] args) {
        new TestEx();
    }
}

