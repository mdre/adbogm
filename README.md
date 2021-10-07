# odbogm-agent
TransparentDirtyDrectorAgent for ODBOGM

This java agent force the implementation of the ITransparentDirtyDetector interface on-the-fly. I could be load at runtime whe the SessionManager is instantiated o load as a parameter of the JVM.

I have found that in certains case, when you use EJB, some classes are loaded before the aplication itself is initiated, so in that case is recomended to set the agent in the JVM parameters.

For example, to add it to Glassfish/Payara, just copy the odbogm-agent-all-1.0.0.jar to the lib/ext dir of the domain and set in Configurations > server-config > JVM Settings > JVM Options an opetion with:
  
-javaagent:/opt/payara/glassfish/domains/domain1/lib/ext/odbogm-agent-all-1.0.0.jar

and restart the server.

The agent instrument all method of the classes that are annotated with @Entity. It add a few method to catch when the internal state of an instance change.
