package com.marginallyclever.ro3.apps.nodetreeview;

import com.marginallyclever.ro3.node.Node;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * {@link NodeTreeBranchRenderer} is a cell renderer for the {@link NodeTreeView}.
 */
public class NodeTreeBranchRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value instanceof NodeTreeBranch) {
            Node node = ((NodeTreeBranch) value).getNode();
            setText(node.getName());
        }
        return this;
    }
}