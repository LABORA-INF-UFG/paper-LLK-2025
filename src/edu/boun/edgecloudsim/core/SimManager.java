/*
 * Title:        EdgeCloudSim - Simulation Manager
 * 
 * Description: 
 * SimManager is an singleton class providing many abstract classeses such as
 * Network Model, Mobility Model, Edge Orchestrator to other modules
 * Critical simulation related information would be gathered via this class 
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.core;

import java.io.IOException;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;


import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import edu.boun.edgecloudsim.edge_server.EdgeVmAllocationPolicy_Custom;
import edu.boun.edgecloudsim.cloud_server.CloudServerManager;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.mobility.MobilityModel;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.cloudbus.cloudsim.Vm;

import edu.boun.edgecloudsim.edge_client.Task;

import edu.boun.edgecloudsim.utils.Decision;

public class SimManager extends SimEntity {
	private static final int CREATE_TASK = 0;
	private static final int CHECK_ALL_VM = 1;
	private static final int GET_LOAD_LOG = 2;
	private static final int PRINT_PROGRESS = 3;
	private static final int STOP_SIMULATION = 4;
	private static final int GET_LOAD_PER_VM_LOG = 5;
	private static final int ORQUESTRA = 6;
	
	private String simScenario;
	private String orchestratorPolicy;
	private int numOfMobileDevice;
	private NetworkModel networkModel;
	private MobilityModel mobilityModel;
	private ScenarioFactory scenarioFactory;
	private EdgeOrchestrator edgeOrchestrator;
	private EdgeServerManager edgeServerManager;
	private CloudServerManager cloudServerManager;
	private MobileServerManager mobileServerManager;
	private LoadGeneratorModel loadGeneratorModel;
	private MobileDeviceManager mobileDeviceManager;
	
	private static SimManager instance = null;
	
	public SimManager(ScenarioFactory _scenarioFactory, int _numOfMobileDevice, String _simScenario, String _orchestratorPolicy) throws Exception {
		super("SimManager");
		simScenario = _simScenario;
		scenarioFactory = _scenarioFactory;
		numOfMobileDevice = _numOfMobileDevice;
		orchestratorPolicy = _orchestratorPolicy;

		SimLogger.print("Creating tasks...");
		loadGeneratorModel = scenarioFactory.getLoadGeneratorModel();
		loadGeneratorModel.initializeModel();
		SimLogger.printLine("Done, ");
		
		SimLogger.print("Creating device locations...");
		mobilityModel = scenarioFactory.getMobilityModel();
		mobilityModel.initialize();
		SimLogger.printLine("Done.");

		//Generate network model
		networkModel = scenarioFactory.getNetworkModel();
		networkModel.initialize();
		
		//Generate edge orchestrator
		edgeOrchestrator = scenarioFactory.getEdgeOrchestrator();
		edgeOrchestrator.initialize();
		
		//Create Physical Servers
		edgeServerManager = scenarioFactory.getEdgeServerManager();
		edgeServerManager.initialize();
		
		//Create Physical Servers on cloud
		cloudServerManager = scenarioFactory.getCloudServerManager();
		cloudServerManager.initialize();
		
		//Create Physical Servers on mobile devices
		mobileServerManager = scenarioFactory.getMobileServerManager();
		mobileServerManager.initialize();

		//Create Client Manager
		mobileDeviceManager = scenarioFactory.getMobileDeviceManager();
		mobileDeviceManager.initialize();
		
		instance = this;
	}
	
	public static SimManager getInstance(){
		return instance;
	}
	
	/**
	 * Triggering CloudSim to start simulation
	 */
	public void startSimulation() throws Exception{
		//Starts the simulation
		SimLogger.print(super.getName()+" is starting...");
		
		//Start Edge Datacenters & Generate VMs
		edgeServerManager.startDatacenters();
		edgeServerManager.createVmList(mobileDeviceManager.getId());
		
		//Start Edge Datacenters & Generate VMs
		cloudServerManager.startDatacenters();
		cloudServerManager.createVmList(mobileDeviceManager.getId());
		
		//Start Mobile Datacenters & Generate VMs
		mobileServerManager.startDatacenters();
		mobileServerManager.createVmList(mobileDeviceManager.getId());
		
		CloudSim.startSimulation();
	}

	public String getSimulationScenario(){
		return simScenario;
	}

	public String getOrchestratorPolicy(){
		return orchestratorPolicy;
	}
	
	public ScenarioFactory getScenarioFactory(){
		return scenarioFactory;
	}
	
	public int getNumOfMobileDevice(){
		return numOfMobileDevice;
	}
	
	public NetworkModel getNetworkModel(){
		return networkModel;
	}

	public MobilityModel getMobilityModel(){
		return mobilityModel;
	}
	
	public EdgeOrchestrator getEdgeOrchestrator(){
		return edgeOrchestrator;
	}
	
	public EdgeServerManager getEdgeServerManager(){
		return edgeServerManager;
	}
	
	public CloudServerManager getCloudServerManager(){
		return cloudServerManager;
	}
	
	public MobileServerManager getMobileServerManager(){
		return mobileServerManager;
	}

	public LoadGeneratorModel getLoadGeneratorModel(){
		return loadGeneratorModel;
	}
	
	public MobileDeviceManager getMobileDeviceManager(){
		return mobileDeviceManager;
	}
	
	@Override
	public void startEntity() {
		int hostCounter=0;

		for(int i= 0; i<edgeServerManager.getDatacenterList().size(); i++) {
			List<? extends Host> list = edgeServerManager.getDatacenterList().get(i).getHostList();
			for (int j=0; j < list.size(); j++) {
				mobileDeviceManager.submitVmList(edgeServerManager.getVmList(hostCounter));
				hostCounter++;
			}
		}
		
		for(int i= 0; i<SimSettings.getInstance().getNumOfCloudHost(); i++) {
			mobileDeviceManager.submitVmList(cloudServerManager.getVmList(i));
		}

		for(int i=0; i<numOfMobileDevice; i++){
			if(mobileServerManager.getVmList(i) != null)
				mobileDeviceManager.submitVmList(mobileServerManager.getVmList(i));
		}
		

		System.out.println("\n\nQUANTAS TAREFAS: " + loadGeneratorModel.getTaskList().size() + "\n\n");



		if(SimSettings.getInstance().getOrchestratorPolicies()[0].equals("LOTOS")){
			/*Ordenação das tarefas conforme o tempo de geração da tarefa*/
			List<TaskProperty> orderedTasks = loadGeneratorModel.getTaskList();
			orderedTasks.sort(Comparator.comparingDouble(TaskProperty::getStartTime));

			/*divisão em lotes*/
			int batch_size = SimSettings.getInstance().getNumberOfTasksSendToSolver();
			double batch_time_limit = SimSettings.getInstance().getBatchTimestampLimit();

			double batch_time_span = 0;
			double time_of_the_last_task = orderedTasks.get(0).getStartTime();

			List<List<TaskProperty>> subLists = new ArrayList<>();
			List<TaskProperty> batch = new ArrayList<>();
			for (int i = 0; i < orderedTasks.size(); i ++) {

				batch_time_span = batch_time_span + (orderedTasks.get(i).getStartTime() - time_of_the_last_task);

				if (batch.size() < batch_size && batch_time_span < batch_time_limit){
					batch.add(orderedTasks.get(i));
					time_of_the_last_task = orderedTasks.get(i).getStartTime();

				}else{
					subLists.add(batch);
					batch = new ArrayList<>();
					batch.add(orderedTasks.get(i));
					time_of_the_last_task = orderedTasks.get(i).getStartTime();
					batch_time_span = 0;
				}
			}
			if (batch.size() != 0){
				subLists.add(batch);
			}

			/*Envio de cada lote ao orquestrador*/
			for (int i = 0; i < subLists.size(); i ++) {
				schedule(getId(), subLists.get(i).get(subLists.get(i).size()-1).getStartTime(), ORQUESTRA, subLists.get(i));
			}
		}else{
			//Creation of tasks are scheduled here!
			for(int i=0; i< loadGeneratorModel.getTaskList().size(); i++)
				schedule(getId(), loadGeneratorModel.getTaskList().get(i).getStartTime(), CREATE_TASK, loadGeneratorModel.getTaskList().get(i));
		}


		//Periodic event loops starts from here!
		schedule(getId(), 5, CHECK_ALL_VM);
		schedule(getId(), SimSettings.getInstance().getSimulationTime()/100, PRINT_PROGRESS);
		schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_LOG);
		schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_PER_VM_LOG);
		schedule(getId(), SimSettings.getInstance().getSimulationTime(), STOP_SIMULATION);
		
		SimLogger.printLine("Done.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void processEvent(SimEvent ev) {
		synchronized(this){
			switch (ev.getTag()) {
			case ORQUESTRA:
				try {
					List<TaskProperty> tasks = (List) ev.getData();

					Decision decision = mobileDeviceManager.submitTasks(tasks);

					for(int i=0; i< tasks.size(); i++){
						List<Object> params = Arrays.asList((TaskProperty)tasks.get(i), (Vm)decision.getVms().get(i), (Integer)decision.getTypes().get(i));
						schedule(getId(), 0, CREATE_TASK, params);
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
				break;
			case CREATE_TASK:
				if(SimSettings.getInstance().getOrchestratorPolicies()[0].equals("LOTOS")){
					try {
						List<?> data = (List<?>) ev.getData();
						TaskProperty edgeTask = (TaskProperty) data.get(0);
						Vm vm = (Vm) data.get(1);
						Integer tipo = (Integer) data.get(2);

						mobileDeviceManager.submitTask(edgeTask, vm, tipo);						
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(0);
					}
				}else{
					try {
						TaskProperty edgeTask = (TaskProperty) ev.getData();
						mobileDeviceManager.submitTask(edgeTask);						
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(1);
					}
				}
				break;
			case CHECK_ALL_VM:
				int totalNumOfVm = SimSettings.getInstance().getNumOfEdgeVMs();
				if(EdgeVmAllocationPolicy_Custom.getCreatedVmNum() != totalNumOfVm){
					SimLogger.printLine("All VMs cannot be created! Terminating simulation...");
					System.exit(0);
				}
				break;
			case GET_LOAD_LOG:
				SimLogger.getInstance().addVmUtilizationLog(
						CloudSim.clock(),
						edgeServerManager.getAvgUtilization(),
						cloudServerManager.getAvgUtilization(),
						mobileServerManager.getAvgUtilization());


				SimLogger.getInstance().addVmUtilizationLogRam(
					CloudSim.clock(),
					edgeServerManager.getAvgUtilizationOfRam(),
					cloudServerManager.getAvgUtilizationOfRam(),
					mobileServerManager.getAvgUtilizationOfRam());
				
				schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_LOG);
				break;
			case GET_LOAD_PER_VM_LOG:
				SimLogger.getInstance().addUtilizationPerVmLog(
						CloudSim.clock(),
						edgeServerManager.getUtilizationPerVm(),
						cloudServerManager.getUtilizationPerVm());


				SimLogger.getInstance().addUtilizationPerVmLogRam(
					CloudSim.clock(),
					edgeServerManager.getUtilizationPerVmOfRam(),
					cloudServerManager.getUtilizationPerVmOfRam());
				
				schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_PER_VM_LOG);
				break;
			case PRINT_PROGRESS:
				int progress = (int)((CloudSim.clock()*100)/SimSettings.getInstance().getSimulationTime());
				if(progress % 10 == 0)
					SimLogger.print(Integer.toString(progress));
				else
					SimLogger.print(".");
				if(CloudSim.clock() < SimSettings.getInstance().getSimulationTime())
					schedule(getId(), SimSettings.getInstance().getSimulationTime()/100, PRINT_PROGRESS);

				break;
			case STOP_SIMULATION:
				SimLogger.printLine("100");
				CloudSim.terminateSimulation();
				try {
					SimLogger.getInstance().simStopped();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(0);
				}
				break;
			default:
				Log.printLine(getName() + ": unknown event type");
				break;
			}
		}
	}

	@Override
	public void shutdownEntity() {
		edgeServerManager.terminateDatacenters();
		cloudServerManager.terminateDatacenters();
		mobileServerManager.terminateDatacenters();
	}
}
