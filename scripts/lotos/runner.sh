#!/bin/sh

script_root_path="$(dirname "$(readlink -f "$0")")"
#echo $script_root_path #OK
simulation_out_folder=$1
scenario_name=$2
edge_devices_file=$3
applications_file=$4
iteration_number=$5

scenario_out_folder=${simulation_out_folder}/${scenario_name}/ite${iteration_number}
scenario_conf_file=${script_root_path}/config/${scenario_name}.properties
echo $scenario_conf_file 
scenario_edge_devices_file=${script_root_path}/config/${edge_devices_file}
scenario_applications_file=${script_root_path}/config/${applications_file}
#echo $scenario_applications_file 

mkdir -p $scenario_out_folder
java -classpath '../../bin:../../lib/cloudsim-4.0.jar:../../lib/commons-math3-3.6.1.jar:../../lib/jFuzzyLogic_v3.0.jar:../../lib/colt.jar:../../lib/json-20240303.jar' edu.boun.edgecloudsim.applications.lotos.LOTOSMainApp $scenario_conf_file $scenario_edge_devices_file $scenario_applications_file $scenario_out_folder $iteration_number >> ${scenario_out_folder}.log
#tar -czf ${scenario_out_folder}.tar.gz -C $simulation_out_folder/${scenario_name} ite${iteration_number}
#rm -rf $scenario_out_folder
