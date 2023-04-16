package com.marginallyclever.robotoverlord.components;

import com.marginallyclever.robotoverlord.ComponentTest;
import org.junit.jupiter.api.Test;

import javax.vecmath.Vector3d;

public class DHComponentTest {

    @Test
    public void saveAndLoad() throws Exception {
        DHComponent a = new DHComponent();
        DHComponent b = new DHComponent();
        ComponentTest.saveAndLoad(a,b);

        a.setD(1);
        a.setR(2);
        a.setAlpha(3);
        a.setThetaMin(4);
        a.setThetaMax(7);
        a.setTheta(5);
        a.setThetaHome(6);

        ComponentTest.saveAndLoad(a,b);
    }
}