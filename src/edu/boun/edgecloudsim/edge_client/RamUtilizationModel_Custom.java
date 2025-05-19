/*
 * Title:        EdgeCloudSim - Custom VM Cpu Utilization Model
 * 
 * Description: 
 * CpuUtilizationModel_Custom implements UtilizationModel and used for
 * VM CPU utilization model. In CloudSim, the CPU utilization of the VM
 * is a simple counter. We provide more realistic utilization model
 * which decide CPU utilization of each application by using the
 * values defined in the applications.xml file. For those who wants to
 * add another VM Cpu Utilization Model to EdgeCloudSim should provide
 * another concreate instance of UtilizationModel via ScenarioFactory
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.edge_client;

import org.cloudbus.cloudsim.UtilizationModel;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimLogger;

public class RamUtilizationModel_Custom implements UtilizationModel {
	private Task task;
	
	public RamUtilizationModel_Custom(){
	}
	
	/*
	 * (non-Javadoc)
	 * @see cloudsim.power.UtilizationModel#getUtilization(double)
	 */
	@Override
	public double getUtilization(double time) {
		int index = 14;
		return SimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][index];
	}
	
	public void setTask(Task _task){
		task=_task;
	}
	
	public double predictUtilization(SimSettings.VM_TYPES _vmType){
		int index = 14;

		return SimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][index];
	}
}
