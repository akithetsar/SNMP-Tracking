package other;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import rmProjekat.Router;

public class Variant5 extends AbstractTableMonitor {

    public Variant5(Router[] routers) throws IOException {
        super(routers);
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{
            "Origin", "AS Path", "Next Hop", "MED", "Local Pref",
            "Atomic Aggregate", "Aggregator AS", "Aggregator Address", "Best Route"
        };
    }

    @Override
    protected List<OID> getOidsToPoll() {
        return Arrays.asList(
            new OID(".1.3.6.1.2.1.15.6.1.4"),  // bgpPathAttrOrigin
            new OID(".1.3.6.1.2.1.15.6.1.5"),  // bgpPathAttrASPathSegment
            new OID(".1.3.6.1.2.1.15.6.1.6"),  // bgpPathAttrNextHop
            new OID(".1.3.6.1.2.1.15.6.1.7"),  // bgpPathAttrMultiExitDisc
            new OID(".1.3.6.1.2.1.15.6.1.8"),  // bgpPathAttrLocalPref
            new OID(".1.3.6.1.2.1.15.6.1.9"),  // bgpPathAttrAtomicAggregate
            new OID(".1.3.6.1.2.1.15.6.1.10"), // bgpPathAttrAggregatorAS
            new OID(".1.3.6.1.2.1.15.6.1.11"), // bgpPathAttrAggregatorAddr
            new OID(".1.3.6.1.2.1.15.6.1.13")  // bgpPathAttrBest
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
            String originNum = rawData.get(i).getVariable().toString();
            String asPath = rawData.get(i + rowCount).getVariable().toString();
            String nextHop = rawData.get(i + 2 * rowCount).getVariable().toString();
            String med = rawData.get(i + 3 * rowCount).getVariable().toString();
            String localPref = rawData.get(i + 4 * rowCount).getVariable().toString();
            String atomicAggNum = rawData.get(i + 5 * rowCount).getVariable().toString();
            String aggregatorAs = rawData.get(i + 6 * rowCount).getVariable().toString();
            String aggregatorAddr = rawData.get(i + 7 * rowCount).getVariable().toString();
            String bestRouteNum = rawData.get(i + 8 * rowCount).getVariable().toString();

            rows.add(new Object[]{
                translateOrigin(originNum),
                asPath,
                nextHop,
                med,
                localPref,
                translateAtomicAggregate(atomicAggNum),
                aggregatorAs,
                aggregatorAddr,
                translateBestRoute(bestRouteNum)
            });
        }
        return rows;
    }

    private String translateOrigin(String originNumber) {
        String origin;
        switch (originNumber) {
            case "1":
                origin = "igp";
                break;
            case "2":
                origin = "egp";
                break;
            case "3":
                origin = "incomplete";
                break;
            default:
                origin = "unknown (" + originNumber + ")";
                break;
        }
        return origin;
    }

    private String translateAtomicAggregate(String aggregateNumber) {
        String aggregate;
        switch (aggregateNumber) {
            case "1":
                aggregate = "less specific";
                break;
            case "2":
                aggregate = "more specific";
                break;
            default:
                aggregate = "N/A (" + aggregateNumber + ")";
                break;
        }
        return aggregate;
    }

    private String translateBestRoute(String bestRouteNumber) {
        return "1".equals(bestRouteNumber) ? "Yes" : "No";
    }
}