package rmProjekat;

import javax.swing.*;
import javax.swing.tree.*;

import org.snmp4j.smi.VariableBinding;

import java.io.IOException;
import java.util.*;


public abstract class AbstractTreeMonitor extends Monitor {

    protected JTree tree;

    protected Map<String, List<?>> dataPerRouter;

    public AbstractTreeMonitor(Router[] routers) throws IOException {
        super(routers);
        this.dataPerRouter = new HashMap<>();
        
        
        frame = new JFrame("SNMP Monitor - " + this.getClass().getSimpleName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Routers");
        tree = new JTree(root);
        tree.setCellRenderer(new DefaultTreeCellRenderer());
        JScrollPane scrollPane = new JScrollPane(tree);
        frame.getContentPane().add(scrollPane);

        frame.setVisible(true);
    }
    

    @Override
    protected void draw(ArrayList<VariableBinding> rawData, Router router) {
    	
        List<?> parsedData = parseRawData(rawData);
        
        List<?> prevData = dataPerRouter.get(router.getName());
        if (parsedData == null || parsedData.equals(prevData)) {
            return; 
        }
        dataPerRouter.put(router.getName(), parsedData);
        
        SwingUtilities.invokeLater(() -> {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            DefaultMutableTreeNode routerNode = findOrCreateRouterNode(model, root, router.getName());
            
            routerNode.removeAllChildren();

            for (Object dataObject : parsedData) {
                DefaultMutableTreeNode dataNode = createDataNode(dataObject);
                if (dataNode != null) {
                    routerNode.add(dataNode);
                }
            }
            
            model.reload(routerNode);
            
            tree.expandPath(new javax.swing.tree.TreePath(routerNode.getPath()));
        });
    }

    private DefaultMutableTreeNode findOrCreateRouterNode(DefaultTreeModel model, DefaultMutableTreeNode root, String routerName) {
        Enumeration<?> children = root.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            if (child.getUserObject().equals(routerName)) {
                return child;
            }
        }
        DefaultMutableTreeNode routerNode = new DefaultMutableTreeNode(routerName);
        model.insertNodeInto(routerNode, root, root.getChildCount());
        return routerNode;
    }
    

    protected abstract List<?> parseRawData(ArrayList<VariableBinding> rawData);
    
    protected abstract DefaultMutableTreeNode createDataNode(Object dataObject);
}