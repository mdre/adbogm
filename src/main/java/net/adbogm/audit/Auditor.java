package net.adbogm.audit;

import com.arcadedb.database.MutableDocument;
import com.arcadedb.remote.RemoteDatabase;
import com.arcadedb.remote.RemoteMutableVertex;
import java.util.ArrayList;
import java.util.UUID;
import net.adbogm.LogginProperties;
import net.adbogm.Transaction;
import net.adbogm.annotations.Audit;
import net.adbogm.proxy.IObjectProxy;
import net.adbogm.utils.DateHelper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 *
 * @author Marcelo D. Ré {@literal <marcelo.re@gmail.com>}
 */
public class Auditor implements IAuditor {
    
    private final static Logger LOGGER = LogManager.getLogger(Auditor.class.getName());

    static {
        Configurator.setLevel(Auditor.class.getName(), LogginProperties.Auditor);
    }
    private final static String ADBAUDITLOGVERTEXTYPE = "OGMAuditLog";
    private final static String ADBAUDITLOGSCHEMA = 
            """
                create vertex type OGMAuditLog if not exists;
                create property OGMAuditLog.rid if not exists string;
                create property OGMAuditLog.timestamp if not exists datetime;
                create property OGMAuditLog.transactionID if not exists string;
                create property OGMAuditLog.opInTx if not exists integer;
                create property OGMAuditLog.user if not exists string;
                create property OGMAuditLog.action if not exists string;
                create property OGMAuditLog.label if not exists string;
                create property OGMAuditLog.log if not exists string;
            """;
    private final Transaction transaction;
    private final String auditUser;
    private final ArrayList<LogData> logdata = new ArrayList<>();
    
    
    public Auditor(Transaction t, String user) {
        this.transaction = t;
        this.auditUser = user;
        
        //verify and create the audit class in schema if necessary:
        if (this.transaction.getSessionManager().getConfig().isAuditorCreatesAuditSchema()) {
            if (this.transaction.getDBType(ADBAUDITLOGVERTEXTYPE) == null) {
                this.transaction.getCurrentGraphDb().command("sqlscript", ADBAUDITLOGSCHEMA);
            }
        }
    }
    

    /**
     * Realiza una auditoría a partir del objeto indicado.
     *
     * @param o IOBjectProxy a auditar
     * @param auditType AuditType
     * @param label Etiqueta de referencia
     * @param data objeto a loguear con un toString
     */
    @Override
    public synchronized void auditLog(IObjectProxy o, int auditType, String label, Object data) {
        // guardar log de auditoría si corresponde.
        if (o.___getBaseClass().isAnnotationPresent(Audit.class)) {
            int logVal = o.___getBaseClass().getAnnotation(Audit.class).log();
            if ((logVal & auditType) > 0) {
                this.logdata.add(new LogData(o, auditType, label, data));
                LOGGER.log(Level.DEBUG, "objeto auditado");
            } else {
                LOGGER.log(Level.DEBUG, "No corresponde auditar");
            }
        } else {
            LOGGER.log(Level.DEBUG, "No auditado: {}", o.___getBaseClass().getSimpleName());
        }
    }
    
    
    @Override
    public void commit() {
        // crear un UUDI para todo el log a comitear.
        String ovLogID = UUID.randomUUID().toString();
        RemoteDatabase odb = this.transaction.getCurrentGraphDb();
        
        //ODatabaseSession odb = this.transaction.getSessionManager().getDBTx();
        int opInTx = 0; //operation number in transaction
        
        for (LogData logData : logdata) {
            opInTx++;
            LOGGER.log(Level.DEBUG, "valid: {} : deleted: {}  : o.getRid: {}   : log.rid: {}", 
                                        new Object[]{logData.o.___isValid(),
                                                     logData.o.___isDeleted(), 
                                                     logData.o.___isDeleted()?"-deleted-":logData.o.___getRid(), 
                                                     logData.rid}
                       );
            RemoteMutableVertex ologData = odb.newVertex(ADBAUDITLOGVERTEXTYPE);

            ologData.set("transactionID", ovLogID);
            ologData.set("opInTx", opInTx);
            ologData.set("rid", (logData.o.___isValid() &
                                         !logData.o.___isDeleted() 
                                        ?logData.o.___getRid():logData.rid));
            ologData.set("timestamp", DateHelper.getCurrentDateTime());
            ologData.set("user", this.auditUser);
            ologData.set("action", logData.auditType);
            ologData.set("label", logData.label);
            ologData.set("log", logData.odata != null ? logData.odata.toString() : logData.data);
            ologData.save();
        }
        
        //odb.commit();
        //odb.close();
        
        //current.activateOnCurrentThread();
        this.logdata.clear();
    }
    
    
    @Override
    public void rollback() {
        //discard the entries that aren't reads
        new ArrayList<>(logdata).forEach(l -> {
            if (l.auditType != Audit.AuditType.READ) {
                this.logdata.remove(l);
            }
        });
    }
}

class LogData {
    public IObjectProxy o;
    public String rid;
    public int auditType;
    public String label;
    public String data;
    public Object odata;

    public LogData(IObjectProxy o, int auditType, String label, Object data) {
        this.o = o;
        this.rid = o.___getVertex().getIdentity().toString();
        this.auditType = auditType;
        this.label = label;
        if (data == null) {
            this.data = "";
        } else {
            //keep the element if it's new, so we can save the final rid and not the temporary in the "log" field:
            if (data instanceof MutableDocument ) {
                //FIXME: verificar si es un nodo nuevo. antes estaba lo siguiente
                // && ((MutableDocument)data).getIdentity().isNew()
                this.odata = data;
            } else {
                this.data = data.toString();
            }
        }
    }
}