package edu.boun.edgecloudsim.utils;

import org.cloudbus.cloudsim.Vm;
import java.util.List;

public class Decision{
    public final List<Vm> vms;
    public final List<Integer> types;

    public Decision(List<Vm> vms, List<Integer> types) {
        this.vms = vms;
        this.types = types;
    }

    public List<Vm> getVms() {
        return vms;
    }

    public List<Integer> getTypes() {
        return types;
    }
}