/*
 * Title:        EdgeCloudSim - Idle/Active Load Generator implementation
 * 
 * Description: 
 * IdleActiveLoadGenerator implements basic load generator model where the
 * mobile devices generate task in active period and waits in idle period.
 * Task interarrival time (load generation period), Idle and active periods
 * are defined in the configuration file.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.task_generator;

import java.util.ArrayList;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class IdleActiveLoadGenerator extends LoadGeneratorModel{
	int taskTypeOfDevices[];
	public IdleActiveLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		super(_numberOfMobileDevices, _simulationTime, _simScenario);
	}

	@Override
	public void initializeModel() {
		taskList = new ArrayList<TaskProperty>();

		long seed = SimSettings.getInstance().getSeed(); // You can change this to any long value
        RandomGenerator random = new Well19937c();
		random.setSeed(seed);
		
		//exponential number generator for file input size, file output size and task length
		ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][3];
		
		//create random number generator for each place
		for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
			if(SimSettings.getInstance().getTaskLookUpTable()[i][0] ==0)
				continue;
			
			expRngList[i][0] = new ExponentialDistribution(random, SimSettings.getInstance().getTaskLookUpTable()[i][5]);
			expRngList[i][1] = new ExponentialDistribution(random, SimSettings.getInstance().getTaskLookUpTable()[i][6]);
			expRngList[i][2] = new ExponentialDistribution(random, SimSettings.getInstance().getTaskLookUpTable()[i][7]);
		}
		
		//Create additional new random generators
		NormalDistribution[] expNorList = new NormalDistribution[2];
		expNorList[0] = new NormalDistribution(random, 155.62,14.10);
		expNorList[1] = new NormalDistribution(random, 322.38,71.18);
		
		//Each mobile device utilizes an app type (task type)
		taskTypeOfDevices = new int[numberOfMobileDevices];
		for(int i=0; i<numberOfMobileDevices; i++) {
			int randomTaskType = -1;
			double taskTypeSelector = SimUtils.getRandomDoubleNumber(0,100);
			double taskTypePercentage = 0;
			for (int j=0; j<SimSettings.getInstance().getTaskLookUpTable().length; j++) {
				taskTypePercentage += SimSettings.getInstance().getTaskLookUpTable()[j][0];
				if(taskTypeSelector <= taskTypePercentage){
					randomTaskType = j;
					break;
				}
			}
			if(randomTaskType == -1){
				SimLogger.printLine("Impossible is occured! no random task type!");
				continue;
			}
			
			taskTypeOfDevices[i] = randomTaskType;
			
			double poissonMean = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][2];
			double activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][3];
			double idlePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][4];
			double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
					SimSettings.CLIENT_ACTIVITY_START_TIME, 
					SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod);  //active period starts shortly after the simulation started (e.g. 10 seconds)
			double virtualTime = activePeriodStartTime;
			
			//Change: for the AR application, we use the model based on MR-Leo
			//To run the original task generator using the theoretical model, comment the if branch
			if (randomTaskType == 0) {//AR is the first element of the list
				
				//Video task
				//Periodic with interarrival time 33ms
				double interval_per = 33;
				while(virtualTime < simulationTime) {
					taskList.add(new TaskProperty(i,16, virtualTime, expRngList, expNorList));
					virtualTime += interval_per;
				}
				
				//User task
				//Poisson distributed with a mean of 5 min (=300000ms)
				ExponentialDistribution rng2 = new ExponentialDistribution(random, 300000);
				while(virtualTime < simulationTime) {
					double interval = rng2.sample();

					if(interval <= 0){
						SimLogger.printLine("Impossible is occured! interval is " + interval + " for device " + i + " time " + virtualTime);
						continue;
					}

					taskList.add(new TaskProperty(i,17, virtualTime, expRngList,expNorList));
					virtualTime += interval;
				}
				
			}
			else {
				//Original Sonmez code 
				ExponentialDistribution rng = new ExponentialDistribution(random, poissonMean);
				while(virtualTime < simulationTime) {

					double interval = rng.sample();

					if(interval <= 0){
						SimLogger.printLine("Impossible is occured! interval is " + interval + " for device " + i + " time " + virtualTime);
						continue;
					}

					if(virtualTime > activePeriodStartTime + activePeriod){
						activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
						virtualTime = activePeriodStartTime;
						continue;
					}
					taskList.add(new TaskProperty(i,randomTaskType, virtualTime, expRngList,expNorList));
					virtualTime += interval;
				}
			}
		}
	}

	@Override
	public int getTaskTypeOfDevice(int deviceId) {
		// TODO Auto-generated method stub
		return taskTypeOfDevices[deviceId];
	}

}
