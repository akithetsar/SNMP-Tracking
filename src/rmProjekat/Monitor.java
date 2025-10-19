package rmProjekat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public abstract class Monitor {

	protected Router[] routers;
	protected long POLLING_INTERVAL;
	protected Snmp snmp;
	protected JFrame frame;
	protected String name;
	
	
	public Monitor(Router[] routers) throws IOException {
		this.routers = routers;
	    this.snmp = new Snmp(new DefaultUdpTransportMapping());
        this.snmp.listen();
	}
	
	
	public void start() {
		Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
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
	}
	
	protected abstract ArrayList<VariableBinding> monitor(Router router) throws IOException;
	protected abstract void draw(ArrayList<VariableBinding> data, Router router);
	
	
	protected ArrayList<VariableBinding> pollData(Router router, CommunityTarget target, OID startOid, OID endOid) throws IOException {
		
		ArrayList<VariableBinding> bindings = new ArrayList<>();
		OID currentOid = startOid;
		
		while(currentOid != null) {
			PDU pdu = new PDU();
            pdu.add(new VariableBinding(currentOid));
            pdu.setType(PDU.GETNEXT);

            ResponseEvent response = snmp.send(pdu, target);

            if (response == null || response.getResponse() == null || response.getResponse().getErrorStatus() != PDU.noError) {
                System.err.println("Error: SNMP request failed for " + router.getIp());
                break;
            }
            PDU responsePDU = response.getResponse();
            VariableBinding vb = responsePDU.get(0);

            if (vb.getOid() == null || !vb.getOid().startsWith(startOid) || (endOid != null && vb.getOid().compareTo(endOid) >= 0)) {
                break;
            }

            bindings.add(vb);
            currentOid = vb.getOid();
		}
		
		return bindings;
	}

    /**
     * Efficiently polls multiple columns of an SNMP table using a single GETBULK request.
     * This is much faster than walking the entire table with GETNEXT.
     *
     * @param router The router to poll.
     * @param target The SNMP community target.
     * @param columnOids An array of OIDs, one for each table column to fetch.
     * @return A list of variable bindings containing the requested data.
     * @throws IOException
     */
    protected ArrayList<VariableBinding> pollTableColumns(Router router, CommunityTarget target, OID[] columnOids) throws IOException {
        PDU pdu = new PDU();
        pdu.setType(PDU.GETBULK);
        pdu.setNonRepeaters(0);
        // Request a few more rows than we expect, to catch any new interfaces.
        pdu.setMaxRepetitions(router.getNum_of_interfaces() + 5); 

        for (OID oid : columnOids) {
            pdu.add(new VariableBinding(oid));
        }

        ResponseEvent response = snmp.send(pdu, target);

        if (response == null || response.getResponse() == null || response.getResponse().getErrorStatus() != PDU.noError) {
            System.err.println("Error: GETBULK request failed for " + router.getIp());
            return new ArrayList<>(); // Return empty list on error
        }

        PDU responsePDU = response.getResponse();
        ArrayList<VariableBinding> result = new ArrayList<>();
        
        // Filter the response to include only variables from the requested columns.
        // This prevents the list from including data from outside the target table.
        for (int i = 0; i < responsePDU.size(); i++) {
            VariableBinding vb = responsePDU.get(i);
            boolean isInRequestedColumns = false;
            for (OID colOid : columnOids) {
                if (vb.getOid().startsWith(colOid)) {
                    isInRequestedColumns = true;
                    break;
                }
            }
            if (isInRequestedColumns) {
                result.add(vb);
            }
        }
        
        return result;
    }
}