package other;

import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;

import rmProjekat.Config;
import rmProjekat.Monitor;
import rmProjekat.Router;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.*;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class Variant2 extends Monitor {

    private static final OID OID_IF_IN_OCTETS = new OID("1.3.6.1.2.1.2.2.1.10");
    private static final OID OID_IF_OUT_OCTETS = new OID("1.3.6.1.2.1.2.2.1.16");
    private static final OID OID_IF_IN_UCAST_PKTS = new OID("1.3.6.1.2.1.2.2.1.11");
    private static final OID OID_IF_OUT_UCAST_PKTS = new OID("1.3.6.1.2.1.2.2.1.17");
    private static final OID OID_IF_IN_NUCAST_PKTS = new OID("1.3.6.1.2.1.2.2.1.12");
    private static final OID OID_IF_OUT_NUCAST_PKTS = new OID("1.3.6.1.2.1.2.2.1.18");

    
    private Map<String, Map<Integer, InterfaceStats>> previousStats = new HashMap<>();
    private Map<String, Long> lastPollTime = new HashMap<>();
    private Timer timer;
    private volatile boolean running = true;

    public Variant2(Router[] routers) throws IOException {
        super(routers);
        this.POLLING_INTERVAL = 10000; // 10 seconds
        
        System.out.println("=== Variant2 Network Traffic Monitor ===");
        System.out.println("Monitoring " + routers.length + " routers every " + (POLLING_INTERVAL/1000) + " seconds");
        System.out.println("Press ESC to stop monitoring");
        System.out.println("=========================================");
        

        for (Router router : routers) {
            previousStats.put(router.getIp(), new HashMap<>());
            lastPollTime.put(router.getIp(), System.currentTimeMillis());
        }
        

        setupKeyListener();
    }
    
    private void setupKeyListener() {
        JFrame keyFrame = new JFrame("Variant2 Monitor - Press ESC to exit");
        keyFrame.setSize(400, 100);
        keyFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        keyFrame.setAlwaysOnTop(true);
        keyFrame.setLocationRelativeTo(null);
        
        JLabel label = new JLabel("<html><center>Network Traffic Monitor Running<br>Press ESC to stop</center></html>");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        keyFrame.add(label);
        
        keyFrame.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.out.println("\nESC pressed - shutting down monitor...");
                    stopMonitoring();
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
    
    private void stopMonitoring() {
        running = false;
        if (timer != null) {
            timer.cancel();
        }
        try {
            if (snmp != null) {
                snmp.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing SNMP: " + e.getMessage());
        }
    }

    @Override
    public void start() {
        timer = new Timer(false);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!running) {
                    return;
                }
                for(Router router : routers) {
                    try {
                        ArrayList<VariableBinding> data = monitor(router);
                        draw(data, router);
                    } catch (IOException e) {
                        System.err.println("Failed to poll router " + router.getIp() + ": " + e.getMessage());
                    }
                }
            }
        }, 0, POLLING_INTERVAL);
        

        try {

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down monitor...");
                stopMonitoring();
            }));
            

            Object lock = new Object();
            synchronized (lock) {
                while (running) {
                    lock.wait(1000); 
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Monitor interrupted, shutting down...");
            stopMonitoring();
        }
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
        
        ArrayList<VariableBinding> allData = new ArrayList<>();
        

        System.out.println("\n[" + new Date() + "] Polling " + router.getName() + "...");
        
        try {
            allData.addAll(pollData(router, target, OID_IF_IN_OCTETS, null));
            allData.addAll(pollData(router, target, OID_IF_OUT_OCTETS, null));
            allData.addAll(pollData(router, target, OID_IF_IN_UCAST_PKTS, null));
            allData.addAll(pollData(router, target, OID_IF_OUT_UCAST_PKTS, null));
            allData.addAll(pollData(router, target, OID_IF_IN_NUCAST_PKTS, null));
            allData.addAll(pollData(router, target, OID_IF_OUT_NUCAST_PKTS, null));
        } catch (Exception e) {
            System.err.println("Error polling " + router.getName() + ": " + e.getMessage());
        }
        
        System.out.println("Collected " + allData.size() + " data points from " + router.getName());
        return allData;
    }

    @Override
    protected void draw(ArrayList<VariableBinding> data, Router router) {
        if (data == null || data.isEmpty()) {
            System.out.println("No data received for router " + router.getName());
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastPollTime.get(router.getIp());
        double timeInterval = timeDiff / 1000.0;

        Map<Integer, InterfaceStats> currentStats = parseInterfaceStats(data);
        Map<Integer, InterfaceStats> prevStats = previousStats.get(router.getIp());

        System.out.println("\n=== " + router.getName() + " (" + router.getIp() + ") ===");
        System.out.println("Time interval: " + String.format("%.1f", timeInterval) + " seconds");
        System.out.println("Active interfaces: " + currentStats.size());

        for (Map.Entry<Integer, InterfaceStats> entry : currentStats.entrySet()) {
            int ifIndex = entry.getKey();
            InterfaceStats current = entry.getValue();
            InterfaceStats previous = prevStats.get(ifIndex);

            System.out.println("\nInterface " + ifIndex + ":");
            
            if (previous != null && timeInterval > 0) {
                long inOctetsDiff = current.inOctets - previous.inOctets;
                long outOctetsDiff = current.outOctets - previous.outOctets;
                
                if (inOctetsDiff < 0) inOctetsDiff += (1L << 32);
                if (outOctetsDiff < 0) outOctetsDiff += (1L << 32);
                
                double inThroughput = (8.0 * inOctetsDiff) / timeInterval;
                double outThroughput = (8.0 * outOctetsDiff) / timeInterval;

                long inUcastDiff = current.inUcastPkts - previous.inUcastPkts;
                long outUcastDiff = current.outUcastPkts - previous.outUcastPkts;
                long inNucastDiff = current.inNucastPkts - previous.inNucastPkts;
                long outNucastDiff = current.outNucastPkts - previous.outNucastPkts;

                if (inUcastDiff < 0) inUcastDiff += (1L << 32);
                if (outUcastDiff < 0) outUcastDiff += (1L << 32);
                if (inNucastDiff < 0) inNucastDiff += (1L << 32);
                if (outNucastDiff < 0) outNucastDiff += (1L << 32);

                double inUcastRate = inUcastDiff / timeInterval;
                double outUcastRate = outUcastDiff / timeInterval;
                double inNucastRate = inNucastDiff / timeInterval;
                double outNucastRate = outNucastDiff / timeInterval;

                // Calculate total packet counts and rates
                long totalInPkts = current.inUcastPkts + current.inNucastPkts;
                long totalOutPkts = current.outUcastPkts + current.outNucastPkts;
                double totalInPktRate = inUcastRate + inNucastRate;
                double totalOutPktRate = outUcastRate + outNucastRate;

                System.out.printf("  TOTAL PACKETS:\n");
                System.out.printf("    IN:  %,d packets (%.1f pkt/s)\n", totalInPkts, totalInPktRate);
                System.out.printf("    OUT: %,d packets (%.1f pkt/s)\n", totalOutPkts, totalOutPktRate);
                
                System.out.printf("  TRAFFIC THROUGHPUT:\n");
                System.out.printf("    IN:  %,d octets -> %.0f bit/s\n", current.inOctets, inThroughput);
                System.out.printf("    OUT: %,d octets -> %.0f bit/s\n", current.outOctets, outThroughput);
                
                System.out.printf("  UNICAST PACKETS:\n");
                System.out.printf("    IN:  %,d packets (%.1f pkt/s)\n", current.inUcastPkts, inUcastRate);
                System.out.printf("    OUT: %,d packets (%.1f pkt/s)\n", current.outUcastPkts, outUcastRate);
                
                System.out.printf("  NON-UNICAST PACKETS:\n");
                System.out.printf("    IN:  %,d packets (%.1f pkt/s)\n", current.inNucastPkts, inNucastRate);
                System.out.printf("    OUT: %,d packets (%.1f pkt/s)\n", current.outNucastPkts, outNucastRate);
                    
            } else {
                long totalInPkts = current.inUcastPkts + current.inNucastPkts;
                long totalOutPkts = current.outUcastPkts + current.outNucastPkts;
                
                System.out.printf("  TOTAL PACKETS (baseline):\n");
                System.out.printf("    IN:  %,d packets\n", totalInPkts);
                System.out.printf("    OUT: %,d packets\n", totalOutPkts);
                
                System.out.printf("  TRAFFIC (baseline):\n");
                System.out.printf("    IN:  %,d octets\n", current.inOctets);
                System.out.printf("    OUT: %,d octets\n", current.outOctets);
                
                System.out.printf("  UNICAST PACKETS (baseline):\n");
                System.out.printf("    IN:  %,d packets\n", current.inUcastPkts);
                System.out.printf("    OUT: %,d packets\n", current.outUcastPkts);
                
                System.out.printf("  NON-UNICAST PACKETS (baseline):\n");
                System.out.printf("    IN:  %,d packets\n", current.inNucastPkts);
                System.out.printf("    OUT: %,d packets\n", current.outNucastPkts);
            }
        }

        previousStats.put(router.getIp(), currentStats);
        lastPollTime.put(router.getIp(), currentTime);
        
        System.out.println("===================================");
    }

    private Map<Integer, InterfaceStats> parseInterfaceStats(ArrayList<VariableBinding> rawData) {
        Map<Integer, InterfaceStats> statsMap = new TreeMap<>();
        
        for (VariableBinding vb : rawData) {
            OID oid = vb.getOid();
            if (oid.size() <= OID_IF_IN_OCTETS.size()) continue;
            
            int ifIndex = oid.last();
            InterfaceStats stats = statsMap.computeIfAbsent(ifIndex, InterfaceStats::new);
            
            try {
                long value = vb.getVariable().toLong();
                
                if (oid.startsWith(OID_IF_IN_OCTETS)) {
                    stats.inOctets = value;
                } else if (oid.startsWith(OID_IF_OUT_OCTETS)) {
                    stats.outOctets = value;
                } else if (oid.startsWith(OID_IF_IN_UCAST_PKTS)) {
                    stats.inUcastPkts = value;
                } else if (oid.startsWith(OID_IF_OUT_UCAST_PKTS)) {
                    stats.outUcastPkts = value;
                } else if (oid.startsWith(OID_IF_IN_NUCAST_PKTS)) {
                    stats.inNucastPkts = value;
                } else if (oid.startsWith(OID_IF_OUT_NUCAST_PKTS)) {
                    stats.outNucastPkts = value;
                }
            } catch (Exception e) {
                System.err.println("Error parsing value for OID " + oid + ": " + e.getMessage());
            }
        }
        
        return statsMap;
    }

    private static class InterfaceStats {
        int ifIndex;
        long inOctets = 0;
        long outOctets = 0;
        long inUcastPkts = 0;
        long outUcastPkts = 0;
        long inNucastPkts = 0;
        long outNucastPkts = 0;

        InterfaceStats(int ifIndex) {
            this.ifIndex = ifIndex;
        }
    }
}