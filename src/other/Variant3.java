package other;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import rmProjekat.Router;

public class Variant3 extends AbstractTableMonitor {

    public Variant3(Router[] routers) throws IOException {
        super(routers);
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{"Destination", "Mask", "Next Hop", "Protocol"};
    }

    @Override
    protected List<OID> getOidsToPoll() {
        return Arrays.asList(
            new OID("1.3.6.1.2.1.4.21.1.1"),   // ipRouteDest
            new OID("1.3.6.1.2.1.4.21.1.11"),  // ipRouteMask
            new OID("1.3.6.1.2.1.4.21.1.7"),   // ipRouteNextHop
            new OID("1.3.6.1.2.1.4.21.1.9")    // ipRouteProto
        );
    }

    @Override
    protected List<Object[]> parseDataToRows(ArrayList<VariableBinding> rawData) {
        List<Object[]> rows = new ArrayList<>();
        if (rawData.isEmpty()) {
            return rows;
        }

        int columnCount = getOidsToPoll().size();
        int rowCount = rawData.size() / columnCount;

        for (int i = 0; i < rowCount; i++) {
            String dest = rawData.get(i).getVariable().toString();
            String mask = rawData.get(i + rowCount).getVariable().toString();
            String nextHop = rawData.get(i + 2 * rowCount).getVariable().toString();
            String protocolNum = rawData.get(i + 3 * rowCount).getVariable().toString();
            
            String protocol = translateRouteProtocol(protocolNum);

            rows.add(new Object[]{dest, mask, nextHop, protocol});
        }

        return rows;
    }

    private String translateRouteProtocol(String protocolNumber) {
        String protocol;
        switch (protocolNumber) {
            case "1":
                protocol = "other";
                break;
            case "2":
                protocol = "local";
                break;
            case "3":
                protocol = "netmgmt (static)";
                break;
            case "4":
                protocol = "icmp";
                break;
            case "8":
                protocol = "rip";
                break;
            case "9":
                protocol = "is-is";
                break;
            case "13":
                protocol = "ospf";
                break;
            case "14":
                protocol = "bgp";
                break;
            default:
                protocol = "unknown (" + protocolNumber + ")";
                break;
        }
        return protocol;
    }
}