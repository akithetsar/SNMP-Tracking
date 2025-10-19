package rmProjekat;

import org.snmp4j.smi.OID;

public class Config {

    public static final String COMMUNITY = "si2019";
    public static final int SNMP_PORT = 161;
    public static final int SNMP_TRAP_PORT = 162; 
	public static final int numOfRouters = 3;
	public static String[] ips = {"192.168.10.1", "192.168.20.1", "192.168.30.1"};

	// OID Constants
    public static final OID OID_SYS_NAME = new OID("1.3.6.1.2.1.1.5.0");
    public static final OID OID_IF_NUMBER = new OID("1.3.6.1.2.1.2.1.0");
    public static final OID OID_IF_TABLE_ENTRY = new OID("1.3.6.1.2.1.2.2.1");
    public static final OID OID_IF_TABLE_END = new OID("1.3.6.1.2.1.2.2.1.9");
}