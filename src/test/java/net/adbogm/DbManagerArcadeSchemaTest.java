package net.adbogm;

import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DbManagerArcadeSchemaTest {

    @Test
    public void generatesIdempotentArcadeSchemaIntrospection() throws Exception {
        List<String> sql = new DbManager().generateDBSQL("test");
        String script = String.join("\n", sql);

        assertTrue(script.contains("if not exists"));
        assertTrue(script.contains("schema:types"));
        assertFalse(script.contains("metadata:schema"));
        assertFalse(script.contains("if not exist "));
    }
}
