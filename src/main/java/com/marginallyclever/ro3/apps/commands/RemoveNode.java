package com.marginallyclever.ro3.apps.commands;

import com.marginallyclever.ro3.Registry;
import com.marginallyclever.ro3.node.Node;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remove {@link Node}s from the scene.
 */
public class RemoveNode extends AbstractUndoableEdit {
    private final Map<Node,Node> childParentMap = new HashMap<>();
    public RemoveNode(List<Node> selection) {
        super();
        selection.remove(Registry.getScene());
        for(var node : selection) {
            childParentMap.put(node,node.getParent());
        }
        execute();
    }

    @Override
    public String getPresentationName() {
        int count = childParentMap.size();
        return count>1? "Remove "+count+" Nodes" : "Remove node";
    }

    @Override
    public void redo() {
        super.redo();
        execute();
    }

    public void execute() {
        for(Map.Entry<Node, Node> entry : childParentMap.entrySet()) {
            Node parent = entry.getValue();
            Node child = entry.getKey();
            parent.removeChild(child);
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        reverse();
    }

    public void reverse() {
        for(Map.Entry<Node, Node> entry : childParentMap.entrySet()) {
            Node parent = entry.getValue();
            Node child = entry.getKey();
            parent.addChild(child);
        }
    }
}