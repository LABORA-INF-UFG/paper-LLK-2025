package edu.boun.edgecloudsim.scheduler;

import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.ResCloudlet;
import edu.boun.edgecloudsim.edge_client.Task;

import java.util.ArrayList;
import java.util.List;

public class CustomizedCloudletSchedulerTimeShared extends CloudletSchedulerTimeShared{
    

    	public double getTotalUtilizationOfRam(double time) {
		double totalUtilization = 0;
		List<Integer> list_of_devices = new ArrayList<>();
		for (ResCloudlet gl : getCloudletExecList()) {
			Task tsk = (Task)gl.getCloudlet();
			if (!list_of_devices.contains(tsk.getMobileDeviceId())){
				totalUtilization += gl.getCloudlet().getUtilizationOfRam(time);
				list_of_devices.add(tsk.getMobileDeviceId());
			}
		}
		if (totalUtilization > 0){
			totalUtilization += 1300;
		}
		return totalUtilization;
	}
}
