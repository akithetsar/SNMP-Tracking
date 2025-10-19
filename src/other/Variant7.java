package other;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import rmProjekat.Config;
import rmProjekat.Router;

public class Variant7 extends AbstractTableMonitor {

    public Variant7(Router[] routers) throws IOException {
        super(routers);
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{
            "SNMP In Packets",
            "SNMP Out Packets",
            "SNMP Get Requests",
            "SNMP Set Requests",
            "SNMP Out Traps",
            "SNMP Bad Community Names"
        };
    }

    @Override
    protected List<OID> getOidsToPoll() {
        return Arrays.asList(
            new OID(".1.3.6.1.2.1.11.1.0"),  // snmpInPkts
            new OID(".1.3.6.1.2.1.11.2.0"),  // snmpOutPkts
            new OID(".1.3.6.1.2.1.11.15.0"), // snmpOutGetRequests
            new OID(".1.3.6.1.2.1.11.17.0"), // snmpOutSetRequests
            new OID(".1.3.6.1.2.1.11.5.0"),  // snmpOutTraps
            new OID(".1.3.6.1.2.1.11.4.0")   // snmpInBadCommunityNames
        );
    }

    /**
     * Overrides the monitor method to use PDU.GET for scalar values
     * instead of the default table-walking logic in AbstractTableMonitor.
     */
    @Override
    protected ArrayList<VariableBinding> monitor(Router router) throws IOException {
        CommunityTarget target = new CommunityTarget();
        Address targetAddress = GenericAddress.parse("udp:" + router.getIp() + "/" + Config.SNMP_PORT);
        target.setCommunity(new OctetString(Config.COMMUNITY));
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);

        PDU pdu = new PDU();
        pdu.setType(PDU.GET); 
        for (OID oid : getOidsToPoll()) {
            pdu.add(new VariableBinding(oid));
        }

        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();
        Snmp snmp = new Snmp(transport);

        ResponseEvent response = snmp.send(pdu, target);
        snmp.close();

        if (response != null && response.getResponse() != null && response.getResponse().getErrorStatus() == PDU.noError) {
            return new ArrayList<>(response.getResponse().getVariableBindings());
        } else {
            System.err.println("Error polling SNMP stats for " + router.getName());
            return new ArrayList<>(); 
        }
    }

    @Override
    protected List<Object[]> parseDataToRows(ArrayList<VariableBinding> rawData) {
        List<Object[]> rows = new ArrayList<>();
        
        if (rawData.size() >= getColumnNames().length) {
            Object[] row = new Object[getColumnNames().length];
            for (int i = 0; i < getColumnNames().length; i++) {
                row[i] = rawData.get(i).getVariable().toString();
            }
            rows.add(row);
        }
        
        return rows;
    }
}