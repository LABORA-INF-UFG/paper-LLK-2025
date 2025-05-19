#from docplex.mp.model import Model
#import cplex
#import random as rd
import json
from classes import *
from tools import *
import os
import time
import sys

os.chdir(os.path.dirname(os.path.abspath(__file__)))

CONFIG_FILE = '../../../../../../../scripts/lotos/solver_configs/_conf.json'
EXCLUSION_FILE = '../../../../../../../scripts/lotos/solver_configs/_exclusion.json'
TASKS_FILE = '../../../../../../../scripts/lotos/solver_configs/_tasks.json'
VMS_FILE = '../../../../../../../scripts/lotos/solver_configs/_vms.json'
APS_FILE = '../../../../../../../scripts/lotos/solver_configs/_aps.json'



def config_data(v, combination, tasks, vms, dictionary, i, man, sim_time):

    delta_final, delta_inicial, cpu_max, ram_max = algorithm1(vms[v], combination, tasks)

    dictionary['conf'][i] = {}
    dictionary['conf'][i]['id'] = i
    dictionary['conf'][i]['cost'] = len(combination)
    dictionary['conf'][i]['vm'] = v
    dictionary['conf'][i]['tasks'] = list(combination)
    dictionary['conf'][i]['cpu'] = cpu_max
    dictionary['conf'][i]['ram'] = ram_max
    
    wlan = {}
    proc_t = {}
    comu_t = {}
    wait_t = {}
    users_in_aps = {}

    count_man_usage = 0
    
    for c in combination:

        if tasks[c].ap not in wlan:
            wlan[tasks[c].ap] = 0
            users_in_aps[tasks[c].ap] = []

        if tasks[c].user_id not in users_in_aps[tasks[c].ap]:
            users_in_aps[tasks[c].ap].append(tasks[c].user_id)
            wlan[tasks[c].ap] = wlan[tasks[c].ap] + 1
            
        proc_t[c] = float(delta_final[c]) - float(delta_inicial[c])
        wait_t[c] = tasks[c].waiting_time


    dictionary['conf'][i]['wlan'] = wlan

    if vms[v].typ == 'Cloud':
        dictionary['conf'][i]['wan'] = wlan
    else:
        dictionary['conf'][i]['wan'] = []

    mean_task_size_ul = 0
    mean_task_size_dl = 0

    for c in combination:

        if tasks[c].ap != vms[v].ap:
            count_man_usage = count_man_usage + 1

        mean_task_size_ul = mean_task_size_ul + tasks[c].upload_size
        mean_task_size_dl = mean_task_size_dl + tasks[c].download_size

    mean_task_size_ul = mean_task_size_ul/len(combination)
    mean_task_size_dl = mean_task_size_dl/len(combination)
    
    dictionary['conf'][i]['man'] = count_man_usage

    comu_t = calculate_com_time(combination, vms[v], tasks, 
                                dictionary['conf'][i]['wlan'], dictionary['conf'][i]['wan'], dictionary['conf'][i]['man'],
                                mean_task_size_ul, mean_task_size_dl, man, proc_t, delta_inicial, sim_time, wait_t)

    if comu_t == None:
        del dictionary['conf'][i]
        return dictionary

    dictionary['conf'][i]['processing_time'] = proc_t
    dictionary['conf'][i]['comunication_time'] = comu_t
    dictionary['conf'][i]['waiting_time'] = wait_t

    dictionary['conf'][i]['cost_initialize'] = vms[v].cost_initialize
    dictionary['conf'][i]['cost_per_time'] = vms[v].cost_per_time

    verification_delay = True
    for c in dictionary['conf'][i]['processing_time']:
        if dictionary['conf'][i]['processing_time'][c] + dictionary['conf'][i]['comunication_time'][c] + dictionary['conf'][i]['waiting_time'][c] > tasks[c].delay_limit:
            verification_delay = False

    if cpu_max > vms[v].cpu_capacity or ram_max > vms[v].ram_capacity or verification_delay == False:
        del dictionary['conf'][i]

    return dictionary
    
def create_config(tasks, users, aps, vms, man, sim_time):
    dictionary = {}
    dictionary['conf'] = {}

    exclusion = {}
    exclusion['exclusion'] = {}

    for t in tasks:
        exclusion['exclusion'][t] = []

    i = 1
    comb = generate_combinations(tasks)[1:]

    
    for v in vms:
        for c in comb:
            for c_item in c:
                exclusion['exclusion'][c_item].append(i)
            conf = config_data(v, c, tasks, vms, dictionary, i, man, sim_time)
            i = i + 1

    json_object = json.dumps(dictionary, indent=4)
    with open(CONFIG_FILE, "w") as outfile:
        outfile.write(json_object)

        
    json_object = json.dumps(exclusion, indent=4)
    with open(EXCLUSION_FILE, "w") as outfile2:
        outfile2.write(json_object)
    

if __name__ == '__main__':
    start = time.time()

    tasks, users, sim_time = read_tasks()

    vms = read_vms()

    aps = read_aps()

    man = read_man()


    create_config(tasks, users, aps, vms, man, sim_time)

    final = time.time()

    record_time_of_solution("create_config=", final - start, sys.argv[1])






