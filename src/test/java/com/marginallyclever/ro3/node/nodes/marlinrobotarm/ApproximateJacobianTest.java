package com.marginallyclever.ro3.node.nodes.marlinrobotarm;

import com.marginallyclever.ro3.Registry;
import com.marginallyclever.ro3.apps.actions.LoadScene;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Arrays;

/**
 * Checking if Approximate jacobians are commutative.  This test is ignored because they are not.
 *
 * @since 2.6.1
 * @author Dan Royer
 */
public class ApproximateJacobianTest {
    @BeforeEach
    public void setup() {
        Registry.start();
    }

    private MarlinRobotArm build6AxisArm() throws Exception {
        // TODO load a robot from a file.
        var load = new LoadScene(null,null);
        // find file {project root}/src/test/resources/com/marginallyclever/ro3/apps/node/nodes/marlinrobotarm/Sixi3-5.RO
        File file = new File("src/test/resources/com/marginallyclever/ro3/apps/node/nodes/marlinrobotarm/Sixi3-5.RO");
        load.commitLoad(file);
        return (MarlinRobotArm) Registry.getScene().get("./Sixi3/MarlinRobotArm");
    }

    /**
     * Compare the two methods.
     * @throws Exception if error
     */
    @Test
    @Disabled
    public void compare() throws Exception {
        MarlinRobotArm robot = build6AxisArm();
        ApproximateJacobian finite = new ApproximateJacobianFiniteDifferences(robot);
        ApproximateJacobian screw = new ApproximateJacobianScrewTheory(robot);

        System.out.println("Finite "+finite);
        System.out.println("Screw "+screw);
        double [][] finiteJacobian = finite.getJacobian();
        double [][] screwJacobian = screw.getJacobian();
        for(int i=0;i<finiteJacobian.length;++i) {
            for(int j=0;j<finiteJacobian[0].length;++j) {
                Assertions.assertEquals(finiteJacobian[i][j],screwJacobian[i][j],0.1);
            }
        }

        double [] v = new double[] {1,2,3,4,5,6};
        double [] vFinite = finite.getJointFromCartesian(v);
        double [] vScrew = screw.getJointFromCartesian(v);
        System.out.println(Arrays.toString(vFinite));
        System.out.println(Arrays.toString(vScrew));
        for(int i=0;i<vFinite.length;++i) {
            Assertions.assertEquals(vFinite[i],vScrew[i],0.01);
        }
    }
}