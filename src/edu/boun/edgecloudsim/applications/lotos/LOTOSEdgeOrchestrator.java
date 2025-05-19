/*
 * Title:        EdgeCloudSim - Basic Edge Orchestrator implementation
 * 
 * Description: 
 * BasicEdgeOrchestrator implements basic algorithms which are
 * first/next/best/worst/random fit algorithms while assigning
 * requests to the edge devices.
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.lotos;

import org.json.JSONObject;
import java.io.FileReader;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.io.IOException;

import java.io.File;
import java.io.FileWriter;

import java.util.Random;

import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.Decision;

import java.util.Map;
import java.util.HashMap;

import java.io.InputStreamReader;
import java.io.BufferedReader;

import edu.boun.edgecloudsim.utils.TaskProperty;

import edu.boun.edgecloudsim.scheduler.CustomizedCloudletSchedulerTimeShared;

import org.antlr.runtime.RecognitionException;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.utils.SimLogger;

public class LOTOSEdgeOrchestrator extends EdgeOrchestrator {
	public static final double MAX_DATA_SIZE=2500;
	
	private int numberOfEdgeHost; //used by load balancer
	private int numberOfCloudHost;

	public LOTOSEdgeOrchestrator(String _policy, String _simScenario) {
		super(_policy, _simScenario);
	}

	@Override
	public void initialize() {
		numberOfEdgeHost=SimSettings.getInstance().getNumOfEdgeHosts();
		numberOfCloudHost=SimSettings.getInstance().getNumOfCloudHost();
	}
	
	@Override
	public Vm getVmToOffload(Task task, int deviceId) {
		Vm selectedVM = null;
		
		if(deviceId == SimSettings.CLOUD_DATACENTER_ID){
			//Select VM on cloud devices via Least Loaded algorithm!
			double selectedVmCapacity = 0; //start with min value
			List<Host> list = SimManager.getInstance().getCloudServerManager().getDatacenter().getHostList();
			for (int hostIndex=0; hostIndex < list.size(); hostIndex++) {
				List<CloudVM> vmArray = SimManager.getInstance().getCloudServerManager().getVmList(hostIndex);
				for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
					double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
					double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
					if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
						selectedVM = vmArray.get(vmIndex);
						selectedVmCapacity = targetVmCapacity;
					}
	            }
			}
		}
		else if(deviceId == SimSettings.GENERIC_EDGE_DEVICE_ID){
			//Select VM on edge devices via Least Loaded algorithm!
			double selectedVmCapacity = 0; //start with min value
			for(int hostIndex=0; hostIndex<numberOfEdgeHost; hostIndex++){
				List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
				for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
					double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
					double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
					if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
						selectedVM = vmArray.get(vmIndex);
						selectedVmCapacity = targetVmCapacity;
					}
				}
			}
		}
		else{
			//if the host is specifically defined!
			List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(deviceId);
			//Select VM on edge devices via Least Loaded algorithm!
			double selectedVmCapacity = 0; //start with min value
			for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
				double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
				double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
				if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
					selectedVM = vmArray.get(vmIndex);
					selectedVmCapacity = targetVmCapacity;
				}
			}
		}
		return selectedVM;
	}

	@Override
	public int getDeviceToOffload(Task task){
		return 1;
	}

	/*Abordagem aleatória*/ 
	@SuppressWarnings("unchecked")
	@Override
	public Decision chooseVmToOffload(List<TaskProperty> tasks){
		List<Vm> listOfVms = new ArrayList<Vm>();
		List<Integer> listOfTypes = new ArrayList<Integer>();

		//------------------Record of the elements in json----------------------
		generateMANInformation();

		double mean_size = generateTaskInformation(tasks);

		List<Object> returned_elements = generateVMInformation(mean_size);
		Map<String, Vm> vmdictionary = (Map<String, Vm>)returned_elements.get(0);
		Map<String, Integer> typedictionary = (Map<String, Integer>)returned_elements.get(1);
		//----------------------------------------------------------------------

		//-----------------------Running the Solver-----------------------------
		ProcessBuilder processBuilder = new ProcessBuilder();
        try {
			Process p = new ProcessBuilder("python3", "../../src/edu/boun/edgecloudsim/applications/lotos/LOTOS/create_config.py", Integer.toString(SimManager.getInstance().getNumOfMobileDevice())).redirectErrorStream(true).start();
			p.getInputStream().transferTo(System.out);
			int rc = p.waitFor();
		} catch (Exception e) {
            e.printStackTrace();
        }
        try {
			Process p = new ProcessBuilder("python3", "../../src/edu/boun/edgecloudsim/applications/lotos/LOTOS/model.py", Integer.toString(SimManager.getInstance().getNumOfMobileDevice())).redirectErrorStream(true).start();
			p.getInputStream().transferTo(System.out);
			int rc = p.waitFor();
		} catch (Exception e) {
            e.printStackTrace();
        }
		//----------------------------------------------------------------------

		//-----------------------Reding the solution----------------------------
        StringBuilder jsonContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("solver_solutions/S2_sol.json"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = new JSONObject(jsonContent.toString());
		JSONObject solutionObject = jsonObject.getJSONObject("solution");
		for (int i=0; i< tasks.size(); i++){
			String key = Integer.toString(i);
			if (solutionObject.has(key)) { 
				listOfVms.add(vmdictionary.get(solutionObject.getString(key)));
				listOfTypes.add(typedictionary.get(solutionObject.getString(key)));
			} else {
				listOfVms.add(null); 
				listOfTypes.add(null);
    		}
		}
		//----------------------------------------------------------------------

		Decision decision = new Decision(listOfVms, listOfTypes);
		return decision;
	}

	@Override
	public void processEvent(SimEvent arg0) {
		// Nothing to do!
	}

	@Override
	public void shutdownEntity() {
		// Nothing to do!
	}

	@Override
	public void startEntity() {
		// Nothing to do!
	}

	@SuppressWarnings("unchecked")
	public void generateMANInformation(){
		JSONObject Obj = new JSONObject();
		JSONObject jsObj = new JSONObject();

		/*Information about tasks*/
		int devCount = LOTOSExperimentalNetworkModel.getInstance().getNumOfDevices();
		double poissonDL = LOTOSExperimentalNetworkModel.getInstance().getPoissonDownload();
		double poissonUL = LOTOSExperimentalNetworkModel.getInstance().getPoissonUpload();
		double avgInput = LOTOSExperimentalNetworkModel.getInstance().getavgManTaskInputSize();
		double avgOutput = LOTOSExperimentalNetworkModel.getInstance().getavgManTaskOutputSize();
		double manBW = LOTOSExperimentalNetworkModel.getInstance().getMANBand();
		/*------------------------*/

		jsObj.put("dev_count", devCount);
		jsObj.put("poisson_dl", poissonDL);
		jsObj.put("poisson_ul", poissonUL);
		jsObj.put("avg_upload", avgInput);
		jsObj.put("avg_download", avgOutput);
		jsObj.put("man_bandwidth", manBW);

		Obj.put("man", jsObj);


		try {
			FileWriter jsFile = new FileWriter("solver_configs/_man.json");
			jsFile.write(Obj.toString(4));


			jsFile.flush();
			jsFile.close();
		} catch (IOException e) {
    		e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public double generateTaskInformation(List<TaskProperty> tasks){

		JSONObject Obj = new JSONObject();
		JSONObject jsObj = new JSONObject();

		double mean_size_ul = 0;
		double mean_size_dl = 0;

		for (int i=0; i< tasks.size(); i++){

			mean_size_ul = mean_size_ul + tasks.get(i).getOutputFileSize();
			mean_size_dl = mean_size_dl + tasks.get(i).getInputFileSize();

			JSONObject taskObj = new JSONObject();


			/*Information about tasks*/
			int taskID = i;
			int userID = tasks.get(i).getMobileDeviceId();
			double download_size = tasks.get(i).getInputFileSize();
			double upload_size = tasks.get(i).getOutputFileSize();
			double cores = tasks.get(i).getPesNumber();
			double milps = tasks.get(i).getLength();
			Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(tasks.get(i).getMobileDeviceId(), CloudSim.clock());
			int ap = currentLocation.getServingWlanId();
			double start = ((TaskProperty)tasks.get(i)).getStartTime();
			double waiting_time = (CloudSim.clock() - start);
			double processing_edge = SimSettings.getInstance().getTaskLookUpTable()[tasks.get(i).getTaskType()][9];
			double processing_cloud = SimSettings.getInstance().getTaskLookUpTable()[tasks.get(i).getTaskType()][10];
			double ram = SimSettings.getInstance().getTaskLookUpTable()[tasks.get(i).getTaskType()][14];
			double delay = SimSettings.getInstance().getTaskLookUpTable()[tasks.get(i).getTaskType()][13];
			/*-----------------------*/

			taskObj.put("task_id", taskID);
			taskObj.put("user_id", userID);
			taskObj.put("download_size", download_size);
			taskObj.put("upload_size", upload_size);
			taskObj.put("cores_demand", cores);
			taskObj.put("millions_of_instructions", milps);
			taskObj.put("ap", ap);
			taskObj.put("delta_inicial", start);
			taskObj.put("processing_demand_edge", processing_edge);
			taskObj.put("processing_demand_cloud", processing_cloud);
			taskObj.put("ram_demand", ram);
			taskObj.put("delay_limit", delay);
			taskObj.put("waiting_time", waiting_time);

			jsObj.put(Integer.toString(taskID), taskObj);

		}

		mean_size_ul = mean_size_ul/tasks.size();
		mean_size_dl = mean_size_dl/tasks.size();	

		Obj.put("tasks", jsObj);
		Obj.put("simulation_time", SimSettings.getInstance().getSimulationTime());

		try {
			FileWriter jsFile = new FileWriter("solver_configs/_tasks.json");
			jsFile.write(Obj.toString(4));


			jsFile.flush();
			jsFile.close();
		} catch (IOException e) {
    		e.printStackTrace();
		}

		if(mean_size_ul > mean_size_dl){
			return mean_size_ul;
		}else{
			return mean_size_dl;
		}
	}


	@SuppressWarnings("unchecked")
	public List<Object> generateVMInformation(double mean_size){

		Map<String, Vm> vmdictionary = new HashMap<>();
		Map<String, Integer> typedictionary = new HashMap<>();

		JSONObject vmTotalObj = new JSONObject();
		JSONObject vmjsObj = new JSONObject();

		JSONObject apTotalObj = new JSONObject();
		JSONObject apjsObj = new JSONObject();

		int number_of_edge_vms = 0;
		int number_of_cloud_vms = 0;

		double man_capacity = 1300*1024/mean_size;

		double vm_feature;

		for(int hostIndex=0; hostIndex<numberOfEdgeHost; hostIndex++){

			JSONObject apObj = new JSONObject();

			List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);

			EdgeHost host = (EdgeHost)(vmArray.get(0).getHost());

			apObj.put("ap_id", host.getLocation().getServingWlanId());

			vm_feature = 25-SimManager.getInstance().getNetworkModel().getWanClients(host.getLocation().getServingWlanId());
			vm_feature = (vm_feature < 0) ? 0 : vm_feature;
			apObj.put("wan_capacity", vm_feature);

			vm_feature = 100-SimManager.getInstance().getNetworkModel().getWlanClients(host.getLocation().getServingWlanId());
			vm_feature = (vm_feature < 0) ? 0 : vm_feature;
			apObj.put("wlan_capacity", vm_feature);

			apjsObj.put(Integer.toString(host.getLocation().getServingWlanId()), apObj);

			for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){

        		vmdictionary.put(Integer.toString(number_of_edge_vms), vmArray.get(vmIndex));
        		typedictionary.put(Integer.toString(number_of_edge_vms), hostIndex);

				JSONObject vmObj = new JSONObject();
				vmObj.put("vm_id", number_of_edge_vms);
				vm_feature = ((double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock()));
				vm_feature = (vm_feature < 0) ? 0 : vm_feature;
				vmObj.put("cpu_capacity", vm_feature);

				CustomizedCloudletSchedulerTimeShared fuzzyCloudSched = (CustomizedCloudletSchedulerTimeShared)vmArray.get(vmIndex).getCloudletScheduler();

				vm_feature = vmArray.get(vmIndex).getRam() - fuzzyCloudSched.getTotalUtilizationOfRam(CloudSim.clock());
				vm_feature = (vm_feature < 0) ? 0 : vm_feature;
				vmObj.put("ram_capacity", vm_feature);
				vmObj.put("cores", vmArray.get(vmIndex).getNumberOfPes());
				vmObj.put("millions_of_instructions", vmArray.get(vmIndex).getMips());
				host = (EdgeHost)(vmArray.get(vmIndex).getHost());
				vmObj.put("ap", host.getLocation().getServingWlanId());
				vmObj.put("type", "Edge");
				vmObj.put("cost_initialize", SimSettings.getInstance().getEdgeLookUpTable().get(number_of_edge_vms)[0]);
				vmObj.put("cost_per_time", SimSettings.getInstance().getEdgeLookUpTable().get(number_of_edge_vms)[1]);
				vmObj.put("legacy_tasks", SimLogger.getInstance().getVMMapTaskNumber(vmArray.get(vmIndex).getId()));

				vmjsObj.put(Integer.toString(number_of_edge_vms), vmObj);
				
				number_of_edge_vms = number_of_edge_vms + 1;
			}
		}

		for(int hostIndex=0; hostIndex<numberOfCloudHost; hostIndex++){
			List<CloudVM> vmArray = SimManager.getInstance().getCloudServerManager().getVmList(hostIndex);
			for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
				vmdictionary.put(Integer.toString(number_of_edge_vms + number_of_cloud_vms), vmArray.get(vmIndex));
				typedictionary.put(Integer.toString(number_of_edge_vms + number_of_cloud_vms), SimSettings.CLOUD_DATACENTER_ID);

				JSONObject vmObj = new JSONObject();
				vmObj.put("vm_id", number_of_edge_vms + number_of_cloud_vms);
				vm_feature = ((double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock()));
				vm_feature = (vm_feature < 0) ? 0 : vm_feature;

				vmObj.put("cpu_capacity", vm_feature);

				CustomizedCloudletSchedulerTimeShared fuzzyCloudSched = (CustomizedCloudletSchedulerTimeShared)vmArray.get(vmIndex).getCloudletScheduler();

				vm_feature = vmArray.get(vmIndex).getRam() - fuzzyCloudSched.getTotalUtilizationOfRam(CloudSim.clock());
				vm_feature = (vm_feature < 0) ? 0 : vm_feature;
				vmObj.put("ram_capacity", vm_feature);


				vmObj.put("cores", vmArray.get(vmIndex).getNumberOfPes());
				vmObj.put("millions_of_instructions", vmArray.get(vmIndex).getMips());
				vmObj.put("ap", -1);
				vmObj.put("type", "Cloud");
				vmObj.put("cost_initialize", SimSettings.getInstance().getCloudCostInit()[number_of_cloud_vms]);
				vmObj.put("cost_per_time", SimSettings.getInstance().getCloudCostSec()[number_of_cloud_vms]);
				vmObj.put("legacy_tasks", SimLogger.getInstance().getVMMapTaskNumber(vmArray.get(vmIndex).getId()));
				
				vmjsObj.put(Integer.toString(number_of_edge_vms + number_of_cloud_vms), vmObj);

				number_of_cloud_vms = number_of_cloud_vms + 1;
			}
		}

		vmTotalObj.put("vms", vmjsObj);
		apTotalObj.put("aps", apjsObj);

		vm_feature = man_capacity - SimManager.getInstance().getNetworkModel().getManClients();
		vm_feature = (vm_feature < 0) ? 0 : vm_feature;
		apTotalObj.put("man_capacity", vm_feature);

		try {
			FileWriter vmjsFile = new FileWriter("solver_configs/_vms.json");
			FileWriter apjsFile = new FileWriter("solver_configs/_aps.json");

			vmjsFile.write(vmTotalObj.toString(4));
			apjsFile.write(apTotalObj.toString(4));


			vmjsFile.flush();
			vmjsFile.close();


			apjsFile.flush();
			apjsFile.close();

		} catch (IOException e) {
    		e.printStackTrace();
		}
		return Arrays.asList(vmdictionary, typedictionary);	
	}
}
