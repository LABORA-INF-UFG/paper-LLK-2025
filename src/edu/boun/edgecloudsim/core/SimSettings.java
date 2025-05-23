/*
 * Title:        EdgeCloudSim - Simulation Settings class
 * 
 * Description: 
 * SimSettings provides system wide simulation settings. It is a
 * singleton class and provides all necessary information to other modules.
 * If you need to use another simulation setting variable in your
 * config file, add related getter method in this class.
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

import edu.boun.edgecloudsim.utils.SimLogger;

public class SimSettings {
	private static SimSettings instance = null;
	private Document edgeDevicesDoc = null;

	public static final double CLIENT_ACTIVITY_START_TIME = 10;

	//enumarations for the VM types
	public static enum VM_TYPES { MOBILE_VM, EDGE_VM, CLOUD_VM }

	//enumarations for the VM types
	public static enum NETWORK_DELAY_TYPES { WLAN_DELAY, MAN_DELAY, WAN_DELAY, GSM_DELAY }

	//predifined IDs for the components.
	public static final int CLOUD_DATACENTER_ID = 1000;
	public static final int MOBILE_DATACENTER_ID = 1001;
	public static final int EDGE_ORCHESTRATOR_ID = 1002;
	public static final int GENERIC_EDGE_DEVICE_ID = 1003;

	//delimiter for output file.
	public static final String DELIMITER = ";";

	public static final String NEW_LINE = "\n";

	private double SIMULATION_TIME; //minutes unit in properties file
	private double WARM_UP_PERIOD; //minutes unit in properties file
	private double INTERVAL_TO_GET_VM_LOAD_LOG; //minutes unit in properties file
	private double INTERVAL_TO_GET_LOCATION_LOG; //minutes unit in properties file
	private double INTERVAL_TO_GET_AP_DELAY_LOG; //minutes unit in properties file
	private boolean FILE_LOG_ENABLED; //boolean to check file logging option
	private boolean DEEP_FILE_LOG_ENABLED; //boolean to check deep file logging option

	private int MIN_NUM_OF_MOBILE_DEVICES;
	private int MAX_NUM_OF_MOBILE_DEVICES;
	private int MOBILE_DEVICE_COUNTER_SIZE;
	private int WLAN_RANGE;

	private int NUM_OF_EDGE_DATACENTERS;
	private int NUM_OF_EDGE_HOSTS;
	private int NUM_OF_EDGE_VMS;
	private int NUM_OF_PLACE_TYPES;

	private double WAN_PROPAGATION_DELAY; //seconds unit in properties file
	private double GSM_PROPAGATION_DELAY; //seconds unit in properties file
	private double LAN_INTERNAL_DELAY; //seconds unit in properties file
	private int BANDWITH_WLAN; //Mbps unit in properties file
	private int BANDWITH_MAN; //Mbps unit in properties file
	private int BANDWITH_WAN; //Mbps unit in properties file
	private int BANDWITH_GSM; //Mbps unit in properties file

	private int NUM_OF_HOST_ON_CLOUD_DATACENTER;
	private int NUM_OF_VM_ON_CLOUD_HOST;
	private int[] CORE_FOR_CLOUD_VM = new int[4];
	private int[] MIPS_FOR_CLOUD_VM = new int[4]; //MIPS
	private int[] RAM_FOR_CLOUD_VM = new int[4]; //MB
	private int STORAGE_FOR_CLOUD_VM; //Byte
	private double[] COST_INITIALIZATION_CLOUD = new double[4];
	private double[] COST_SEC_CLOUD = new double[4];

	private int CORE_FOR_VM;
	private int MIPS_FOR_VM; //MIPS
	private int RAM_FOR_VM; //MB
	private int STORAGE_FOR_VM; //Byte

	private String[] SIMULATION_SCENARIOS;
	private String[] ORCHESTRATOR_POLICIES;

	private double NORTHERN_BOUND;
	private double EASTERN_BOUND;
	private double SOUTHERN_BOUND;
	private double WESTERN_BOUND;

	private int NUMBER_OF_TASKS_SEND_TO_SOLVER;

	private double BATCH_TIMESTAMP_LIMIT;

	private int SEED;

	// mean waiting time (minute) is stored for each place types
	private double[] mobilityLookUpTable;

	// following values are stored for each applications defined in applications.xml
	// [0] usage percentage (%)
	// [1] prob. of selecting cloud (%)
	// [2] poisson mean (sec)
	// [3] active period (sec)
	// [4] idle period (sec)
	// [5] avg data upload (KB)
	// [6] avg data download (KB)
	// [7] avg task length (MI)
	// [8] required # of cores
	// [9] vm utilization on edge (%)
	// [10] vm utilization on cloud (%)
	// [11] vm utilization on mobile (%)
	// [12] delay sensitivity [0-1]
	private double[][] taskLookUpTable = null;

	private ArrayList<double[]> edgeLookUpTable = null;
	//private ArrayList<double[]> cloudLookUpTable = null;

	private String[] taskNames = null;

	private SimSettings() {
		NUM_OF_PLACE_TYPES = 0;
	}

	public static SimSettings getInstance() {
		if(instance == null) {
			instance = new SimSettings();
		}
		return instance;
	}

	/**
	 * Reads configuration file and stores information to local variables
	 * @param propertiesFile
	 * @return
	 */
	public boolean initialize(String propertiesFile, String edgeDevicesFile, String applicationsFile){
		boolean result = false;
		InputStream input = null;
		try {
			input = new FileInputStream(propertiesFile);

			// load a properties file
			Properties prop = new Properties();
			prop.load(input);

			SIMULATION_TIME = Double.parseDouble(prop.getProperty("simulation_time")); //miliseconds
			WARM_UP_PERIOD = Double.parseDouble(prop.getProperty("warm_up_period")); //miliseconds
			INTERVAL_TO_GET_VM_LOAD_LOG = Double.parseDouble(prop.getProperty("vm_load_check_interval")); //miliseconds
			INTERVAL_TO_GET_LOCATION_LOG = Double.parseDouble(prop.getProperty("vm_location_check_interval")); //miliseconds
			INTERVAL_TO_GET_AP_DELAY_LOG = Double.parseDouble(prop.getProperty("ap_delay_check_interval", "0")); //miliseconds		
			FILE_LOG_ENABLED = Boolean.parseBoolean(prop.getProperty("file_log_enabled"));
			DEEP_FILE_LOG_ENABLED = Boolean.parseBoolean(prop.getProperty("deep_file_log_enabled"));

			MIN_NUM_OF_MOBILE_DEVICES = Integer.parseInt(prop.getProperty("min_number_of_mobile_devices"));
			MAX_NUM_OF_MOBILE_DEVICES = Integer.parseInt(prop.getProperty("max_number_of_mobile_devices"));
			MOBILE_DEVICE_COUNTER_SIZE = Integer.parseInt(prop.getProperty("mobile_device_counter_size"));
			WLAN_RANGE = Integer.parseInt(prop.getProperty("wlan_range", "0"));

			WAN_PROPAGATION_DELAY = Double.parseDouble(prop.getProperty("wan_propagation_delay", "0"));
			GSM_PROPAGATION_DELAY = Double.parseDouble(prop.getProperty("gsm_propagation_delay", "0"));
			LAN_INTERNAL_DELAY = Double.parseDouble(prop.getProperty("lan_internal_delay", "0"));
			BANDWITH_WLAN = 1000 * Integer.parseInt(prop.getProperty("wlan_bandwidth"));
			BANDWITH_MAN = 1000 * Integer.parseInt(prop.getProperty("man_bandwidth", "0"));
			BANDWITH_WAN = 1000 * Integer.parseInt(prop.getProperty("wan_bandwidth", "0"));
			BANDWITH_GSM =  1000 * Integer.parseInt(prop.getProperty("gsm_bandwidth", "0"));

			NUM_OF_HOST_ON_CLOUD_DATACENTER = Integer.parseInt(prop.getProperty("number_of_host_on_cloud_datacenter"));
			NUM_OF_VM_ON_CLOUD_HOST = Integer.parseInt(prop.getProperty("number_of_vm_on_cloud_host"));
			CORE_FOR_CLOUD_VM = new int[]{Integer.parseInt(prop.getProperty("core_for_cloud_vm1", "0")), Integer.parseInt(prop.getProperty("core_for_cloud_vm2", "0")), Integer.parseInt(prop.getProperty("core_for_cloud_vm3", "0")), Integer.parseInt(prop.getProperty("core_for_cloud_vm4", "0"))};
			MIPS_FOR_CLOUD_VM = new int[]{Integer.parseInt(prop.getProperty("mips_for_cloud_vm1", "0")), Integer.parseInt(prop.getProperty("mips_for_cloud_vm2", "0")), Integer.parseInt(prop.getProperty("mips_for_cloud_vm3", "0")), Integer.parseInt(prop.getProperty("mips_for_cloud_vm4", "0"))};
			RAM_FOR_CLOUD_VM = new int[]{Integer.parseInt(prop.getProperty("ram_for_cloud_vm1", "0")), Integer.parseInt(prop.getProperty("ram_for_cloud_vm2", "0")), Integer.parseInt(prop.getProperty("ram_for_cloud_vm3", "0")), Integer.parseInt(prop.getProperty("ram_for_cloud_vm4", "0"))};
			STORAGE_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("storage_for_cloud_vm"));
			COST_INITIALIZATION_CLOUD = new double[]{Double.parseDouble(prop.getProperty("v1_cost_init", "0")), Double.parseDouble(prop.getProperty("v2_cost_init", "0")), Double.parseDouble(prop.getProperty("v3_cost_init", "0")), Double.parseDouble(prop.getProperty("v4_cost_init", "0"))};
			COST_SEC_CLOUD = new double[]{Double.parseDouble(prop.getProperty("v1_cost_sec", "0")), Double.parseDouble(prop.getProperty("v2_cost_sec", "0")), Double.parseDouble(prop.getProperty("v3_cost_sec", "0")), Double.parseDouble(prop.getProperty("v4_cost_sec", "0"))};

			RAM_FOR_VM = Integer.parseInt(prop.getProperty("ram_for_mobile_vm"));
			CORE_FOR_VM = Integer.parseInt(prop.getProperty("core_for_mobile_vm"));
			MIPS_FOR_VM = Integer.parseInt(prop.getProperty("mips_for_mobile_vm"));
			STORAGE_FOR_VM = Integer.parseInt(prop.getProperty("storage_for_mobile_vm"));

			ORCHESTRATOR_POLICIES = prop.getProperty("orchestrator_policies").split(",");

			SIMULATION_SCENARIOS = prop.getProperty("simulation_scenarios").split(",");

			NORTHERN_BOUND = Double.parseDouble(prop.getProperty("northern_bound", "0"));
			SOUTHERN_BOUND = Double.parseDouble(prop.getProperty("southern_bound", "0"));
			EASTERN_BOUND = Double.parseDouble(prop.getProperty("eastern_bound", "0"));
			WESTERN_BOUND = Double.parseDouble(prop.getProperty("western_bound", "0"));

			//avg waiting time in a place (min)
			double place1_mean_waiting_time = Double.parseDouble(prop.getProperty("attractiveness_L1_mean_waiting_time"));
			double place2_mean_waiting_time = Double.parseDouble(prop.getProperty("attractiveness_L2_mean_waiting_time"));
			double place3_mean_waiting_time = Double.parseDouble(prop.getProperty("attractiveness_L3_mean_waiting_time"));

			//mean waiting time (minute)
			mobilityLookUpTable = new double[]{
					place1_mean_waiting_time, //ATTRACTIVENESS_L1
					place2_mean_waiting_time, //ATTRACTIVENESS_L2
					place3_mean_waiting_time  //ATTRACTIVENESS_L3
			};

			NUMBER_OF_TASKS_SEND_TO_SOLVER = Integer.parseInt(prop.getProperty("number_of_tasks_send_to_solver", "0"));

			BATCH_TIMESTAMP_LIMIT = Integer.parseInt(prop.getProperty("batch_timestamp_limit", "0"));

			SEED = Integer.parseInt(prop.getProperty("seed", "0"));


		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
					result = true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		parseApplicationsXML(applicationsFile);
		parseEdgeDevicesXML(edgeDevicesFile);

		return result;
	}

	/**
	 * returns the parsed XML document for edge_devices.xml
	 */
	public Document getEdgeDevicesDocument(){
		return edgeDevicesDoc;
	}


	/**
	 * returns simulation time (in seconds unit) from properties file
	 */
	public double getSimulationTime()
	{
		return SIMULATION_TIME;
	}

	public int getNumberOfTasksSendToSolver()
	{
		return NUMBER_OF_TASKS_SEND_TO_SOLVER;
	}

	public double getBatchTimestampLimit()
	{
		return BATCH_TIMESTAMP_LIMIT;
	}

	public int getSeed()
	{
		return SEED;
	}

	/**
	 * returns warm up period (in seconds unit) from properties file
	 */
	public double getWarmUpPeriod()
	{
		return WARM_UP_PERIOD; 
	}

	/**
	 * returns VM utilization log collection interval (in seconds unit) from properties file
	 */
	public double getVmLoadLogInterval()
	{
		return INTERVAL_TO_GET_VM_LOAD_LOG; 
	}

	/**
	 * returns VM location log collection interval (in seconds unit) from properties file
	 */
	public double getLocationLogInterval()
	{
		return INTERVAL_TO_GET_LOCATION_LOG; 
	}

	/**
	 * returns VM location log collection interval (in seconds unit) from properties file
	 */
	public double getApDelayLogInterval()
	{
		return INTERVAL_TO_GET_AP_DELAY_LOG; 
	}

	/**
	 * returns deep statistics logging status from properties file
	 */
	public boolean getDeepFileLoggingEnabled()
	{
		return FILE_LOG_ENABLED && DEEP_FILE_LOG_ENABLED; 
	}

	/**
	 * returns deep statistics logging status from properties file
	 */
	public boolean getFileLoggingEnabled()
	{
		return FILE_LOG_ENABLED; 
	}

	/**
	 * returns WAN propagation delay (in second unit) from properties file
	 */
	public double getWanPropagationDelay()
	{
		return WAN_PROPAGATION_DELAY;
	}

	/**
	 * returns GSM propagation delay (in second unit) from properties file
	 */
	public double getGsmPropagationDelay()
	{
		return GSM_PROPAGATION_DELAY;
	}

	/**
	 * returns internal LAN propagation delay (in second unit) from properties file
	 */
	public double getInternalLanDelay()
	{
		return LAN_INTERNAL_DELAY;
	}

	/**
	 * returns WLAN bandwidth (in Mbps unit) from properties file
	 */
	public int getWlanBandwidth()
	{
		return BANDWITH_WLAN;
	}

	/**
	 * returns MAN bandwidth (in Mbps unit) from properties file
	 */
	public int getManBandwidth()
	{
		return BANDWITH_MAN;
	}

	/**
	 * returns WAN bandwidth (in Mbps unit) from properties file
	 */
	public int getWanBandwidth()
	{
		return BANDWITH_WAN; 
	}

	/**
	 * returns GSM bandwidth (in Mbps unit) from properties file
	 */
	public int getGsmBandwidth()
	{
		return BANDWITH_GSM;
	}

	/**
	 * returns the minimum number of the mobile devices used in the simulation
	 */
	public int getMinNumOfMobileDev()
	{
		return MIN_NUM_OF_MOBILE_DEVICES;
	}

	/**
	 * returns the maximum number of the mobile devices used in the simulation
	 */
	public int getMaxNumOfMobileDev()
	{
		return MAX_NUM_OF_MOBILE_DEVICES;
	}

	/**
	 * returns the number of increase on mobile devices
	 * while iterating from min to max mobile device
	 */
	public int getMobileDevCounterSize()
	{
		return MOBILE_DEVICE_COUNTER_SIZE;
	}

	/**
	 * returns edge device range in meter
	 */
	public int getWlanRange()
	{
		return WLAN_RANGE;
	}

	/**
	 * returns the number of edge datacenters
	 */
	public int getNumOfEdgeDatacenters()
	{
		return NUM_OF_EDGE_DATACENTERS;
	}

	/**
	 * returns the number of edge hosts running on the datacenters
	 */
	public int getNumOfEdgeHosts()
	{
		return NUM_OF_EDGE_HOSTS;
	}

	/**
	 * returns the number of edge VMs running on the hosts
	 */
	public int getNumOfEdgeVMs()
	{
		return NUM_OF_EDGE_VMS;
	}

	/**
	 * returns the number of different place types
	 */
	public int getNumOfPlaceTypes()
	{
		return NUM_OF_PLACE_TYPES;
	}

	/**
	 * returns the number of cloud datacenters
	 */
	public int getNumOfCloudHost()
	{
		return NUM_OF_HOST_ON_CLOUD_DATACENTER;
	}

	/**
	 * returns the number of cloud VMs per Host
	 */
	public int getNumOfCloudVMsPerHost()
	{
		return NUM_OF_VM_ON_CLOUD_HOST;
	}

	/**
	 * returns the total number of cloud VMs
	 */
	public int getNumOfCloudVMs()
	{
		return NUM_OF_VM_ON_CLOUD_HOST * NUM_OF_HOST_ON_CLOUD_DATACENTER;
	}

	/**
	 * returns the number of cores for cloud VMs
	 */
	public int getCoreForCloudVM(int vm)
	{
		return CORE_FOR_CLOUD_VM[vm];
	}

	/**
	 * returns MIPS of the central cloud VMs
	 */
	public int getMipsForCloudVM(int vm)
	{
		return MIPS_FOR_CLOUD_VM[vm];
	}

	/**
	 * returns RAM of the central cloud VMs
	 */
	public int getRamForCloudVM(int vm)
	{
		return RAM_FOR_CLOUD_VM[vm];
	}

	/**
	 * returns Storage of the central cloud VMs
	 */
	public int getStorageForCloudVM()
	{
		return STORAGE_FOR_CLOUD_VM;
	}

	/**
	 * returns initialization cost of the central cloud VMs
	 */
	public double[] getCloudCostInit()
	{
		return COST_INITIALIZATION_CLOUD;
	}

	/**
	 * returns cost per time of the central cloud VMs
	 */
	public double[] getCloudCostSec()
	{
		return COST_SEC_CLOUD;
	}

	/**
	 * returns RAM of the mobile (processing unit) VMs
	 */
	public int getRamForMobileVM()
	{
		return RAM_FOR_VM;
	}

	/**
	 * returns the number of cores for mobile VMs
	 */
	public int getCoreForMobileVM()
	{
		return CORE_FOR_VM;
	}

	/**
	 * returns MIPS of the mobile (processing unit) VMs
	 */
	public int getMipsForMobileVM()
	{
		return MIPS_FOR_VM;
	}

	/**
	 * returns Storage of the mobile (processing unit) VMs
	 */
	public int getStorageForMobileVM()
	{
		return STORAGE_FOR_VM;
	}

	/**
	 * returns simulation screnarios as string
	 */
	public String[] getSimulationScenarios()
	{
		return SIMULATION_SCENARIOS;
	}

	/**
	 * returns orchestrator policies as string
	 */
	public String[] getOrchestratorPolicies()
	{
		return ORCHESTRATOR_POLICIES;
	}


	public double getNorthernBound() {
		return NORTHERN_BOUND;
	}

	public double getEasternBound() {
		return EASTERN_BOUND;
	}

	public double getSouthernBound() {
		return SOUTHERN_BOUND;
	}

	public double getWesternBound() {
		return WESTERN_BOUND;
	}

	/**
	 * returns mobility characteristic within an array
	 * the result includes mean waiting time (minute) or each place type
	 */ 
	public double[] getMobilityLookUpTable()
	{
		return mobilityLookUpTable;
	}

	/**
	 * returns application characteristic within two dimensional array
	 * the result includes the following values for each application type
	 * [0] usage percentage (%)
	 * [1] prob. of selecting cloud (%)
	 * [2] poisson mean (sec)
	 * [3] active period (sec)
	 * [4] idle period (sec)
	 * [5] avg data upload (KB)
	 * [6] avg data download (KB)
	 * [7] avg task length (MI)
	 * [8] required # of cores
	 * [9] vm utilization on edge (%)
	 * [10] vm utilization on cloud (%)
	 * [11] vm utilization on mobile (%)
	 * [12] delay sensitivity [0-1]
	 * [13] maximum delay requirement (sec)
	 */ 
	public double[][] getTaskLookUpTable()
	{
		return taskLookUpTable;
	}


	public ArrayList<double[]> getEdgeLookUpTable()
	{
		return edgeLookUpTable;
	}

	public double[] getTaskProperties(String taskName) {
		double[] result = null;
		int index = -1;
		for (int i=0;i<taskNames.length;i++) {
			if (taskNames[i].equals(taskName)) {
				index = i;
				break;
			}
		}

		if(index >= 0 && index < taskLookUpTable.length)
			result = taskLookUpTable[index];

		return result;
	}

	public String getTaskName(int taskType)
	{
		return taskNames[taskType];
	}

	private void isAttributePresent(Element element, String key) {
		String value = element.getAttribute(key);
		if (value.isEmpty() || value == null){
			throw new IllegalArgumentException("Attribute '" + key + "' is not found in '" + element.getNodeName() +"'");
		}
	}

	private void isElementPresent(Element element, String key) {
		try {
			String value = element.getElementsByTagName(key).item(0).getTextContent();
			if (value.isEmpty() || value == null){
				throw new IllegalArgumentException("Element '" + key + "' is not found in '" + element.getNodeName() +"'");
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Element '" + key + "' is not found in '" + element.getNodeName() +"'");
		}
	}

	private Boolean checkElement(Element element, String key) {
		Boolean result = true;
		try {
			String value = element.getElementsByTagName(key).item(0).getTextContent();
			if (value.isEmpty() || value == null){
				result = false;
			}
		} catch (Exception e) {
			result = false;
		}

		return result;
	}

	private void parseApplicationsXML(String filePath)
	{
		Document doc = null;
		try {	
			File devicesFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(devicesFile);
			doc.getDocumentElement().normalize();

			String mandatoryAttributes[] = {
					"usage_percentage", //usage percentage [0-100]
					"prob_cloud_selection", //prob. of selecting cloud [0-100]
					"poisson_interarrival", //poisson mean (sec)
					"active_period", //active period (sec)
					"idle_period", //idle period (sec)
					"data_upload", //avg data upload (KB)
					"data_download", //avg data download (KB)
					"task_length", //avg task length (MI)
					"required_core", //required # of core
					"vm_utilization_on_edge", //vm utilization on edge vm [0-100]
					"vm_utilization_on_cloud", //vm utilization on cloud vm [0-100]
					"vm_utilization_on_mobile", //vm utilization on mobile vm [0-100]
			"delay_sensitivity"}; //delay_sensitivity [0-1]

			String optionalAttributes[] = {
			"max_delay_requirement", //maximum delay requirement (sec)
			"vm_ram_utilization"}; 

			NodeList appList = doc.getElementsByTagName("application");
			taskLookUpTable = new double[appList.getLength()]
					[mandatoryAttributes.length + optionalAttributes.length];

			taskNames = new String[appList.getLength()];
			for (int i = 0; i < appList.getLength(); i++) {
				Node appNode = appList.item(i);

				Element appElement = (Element) appNode;
				isAttributePresent(appElement, "name");
				String taskName = appElement.getAttribute("name");
				taskNames[i] = taskName;

				for(int m=0; m<mandatoryAttributes.length; m++){
					isElementPresent(appElement, mandatoryAttributes[m]);
					taskLookUpTable[i][m] = Double.parseDouble(appElement.
							getElementsByTagName(mandatoryAttributes[m]).item(0).getTextContent());
				}

				for(int o=0; o<optionalAttributes.length; o++){
					double value = 0;
					if(checkElement(appElement, optionalAttributes[o]))
						value =  Double.parseDouble(appElement.getElementsByTagName(optionalAttributes[o]).item(0).getTextContent());

					taskLookUpTable[i][mandatoryAttributes.length + o] = value;
				}
			}
		} catch (Exception e) {
			SimLogger.printLine("Edge Devices XML cannot be parsed! Terminating simulation...");
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void parseEdgeDevicesXML(String filePath)
	{
		try {	
			edgeLookUpTable = new ArrayList<>();

			File devicesFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			edgeDevicesDoc = dBuilder.parse(devicesFile);
			edgeDevicesDoc.getDocumentElement().normalize();

			NodeList datacenterList = edgeDevicesDoc.getElementsByTagName("datacenter");
			for (int i = 0; i < datacenterList.getLength(); i++) {
				NUM_OF_EDGE_DATACENTERS++;
				Node datacenterNode = datacenterList.item(i);

				Element datacenterElement = (Element) datacenterNode;
				isAttributePresent(datacenterElement, "arch");
				isAttributePresent(datacenterElement, "os");
				isAttributePresent(datacenterElement, "vmm");
				isElementPresent(datacenterElement, "costPerBw");
				isElementPresent(datacenterElement, "costPerSec");
				isElementPresent(datacenterElement, "costPerMem");
				isElementPresent(datacenterElement, "costPerStorage");

				Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
				isElementPresent(location, "attractiveness");
				isElementPresent(location, "wlan_id");
				isElementPresent(location, "x_pos");
				isElementPresent(location, "y_pos");

				String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
				int placeTypeIndex = Integer.parseInt(attractiveness);
				if(NUM_OF_PLACE_TYPES < placeTypeIndex+1)
					NUM_OF_PLACE_TYPES = placeTypeIndex+1;

				NodeList hostList = datacenterElement.getElementsByTagName("host");
				for (int j = 0; j < hostList.getLength(); j++) {
					NUM_OF_EDGE_HOSTS++;
					Node hostNode = hostList.item(j);

					Element hostElement = (Element) hostNode;
					isElementPresent(hostElement, "core");
					isElementPresent(hostElement, "mips");
					isElementPresent(hostElement, "ram");
					isElementPresent(hostElement, "storage");

					NodeList vmList = hostElement.getElementsByTagName("VM");
					for (int k = 0; k < vmList.getLength(); k++) {
						NUM_OF_EDGE_VMS++;
						Node vmNode = vmList.item(k);

						Element vmElement = (Element) vmNode;
						isAttributePresent(vmElement, "vmm");
						isElementPresent(vmElement, "core");
						isElementPresent(vmElement, "mips");
						isElementPresent(vmElement, "ram");
						isElementPresent(vmElement, "storage");

						if(!ORCHESTRATOR_POLICIES[0].equals("AI_BASED")){
						isElementPresent(vmElement, "initialization_cost");
						isElementPresent(vmElement, "costPerSec");
						double init_cost = Double.parseDouble(vmElement.getElementsByTagName("initialization_cost").item(0).getTextContent());
						double cps = Double.parseDouble(vmElement.getElementsByTagName("costPerSec").item(0).getTextContent());

						edgeLookUpTable.add(new double[]{init_cost, cps});		
						}				
					}
				}
			}

		} catch (Exception e) {
			SimLogger.printLine("Edge Devices XML cannot be parsed! Terminating simulation...");
			e.printStackTrace();
			System.exit(1);
		}
	}
}