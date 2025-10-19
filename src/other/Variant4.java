package other;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import rmProjekat.Router;

public class Variant4 extends AbstractTableMonitor {

    public Variant4(Router[] routers) throws IOException {
        super(routers);
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{
            "Peer ID", "State", "Version", "Peer IP", "Peer AS", 
            "Updates In", "Updates Out", "Keep-Alive", "Elapsed Time"
        };
    }

    @Override
    protected List<OID> getOidsToPoll() {
        return Arrays.asList(
            new OID(".1.3.6.1.2.1.15.3.1.1"),   // bgpPeerIdentifier
            new OID(".1.3.6.1.2.1.15.3.1.2"),   // bgpPeerState
            new OID(".1.3.6.1.2.1.15.3.1.4"),   // bgpPeerVersion
            new OID(".1.3.6.1.2.1.15.3.1.5"),   // bgpPeerRemoteAddr
            new OID(".1.3.6.1.2.1.15.3.1.9"),   // bgpPeerRemoteAs
            new OID(".1.3.6.1.2.1.15.3.1.10"),  // bgpPeerInUpdates
            new OID(".1.3.6.1.2.1.15.3.1.11"),  // bgpPeerOutUpdates
            new OID(".1.3.6.1.2.1.15.3.1.19"),  // bgpPeerKeepAlive
            new OID(".1.3.6.1.2.1.15.3.1.16")   // bgpPeerFsmEstablishedTime
        );
    }

    @Override
    protected List<Object[]> parseDataToRows(ArrayList<VariableBinding> rawData) {
        List<Object[]> rows = new ArrayList<>();
        if (rawData.isEmpty()) {
            return rows;
        }

        int columnCount = getColumnNames().length;
        int rowCount = rawData.size() / columnCount;

        for (int i = 0; i < rowCount; i++) {
            String peerId = rawData.get(i).getVariable().toString();
            String stateNum = rawData.get(i + rowCount).getVariable().toString();
            String peerVersion = rawData.get(i + 2 * rowCount).getVariable().toString();
            String peerIp = rawData.get(i + 3 * rowCount).getVariable().toString();
            String peerAs = rawData.get(i + 4 * rowCount).getVariable().toString();
            String updatesIn = rawData.get(i + 5 * rowCount).getVariable().toString();
            String updatesOut = rawData.get(i + 6 * rowCount).getVariable().toString();
            String keepAlive = rawData.get(i + 7 * rowCount).getVariable().toString();
            String elapsedTime = rawData.get(i + 8 * rowCount).getVariable().toString();

            String state = translateBGPState(stateNum);

            rows.add(new Object[]{
                peerId, state, peerVersion, peerIp, peerAs, 
                updatesIn, updatesOut, keepAlive, elapsedTime
            });
        }

        return rows;
    }


    private String translateBGPState(String stateNumber) {
        String state;
        switch (stateNumber) {
            case "1":
                state = "idle";
                break;
            case "2":
                state = "connect";
                break;
            case "3":
                state = "active";
                break;
            case "4":
                state = "opensent";
                break;
            case "5":
                state = "openconfirm";
                break;
            case "6":
                state = "established";
                break;
            default:
                state = "unknown (" + stateNumber + ")";
                break;
        }
        return state;
    }
}