/*
 * Title:        EdgeCloudSim - Simulation Logger
 * 
 * Description: 
 * SimLogger is responsible for storing simulation events/results
 * in to the files in a specific format.
 * Format is decided in a way to use results in matlab efficiently.
 * If you need more results or another file format, you should modify
 * this class.
 * 
 * IMPORTANT NOTES:
 * EdgeCloudSim is designed to perform file logging operations with
 * a low memory consumption. Deep file logging is performed whenever
 * a task is completed. This may cause too many file IO operation and
 * increase the time consumption!
 * 
 * The basic results are kept in the memory, and saved to the files
 * at the end of the simulation. So, basic file logging does
 * bring too much overhead to the time complexity. 
 * 
 * In the earlier versions (v3 and older), EdgeCloudSim keeps all the 
 * task results in the memory and save them to the files when the
 * simulation ends. Since this approach increases memory consumption
 * too much, we sacrificed the time complexity.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.cloudbus.cloudsim.core.CloudSim;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.SimSettings.NETWORK_DELAY_TYPES;
import edu.boun.edgecloudsim.utils.SimLogger.NETWORK_ERRORS;

public class SimLogger {
	public static enum TASK_STATUS {
		CREATED, UPLOADING, PROCESSING, DOWNLOADING, COMLETED,
		REJECTED_DUE_TO_VM_CAPACITY, REJECTED_DUE_TO_BANDWIDTH,
		UNFINISHED_DUE_TO_BANDWIDTH, UNFINISHED_DUE_TO_MOBILITY,
		REJECTED_DUE_TO_WLAN_COVERAGE,
		ERROR_DUE_TO_RAM_CAPACITY,
		ERROR_DUE_TO_DELAY_LIMIT,
		REJECTED_BY_SOLVER
	}
	
	public static enum NETWORK_ERRORS {
		LAN_ERROR, MAN_ERROR, WAN_ERROR, GSM_ERROR, NONE
	}

	private long startTime;
	private long endTime;
	private static boolean fileLogEnabled;
	private static boolean printLogEnabled;
	private String filePrefix;
	private String outputFolder;
	private Map<Integer, LogItem> taskMap;
	private Map<Integer, LogItem> taskMapPersisted;
	private LinkedList<VmLoadLogItem> vmLoadList;
	private LinkedList<ApDelayLogItem> apDelayList;
	private LinkedList<LoadPerVmLogItem> LoadPerVmList;
	private LinkedList<VmLoadLogItem> vmLoadListRam;
	private LinkedList<LoadPerVmLogItem> LoadPerVmListRam;

	private Map<Integer, VMItem> vmMap;

	private static SimLogger singleton = new SimLogger();
	
	private int numOfAppTypes;

	private LinkedList<LinkUsageLogItem> LinkUsage;
	
	private File successFile = null, failFile = null;
	private FileWriter successFW = null, failFW = null;
	private BufferedWriter successBW = null, failBW = null;

	// extract following values for each app type.
	// last index is average of all app types
	private int[] uncompletedTask = null;
	private int[] uncompletedTaskOnCloud = null;
	private int[] uncompletedTaskOnEdge = null;
	private int[] uncompletedTaskOnMobile = null;

	private int[] completedTask = null;
	private int[] completedTaskOnCloud = null;
	private int[] completedTaskOnEdge = null;
	private int[] completedTaskOnMobile = null;

	private int[] failedTask = null;
	private int[] failedTaskOnCloud = null;
	private int[] failedTaskOnEdge = null;
	private int[] failedTaskOnMobile = null;

	private double[] networkDelay = null;
	private double[] gsmDelay = null;
	private double[] wanDelay = null;
	private double[] manDelay = null;
	private double[] lanDelay = null;
	
	private double[] gsmUsage = null;
	private double[] wanUsage = null;
	private double[] manUsage = null;
	private double[] lanUsage = null;

	private double[] serviceTime = null;
	private double[] serviceTimeOnCloud = null;
	private double[] serviceTimeOnEdge = null;
	private double[] serviceTimeOnMobile = null;

	private double[] processingTime = null;
	private double[] processingTimeOnCloud = null;
	private double[] processingTimeOnEdge = null;
	private double[] processingTimeOnMobile = null;

	private int[] failedTaskDueToVmCapacity = null;
	private int[] failedTaskDueToVmCapacityOnCloud = null;
	private int[] failedTaskDueToVmCapacityOnEdge = null;
	private int[] failedTaskDueToVmCapacityOnMobile = null;

	private int[] failTaskDueToRamCapacity = null;
	private int[] failTaskDueToDelayLimit = null;
	private int[] rejectedTasksBySolver = null;
	
	private double[] cost = null;
	private double[] QoE = null;
	private int[] failedTaskDuetoBw = null;
	private int[] failedTaskDuetoLanBw = null;
	private int[] failedTaskDuetoManBw = null;
	private int[] failedTaskDuetoWanBw = null;
	private int[] failedTaskDuetoGsmBw = null;
	private int[] failedTaskDuetoMobility = null;
	private int[] refectedTaskDuetoWlanRange = null;
	
	private double[] orchestratorOverhead = null;

	/*
	 * A private Constructor prevents any other class from instantiating.
	 */
	private SimLogger() {
		fileLogEnabled = false;
		printLogEnabled = false;
	}

	/* Static 'instance' method */
	public static SimLogger getInstance() {
		return singleton;
	}

	public static void enableFileLog() {
		fileLogEnabled = true;
	}

	public static void enablePrintLog() {
		printLogEnabled = true;
	}

	public static boolean isFileLogEnabled() {
		return fileLogEnabled;
	}

	public static void disableFileLog() {
		fileLogEnabled = false;
	}
	
	public static void disablePrintLog() {
		printLogEnabled = false;
	}
	
	public String getOutputFolder() {
		return outputFolder;
	}

	private void appendToFile(BufferedWriter bw, String line) throws IOException {
		bw.write(line);
		bw.newLine();
	}

	public static void printLine(String msg) {
		if (printLogEnabled)
			System.out.println(msg);
	}

	public static void print(String msg) {
		if (printLogEnabled)
			System.out.print(msg);
	}

	public void simStarted(String outFolder, String fileName) {
		startTime = System.currentTimeMillis();
		filePrefix = fileName;
		outputFolder = outFolder;
		taskMap = new HashMap<Integer, LogItem>();
		taskMapPersisted = new HashMap<Integer, LogItem>();
		vmLoadList = new LinkedList<VmLoadLogItem>();
		apDelayList = new LinkedList<ApDelayLogItem>();
		LoadPerVmList = new LinkedList<LoadPerVmLogItem>();
		vmLoadListRam = new LinkedList<VmLoadLogItem>();
		LoadPerVmListRam = new LinkedList<LoadPerVmLogItem>();
		LinkUsage = new LinkedList<LinkUsageLogItem>();
		vmMap = new HashMap<Integer, VMItem>();

		numOfAppTypes = SimSettings.getInstance().getTaskLookUpTable().length;
		
		if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
			try {
				successFile = new File(outputFolder, filePrefix + "_SUCCESS.log");
				successFW = new FileWriter(successFile, true);
				successBW = new BufferedWriter(successFW);

				failFile = new File(outputFolder, filePrefix + "_FAIL.log");
				failFW = new FileWriter(failFile, true);
				failBW = new BufferedWriter(failFW);
				
				appendToFile(successBW, "#auto generated file!");
				appendToFile(failBW, "#auto generated file!");
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		// extract following values for each app type.
		// last index is average of all app types
		uncompletedTask = new int[numOfAppTypes + 1];
		uncompletedTaskOnCloud = new int[numOfAppTypes + 1];
		uncompletedTaskOnEdge = new int[numOfAppTypes + 1];
		uncompletedTaskOnMobile = new int[numOfAppTypes + 1];

		completedTask = new int[numOfAppTypes + 1];
		completedTaskOnCloud = new int[numOfAppTypes + 1];
		completedTaskOnEdge = new int[numOfAppTypes + 1];
		completedTaskOnMobile = new int[numOfAppTypes + 1];

		failedTask = new int[numOfAppTypes + 1];
		failedTaskOnCloud = new int[numOfAppTypes + 1];
		failedTaskOnEdge = new int[numOfAppTypes + 1];
		failedTaskOnMobile = new int[numOfAppTypes + 1];

		networkDelay = new double[numOfAppTypes + 1];
		gsmDelay = new double[numOfAppTypes + 1];
		wanDelay = new double[numOfAppTypes + 1];
		manDelay = new double[numOfAppTypes + 1];
		lanDelay = new double[numOfAppTypes + 1];
		
		gsmUsage = new double[numOfAppTypes + 1];
		wanUsage = new double[numOfAppTypes + 1];
		manUsage = new double[numOfAppTypes + 1];
		lanUsage = new double[numOfAppTypes + 1];

		serviceTime = new double[numOfAppTypes + 1];
		serviceTimeOnCloud = new double[numOfAppTypes + 1];
		serviceTimeOnEdge = new double[numOfAppTypes + 1];
		serviceTimeOnMobile = new double[numOfAppTypes + 1];

		processingTime = new double[numOfAppTypes + 1];
		processingTimeOnCloud = new double[numOfAppTypes + 1];
		processingTimeOnEdge = new double[numOfAppTypes + 1];
		processingTimeOnMobile = new double[numOfAppTypes + 1];

		failedTaskDueToVmCapacity = new int[numOfAppTypes + 1];
		failedTaskDueToVmCapacityOnCloud = new int[numOfAppTypes + 1];
		failedTaskDueToVmCapacityOnEdge = new int[numOfAppTypes + 1];
		failedTaskDueToVmCapacityOnMobile = new int[numOfAppTypes + 1];

		failTaskDueToRamCapacity = new int[numOfAppTypes + 1];
		failTaskDueToDelayLimit = new int[numOfAppTypes + 1];
		rejectedTasksBySolver = new int[numOfAppTypes + 1];
		
		cost = new double[numOfAppTypes + 1];
		QoE = new double[numOfAppTypes + 1];
		failedTaskDuetoBw = new int[numOfAppTypes + 1];
		failedTaskDuetoLanBw = new int[numOfAppTypes + 1];
		failedTaskDuetoManBw = new int[numOfAppTypes + 1];
		failedTaskDuetoWanBw = new int[numOfAppTypes + 1];
		failedTaskDuetoGsmBw = new int[numOfAppTypes + 1];
		failedTaskDuetoMobility = new int[numOfAppTypes + 1];
		refectedTaskDuetoWlanRange = new int[numOfAppTypes + 1];

		orchestratorOverhead = new double[numOfAppTypes + 1];
	}

	public void addLog(int deviceId, int taskId, int taskType,
			int taskLenght, int taskInputType, int taskOutputSize, double time) {
		taskMap.put(taskId, new LogItem(deviceId, taskType, taskLenght, taskInputType, taskOutputSize, time));
		taskMapPersisted.put(taskId, new LogItem(deviceId, taskType, taskLenght, taskInputType, taskOutputSize, time));
	}

	public void taskStarted(int taskId, double time) {
		taskMap.get(taskId).taskStarted(time);
		taskMapPersisted.get(taskId).taskStarted(time);
	}

	public void setUploadDelay(int taskId, double delay, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).setUploadDelay(delay, delayType);
		taskMapPersisted.get(taskId).setUploadDelay(delay, delayType);
	}

	public void setDownloadDelay(int taskId, double delay, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).setDownloadDelay(delay, delayType);
		taskMapPersisted.get(taskId).setDownloadDelay(delay, delayType);
	}
	
	public void setApUsage(int taskId, double usage, NETWORK_DELAY_TYPES usageType, int type) {
		if (type == 1){
			taskMap.get(taskId).setApUsageUpload(usage, usageType);
			taskMapPersisted.get(taskId).setApUsageUpload(usage, usageType);
		}else if(type == 2){
			taskMap.get(taskId).setApUsageDownload(usage, usageType);
			taskMapPersisted.get(taskId).setApUsageDownload(usage, usageType);
		}
	}


	public void timeUpOfVm(int vmId, double time, int vmType, String type) {
		if (!vmMap.containsKey(vmId)) {
			vmMap.put(vmId, new VMItem(vmId, vmType));
		}
		if (type == "start"){
			vmMap.get(vmId).addusertovm(time);
		}else if(type == "stop"){
			vmMap.get(vmId).removeuserfromvm(time);
		}
	}

	public int getVMMapTaskNumber(int vmId){

		if (!vmMap.containsKey(vmId)) {
			return 0;
		}else{
			return vmMap.get(vmId).getNumberOfTasks();
		}
	}

	public void taskAssigned(int taskId, int datacenterId, int hostId, int vmId, int vmType, double time) {
		taskMap.get(taskId).taskAssigned(datacenterId, hostId, vmId, vmType, time);
		taskMapPersisted.get(taskId).taskAssigned(datacenterId, hostId, vmId, vmType, time);
	}

	public void taskExecuted(int taskId, double time) {
		taskMap.get(taskId).taskExecuted(time);
		taskMapPersisted.get(taskId).taskExecuted(time);
	}

	public void taskEnded(int taskId, double time) {
		taskMap.get(taskId).taskEnded(time);
		taskMapPersisted.get(taskId).taskEnded(time);

		taskMap.get(taskId).taskErrorDueToDelayLimit();
		taskMapPersisted.get(taskId).taskErrorDueToDelayLimit();
		
		recordLog(taskId);
	}

	public void rejectedDueToVMCapacity(int taskId, double time, int vmType) {
		taskMap.get(taskId).taskRejectedDueToVMCapacity(time, vmType);
		taskMapPersisted.get(taskId).taskRejectedDueToVMCapacity(time, vmType);
		recordLog(taskId);
	}

	public void errorDueToRamCapacity(int taskId) {
		taskMap.get(taskId).taskErrorDueToRamCapacity();
		taskMapPersisted.get(taskId).taskErrorDueToRamCapacity();
		recordLog(taskId);
	}


    public void rejectedDueToWlanCoverage(int taskId, double time, int vmType) {
    	taskMap.get(taskId).taskRejectedDueToWlanCoverage(time, vmType);
		taskMapPersisted.get(taskId).taskRejectedDueToWlanCoverage(time, vmType);
		recordLog(taskId);
    }
    
	public void rejectedDueToBandwidth(int taskId, double time, int vmType, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).taskRejectedDueToBandwidth(time, vmType, delayType);
		taskMapPersisted.get(taskId).taskRejectedDueToBandwidth(time, vmType, delayType);
		recordLog(taskId);
	}

	public void rejectedBySolver(int taskId) {
		taskMap.get(taskId).taskRejectedBySolver();
		taskMapPersisted.get(taskId).taskRejectedBySolver();
		recordLog(taskId);
	}

	public void failedDueToBandwidth(int taskId, double time, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).taskFailedDueToBandwidth(time, delayType);
		taskMapPersisted.get(taskId).taskFailedDueToBandwidth(time, delayType);
		recordLog(taskId);
	}

	public void failedDueToMobility(int taskId, double time) {
		taskMap.get(taskId).taskFailedDueToMobility(time);
		taskMapPersisted.get(taskId).taskFailedDueToMobility(time);
		recordLog(taskId);
	}

	public void setQoE(int taskId, double QoE){
		taskMap.get(taskId).setQoE(QoE);
		taskMapPersisted.get(taskId).setQoE(QoE);
	}
	
	public void setOrchestratorOverhead(int taskId, double overhead){
		taskMap.get(taskId).setOrchestratorOverhead(overhead);
		taskMapPersisted.get(taskId).setOrchestratorOverhead(overhead);
	}

	public void addVmUtilizationLog(double time, double loadOnEdge, double loadOnCloud, double loadOnMobile) {
		if(SimSettings.getInstance().getLocationLogInterval() != 0)
			vmLoadList.add(new VmLoadLogItem(time, loadOnEdge, loadOnCloud, loadOnMobile));
	}

	public void addUtilizationPerVmLog(double time, String loadOnEdge, String loadOnCloud) {
		if(SimSettings.getInstance().getLocationLogInterval() != 0)
			LoadPerVmList.add(new LoadPerVmLogItem(time, loadOnEdge, loadOnCloud));
	}


	public void addVmUtilizationLogRam(double time, double loadOnEdge, double loadOnCloud, double loadOnMobile) {
		if(SimSettings.getInstance().getLocationLogInterval() != 0)
			vmLoadListRam.add(new VmLoadLogItem(time, loadOnEdge, loadOnCloud, loadOnMobile));
	}

	public void addUtilizationPerVmLogRam(double time, String loadOnEdge, String loadOnCloud) {
		if(SimSettings.getInstance().getLocationLogInterval() != 0)
			LoadPerVmListRam.add(new LoadPerVmLogItem(time, loadOnEdge, loadOnCloud));
	}

	public void addApDelayLog(double time, double[] apUploadDelays, double[] apDownloadDelays) {
		if(SimSettings.getInstance().getApDelayLogInterval() != 0)
			apDelayList.add(new ApDelayLogItem(time, apUploadDelays, apDownloadDelays));
	}
	
	public void simStopped() throws IOException {
		endTime = System.currentTimeMillis();
		File vmLoadFile = null, locationFile = null, apUploadDelayFile = null, apDownloadDelayFile = null, loadPerEdgeVmFile = null, loadPerCloudVmFile = null, vmLoadFileRam = null, loadPerEdgeVmFileRam = null, loadPerCloudVmFileRam = null, linkFile = null, tasksFile = null;
		FileWriter vmLoadFW = null, locationFW = null, apUploadDelayFW = null, apDownloadDelayFW = null, loadPerEdgeVmFW = null, loadPerCloudVmFW = null, vmLoadFWRam = null, loadPerEdgeVmFWRam = null, loadPerCloudVmFWRam = null, linkFW = null, tasksFW = null;
		BufferedWriter vmLoadBW = null, locationBW = null, apUploadDelayBW = null, apDownloadDelayBW = null, loadPerEdgeVmBW = null, loadPerCloudVmBW = null, vmLoadBWRam = null, loadPerEdgeVmBWRam = null, loadPerCloudVmBWRam = null, linkBW = null, tasksBW = null;

		// Save generic results to file for each app type. last index is average
		// of all app types
		File[] genericFiles = new File[numOfAppTypes + 1];
		FileWriter[] genericFWs = new FileWriter[numOfAppTypes + 1];
		BufferedWriter[] genericBWs = new BufferedWriter[numOfAppTypes + 1];

		// open all files and prepare them for write
		if (fileLogEnabled) {
			vmLoadFile = new File(outputFolder, filePrefix + "_VM_LOAD.csv");
			vmLoadFW = new FileWriter(vmLoadFile, true);
			vmLoadBW = new BufferedWriter(vmLoadFW);

			locationFile = new File(outputFolder, filePrefix + "_LOCATION.csv");
			locationFW = new FileWriter(locationFile, true);
			locationBW = new BufferedWriter(locationFW);

			loadPerEdgeVmFile = new File(outputFolder, filePrefix + "_LOAD_PER_EDGE_VM.csv");
			loadPerEdgeVmFW = new FileWriter(loadPerEdgeVmFile, true);
			loadPerEdgeVmBW = new BufferedWriter(loadPerEdgeVmFW);

			loadPerCloudVmFile = new File(outputFolder, filePrefix + "_LOAD_PER_CLOUD_VM.csv");
			loadPerCloudVmFW = new FileWriter(loadPerCloudVmFile, true);
			loadPerCloudVmBW = new BufferedWriter(loadPerCloudVmFW);

			vmLoadFileRam = new File(outputFolder, filePrefix + "_VM_RAM.csv");
			vmLoadFWRam = new FileWriter(vmLoadFileRam, true);
			vmLoadBWRam = new BufferedWriter(vmLoadFWRam);

			loadPerEdgeVmFileRam = new File(outputFolder, filePrefix + "_RAM_PER_EDGE_VM.csv");
			loadPerEdgeVmFWRam = new FileWriter(loadPerEdgeVmFileRam, true);
			loadPerEdgeVmBWRam = new BufferedWriter(loadPerEdgeVmFWRam);

			loadPerCloudVmFileRam = new File(outputFolder, filePrefix + "_RAM_PER_CLOUD_VM.csv");
			loadPerCloudVmFWRam = new FileWriter(loadPerCloudVmFileRam, true);
			loadPerCloudVmBWRam = new BufferedWriter(loadPerCloudVmFWRam);

			linkFile = new File(outputFolder, filePrefix + "_LINK_USAGE.csv");
			linkFW = new FileWriter(linkFile, true);
			linkBW = new BufferedWriter(linkFW);

			tasksFile = new File(outputFolder, filePrefix + "_TASKS.csv");
			tasksFW = new FileWriter(tasksFile, true);
			tasksBW = new BufferedWriter(tasksFW);

			for (int i = 0; i < numOfAppTypes + 1; i++) {
				String fileName = "ALL_APPS_GENERIC.log";

				if (i < numOfAppTypes) {
					// if related app is not used in this simulation, just discard it
					if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
						continue;

					fileName = SimSettings.getInstance().getTaskName(i) + "_GENERIC.log";
				}

				genericFiles[i] = new File(outputFolder, filePrefix + "_" + fileName);
				genericFWs[i] = new FileWriter(genericFiles[i], true);
				genericBWs[i] = new BufferedWriter(genericFWs[i]);
			}

			String csv_header = "time" + SimSettings.DELIMITER;
			for (int i = 0; i < SimSettings.getInstance().getNumOfEdgeDatacenters(); i++){
				csv_header = csv_header + "ap" + i;
				if (i < SimSettings.getInstance().getNumOfEdgeDatacenters() - 1)
					csv_header = csv_header + SimSettings.DELIMITER;
			}

			appendToFile(vmLoadBW, "time;load_on_edge;load_on_cloud;load_on_mobile");
			appendToFile(locationBW, csv_header);
			appendToFile(vmLoadBWRam, "time;load_on_edge;load_on_cloud;load_on_mobile");
			appendToFile(linkBW, "time;wlan;wan;man");
			appendToFile(tasksBW, "taskId;deviceId;datacenterId;hostId;vmId;vmType;taskType;taskLenght;taskInputSize;taskOutputSize;taskCreationTime;taskStartTime;taskFinishedUploadTime;taskStartedDownloadTime;taskEndTime;status;netDelay;wlanDelay;manDelay;wanDelay;gsmDelay");
		}

		//the tasks in the map is not completed yet!
		for (Map.Entry<Integer, LogItem> entry : taskMap.entrySet()) {
			LogItem value = entry.getValue();
			if (!value.isInWarmUpPeriod()){
				uncompletedTask[value.getTaskType()]++;
				if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
					uncompletedTaskOnCloud[value.getTaskType()]++;
				else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
					uncompletedTaskOnMobile[value.getTaskType()]++;
				else
					uncompletedTaskOnEdge[value.getTaskType()]++;
			}
		}

		// calculate total values
		uncompletedTask[numOfAppTypes] = IntStream.of(uncompletedTask).sum();
		uncompletedTaskOnCloud[numOfAppTypes] = IntStream.of(uncompletedTaskOnCloud).sum();
		uncompletedTaskOnEdge[numOfAppTypes] = IntStream.of(uncompletedTaskOnEdge).sum();
		uncompletedTaskOnMobile[numOfAppTypes] = IntStream.of(uncompletedTaskOnMobile).sum();

		completedTask[numOfAppTypes] = IntStream.of(completedTask).sum();
		completedTaskOnCloud[numOfAppTypes] = IntStream.of(completedTaskOnCloud).sum();
		completedTaskOnEdge[numOfAppTypes] = IntStream.of(completedTaskOnEdge).sum();
		completedTaskOnMobile[numOfAppTypes] = IntStream.of(completedTaskOnMobile).sum();

		failedTask[numOfAppTypes] = IntStream.of(failedTask).sum();
		failedTaskOnCloud[numOfAppTypes] = IntStream.of(failedTaskOnCloud).sum();
		failedTaskOnEdge[numOfAppTypes] = IntStream.of(failedTaskOnEdge).sum();
		failedTaskOnMobile[numOfAppTypes] = IntStream.of(failedTaskOnMobile).sum();

		networkDelay[numOfAppTypes] = DoubleStream.of(networkDelay).sum();
		lanDelay[numOfAppTypes] = DoubleStream.of(lanDelay).sum();
		manDelay[numOfAppTypes] = DoubleStream.of(manDelay).sum();
		wanDelay[numOfAppTypes] = DoubleStream.of(wanDelay).sum();
		gsmDelay[numOfAppTypes] = DoubleStream.of(gsmDelay).sum();
		
		lanUsage[numOfAppTypes] = DoubleStream.of(lanUsage).sum();
		manUsage[numOfAppTypes] = DoubleStream.of(manUsage).sum();
		wanUsage[numOfAppTypes] = DoubleStream.of(wanUsage).sum();
		gsmUsage[numOfAppTypes] = DoubleStream.of(gsmUsage).sum();

		serviceTime[numOfAppTypes] = DoubleStream.of(serviceTime).sum();
		serviceTimeOnCloud[numOfAppTypes] = DoubleStream.of(serviceTimeOnCloud).sum();
		serviceTimeOnEdge[numOfAppTypes] = DoubleStream.of(serviceTimeOnEdge).sum();
		serviceTimeOnMobile[numOfAppTypes] = DoubleStream.of(serviceTimeOnMobile).sum();

		processingTime[numOfAppTypes] = DoubleStream.of(processingTime).sum();
		processingTimeOnCloud[numOfAppTypes] = DoubleStream.of(processingTimeOnCloud).sum();
		processingTimeOnEdge[numOfAppTypes] = DoubleStream.of(processingTimeOnEdge).sum();
		processingTimeOnMobile[numOfAppTypes] = DoubleStream.of(processingTimeOnMobile).sum();

		failedTaskDueToVmCapacity[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacity).sum();
		failedTaskDueToVmCapacityOnCloud[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacityOnCloud).sum();
		failedTaskDueToVmCapacityOnEdge[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacityOnEdge).sum();
		failedTaskDueToVmCapacityOnMobile[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacityOnMobile).sum();
		
		failTaskDueToRamCapacity[numOfAppTypes] = IntStream.of(failTaskDueToRamCapacity).sum();
		failTaskDueToDelayLimit[numOfAppTypes] = IntStream.of(failTaskDueToDelayLimit).sum();
		rejectedTasksBySolver[numOfAppTypes] = IntStream.of(rejectedTasksBySolver).sum();

		cost[numOfAppTypes] = DoubleStream.of(cost).sum();
		QoE[numOfAppTypes] = DoubleStream.of(QoE).sum();
		failedTaskDuetoBw[numOfAppTypes] = IntStream.of(failedTaskDuetoBw).sum();
		failedTaskDuetoGsmBw[numOfAppTypes] = IntStream.of(failedTaskDuetoGsmBw).sum();
		failedTaskDuetoWanBw[numOfAppTypes] = IntStream.of(failedTaskDuetoWanBw).sum();
		failedTaskDuetoManBw[numOfAppTypes] = IntStream.of(failedTaskDuetoManBw).sum();
		failedTaskDuetoLanBw[numOfAppTypes] = IntStream.of(failedTaskDuetoLanBw).sum();
		failedTaskDuetoMobility[numOfAppTypes] = IntStream.of(failedTaskDuetoMobility).sum();
		refectedTaskDuetoWlanRange[numOfAppTypes] = IntStream.of(refectedTaskDuetoWlanRange).sum();

		orchestratorOverhead[numOfAppTypes] = DoubleStream.of(orchestratorOverhead).sum();
		
		// calculate server load
		double totalVmLoadOnEdge = 0;
		double totalVmLoadOnCloud = 0;
		double totalVmLoadOnMobile = 0;
		for (VmLoadLogItem entry : vmLoadList) {
			totalVmLoadOnEdge += entry.getEdgeLoad();
			totalVmLoadOnCloud += entry.getCloudLoad();
			totalVmLoadOnMobile += entry.getMobileLoad();
			if (fileLogEnabled && SimSettings.getInstance().getVmLoadLogInterval() != 0)
				appendToFile(vmLoadBW, entry.toString());
		}

		for (VmLoadLogItem entry : vmLoadListRam) {
			if (fileLogEnabled && SimSettings.getInstance().getVmLoadLogInterval() != 0)
				appendToFile(vmLoadBWRam, entry.toString());
		}

		for (LoadPerVmLogItem entry : LoadPerVmList){
			if (fileLogEnabled && SimSettings.getInstance().getVmLoadLogInterval() != 0)
				appendToFile(loadPerEdgeVmBW, entry.getTime() + SimSettings.DELIMITER + entry.getEdgeLoad());
				appendToFile(loadPerCloudVmBW, entry.getTime() + SimSettings.DELIMITER + entry.getCloudLoad());
		}

		for (LoadPerVmLogItem entry : LoadPerVmListRam){
			if (fileLogEnabled && SimSettings.getInstance().getVmLoadLogInterval() != 0)
				appendToFile(loadPerEdgeVmBWRam, entry.getTime() + SimSettings.DELIMITER + entry.getEdgeLoad());
				appendToFile(loadPerCloudVmBWRam, entry.getTime() + SimSettings.DELIMITER + entry.getCloudLoad());
		}

		for (Map.Entry<Integer, LogItem> entry : taskMapPersisted.entrySet()){

			appendToFile(tasksBW, entry.getValue().toString(entry.getKey()));
		}

		double locationLogInterval = SimSettings.getInstance().getLocationLogInterval();
		for (int t = 1; t < (SimSettings.getInstance().getSimulationTime() / locationLogInterval); t++){
			double wlanusage = 0.0, wanusage = 0.0, manusage = 0.0;
			Double time = t * SimSettings.getInstance().getLocationLogInterval();
			for (Map.Entry<Integer, LogItem> entry : taskMapPersisted.entrySet()){
				Double start = entry.getValue().gettaskStartTime();
				Double finishedUpload = entry.getValue().gettaskFinishedUploadTime();
				Double startedDownload = entry.getValue().gettaskStartedDownloadTime();
				Double finished = entry.getValue().gettaskEndTime();
				SimLogger.TASK_STATUS status = entry.getValue().getStatus();

				if (status == SimLogger.TASK_STATUS.COMLETED){
					if(start >=time && start <= time + SimSettings.getInstance().getVmLoadLogInterval()){
						wlanusage += entry.getValue().getwlanUsageUpload();
						wanusage += entry.getValue().getwanUsageUpload();
						manusage += entry.getValue().getmanUsageUpload();
					}
					else if(startedDownload >=time && startedDownload <= time + SimSettings.getInstance().getVmLoadLogInterval()){
						wlanusage += entry.getValue().getwlanUsageDownload();
						wanusage += entry.getValue().getwanUsageDownload();
						manusage += entry.getValue().getmanUsageDownload();
					}
				}
			}
			LinkUsage.add(new LinkUsageLogItem(time, wlanusage/1000, wanusage/1000, manusage/1000));
		}


		for (LinkUsageLogItem entry : LinkUsage){
			appendToFile(linkBW, entry.toString());
		}

		double costOfVmUtilization = 0;
		for (Map.Entry<Integer, VMItem> entry : vmMap.entrySet()){

			VMItem value = entry.getValue();

			value.stopvm(CloudSim.clock());

			costOfVmUtilization = costOfVmUtilization + (value.getinitialcost() + (value.getcostsec()*value.getTime()));

		}

		System.out.println("\nTotal cost:" + costOfVmUtilization + "\n");

		if (fileLogEnabled) {
			// write location info to file for each location
			// assuming each location has only one access point
			locationLogInterval = SimSettings.getInstance().getLocationLogInterval();
			if(locationLogInterval != 0) {
				for (int t = 1; t < (SimSettings.getInstance().getSimulationTime() / locationLogInterval); t++) {
					int[] locationInfo = new int[SimSettings.getInstance().getNumOfEdgeDatacenters()];
					Double time = t * SimSettings.getInstance().getLocationLogInterval();
					
					if (time < SimSettings.CLIENT_ACTIVITY_START_TIME)
						continue;

					for (int i = 0; i < SimManager.getInstance().getNumOfMobileDevice(); i++) {
						Location loc = SimManager.getInstance().getMobilityModel().getLocation(i, time);
						locationInfo[loc.getServingWlanId()]++;
					}

					locationBW.write(time.toString());
					for (int i = 0; i < locationInfo.length; i++)
						locationBW.write(SimSettings.DELIMITER + locationInfo[i]);

					locationBW.newLine();
				}
			}
			
			for (int i = 0; i < numOfAppTypes + 1; i++) {

				if (i < numOfAppTypes) {
					// if related app is not used in this simulation, just discard it
					if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
						continue;
				}

				// check if the divisor is zero in order to avoid division by
				// zero problem
				double _serviceTime = (completedTask[i] == 0) ? 0.0 : (serviceTime[i] / (double) completedTask[i]);
				double _networkDelay = (completedTask[i] == 0) ? 0.0 : (networkDelay[i] / ((double) completedTask[i] - (double)completedTaskOnMobile[i]));
				double _processingTime = (completedTask[i] == 0) ? 0.0 : (processingTime[i] / (double) completedTask[i]);
				double _vmLoadOnEdge = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnEdge / (double) vmLoadList.size());
				double _vmLoadOnClould = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnCloud / (double) vmLoadList.size());
				double _vmLoadOnMobile = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnMobile / (double) vmLoadList.size());
				double _cost = (completedTask[i] == 0) ? 0.0 : (cost[i] / (double) completedTask[i]);
				double _QoE1 = (completedTask[i] == 0) ? 0.0 : (QoE[i] / (double) completedTask[i]);
				double _QoE2 = (completedTask[i] == 0) ? 0.0 : (QoE[i] / (double) (failedTask[i] + completedTask[i]));

				double _lanDelay = (lanUsage[i] == 0) ? 0.0
						: (lanDelay[i] / (double) lanUsage[i]);
				double _manDelay = (manUsage[i] == 0) ? 0.0
						: (manDelay[i] / (double) manUsage[i]);
				double _wanDelay = (wanUsage[i] == 0) ? 0.0
						: (wanDelay[i] / (double) wanUsage[i]);
				double _gsmDelay = (gsmUsage[i] == 0) ? 0.0
						: (gsmDelay[i] / (double) gsmUsage[i]);
				
				// write generic results
				String genericResult1 = "total_tasks=" + Integer.toString(failedTask[i] + completedTask[i] + uncompletedTask[i]) + SimSettings.NEW_LINE
				   					  + "total_completed_tasks=" + Integer.toString(completedTask[i]) + SimSettings.NEW_LINE
									  + "total_failed_tasks_total=" + Integer.toString(failedTask[i]) + SimSettings.NEW_LINE
									  + "total_uncompleted_tasks=" + Integer.toString(uncompletedTask[i]) + SimSettings.NEW_LINE
									  + "failed_tasks_due_to_bw=" + Integer.toString(failedTaskDuetoBw[i]) + SimSettings.NEW_LINE
									  + "total_service_time=" + Double.toString(_serviceTime) + SimSettings.NEW_LINE
									  + "total_processing_time=" + Double.toString(_processingTime) + SimSettings.NEW_LINE
									  + "network_delay=" + Double.toString(_networkDelay) + SimSettings.NEW_LINE
									  + "total_failed_tasks_due_to_vm_capacity=" + Integer.toString(failedTaskDueToVmCapacity[i]) + SimSettings.NEW_LINE
									  + "failed_tasks_due_to_mobility=" + Integer.toString(failedTaskDuetoMobility[i]) + SimSettings.NEW_LINE
									  + "rejected_tasks_due_to_wlan_range=" + Integer.toString(refectedTaskDuetoWlanRange[i]) + SimSettings.NEW_LINE
									  + "error_tasks_due_to_ram_capacity=" + Integer.toString(failTaskDueToRamCapacity[i]) + SimSettings.NEW_LINE
									  + "error_tasks_due_to_delay_limit=" + Integer.toString(failTaskDueToDelayLimit[i]) + SimSettings.NEW_LINE
									  + "rejected_tasks_by_solver=" + Integer.toString(rejectedTasksBySolver[i]) + SimSettings.NEW_LINE;

				// check if the divisor is zero in order to avoid division by zero problem
				double _serviceTimeOnEdge = (completedTaskOnEdge[i] == 0) ? 0.0
						: (serviceTimeOnEdge[i] / (double) completedTaskOnEdge[i]);
				double _processingTimeOnEdge = (completedTaskOnEdge[i] == 0) ? 0.0
						: (processingTimeOnEdge[i] / (double) completedTaskOnEdge[i]);
				String genericResult2 =  "completed_tasks_on_edge=" + Integer.toString(completedTaskOnEdge[i]) + SimSettings.NEW_LINE
									  +  "failed_tasks_on_edge=" + Integer.toString(failedTaskOnEdge[i]) + SimSettings.NEW_LINE
									  +  "uncompleted_tasks_on_edge=" + Integer.toString(uncompletedTaskOnEdge[i]) + SimSettings.NEW_LINE
									  +  "service_time_on_edge=" + Double.toString(_serviceTimeOnEdge) + SimSettings.NEW_LINE
									  +  "processing_time_on_edge=" + Double.toString(_processingTimeOnEdge) + SimSettings.NEW_LINE
									  +  "vm_load_on_edge=" + Double.toString(_vmLoadOnEdge) + SimSettings.NEW_LINE
									  +  "failed_tasks_due_to_vm_capacity_on_edge=" + Integer.toString(failedTaskDueToVmCapacityOnEdge[i]) + SimSettings.NEW_LINE;

				// check if the divisor is zero in order to avoid division by zero problem
				double _serviceTimeOnCloud = (completedTaskOnCloud[i] == 0) ? 0.0
						: (serviceTimeOnCloud[i] / (double) completedTaskOnCloud[i]);
				double _processingTimeOnCloud = (completedTaskOnCloud[i] == 0) ? 0.0
						: (processingTimeOnCloud[i] / (double) completedTaskOnCloud[i]);
				String genericResult3 =  "completed_tasks_on_cloud=" + Integer.toString(completedTaskOnCloud[i]) + SimSettings.NEW_LINE
						+  "failed_tasks_on_cloud=" + Integer.toString(failedTaskOnCloud[i]) + SimSettings.NEW_LINE
						+  "uncompleted_tasks_on_cloud=" + Integer.toString(uncompletedTaskOnCloud[i]) + SimSettings.NEW_LINE
						+  "service_time_on_cloud=" + Double.toString(_serviceTimeOnCloud) + SimSettings.NEW_LINE
						+  "processing_time_on_cloud=" + Double.toString(_processingTimeOnCloud) + SimSettings.NEW_LINE
						+  "vm_load_on_cloud=" + Double.toString(_vmLoadOnClould) + SimSettings.NEW_LINE 
						+  "failed_tasks_due_to_vm_capacity_on_cloud=" + Integer.toString(failedTaskDueToVmCapacityOnCloud[i]) + SimSettings.NEW_LINE;
				
				// check if the divisor is zero in order to avoid division by zero problem
				double _serviceTimeOnMobile = (completedTaskOnMobile[i] == 0) ? 0.0
						: (serviceTimeOnMobile[i] / (double) completedTaskOnMobile[i]);
				double _processingTimeOnMobile = (completedTaskOnMobile[i] == 0) ? 0.0
						: (processingTimeOnMobile[i] / (double) completedTaskOnMobile[i]);
				String genericResult4 =  "completed_tasks_on_mobile=" + Integer.toString(completedTaskOnMobile[i]) + SimSettings.NEW_LINE
						+  "failed_tasks_on_mobile=" + Integer.toString(failedTaskOnMobile[i]) + SimSettings.NEW_LINE
						+  "uncompleted_tasks_on_mobile=" + Integer.toString(uncompletedTaskOnMobile[i]) + SimSettings.NEW_LINE
						+  "serivice_time_on_mobile=" + Double.toString(_serviceTimeOnMobile) + SimSettings.NEW_LINE
						+  "processing_time_on_mobile=" + Double.toString(_processingTimeOnMobile) + SimSettings.NEW_LINE 
						+  "vm_load_on_mobile=" + Double.toString(_vmLoadOnMobile) + SimSettings.NEW_LINE
						+  "failed_tasks_due_to_vm_capacity_on_mobile=" + Integer.toString(failedTaskDueToVmCapacityOnMobile[i]) + SimSettings.NEW_LINE;
				
				String genericResult5 =  "lan_delay=" + Double.toString(_lanDelay) + SimSettings.NEW_LINE
						+  "man_delay=" + Double.toString(_manDelay) + SimSettings.NEW_LINE
						+  "wan_delay=" + Double.toString(_wanDelay) + SimSettings.NEW_LINE
						+  "gsm_delay=" + Double.toString(_gsmDelay) + SimSettings.NEW_LINE
						+  "failed_tasks_due_to_lan=" + Integer.toString(failedTaskDuetoLanBw[i]) + SimSettings.NEW_LINE
						+  "failed_tasks_due_to_man=" + Integer.toString(failedTaskDuetoManBw[i]) + SimSettings.NEW_LINE
						+  "failed_tasks_due_to_wan=" + Integer.toString(failedTaskDuetoWanBw[i]) + SimSettings.NEW_LINE
						+  "failed_tasks_due_to_gsm=" + Integer.toString(failedTaskDuetoGsmBw[i]) + SimSettings.NEW_LINE;
				
				//performance related values
				double _orchestratorOverhead = orchestratorOverhead[i] / (double) (failedTask[i] + completedTask[i]);
				
				String genericResult6 =  "experiment_time=" + Long.toString((endTime-startTime)/1000)  + SimSettings.NEW_LINE
						+ "cost_of_vm_utilization=" + Double.toString(costOfVmUtilization) + SimSettings.NEW_LINE;
						
				appendToFile(genericBWs[i], genericResult1);
				appendToFile(genericBWs[i], genericResult2);
				appendToFile(genericBWs[i], genericResult3);
				appendToFile(genericBWs[i], genericResult4);
				appendToFile(genericBWs[i], genericResult5);
				
				//append performance related values only to ALL_ALLPS file
				if(i == numOfAppTypes) {
					appendToFile(genericBWs[i], genericResult6);
				}
				else {
					printLine(SimSettings.getInstance().getTaskName(i));
					printLine("# of tasks (Edge/Cloud): "
							+ (failedTask[i] + completedTask[i] + uncompletedTask[i]) + "("
							+ (failedTaskOnEdge[i] + completedTaskOnEdge[i] + uncompletedTaskOnEdge[i]) + "/" 
							+ (failedTaskOnCloud[i]+ completedTaskOnCloud[i] + uncompletedTaskOnCloud[i]) + ")" );
					
					printLine("# of failed tasks (Edge/Cloud): "
							+ failedTask[i] + "("
							+ failedTaskOnEdge[i] + "/"
							+ failedTaskOnCloud[i] + ")");
					
					printLine("# of completed tasks (Edge/Cloud): "
							+ completedTask[i] + "("
							+ completedTaskOnEdge[i] + "/"
							+ completedTaskOnCloud[i] + ")");
					
					printLine("---------------------------------------");
				}
			}

			// close open files
			if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
				successBW.close();
				failBW.close();
			}
			vmLoadBW.close();
			locationBW.close();
			loadPerEdgeVmBW.close();
			loadPerCloudVmBW.close();
			vmLoadBWRam.close();
			loadPerEdgeVmBWRam.close();
			loadPerCloudVmBWRam.close();
			linkBW.close();
			tasksBW.close();
			for (int i = 0; i < numOfAppTypes + 1; i++) {
				if (i < numOfAppTypes) {
					// if related app is not used in this simulation, just
					// discard it
					if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
						continue;
				}
				genericBWs[i].close();
			}
			
		}
		// printout important results
		printLine("# of tasks (Edge/Cloud/Mobile): "
				+ (failedTask[numOfAppTypes] + completedTask[numOfAppTypes] + uncompletedTask[numOfAppTypes]) + "("
				+ (failedTaskOnEdge[numOfAppTypes] + completedTaskOnEdge[numOfAppTypes] + uncompletedTaskOnEdge[numOfAppTypes]) + "/" 
				+ (failedTaskOnCloud[numOfAppTypes]+ completedTaskOnCloud[numOfAppTypes] + uncompletedTaskOnCloud[numOfAppTypes]) + "/" 
				+ (failedTaskOnMobile[numOfAppTypes]+ completedTaskOnMobile[numOfAppTypes] + uncompletedTaskOnMobile[numOfAppTypes]) + ")");
		
		printLine("# of failed tasks (Edge/Cloud/Mobile): "
				+ failedTask[numOfAppTypes] + "("
				+ failedTaskOnEdge[numOfAppTypes] + "/"
				+ failedTaskOnCloud[numOfAppTypes] + "/"
				+ failedTaskOnMobile[numOfAppTypes] + ")");
		
		printLine("# of completed tasks (Edge/Cloud/Mobile): "
				+ completedTask[numOfAppTypes] + "("
				+ completedTaskOnEdge[numOfAppTypes] + "/"
				+ completedTaskOnCloud[numOfAppTypes] + "/"
				+ completedTaskOnMobile[numOfAppTypes] + ")");
		
		printLine("# of uncompleted tasks (Edge/Cloud/Mobile): "
				+ uncompletedTask[numOfAppTypes] + "("
				+ uncompletedTaskOnEdge[numOfAppTypes] + "/"
				+ uncompletedTaskOnCloud[numOfAppTypes] + "/"
				+ uncompletedTaskOnMobile[numOfAppTypes] + ")");

		printLine("# of failed tasks due to vm capacity (Edge/Cloud/Mobile): "
				+ failedTaskDueToVmCapacity[numOfAppTypes] + "("
				+ failedTaskDueToVmCapacityOnEdge[numOfAppTypes] + "/"
				+ failedTaskDueToVmCapacityOnCloud[numOfAppTypes] + "/"
				+ failedTaskDueToVmCapacityOnMobile[numOfAppTypes] + ")");
		
		printLine("# of failed tasks due to Mobility/WLAN Range/Network(WLAN/MAN/WAN/GSM): "
				+ failedTaskDuetoMobility[numOfAppTypes]
				+ "/" + refectedTaskDuetoWlanRange[numOfAppTypes]
				+ "/" + failedTaskDuetoBw[numOfAppTypes] 
				+ "(" + failedTaskDuetoLanBw[numOfAppTypes] 
				+ "/" + failedTaskDuetoManBw[numOfAppTypes] 
				+ "/" + failedTaskDuetoWanBw[numOfAppTypes] 
				+ "/" + failedTaskDuetoGsmBw[numOfAppTypes] + ")");
		
		printLine("percentage of completed tasks: "
				+ String.format("%.6f", ((double) completedTask[numOfAppTypes] * (double) 100)
						/ (double) (completedTask[numOfAppTypes] + failedTask[numOfAppTypes] + uncompletedTask[numOfAppTypes]))
				+ "%");

		printLine("average service time: "
				+ String.format("%.6f", serviceTime[numOfAppTypes] / (double) completedTask[numOfAppTypes])
				+ " seconds. (" + "on Edge: "
				+ String.format("%.6f", serviceTimeOnEdge[numOfAppTypes] / (double) completedTaskOnEdge[numOfAppTypes])
				+ ", " + "on Cloud: "
				+ String.format("%.6f", serviceTimeOnCloud[numOfAppTypes] / (double) completedTaskOnCloud[numOfAppTypes])
				+ ", " + "on Mobile: "
				+ String.format("%.6f", serviceTimeOnMobile[numOfAppTypes] / (double) completedTaskOnMobile[numOfAppTypes])
				+ ")");

		printLine("average processing time: "
				+ String.format("%.6f", processingTime[numOfAppTypes] / (double) completedTask[numOfAppTypes])
				+ " seconds. (" + "on Edge: "
				+ String.format("%.6f", processingTimeOnEdge[numOfAppTypes] / (double) completedTaskOnEdge[numOfAppTypes])
				+ ", " + "on Cloud: " 
				+ String.format("%.6f", processingTimeOnCloud[numOfAppTypes] / (double) completedTaskOnCloud[numOfAppTypes])
				+ ", " + "on Mobile: " 
				+ String.format("%.6f", processingTimeOnMobile[numOfAppTypes] / (double) completedTaskOnMobile[numOfAppTypes])
				+ ")");

		printLine("average network delay: "
				+ String.format("%.6f", networkDelay[numOfAppTypes] / ((double) completedTask[numOfAppTypes] - (double) completedTaskOnMobile[numOfAppTypes]))
				+ " seconds. (" + "LAN delay: "
				+ String.format("%.6f", lanDelay[numOfAppTypes] / (double) lanUsage[numOfAppTypes])
				+ ", " + "MAN delay: "
				+ String.format("%.6f", manDelay[numOfAppTypes] / (double) manUsage[numOfAppTypes])
				+ ", " + "WAN delay: "
				+ String.format("%.6f", wanDelay[numOfAppTypes] / (double) wanUsage[numOfAppTypes])
				+ ", " + "GSM delay: "
				+ String.format("%.6f", gsmDelay[numOfAppTypes] / (double) gsmUsage[numOfAppTypes]) + ")");

		printLine("average server utilization Edge/Cloud/Mobile: " 
				+ String.format("%.6f", totalVmLoadOnEdge / (double) vmLoadList.size()) + "/"
				+ String.format("%.6f", totalVmLoadOnCloud / (double) vmLoadList.size()) + "/"
				+ String.format("%.6f", totalVmLoadOnMobile / (double) vmLoadList.size()));

		printLine("average cost: " + cost[numOfAppTypes] / completedTask[numOfAppTypes] + "$");
		printLine("average overhead: " + orchestratorOverhead[numOfAppTypes] / (failedTask[numOfAppTypes] + completedTask[numOfAppTypes]) + " ns");
		printLine("average QoE (for all): " + QoE[numOfAppTypes] / (failedTask[numOfAppTypes] + completedTask[numOfAppTypes]) + "%");
		printLine("average QoE (for executed): " + QoE[numOfAppTypes] / completedTask[numOfAppTypes] + "%");

		// clear related collections (map list etc.)
		taskMap.clear();
		vmLoadList.clear();
		apDelayList.clear();
		taskMapPersisted.clear();
	}
	
	private void recordLog(int taskId){
		LogItem value = taskMap.remove(taskId);
		
		if (value.isInWarmUpPeriod())
			return;

		/*Rejected by Solver*/
		if (value.getStatus() == SimLogger.TASK_STATUS.REJECTED_BY_SOLVER){
			rejectedTasksBySolver[value.getTaskType()]++;
		}
		if (value.getStatus() == SimLogger.TASK_STATUS.ERROR_DUE_TO_RAM_CAPACITY){
			failTaskDueToRamCapacity[value.getTaskType()]++;
		}
		if (value.getStatus() == SimLogger.TASK_STATUS.ERROR_DUE_TO_DELAY_LIMIT){
			failTaskDueToDelayLimit[value.getTaskType()]++;
		}
		if (value.getStatus() == SimLogger.TASK_STATUS.COMLETED) {
			completedTask[value.getTaskType()]++;

			if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
				completedTaskOnCloud[value.getTaskType()]++;
			else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
				completedTaskOnMobile[value.getTaskType()]++;
			else
				completedTaskOnEdge[value.getTaskType()]++;
		}
		if (value.getStatus() != SimLogger.TASK_STATUS.COMLETED){
			failedTask[value.getTaskType()]++;

			if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
				failedTaskOnCloud[value.getTaskType()]++;
			else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
				failedTaskOnMobile[value.getTaskType()]++;
			else
				failedTaskOnEdge[value.getTaskType()]++;
		}

		if (value.getStatus() == SimLogger.TASK_STATUS.COMLETED) {
			cost[value.getTaskType()] += value.getCost();
			QoE[value.getTaskType()] += value.getQoE();
			serviceTime[value.getTaskType()] += value.getServiceTime();
			networkDelay[value.getTaskType()] += value.getNetworkDelay();
			processingTime[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay());
			orchestratorOverhead[value.getTaskType()] += value.getOrchestratorOverhead();
			
			if(value.getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY) != 0) {
				lanUsage[value.getTaskType()]++;
				lanDelay[value.getTaskType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY);
			}
			if(value.getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY) != 0) {
				manUsage[value.getTaskType()]++;
				manDelay[value.getTaskType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY);
			}
			if(value.getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY) != 0) {
				wanUsage[value.getTaskType()]++;
				wanDelay[value.getTaskType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY);
			}
			if(value.getNetworkDelay(NETWORK_DELAY_TYPES.GSM_DELAY) != 0) {
				gsmUsage[value.getTaskType()]++;
				gsmDelay[value.getTaskType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.GSM_DELAY);
			}
			
			if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal()) {
				serviceTimeOnCloud[value.getTaskType()] += value.getServiceTime();
				processingTimeOnCloud[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay());
			}
			else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal()) {
				serviceTimeOnMobile[value.getTaskType()] += value.getServiceTime();
				processingTimeOnMobile[value.getTaskType()] += value.getServiceTime();
			}
			else {
				serviceTimeOnEdge[value.getTaskType()] += value.getServiceTime();
				processingTimeOnEdge[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay());
			}
		} else if (value.getStatus() == SimLogger.TASK_STATUS.REJECTED_DUE_TO_VM_CAPACITY) {
			failedTaskDueToVmCapacity[value.getTaskType()]++;
			
			if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
				failedTaskDueToVmCapacityOnCloud[value.getTaskType()]++;
			else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
				failedTaskDueToVmCapacityOnMobile[value.getTaskType()]++;
			else
				failedTaskDueToVmCapacityOnEdge[value.getTaskType()]++;
		} else if (value.getStatus() == SimLogger.TASK_STATUS.REJECTED_DUE_TO_BANDWIDTH
				|| value.getStatus() == SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_BANDWIDTH) {
			failedTaskDuetoBw[value.getTaskType()]++;
			if (value.getNetworkError() == NETWORK_ERRORS.LAN_ERROR)
				failedTaskDuetoLanBw[value.getTaskType()]++;
			else if (value.getNetworkError() == NETWORK_ERRORS.MAN_ERROR)
				failedTaskDuetoManBw[value.getTaskType()]++;
			else if (value.getNetworkError() == NETWORK_ERRORS.WAN_ERROR)
				failedTaskDuetoWanBw[value.getTaskType()]++;
			else if (value.getNetworkError() == NETWORK_ERRORS.GSM_ERROR)
				failedTaskDuetoGsmBw[value.getTaskType()]++;
		} else if (value.getStatus() == SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_MOBILITY) {
			failedTaskDuetoMobility[value.getTaskType()]++;
		} else if (value.getStatus() == SimLogger.TASK_STATUS.REJECTED_DUE_TO_WLAN_COVERAGE) {
			refectedTaskDuetoWlanRange[value.getTaskType()]++;;
        }
		
		//if deep file logging is enabled, record every task result
		if (SimSettings.getInstance().getDeepFileLoggingEnabled()){
			try {
				if (value.getStatus() == SimLogger.TASK_STATUS.COMLETED)
					appendToFile(successBW, value.toString(taskId));
				else
					appendToFile(failBW, value.toString(taskId));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}

class VmLoadLogItem {
	private double time;
	private double vmLoadOnEdge;
	private double vmLoadOnCloud;
	private double vmLoadOnMobile;

	VmLoadLogItem(double _time, double _vmLoadOnEdge, double _vmLoadOnCloud, double _vmLoadOnMobile) {
		time = _time;
		vmLoadOnEdge = _vmLoadOnEdge;
		vmLoadOnCloud = _vmLoadOnCloud;
		vmLoadOnMobile = _vmLoadOnMobile;
	}

	public double getEdgeLoad() {
		return vmLoadOnEdge;
	}

	public double getCloudLoad() {
		return vmLoadOnCloud;
	}
	
	public double getMobileLoad() {
		return vmLoadOnMobile;
	}
	
	public String toString() {
		return time + 
				SimSettings.DELIMITER + vmLoadOnEdge +
				SimSettings.DELIMITER + vmLoadOnCloud +
				SimSettings.DELIMITER + vmLoadOnMobile;
	}
}

class LoadPerVmLogItem {
	private double time;
	private String vmLoadOnEdge;
	private String vmLoadOnCloud;

	LoadPerVmLogItem(double _time, String _vmLoadOnEdge, String _vmLoadOnCloud) {
		time = _time;
		vmLoadOnEdge = _vmLoadOnEdge;
		vmLoadOnCloud = _vmLoadOnCloud;
	}

	public String getEdgeLoad() {
		return vmLoadOnEdge;
	}

	public String getCloudLoad() {
		return vmLoadOnCloud;
	}

	public double getTime() {
		return time;
	}
}

class LinkUsageLogItem {
	private double time;
	private double wlanusage;
	private double wanusage;
	private double manusage;

	LinkUsageLogItem(double _time, double _wlanusage, double _wanusage, double _manusage) {
		time = _time;
		wlanusage = _wlanusage;
		wanusage = _wanusage;
		manusage = _manusage;
	}

	public double getTime(){
		return time;
	}
	public double getWlan(){
		return wlanusage;
	}

	public String toString() {

		String entryToString = "" + wlanusage + SimSettings.DELIMITER + wanusage + SimSettings.DELIMITER + manusage;

			
		return time + SimSettings.DELIMITER + entryToString;
	}
}

class ApDelayLogItem {
	private double time;
	private double apUploadDelays[];
	double[] apDownloadDelays;
	
	ApDelayLogItem(double _time, double[] _apUploadDelays, double[] _apDownloadDelays){
		time = _time;
		apUploadDelays = _apUploadDelays;
		apDownloadDelays = _apDownloadDelays;
	}
	
	public String getUploadStat() {
		String result = Double.toString(time);
		for(int i=0; i<apUploadDelays.length; i++)
			result += SimSettings.DELIMITER + apUploadDelays[i];
		
		return result;
	}

	public String getDownloadStat() {
		String result = Double.toString(time);
		for(int i=0; i<apDownloadDelays.length; i++)
			result += SimSettings.DELIMITER + apDownloadDelays[i];
		
		return result;
	}
}


class VMItem {
	private int vmid;
	private double time = 0;
	private double timestart;
	private double timestop;
	private int countbefore = 0;
	private int countafter = 0;
	private int number_of_tasks = 0;
	private double initialization_cost;
	private double costPerSec;
	
	VMItem(int _vmid, int _vmType){
		vmid = _vmid;

		//Cloud
		if(_vmType  ==  SimSettings.CLOUD_DATACENTER_ID){
			initialization_cost = SimSettings.getInstance().getCloudCostInit()[_vmid % SimSettings.getInstance().getNumOfEdgeVMs()];
			costPerSec = SimSettings.getInstance().getCloudCostSec()[_vmid % SimSettings.getInstance().getNumOfEdgeVMs()];
		//Edge
		}else{
			double[] element = SimSettings.getInstance().getEdgeLookUpTable().get(_vmid);
			initialization_cost = element[0];
			costPerSec = element[1];
		}
	}
	
	public void addusertovm(double _time) {
		number_of_tasks = number_of_tasks + 1;
		countafter = countafter + 1;
		verifytime(_time);
	}
	public void removeuserfromvm(double _time) {
		number_of_tasks = number_of_tasks - 1;
		countafter = countafter - 1;
		verifytime(_time);
	}

	public void verifytime(double _time) {
		if(countbefore == 0 && countafter == 1){
			timestart = _time;
		}else if(countbefore == 1 && countafter == 0){
			timestop = _time;
			time = time + (timestop - timestart);
			timestart = 0;
			timestop = 0;
		}
		countbefore = countafter;
	}

	public void stopvm(double _time) {

		if(countbefore > 0 && countafter > 0){
			countafter = 0;
			countbefore = 1;
			verifytime(_time);
		}
	}

	public double getinitialcost(){
		return initialization_cost;
	}

	public double getcostsec(){
		return costPerSec;
	}

	public double getTime(){
		return time;
	}
	public int getNumberOfTasks(){
		return number_of_tasks;
	}
}



class LogItem {
	private SimLogger.TASK_STATUS status;
	private SimLogger.NETWORK_ERRORS networkError;
	private Integer deviceId = null;
	private Integer datacenterId = null;
	private Integer hostId = null;
	private Integer vmId = null;
	private int vmType;
	private int taskType;
	private int taskLenght;
	private int taskInputSize;
	private int taskOutputSize;
	private Double taskCreationTime = null;
	private Double taskStartTime = null;
	private Double taskEndTime = null;
	private Double taskFinishedUploadTime = null;
	private Double taskStartedDownloadTime = null;
	private double lanUploadDelay;
	private double manUploadDelay;
	private double wanUploadDelay;
	private double gsmUploadDelay;
	private double lanDownloadDelay;
	private double manDownloadDelay;
	private double wanDownloadDelay;
	private double gsmDownloadDelay;
	private double lanUsageUpload;
	private double manUsageUpload;
	private double wanUsageUpload;
	private double lanUsageDownload;
	private double manUsageDownload;
	private double wanUsageDownload;
	private double bwCost;
	private double cpuCost;
	private double QoE;
	private double orchestratorOverhead;
	private boolean isInWarmUpPeriod;

	LogItem(int _deviceId, int _taskType, int _taskLenght, int _taskInputSize, int _taskOutputSize, double time) {
		deviceId = _deviceId;
		taskType = _taskType;
		taskLenght = _taskLenght;
		taskInputSize = _taskInputSize;
		taskOutputSize = _taskOutputSize;
		networkError = NETWORK_ERRORS.NONE;
		status = SimLogger.TASK_STATUS.CREATED;
		taskCreationTime = time;

		if (taskCreationTime < SimSettings.getInstance().getWarmUpPeriod()){
			isInWarmUpPeriod = true;
		}else{
			isInWarmUpPeriod = false;
		}
	}
	
	public void taskStarted(double time) {
		taskStartTime = time;
		status = SimLogger.TASK_STATUS.UPLOADING;
	}

	public Double gettaskStartTime(){
		return taskStartTime;
	}

	public Double gettaskStartedDownloadTime(){
		return taskStartedDownloadTime;
	}

	public Double gettaskFinishedUploadTime(){
		return taskFinishedUploadTime;
	}

	public Double gettaskEndTime(){
		return taskEndTime;
	}

	public double getwlanUsageUpload(){
		return lanUsageUpload;
	}
	public double getmanUsageUpload(){
		return manUsageUpload;
	}
	public double getwanUsageUpload(){
		return wanUsageUpload;
	}
	public double getwlanUsageDownload(){
		return lanUsageDownload;
	}
	public double getmanUsageDownload(){
		return manUsageDownload;
	}
	public double getwanUsageDownload(){
		return wanUsageDownload;
	}

	public void setUploadDelay(double delay, NETWORK_DELAY_TYPES delayType) {
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			lanUploadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			manUploadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			wanUploadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			gsmUploadDelay = delay;
	}
	
	public void setDownloadDelay(double delay, NETWORK_DELAY_TYPES delayType) {
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			lanDownloadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			manDownloadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			wanDownloadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			gsmDownloadDelay = delay;
	}
	
	public void setApUsageUpload(double usage, NETWORK_DELAY_TYPES usageType) {
		if(usageType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			lanUsageUpload = usage;
		else if(usageType == NETWORK_DELAY_TYPES.MAN_DELAY)
			manUsageUpload = usage;
		else if(usageType == NETWORK_DELAY_TYPES.WAN_DELAY)
			wanUsageUpload = usage;
	}

	
	public void setApUsageDownload(double usage, NETWORK_DELAY_TYPES usageType) {
		if(usageType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			lanUsageDownload = usage;
		else if(usageType == NETWORK_DELAY_TYPES.MAN_DELAY)
			manUsageDownload = usage;
		else if(usageType == NETWORK_DELAY_TYPES.WAN_DELAY)
			wanUsageDownload = usage;
	}

	public void taskAssigned(int _datacenterId, int _hostId, int _vmId, int _vmType, double time) {
		status = SimLogger.TASK_STATUS.PROCESSING;
		datacenterId = _datacenterId;
		hostId = _hostId;
		vmId = _vmId;
		vmType = _vmType;
		taskFinishedUploadTime = time;
	}

	public void taskExecuted(double time) {
		status = SimLogger.TASK_STATUS.DOWNLOADING;
		taskStartedDownloadTime = time;
	}

	public void taskEnded(double time) {
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.COMLETED;
	}

	public void taskRejectedDueToVMCapacity(double time, int _vmType) {
		vmType = _vmType;
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.REJECTED_DUE_TO_VM_CAPACITY;
	}

	public void taskErrorDueToRamCapacity() {
		status = SimLogger.TASK_STATUS.ERROR_DUE_TO_RAM_CAPACITY;
	}

	public void taskErrorDueToDelayLimit() {
		double deadline = SimSettings.getInstance().getTaskLookUpTable()[taskType][13];

		double timeOfTask = taskEndTime - taskCreationTime;

		if(timeOfTask > deadline){
			status = SimLogger.TASK_STATUS.ERROR_DUE_TO_DELAY_LIMIT;	
		}
	}
	
	public void taskRejectedDueToWlanCoverage(double time, int _vmType) {
		vmType = _vmType;
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.REJECTED_DUE_TO_WLAN_COVERAGE;
	}

	public void taskRejectedDueToBandwidth(double time, int _vmType, NETWORK_DELAY_TYPES delayType) {
		vmType = _vmType;
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.REJECTED_DUE_TO_BANDWIDTH;
		
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			networkError = NETWORK_ERRORS.LAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			networkError = NETWORK_ERRORS.MAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			networkError = NETWORK_ERRORS.WAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			networkError = NETWORK_ERRORS.GSM_ERROR;
	}

	public void taskRejectedBySolver() {
		status = SimLogger.TASK_STATUS.REJECTED_BY_SOLVER;
	}

	public void taskFailedDueToBandwidth(double time, NETWORK_DELAY_TYPES delayType) {
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_BANDWIDTH;
		
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			networkError = NETWORK_ERRORS.LAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			networkError = NETWORK_ERRORS.MAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			networkError = NETWORK_ERRORS.WAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			networkError = NETWORK_ERRORS.GSM_ERROR;
	}

	public void taskFailedDueToMobility(double time) {
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_MOBILITY;
	}

	public void setCost(double _bwCost, double _cpuCos) {
		bwCost = _bwCost;
		cpuCost = _cpuCos;
	}
	
	public void setQoE(double qoe){
		QoE = qoe;
	}
	
	public void setOrchestratorOverhead(double overhead){
		orchestratorOverhead = overhead;
	}

	public boolean isInWarmUpPeriod() {
		return isInWarmUpPeriod;
	}

	public double getCost() {
		return bwCost + cpuCost;
	}

	public double getQoE() {
		return QoE;
	}

	public double getOrchestratorOverhead() {
		return orchestratorOverhead;
	}
	
	public double getNetworkUploadDelay(NETWORK_DELAY_TYPES delayType) {
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			result = gsmUploadDelay;
		
		return result;
	}

	public double getNetworkDownloadDelay(NETWORK_DELAY_TYPES delayType) {
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanDownloadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manDownloadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanDownloadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			result = gsmDownloadDelay;
		
		return result;
	}
	
	public double getNetworkDelay(NETWORK_DELAY_TYPES delayType){
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanDownloadDelay + lanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manDownloadDelay + manUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanDownloadDelay + wanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.GSM_DELAY)
			result = gsmDownloadDelay + gsmUploadDelay;
		
		return result;
	}
	
	public double getNetworkDelay(){
		return  lanUploadDelay +
				manUploadDelay +
				wanUploadDelay +
				gsmUploadDelay +
				lanDownloadDelay +
				manDownloadDelay +
				wanDownloadDelay +
				gsmDownloadDelay;
	}
	
	public double getServiceTime() {
		return taskEndTime - taskStartTime;
	}

	public SimLogger.TASK_STATUS getStatus() {
		return status;
	}

	public SimLogger.NETWORK_ERRORS getNetworkError() {
		return networkError;
	}
	
	public int getVmType() {
		return vmType;
	}

	public int getTaskType() {
		return taskType;
	}

	public String toString(int taskId) {
		String result = taskId + SimSettings.DELIMITER + deviceId + SimSettings.DELIMITER + datacenterId + SimSettings.DELIMITER + hostId
				+ SimSettings.DELIMITER + vmId + SimSettings.DELIMITER + vmType + SimSettings.DELIMITER + taskType
				+ SimSettings.DELIMITER + taskLenght + SimSettings.DELIMITER + taskInputSize + SimSettings.DELIMITER
				+ taskOutputSize + SimSettings.DELIMITER + taskCreationTime + SimSettings.DELIMITER + taskStartTime + SimSettings.DELIMITER + taskFinishedUploadTime
				+ SimSettings.DELIMITER +  taskStartedDownloadTime + SimSettings.DELIMITER + taskEndTime + SimSettings.DELIMITER;

		if (status == SimLogger.TASK_STATUS.COMLETED)//Sent to VM
			result += "COMPLETED" + SimSettings.DELIMITER;
		else if (status == SimLogger.TASK_STATUS.REJECTED_DUE_TO_VM_CAPACITY)//Not sent
			result += "rejected_by_vm_capacity;"; // failure reason 1
		else if (status == SimLogger.TASK_STATUS.REJECTED_DUE_TO_BANDWIDTH)//Not sent
			result += "rejected_by_bandwidth;"; // failure reason 2
		else if (status == SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_BANDWIDTH)//No
			result += "unfinished_by_bandwidth;"; // failure reason 3
		else if (status == SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_MOBILITY)//No
			result += "unfinished_by_mobility;"; // failure reason 4
        else if (status == SimLogger.TASK_STATUS.REJECTED_DUE_TO_WLAN_COVERAGE)//No
            result += "rejected_by_wlan_coverage;"; // failure reason 5
        else if (status == SimLogger.TASK_STATUS.REJECTED_BY_SOLVER)//No
            result += "rejected_by_solver;"; // failure reason 6
        else if (status == SimLogger.TASK_STATUS.ERROR_DUE_TO_DELAY_LIMIT)//Sent to VM
            result += "delay_above_the_limit;"; // failure reason 7
		else if (status == SimLogger.TASK_STATUS.ERROR_DUE_TO_RAM_CAPACITY)
			result += "ram_memory_error;";
		else		
			result += "unfinished_TLE;"; // default failure reason


		result += getNetworkDelay() + SimSettings.DELIMITER;
		result += getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY) + SimSettings.DELIMITER;
		result += getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY) + SimSettings.DELIMITER;
		result += getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY) + SimSettings.DELIMITER;
		result += getNetworkDelay(NETWORK_DELAY_TYPES.GSM_DELAY);

		return result;
	}
}