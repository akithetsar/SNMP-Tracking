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
import java.util.Scanner;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class Variant8 extends Monitor {

    private static final OID OID_CISCO_PROCESS_CPU_5SEC = new OID("1.3.6.1.4.1.9.9.109.1.1.1.1.6");
    private static final OID OID_CISCO_PROCESS_CPU_1MIN = new OID("1.3.6.1.4.1.9.9.109.1.1.1.1.7");
    private static final OID OID_CISCO_PROCESS_CPU_5MIN = new OID("1.3.6.1.4.1.9.9.109.1.1.1.1.8");

    private static final OID OID_CISCO_MEMORY_POOL_NAME = new OID("1.3.6.1.4.1.9.9.48.1.1.1.2");
    private static final OID OID_CISCO_MEMORY_POOL_USED = new OID("1.3.6.1.4.1.9.9.48.1.1.1.5");
    private static final OID OID_CISCO_MEMORY_POOL_FREE = new OID("1.3.6.1.4.1.9.9.48.1.1.1.6");

    private Timer timer;
    private volatile boolean running = true;

    public Variant8(Router[] routers) throws IOException {
        super(routers);
        
        System.out.println("=== Variant8 CPU and Memory Monitor ===");
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter polling interval in seconds (default 10): ");
        String input = scanner.nextLine().trim();
        
        if (input.isEmpty()) {
            this.POLLING_INTERVAL = 10000;
        } else {
            try {
                int seconds = Integer.parseInt(input);
                if (seconds > 0) {
                    this.POLLING_INTERVAL = seconds * 1000;
                } else {
                    System.out.println("Invalid input. Using default 10 seconds.");
                    this.POLLING_INTERVAL = 10000;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Using default 10 seconds.");
                this.POLLING_INTERVAL = 10000;
            }
        }
        
        System.out.println("Monitoring " + routers.length + " routers every " + (this.POLLING_INTERVAL / 1000) + " seconds");
        System.out.println("Press ESC to stop monitoring");
        System.out.println("========================================");
        
        setupKeyListener();
    }
    
    private void setupKeyListener() {
        JFrame keyFrame = new JFrame("Variant8 Monitor - Press ESC to exit");
        keyFrame.setSize(400, 100);
        keyFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        keyFrame.setAlwaysOnTop(true);
        keyFrame.setLocationRelativeTo(null);
        
        JLabel label = new JLabel("<html><center>CPU & Memory Monitor Running<br>Press ESC to stop</center></html>");
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
            allData.addAll(pollData(router, target, OID_CISCO_PROCESS_CPU_5SEC, null));
            allData.addAll(pollData(router, target, OID_CISCO_PROCESS_CPU_1MIN, null));
            allData.addAll(pollData(router, target, OID_CISCO_PROCESS_CPU_5MIN, null));
        } catch (Exception e) {
            System.err.println("Error polling CPU data from " + router.getName() + ": " + e.getMessage());
        }
        
        // Get Memory data
        try {
            allData.addAll(pollData(router, target, OID_CISCO_MEMORY_POOL_NAME, null));
            allData.addAll(pollData(router, target, OID_CISCO_MEMORY_POOL_USED, null));
            allData.addAll(pollData(router, target, OID_CISCO_MEMORY_POOL_FREE, null));
        } catch (Exception e) {
            System.err.println("Error polling memory data from " + router.getName() + ": " + e.getMessage());
        }

        System.out.println("Collected " + allData.size() + " data points from " + router.getName());
        return allData;
    }

    @Override
    protected void draw(ArrayList<VariableBinding> data, Router router) {
        System.out.println("\n=== " + router.getName() + " (" + router.getIp() + ") ===");
        System.out.println("Timestamp: " + new Date());
        
        if (data == null || data.isEmpty()) {
            System.out.println("No data received for router " + router.getName());
            System.out.println("This may indicate that Cisco-specific MIBs are not supported on this device");
            return;
        }
        
        Map<Integer, CpuData> cpuMap = new TreeMap<>();
        Map<Integer, MemoryPoolData> memoryMap = new TreeMap<>();
        
        // Parse the data
        for (VariableBinding vb : data) {
            OID oid = vb.getOid();
            
            if (oid.startsWith(OID_CISCO_PROCESS_CPU_5SEC) || 
                oid.startsWith(OID_CISCO_PROCESS_CPU_1MIN) || 
                oid.startsWith(OID_CISCO_PROCESS_CPU_5MIN)) {
                
                if (oid.size() > OID_CISCO_PROCESS_CPU_5SEC.size()) {
                    int processId = oid.last();
                    CpuData cpuData = cpuMap.computeIfAbsent(processId, CpuData::new);
                    
                    try {
                        int value = vb.getVariable().toInt();
                        
                        if (oid.startsWith(OID_CISCO_PROCESS_CPU_5SEC)) {
                            cpuData.cpu5sec = value;
                        } else if (oid.startsWith(OID_CISCO_PROCESS_CPU_1MIN)) {
                            cpuData.cpu1min = value;
                        } else if (oid.startsWith(OID_CISCO_PROCESS_CPU_5MIN)) {
                            cpuData.cpu5min = value;
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing CPU data: " + e.getMessage());
                    }
                }
            } else if (oid.startsWith(OID_CISCO_MEMORY_POOL_NAME) || 
                       oid.startsWith(OID_CISCO_MEMORY_POOL_USED) || 
                       oid.startsWith(OID_CISCO_MEMORY_POOL_FREE)) {
                
                if (oid.size() > OID_CISCO_MEMORY_POOL_NAME.size()) {
                    int poolId = oid.last();
                    MemoryPoolData memData = memoryMap.computeIfAbsent(poolId, MemoryPoolData::new);
                    
                    try {
                        if (oid.startsWith(OID_CISCO_MEMORY_POOL_NAME)) {
                            memData.name = vb.getVariable().toString();
                        } else if (oid.startsWith(OID_CISCO_MEMORY_POOL_USED)) {
                            memData.used = vb.getVariable().toLong();
                        } else if (oid.startsWith(OID_CISCO_MEMORY_POOL_FREE)) {
                            memData.free = vb.getVariable().toLong();
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing memory data: " + e.getMessage());
                    }
                }
            }
        }
        
        System.out.println("\nCPU USAGE:");
        if (cpuMap.isEmpty()) {
            System.out.println("  No CPU data available");
            System.out.println("  (Cisco Process MIB may not be supported on this device)");
        } else {
            System.out.println("  Process CPU Utilization:");
            for (Map.Entry<Integer, CpuData> entry : cpuMap.entrySet()) {
                CpuData cpu = entry.getValue();
                System.out.printf("    Process %d: 5sec=%d%%, 1min=%d%%, 5min=%d%%\n",
                    entry.getKey(), cpu.cpu5sec, cpu.cpu1min, cpu.cpu5min);
            }
            
            if (!cpuMap.isEmpty()) {
                double avg5sec = cpuMap.values().stream().mapToInt(c -> c.cpu5sec).average().orElse(0);
                double avg1min = cpuMap.values().stream().mapToInt(c -> c.cpu1min).average().orElse(0);
                double avg5min = cpuMap.values().stream().mapToInt(c -> c.cpu5min).average().orElse(0);
                
                System.out.printf("  AVERAGE CPU: 5sec=%.1f%%, 1min=%.1f%%, 5min=%.1f%%\n",
                    avg5sec, avg1min, avg5min);
            }
        }
        
        System.out.println("\nMEMORY POOLS:");
        if (memoryMap.isEmpty()) {
            System.out.println("  No memory pool data available");
            System.out.println("  (Cisco Memory Pool MIB may not be supported on this device)");
        } else {
            long totalUsed = 0;
            long totalFree = 0;
            
            for (Map.Entry<Integer, MemoryPoolData> entry : memoryMap.entrySet()) {
                MemoryPoolData mem = entry.getValue();
                long total = mem.used + mem.free;
                double usedPercent = total > 0 ? (mem.used * 100.0 / total) : 0;
                
                String poolName = mem.name != null ? mem.name : "Pool-" + entry.getKey();
                
                System.out.printf("  %s:\n", poolName);
                System.out.printf("    Used: %,d bytes (%.1f%%)\n", mem.used, usedPercent);
                System.out.printf("    Free: %,d bytes\n", mem.free);
                System.out.printf("    Total: %,d bytes\n", total);
                
                totalUsed += mem.used;
                totalFree += mem.free;
            }
            
            long grandTotal = totalUsed + totalFree;
            if (grandTotal > 0) {
                double totalUsedPercent = (totalUsed * 100.0) / grandTotal;
                System.out.printf("\n  TOTAL MEMORY:\n");
                System.out.printf("    Used: %,d bytes (%.1f%%)\n", totalUsed, totalUsedPercent);
                System.out.printf("    Free: %,d bytes\n", totalFree);
                System.out.printf("    Total: %,d bytes\n", grandTotal);
            }
        }
        
        System.out.println("=============================================");
    }

    private static class CpuData {
        int processId;
        int cpu5sec = -1;
        int cpu1min = -1;
        int cpu5min = -1;

        CpuData(int processId) {
            this.processId = processId;
        }
    }

    private static class MemoryPoolData {
        int poolId;
        String name;
        long used = 0;
        long free = 0;

        MemoryPoolData(int poolId) {
            this.poolId = poolId;
        }
    }
}