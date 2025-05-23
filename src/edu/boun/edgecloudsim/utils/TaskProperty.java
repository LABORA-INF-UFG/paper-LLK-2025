/*
 * Title:        EdgeCloudSim - EdgeTask
 * 
 * Description: 
 * A custom class used in Load Generator Model to store tasks information
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.utils;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

import edu.boun.edgecloudsim.core.SimSettings;

public class TaskProperty {
    private double startTime;
    private long length, inputFileSize, outputFileSize;
    private int taskType;
    private int pesNumber;
    private int mobileDeviceId;
    
    public TaskProperty(double _startTime, int _mobileDeviceId, int _taskType, int _pesNumber, long _length, long _inputFileSize, long _outputFileSize) {
    	startTime=_startTime;
    	mobileDeviceId=_mobileDeviceId;
    	taskType=_taskType;
    	pesNumber = _pesNumber;
    	length = _length;
    	outputFileSize = _inputFileSize;
       	inputFileSize = _outputFileSize;
	}
    
    public TaskProperty(int _mobileDeviceId, int _taskType, double _startTime, ExponentialDistribution[][] expRngList, NormalDistribution[] expNorList) {
    	
    	if (_taskType == 16) {
    		//Hardcoded value for AR Video task
    		mobileDeviceId=_mobileDeviceId;
        	startTime=_startTime;
        	taskType=0;
        	
        	inputFileSize = 41;
        	outputFileSize =41;
        	double computationTypeSelector = SimUtils.getRandomDoubleNumber(0,100);
			int computationTypeCategory = 0;
			if(computationTypeSelector<= 47) {
				computationTypeCategory=0; //first case
			}
			else { 
				computationTypeCategory=1; //second case
			}
			
        	length = (long)expNorList[computationTypeCategory].sample();//MI
        	
        	pesNumber = 3;//Check the power of the cores
    	}
    	else if (_taskType == 17) {
    		//Hardcoded value for AR User task
    		mobileDeviceId=_mobileDeviceId;
        	startTime=_startTime;
        	taskType=0; 
        	
        	inputFileSize = 1;//OBS rounded up due to the constraints
        	outputFileSize =0;
        	length = 0;//MI see what to do
        	
        	pesNumber = 3;
    	}
    	else {
    		//Original Sonmez code
    		mobileDeviceId=_mobileDeviceId;
        	startTime=_startTime;
        	taskType=_taskType;
        	
        	inputFileSize = (long)expRngList[_taskType][0].sample();
        	outputFileSize =(long)expRngList[_taskType][1].sample();
        	length = (long)expRngList[_taskType][2].sample();
        	
        	pesNumber = (int)SimSettings.getInstance().getTaskLookUpTable()[_taskType][8];
    	}
	}
    
    public double getStartTime(){
    	return startTime;
    }
    
    public long getLength(){
    	return length;
    }
    
    public long getInputFileSize(){
    	return inputFileSize;
    }
    
    public long getOutputFileSize(){
    	return outputFileSize;
    }

    public int getTaskType(){
    	return taskType;
    }
    
    public int getPesNumber(){
    	return pesNumber;
    }
    
    public int getMobileDeviceId(){
    	return mobileDeviceId;
    }
}
