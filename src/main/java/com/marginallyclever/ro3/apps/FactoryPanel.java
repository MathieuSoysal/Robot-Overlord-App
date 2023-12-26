package com.marginallyclever.ro3.apps;

import com.marginallyclever.ro3.Factory;
import com.marginallyclever.ro3.apps.nodetreeview.NodeTreeBranch;
import com.marginallyclever.ro3.apps.shared.SearchBar;
import com.marginallyclever.ro3.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link FactoryPanel} allows a user to select from a list of things that can be created by a given {@link Factory}.
 * @param <T> the class of thing to create.
 */
public class FactoryPanel<T> extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(FactoryPanel.class);
    private final JTree tree = new JTree();
    private final Factory<T> factory;
    private final JButton okButton = new JButton("OK");
    private final SearchBar searchBar = new SearchBar();

    public FactoryPanel(Factory<T> factory) {
        super();
        this.factory = factory;

        setMinimumSize(new Dimension(400, 300));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setupTree();
        setupSearch();

        add(new JLabel("Select a node type to create:"));
        add(searchBar);
        add(new JScrollPane(tree));

        populateTree("");
    }

    private void setupSearch() {
        searchBar.addPropertyChangeListener("match", e-> {
            String criteria = (String)e.getNewValue();
            if(criteria==null || criteria.isBlank()) criteria = "";
            populateTree(criteria);
        });
    }

    private void populateTree(String searchCriteria) {
        var root = factory.getRoot();

        List<Factory.Category<T>> matches = findAllTypesMatching(root, searchCriteria);
        logger.debug("Found {} matches", matches.size());
        addAllParents(matches);
        logger.debug("Grown to {} matches", matches.size());

        DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode(root);
        addChildren(root, rootTreeNode, matches);
        tree.setModel(new DefaultTreeModel(rootTreeNode));
    }

    private void addChildren(Factory.Category<T> node, DefaultMutableTreeNode branch, List<Factory.Category<T>> matches) {
        for(Factory.Category<T> child : node.getChildren()) {
            if(!matches.contains(child)) continue;

            DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(child);
            branch.add(childTreeNode);
            addChildren(child, childTreeNode,matches);
        }
    }

    private void addAllParents(List<Factory.Category<T>> matches) {
        List<Factory.Category<T>> toAdd = new ArrayList<>();
        for (Factory.Category<T> node : matches) {
            Factory.Category<T> parent = node.getParent();
            while (parent != null) {
                if(!matches.contains(parent) && !toAdd.contains(parent)) {
                    toAdd.add(parent);
                }
                parent = parent.getParent();
            }
        }
        matches.addAll(toAdd);
    }

    /**
     * Find all categories that match the search criteria.
     * @param root the root of the tree to search
     * @param searchCriteria the search criteria
     * @return a list of all categories that match the search criteria
     */
    private List<Factory.Category<T>> findAllTypesMatching(Factory.Category<T> root, String searchCriteria) {
        boolean isRegex = searchBar.isRegex();
        List<Factory.Category<T>> matches = new ArrayList<>();
        List<Factory.Category<T>> toSearch = new ArrayList<>();
        toSearch.add(root);
        while(!toSearch.isEmpty()) {
            Factory.Category<T> category = toSearch.remove(0);
            String name = category.getName();
            if((isRegex && name.matches(searchCriteria)) || (!isRegex && name.contains(searchCriteria))) {
                matches.add(category);
            }
            toSearch.addAll(category.getChildren());
        }
        return matches;
    }

    private void setupTree() {
        tree.setCellRenderer(new FactoryCategoryCellRenderer());
        tree.addTreeSelectionListener(e -> {
            TreePath path = tree.getSelectionPath();
            if (path != null) {
                Factory.Category<T> category = getCategory((DefaultMutableTreeNode) path.getLastPathComponent());
                okButton.setEnabled(category.getSupplier() != null);
            }
        });

    }

    private class FactoryCategoryCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            var branch = (DefaultMutableTreeNode) value;
            if(branch.getUserObject() instanceof Factory.Category<?> category) {
                if (category.getSupplier() == null) {
                    setForeground(Color.LIGHT_GRAY);
                } else {
                    setForeground(Color.BLACK);
                }
                setText(category.getName());
            }
            return this;
        }
    }

    /**
     * @return either JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION
     */
    public int getResult() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            Factory.Category<T> category = getCategory((DefaultMutableTreeNode) path.getLastPathComponent());
            if (category.getSupplier() != null) {
                return JOptionPane.OK_OPTION;
            }
        }
        return JOptionPane.CANCEL_OPTION;
    }

    public String getSelectedNode() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            Factory.Category<T> category = getCategory((DefaultMutableTreeNode) path.getLastPathComponent());
            return category.getName();
        }
        return null;
    }

    Factory.Category<T> getCategory(DefaultMutableTreeNode node) {
        Object obj = node.getUserObject();
        return (Factory.Category<T>)obj;
    }
}