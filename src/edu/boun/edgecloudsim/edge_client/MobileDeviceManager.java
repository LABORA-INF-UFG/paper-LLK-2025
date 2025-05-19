package edu.boun.edgecloudsim.edge_client;

import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.UtilizationModel;
import java.util.List;
import org.cloudbus.cloudsim.Vm;

import edu.boun.edgecloudsim.utils.TaskProperty;

import edu.boun.edgecloudsim.utils.Decision;

public abstract class MobileDeviceManager  extends DatacenterBroker {

	public MobileDeviceManager() throws Exception {
		super("Global_Broker");
	}
	
	/*
	 * initialize mobile device manager if needed
	 */
	public abstract void initialize();
	
	/*
	 * provides abstract CPU Utilization Model
	 */
	public abstract UtilizationModel getCpuUtilizationModel();

	public abstract UtilizationModel getRamUtilizationModel();
	
	public abstract void submitTask(TaskProperty edgeTask);

	public abstract void submitTask(TaskProperty edgeTask, Vm vm, Integer tipo);

	public abstract Decision submitTasks(List<TaskProperty> edgeTask);
}
