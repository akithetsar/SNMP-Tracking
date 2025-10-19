package rmProjekat;

import java.io.IOException;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class Router {

	private String ip;
	private int num_of_interfaces;
	private String name;
	
	public String getName() {
		return name;
	}

	public String getIp() {
		return ip;
	}

	public Router(String ip) throws IOException {
		this.ip = ip;
		
        
	    Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
	    snmp.listen();
	    
        Address targetAddress = GenericAddress.parse("udp:" + ip + "/" + Config.SNMP_PORT);
	    CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(Config.COMMUNITY));
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);


        this.num_of_interfaces = getIntValue(snmp, target, Config.OID_IF_NUMBER);

        this.name = getStringValue(snmp, target, Config.OID_SYS_NAME);

        snmp.close();
	}

    private int getIntValue(Snmp snmp, CommunityTarget target, OID oid) throws IOException {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(oid));
        pdu.setType(PDU.GET);
        ResponseEvent response = snmp.send(pdu, target);

        if (response == null || response.getResponse() == null) {
            throw new IOException("Error: No response from target " + ip);
        }
        return response.getResponse().get(0).getVariable().toInt();
    }

    private String getStringValue(Snmp snmp, CommunityTarget target, OID oid) throws IOException {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(oid));
        pdu.setType(PDU.GET);
        ResponseEvent response = snmp.send(pdu, target);

        if (response == null || response.getResponse() == null) {
            throw new IOException("Error: No response from target " + ip);
        }
        return response.getResponse().get(0).getVariable().toString();
    }


	public int getNum_of_interfaces() {
		return num_of_interfaces;
	}
}