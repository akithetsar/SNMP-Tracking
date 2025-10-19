package rmProjekat;

import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.util.*;

public class Variant1 extends AbstractTreeMonitor {

    private static final OID OID_IF_DESCR = new OID("1.3.6.1.2.1.2.2.1.2");
    private static final OID OID_IF_TYPE = new OID("1.3.6.1.2.1.2.2.1.3");
    private static final OID OID_IF_MTU = new OID("1.3.6.1.2.1.2.2.1.4");
    private static final OID OID_IF_SPEED = new OID("1.3.6.1.2.1.2.2.1.5");
    private static final OID OID_IF_PHYS_ADDRESS = new OID("1.3.6.1.2.1.2.2.1.6");
    private static final OID OID_IF_ADMIN_STATUS = new OID("1.3.6.1.2.1.2.2.1.7");
    private static final OID OID_IF_OPER_STATUS = new OID("1.3.6.1.2.1.2.2.1.8");

    public Variant1(Router[] routers) throws IOException {
        super(routers);
        this.POLLING_INTERVAL = 10000;
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
        
        OID[] columnsToFetch = new OID[] {
            OID_IF_DESCR, OID_IF_TYPE, OID_IF_MTU, OID_IF_SPEED,
            OID_IF_PHYS_ADDRESS, OID_IF_ADMIN_STATUS, OID_IF_OPER_STATUS
        };
        
        System.out.println("POLL START " + router.getName());
        ArrayList<VariableBinding> data = pollTableColumns(router, target, columnsToFetch);
        System.out.println("POLL ENDS "  + router.getName());
        return data;
    }
    
    @Override
    protected List<InterfaceData> parseRawData(ArrayList<VariableBinding> rawData) {
        Map<Integer, InterfaceData> interfaceMap = new TreeMap<>();
        if (rawData == null) return Collections.emptyList();

        for (VariableBinding vb : rawData) {
            OID oid = vb.getOid();
            if (oid.size() <= OID_IF_DESCR.size()) continue;
            
            int ifIndex = oid.last();
            InterfaceData ifData = interfaceMap.computeIfAbsent(ifIndex, InterfaceData::new);
            
            if (oid.startsWith(OID_IF_DESCR)) {
                ifData.description = vb.getVariable().toString();
            } else if (oid.startsWith(OID_IF_TYPE)) {
                ifData.type = vb.getVariable().toString();
            } else if (oid.startsWith(OID_IF_MTU)) {
                ifData.mtu = vb.getVariable().toString();
            } else if (oid.startsWith(OID_IF_SPEED)) {
                ifData.speed = vb.getVariable().toString();
            } else if (oid.startsWith(OID_IF_PHYS_ADDRESS)) {
                ifData.physAddress = vb.getVariable().toString();
            } else if (oid.startsWith(OID_IF_ADMIN_STATUS)) {
                ifData.adminStatus = vb.getVariable().toInt() == 1 ? "up" : "down";
            } else if (oid.startsWith(OID_IF_OPER_STATUS)) {
                ifData.operStatus = vb.getVariable().toInt() == 1 ? "up" : "down";
            }
        }
        return new ArrayList<>(interfaceMap.values());
    }

    @Override
    protected DefaultMutableTreeNode createDataNode(Object dataObject) {
        if (!(dataObject instanceof InterfaceData)) {
            return null;
        }
        InterfaceData ifData = (InterfaceData) dataObject;
        
        DefaultMutableTreeNode interfaceNode = new DefaultMutableTreeNode("Interface " + ifData.index);

        interfaceNode.add(new DefaultMutableTreeNode("Description: " + ifData.description));
        interfaceNode.add(new DefaultMutableTreeNode("Type: " + ifData.type));
        interfaceNode.add(new DefaultMutableTreeNode("MTU: " + ifData.mtu));
        interfaceNode.add(new DefaultMutableTreeNode("Speed: " + ifData.speed));
        interfaceNode.add(new DefaultMutableTreeNode("Physical Address: " + ifData.physAddress));
        
        String adminStatusColor = ifData.adminStatus.equals("up") ? "<font color='green'>•</font>" : "<font color='red'>•</font>";
        interfaceNode.add(new DefaultMutableTreeNode("<html>Admin Status: " + adminStatusColor + " " + ifData.adminStatus + "</html>"));

        String operStatusColor = ifData.operStatus.equals("up") ? "<font color='green'>•</font>" : "<font color='red'>•</font>";
        interfaceNode.add(new DefaultMutableTreeNode("<html>Oper Status: " + operStatusColor + " " + ifData.operStatus + "</html>"));

        return interfaceNode;
    }
}