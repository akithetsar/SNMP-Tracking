package other;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

import rmProjekat.Config;
import rmProjekat.Monitor;
import rmProjekat.Router;

public abstract class AbstractTableMonitor extends Monitor {

    private final Map<String, JTable> routerTables;

    public AbstractTableMonitor(Router[] routers) throws IOException {
        super(routers);
        this.POLLING_INTERVAL = 10000;
        this.routerTables = new HashMap<>();

        frame = new JFrame("SNMP Monitor - " + this.getClass().getSimpleName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        String[] columnNames = getColumnNames();

        for (Router router : routers) {
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
            JTable table = new JTable(tableModel);
            routerTables.put(router.getName(), table);

            JPanel routerTablePanel = new JPanel(new BorderLayout());
            routerTablePanel.setBorder(BorderFactory.createTitledBorder("Routing Table for: " + router.getName()));
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
        
        for (OID oid : getOidsToPoll()) {
            data.addAll(pollData(router, target, oid, null));
        }
        
        return data;
    }

    @Override
    protected void draw(ArrayList<VariableBinding> data, Router router) {
        SwingUtilities.invokeLater(() -> {
            JTable table = routerTables.get(router.getName());
            if (table != null) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0); 

                List<Object[]> rows = parseDataToRows(data);
                for (Object[] row : rows) {
                    model.addRow(row);
                }
            }
        });
    }

    
    protected abstract String[] getColumnNames();
    protected abstract List<OID> getOidsToPoll();
    protected abstract List<Object[]> parseDataToRows(ArrayList<VariableBinding> rawData);
}