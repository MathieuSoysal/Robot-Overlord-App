package com.marginallyclever.convenience;

import com.marginallyclever.ro3.node.Node;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathCalculator {
    /**
     * Return the relative path from origin to goal.
     * @param origin the starting point
     * @param goal the destination
     * @return the relative path from origin to goal
     */
    public static String getRelativePath(String origin, String goal) {
        Path pathOrigin = Paths.get(origin);
        Path pathGoal = Paths.get(goal);

        Path relativePath = pathOrigin.relativize(pathGoal);
        return relativePath.toString().replace(File.separator, "/");
    }

    /**
     * Return the relative path from origin to goal.
     * @param origin the starting point
     * @param goal the destination
     * @return the relative path from origin to goal or null if goal is null.
     * @throws NullPointerException if origin is null
     */
    public static String getRelativePath(Node origin, Node goal) {
        if(origin==null) throw new NullPointerException("origin cannot be null");
        if(goal==null) return null;
        return getRelativePath(origin.getAbsolutePath(),goal.getAbsolutePath());
    }
}
