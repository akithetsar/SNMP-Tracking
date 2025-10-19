package other;

import org.snmp4j.*;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

import rmProjekat.Config;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class Variant6 implements CommandResponder {

    // BGP MIB Trap OIDs
    private static final OID BGP_ESTABLISHED_NOTIFICATION = new OID("1.3.6.1.2.1.15.7.1");
    private static final OID BGP_BACKWARD_TRANS_NOTIFICATION = new OID("1.3.6.1.2.1.15.7.2");
    
    // BGP MIB OIDs for trap variables
    private static final OID BGP_PEER_LAST_ERROR = new OID("1.3.6.1.2.1.15.3.1.14");
    private static final OID BGP_PEER_STATE = new OID("1.3.6.1.2.1.15.3.1.2");
    private static final OID BGP_PEER_REMOTE_ADDR = new OID("1.3.6.1.2.1.15.3.1.7");

    private Snmp snmp;
    private volatile boolean running = true;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Variant6() throws IOException {
        System.out.println("=== Variant6 BGP SNMP Trap Listener ===");
        System.out.println("Listening for BGP notifications on port " + Config.SNMP_TRAP_PORT);
        System.out.println("Monitoring for:");
        System.out.println("  - bgpEstablishedNotification (" + BGP_ESTABLISHED_NOTIFICATION + ")");
        System.out.println("  - bgpBackwardTransNotification (" + BGP_BACKWARD_TRANS_NOTIFICATION + ")");
        System.out.println("Press ESC to stop listening");
        System.out.println("========================================");
        
        setupKeyListener();
        initializeSnmpListener();
    }

    private void setupKeyListener() {
        JFrame keyFrame = new JFrame("Variant6 BGP Trap Listener - Press ESC to exit");
        keyFrame.setSize(450, 100);
        keyFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        keyFrame.setAlwaysOnTop(true);
        keyFrame.setLocationRelativeTo(null);
        
        JLabel label = new JLabel("<html><center>BGP Trap Listener Running<br>Press ESC to stop</center></html>");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        keyFrame.add(label);
        
        keyFrame.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.out.println("\nESC pressed - shutting down trap listener...");
                    stopListener();
                    keyFrame.dispose();
                    System.exit(0);
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {}
            
            @Override
            public void keyTyped(KeyEvent e) {}
        });
        
        keyFrame.setFocusable(true);
        keyFrame.setVisible(true);
        keyFrame.requestFocus();
    }

    private void initializeSnmpListener() throws IOException {
        DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping(
            new UdpAddress("0.0.0.0/" + Config.SNMP_TRAP_PORT));
        
        ThreadPool threadPool = ThreadPool.create("Trap", 2);
        MultiThreadedMessageDispatcher dispatcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());
        
        dispatcher.addMessageProcessingModel(new MPv1());
        dispatcher.addMessageProcessingModel(new MPv2c());
        
        SecurityProtocols.getInstance().addDefaultProtocols();
        SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());
        
        snmp = new Snmp(dispatcher, transport);
        snmp.addCommandResponder(this);
        
        transport.listen();
        
        System.out.println("SNMP Trap Listener started successfully on port " + Config.SNMP_TRAP_PORT);
        System.out.println("Waiting for BGP traps...\n");
    }

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("\nShutting down trap listener...");
                stopListener();
            }
        }));
        
        try {
            Object lock = new Object();
            synchronized (lock) {
                while (running) {
                    lock.wait(1000); 
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Trap listener interrupted, shutting down...");
            stopListener();
        }
    }

    private void stopListener() {
        running = false;
        try {
            if (snmp != null) {
                snmp.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing SNMP: " + e.getMessage());
        }
    }

    @Override
    public void processPdu(CommandResponderEvent event) {
        PDU pdu = event.getPDU();
        if (pdu == null) {
            return;
        }

        Address sourceAddress = event.getPeerAddress();
        String sourceIP = extractIPFromAddress(sourceAddress.toString());
        String currentTime = dateFormat.format(new Date());

        if (pdu.getType() == PDU.TRAP || pdu.getType() == PDU.INFORM) {
            processTrap(pdu, sourceIP, currentTime);
        }
    }

    private void processTrap(PDU pdu, String sourceIP, String currentTime) {
        System.out.println("\n" + createRepeatedString("=", 60));
        System.out.println("SNMP TRAP RECEIVED");
        System.out.println("Time: " + currentTime);
        System.out.println("Source: " + sourceIP);
        System.out.println(createRepeatedString("=", 60));

        OID trapOID = null;
        if (pdu.getType() == PDU.TRAP) {
            if (pdu instanceof PDUv1) {
                PDUv1 v1pdu = (PDUv1) pdu;
                trapOID = v1pdu.getEnterprise();
                System.out.println("SNMPv1 Trap - Enterprise OID: " + trapOID);
                System.out.println("Generic Trap: " + v1pdu.getGenericTrap());
                System.out.println("Specific Trap: " + v1pdu.getSpecificTrap());
            }
        } else {
            Vector<? extends VariableBinding> varBinds = pdu.getVariableBindings();
            for (int i = 0; i < varBinds.size(); i++) {
                VariableBinding vb = varBinds.get(i);
                if (vb.getOid().equals(SnmpConstants.snmpTrapOID)) {
                    trapOID = (OID) vb.getVariable();
                    break;
                }
            }
        }

        boolean isBGPTrap = false;
        String trapType = "Unknown";
        
        if (trapOID != null) {
            System.out.println("Trap OID: " + trapOID);
            
            if (trapOID.equals(BGP_ESTABLISHED_NOTIFICATION)) {
                isBGPTrap = true;
                trapType = "BGP Established Notification";
                System.out.println("\n[BGP] BGP ESTABLISHED NOTIFICATION DETECTED!");
                System.out.println("A BGP session has been established.");
            } else if (trapOID.equals(BGP_BACKWARD_TRANS_NOTIFICATION)) {
                isBGPTrap = true;
                trapType = "BGP Backward Transition Notification";
                System.out.println("\n[BGP] BGP BACKWARD TRANSITION NOTIFICATION DETECTED!");
                System.out.println("A BGP session has transitioned to a lower state.");
            } else if (trapOID.startsWith(new OID("1.3.6.1.2.1.15.7"))) {
                isBGPTrap = true;
                trapType = "BGP Notification (Other)";
                System.out.println("\n[BGP] OTHER BGP NOTIFICATION DETECTED!");
                System.out.println("BGP notification OID: " + trapOID);
            }
        }

        if (isBGPTrap) {
            System.out.println("\n" + createRepeatedString("!", 60));
            System.out.println("                    *** BGP ALARM ***");
            System.out.println(createRepeatedString("!", 60));
            System.out.println("Event Type: " + trapType);
            System.out.println("Router: " + getRouterName(sourceIP) + " (" + sourceIP + ")");
            System.out.println("Time: " + currentTime);
            System.out.println(createRepeatedString("!", 60));
        }

        System.out.println("\nTrap Variables:");
        Vector<? extends VariableBinding> varBinds = pdu.getVariableBindings();
        if (varBinds.isEmpty()) {
            System.out.println("  No variables in trap");
        } else {
            for (int i = 0; i < varBinds.size(); i++) {
                VariableBinding vb = varBinds.get(i);
                OID oid = vb.getOid();
                Variable var = vb.getVariable();
                
                String description = getOIDDescription(oid);
                System.out.println("  " + oid + " = " + var + " (" + description + ")");
                
                if (isBGPTrap) {
                    decodeBGPVariable(oid, var);
                }
            }
        }

        if (isBGPTrap) {
            logBGPEvent(trapType, sourceIP, currentTime);
        }

        System.out.println(createRepeatedString("=", 60) + "\n");
    }

    private void decodeBGPVariable(OID oid, Variable var) {
        try {
            if (oid.startsWith(BGP_PEER_STATE)) {
                int state = var.toInt();
                String stateDesc = getBGPStateDescription(state);
                System.out.println("    BGP Peer State: " + state + " (" + stateDesc + ")");
            } else if (oid.startsWith(BGP_PEER_LAST_ERROR)) {
                System.out.println("    BGP Last Error: " + var.toString());
            } else if (oid.startsWith(BGP_PEER_REMOTE_ADDR)) {
                System.out.println("    BGP Peer Address: " + var.toString());
            }
        } catch (Exception e) {
        }
    }

    private String getBGPStateDescription(int state) {
        switch (state) {
            case 1: return "idle";
            case 2: return "connect";
            case 3: return "active";
            case 4: return "opensent";
            case 5: return "openconfirm";
            case 6: return "established";
            default: return "unknown";
        }
    }

    private String getOIDDescription(OID oid) {
        if (oid.equals(SnmpConstants.snmpTrapOID)) {
            return "SNMP Trap OID";
        } else if (oid.equals(SnmpConstants.sysUpTime)) {
            return "System Uptime";
        } else if (oid.startsWith(BGP_PEER_STATE)) {
            return "BGP Peer State";
        } else if (oid.startsWith(BGP_PEER_LAST_ERROR)) {
            return "BGP Peer Last Error";
        } else if (oid.startsWith(BGP_PEER_REMOTE_ADDR)) {
            return "BGP Peer Remote Address";
        } else if (oid.startsWith(new OID("1.3.6.1.2.1.15"))) {
            return "BGP MIB Variable";
        } else {
            return "Unknown";
        }
    }

    private String getRouterName(String ip) {
        if ("192.168.10.1".equals(ip)) {
            return "R1";
        } else if ("192.168.20.1".equals(ip)) {
            return "R2";
        } else if ("192.168.30.1".equals(ip)) {
            return "R3";
        } else {
            return "Unknown Router";
        }
    }

    private String extractIPFromAddress(String address) {
        if (address.contains("/")) {
            return address.split("/")[0];
        }
        return address;
    }

    private void logBGPEvent(String eventType, String sourceIP, String timestamp) {
        String logEntry = String.format("[%s] BGP_EVENT: %s from %s (%s)",
            timestamp, eventType, getRouterName(sourceIP), sourceIP);
        System.out.println("\nLOG: " + logEntry);
    }

    private String createRepeatedString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        try {
            Variant6 trapListener = new Variant6();
            trapListener.start();
        } catch (IOException e) {
            System.err.println("Failed to start BGP trap listener: " + e.getMessage());
            e.printStackTrace();
        }
    }
}