package com.marginallyclever.ro3;

import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * {@link FactoryPanel} allows a user to select from a list of things that can be created by a given {@link Factory}.
 * @param <T> the class of thing to create.
 */
public class FactoryPanel<T> extends JPanel {
    private final Factory<T> factory;
    private final JTree tree;
    private final JButton okButton = new JButton("OK");

    public FactoryPanel(Factory<T> factory) {
        super();
        this.factory = factory;

        setMinimumSize(new Dimension(400, 300));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new JLabel("Select a node type to create:"));

        DefaultMutableTreeNode root = createTreeNode(factory.getRoot());
        tree = new JTree(new DefaultTreeModel(root));
        tree.setCellRenderer(new FactoryCategoryCellRenderer());
        tree.addTreeSelectionListener(e -> {
            TreePath path = tree.getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Factory.Category<T> category = (Factory.Category<T>) node.getUserObject();
                okButton.setEnabled(category.supplier != null);
            }
        });

        add(new JScrollPane(tree));
    }

    private DefaultMutableTreeNode createTreeNode(Factory.Category<T> category) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(category);
        for (Factory.Category<T> child : category.children) {
            node.add(createTreeNode(child));
        }
        return node;
    }

    private class FactoryCategoryCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Factory.Category<T> category = (Factory.Category<T>) node.getUserObject();
            if (category.supplier == null) {
                setForeground(Color.LIGHT_GRAY);
            }
            setText(category.name);
            return this;
        }
    }

    /**
     * @return either JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION
     */
    public int getResult() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Factory.Category<T> category = (Factory.Category<T>) node.getUserObject();
            if (category.supplier != null) {
                return JOptionPane.OK_OPTION;
            }
        }
        return JOptionPane.CANCEL_OPTION;
    }

    public String getSelectedNode() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Factory.Category<T> category = (Factory.Category<T>) node.getUserObject();
            return category.name;
        }
        return null;
    }
}