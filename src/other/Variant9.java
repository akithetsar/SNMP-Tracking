package other;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;

import rmProjekat.Config;
import rmProjekat.Monitor;
import rmProjekat.Router;

public class Variant9 extends Monitor {

    private final Map<String, JTable> routerTables = new HashMap<>();
    private final String[] columnNames = {
        "Protocol", "Local Address", "Local Port",
        "Remote Address", "Remote Port", "State"
    };

    private static final OID OID_TCP_CONN_STATE = new OID(".1.3.6.1.2.1.6.13.1.1");
    private static final OID OID_TCP_LOCAL_ADDR = new OID(".1.3.6.1.2.1.6.13.1.2");
    private static final OID OID_TCP_LOCAL_PORT = new OID(".1.3.6.1.2.1.6.13.1.3");
    private static final OID OID_TCP_REMOTE_ADDR = new OID(".1.3.6.1.2.1.6.13.1.4");
    private static final OID OID_TCP_REMOTE_PORT = new OID(".1.3.6.1.2.1.6.13.1.5");

    private static final OID OID_UDP_LOCAL_ADDR = new OID(".1.3.6.1.2.1.7.5.1.1");
    private static final OID OID_UDP_LOCAL_PORT = new OID(".1.3.6.1.2.1.7.5.1.2");

    public Variant9(Router[] routers) throws IOException {
        super(routers);
        this.POLLING_INTERVAL = 5000;
        this.name = "Variant9";
        frame = new JFrame("SNMP Monitor - TCP/UDP Connections");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        for (Router router : routers) {
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
            JTable table = new JTable(tableModel);
            routerTables.put(router.getName(), table);

            JPanel routerTablePanel = new JPanel(new BorderLayout());
            routerTablePanel.setBorder(BorderFactory.createTitledBorder("Connections for: " + router.getName()));
            routerTablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
            mainPanel.add(routerTablePanel);
        }

        frame.add(new JScrollPane(mainPanel));
        frame.setVisible(true);
    }

    @Override
    protected ArrayList<VariableBinding> monitor(Router router) throws IOException {
        CommunityTarget target = new CommunityTarget();
        Address targetAddress = GenericAddress.parse("udp:" + router.getIp() + "/" + Config.SNMP_PORT);
        target.setCommunity(new OctetString(Config.COMMUNITY));
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);

        ArrayList<VariableBinding> data = new ArrayList<>();
        List<OID> oidsToPoll = Arrays.asList(
            OID_TCP_CONN_STATE, OID_TCP_LOCAL_ADDR, OID_TCP_LOCAL_PORT, OID_TCP_REMOTE_ADDR, OID_TCP_REMOTE_PORT,
            OID_UDP_LOCAL_ADDR, OID_UDP_LOCAL_PORT
        );
        
        for (OID oid : oidsToPoll) {
            data.addAll(pollData(router, target, oid, null));
        }
        return data;
    }

    @Override
    protected void draw(ArrayList<VariableBinding> data, Router router) {
        SwingUtilities.invokeLater(() -> {
            JTable table = routerTables.get(router.getName());
            if (table == null) return;

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);

            List<VariableBinding> tcpStates = new ArrayList<>();
            List<VariableBinding> tcpLocalAddrs = new ArrayList<>();
            List<VariableBinding> tcpLocalPorts = new ArrayList<>();
            List<VariableBinding> tcpRemoteAddrs = new ArrayList<>();
            List<VariableBinding> tcpRemotePorts = new ArrayList<>();
            List<VariableBinding> udpLocalAddrs = new ArrayList<>();
            List<VariableBinding> udpLocalPorts = new ArrayList<>();

            for (VariableBinding vb : data) {
                if (vb.getOid().startsWith(OID_TCP_CONN_STATE)) tcpStates.add(vb);
                else if (vb.getOid().startsWith(OID_TCP_LOCAL_ADDR)) tcpLocalAddrs.add(vb);
                else if (vb.getOid().startsWith(OID_TCP_LOCAL_PORT)) tcpLocalPorts.add(vb);
                else if (vb.getOid().startsWith(OID_TCP_REMOTE_ADDR)) tcpRemoteAddrs.add(vb);
                else if (vb.getOid().startsWith(OID_TCP_REMOTE_PORT)) tcpRemotePorts.add(vb);
                else if (vb.getOid().startsWith(OID_UDP_LOCAL_ADDR)) udpLocalAddrs.add(vb);
                else if (vb.getOid().startsWith(OID_UDP_LOCAL_PORT)) udpLocalPorts.add(vb);
            }

            for (int i = 0; i < tcpLocalAddrs.size(); i++) {
                model.addRow(new Object[]{
                    "TCP",
                    tcpLocalAddrs.get(i).getVariable().toString(),
                    tcpLocalPorts.get(i).getVariable().toString(),
                    tcpRemoteAddrs.get(i).getVariable().toString(),
                    tcpRemotePorts.get(i).getVariable().toString(),
                    translateTcpState(tcpStates.get(i).getVariable().toString())
                });
            }

            for (int i = 0; i < udpLocalPorts.size(); i++) {
                model.addRow(new Object[]{
                    "UDP",
                    udpLocalAddrs.get(i).getVariable().toString(),
                    udpLocalPorts.get(i).getVariable().toString(),
                    "", 
                    "",
                    "Listening"
                });
            }
        });
    }

    private String translateTcpState(String stateNumber) {
        switch (stateNumber) {
            case "1": return "closed";
            case "2": return "listen";
            case "3": return "synSent";
            case "4": return "synReceived";
            case "5": return "established";
            case "6": return "finWait1";
            case "7": return "finWait2";
            case "8": return "closeWait";
            case "9": return "lastAck";
            case "10": return "closing";
            case "11": return "timeWait";
            case "12": return "deleteTCB";
            default: return "unknown";
        }
    }
}