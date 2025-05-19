import itertools
from classes import *
import copy

man_prop_delay = 5
wan_prop_delay = 0
wan_band_values = {1:20703.973, 2:12023.957, 3:9887.785, 4:8915.775, 5:8259.277, 6:7560.574, 7:7262.140, 8:7155.361, 9:7041.153, 10:6994.595, 
                    11:6653.232, 12:6111.868, 13:5570.505, 14:5029.142, 15:4487.779, 16:3899.729, 17:3311.680, 18:2723.631, 19:2135.582, 20:1547.533, 
                    21:1500.252, 22:1452.972, 23:1405.692, 24:1358.411, 25:1311.131}

wlan_band_values = {1:88040.279, 2:45150.982, 3:30303.641, 4:27617.211, 5:24868.616, 6:22242.296, 7:20524.064, 8:18744.889, 9:17058.827, 10:15690.455, 
                    11:14127.744, 12:13522.408 , 13:13177.631 , 14:12811.330, 15:12584.387, 16:11705.638, 17:11276.116, 18:10846.594, 19:10417.071, 20:9987.549, 
                    21:9367.587, 22:8747.625, 23:8127.663, 24:7907.701, 25:7887.739, 26:7690.831, 27:7393.922, 28:7297.014, 29:7100.106, 30:6903.197, 
                    31:6701.986, 32:6500.776, 33:6399.565, 34:6098.354, 35:5897.143, 36:5552.127, 37:5207.111, 38:4862.096, 39:4517.080, 40:4172.064, 
                    41:4092.922, 42:4013.781, 43:3934.639, 44:3855.498, 45:3776.356, 46:3697.215, 47:3618.073, 48:3538.932, 49:3459.790, 50:3380.649,
                    51:3274.611, 52:3168.573, 53:3062.536, 54:2956.498, 55:2850.461, 56:2744.423, 57:2638.386, 58:2532.348, 59:2426.310, 60:2320.273, 
                    61:2283.828, 62:2247.383, 63:2210.939, 64:2174.494, 65:2138.049, 66:2101.604, 67:2065.160, 68: 2028.715, 69:1992.270 , 70:1955.825, 
                    71:1946.788, 72:937.751, 73:1928.714, 74:1919.677, 75:1910.640, 76:1901.603, 77:1892.566, 78:1883.529, 79:1874.492, 80:1865.455,
                    81:1833.185, 82:1800.915, 83:1768.645, 84:1736.375, 85:1704.106, 86:1671.836, 87:1639.566, 88:1607.296, 89:1575.026, 90:1542.756, 
                    91:1538.544, 92:1534.331, 93:1530.119, 94:1525.906, 95:1521.694, 96:1517.481, 97:1513.269, 98:1509.056, 99:1504.844, 100:1500.631}

man_band_values = 1300*1024

def generate_combinations(input_set):
    all_combinations = []
    for r in range(len(input_set) + 1):
        combinations = itertools.combinations(input_set, r)
        all_combinations.extend(combinations)
    return all_combinations



def return_delta_inicial(item, tasks):
    return tasks[item].delta_inicial



def return_remaining_demand(item, tasks):
    return tasks[item].cores_demand * tasks[item].millions_of_instructions



def calculate_cpu_ram(concorrent, tasks, v):
    cpu = 0
    ram = 0
    for item in concorrent:
        if v.typ == "Edge":
            cpu = cpu + tasks[item].processing_demand_edge
        elif v.typ == "Cloud":
            cpu = cpu + tasks[item].processing_demand_cloud
        ram = ram + tasks[item].ram_demand
    return cpu, ram


def return_wan_band(users_in_ap):
    if users_in_ap in wan_band_values:
        return wan_band_values[users_in_ap]
    else:
        return 0


def return_man_band():
    return 14


def return_wlan_band(users_in_ap):
    if users_in_ap in wlan_band_values:
        return wlan_band_values[users_in_ap]
    else:
        return 0


def calculate_wlan(task, wlan):
    return ((task.upload_size*8) + (task.download_size*8)) / (return_wlan_band(wlan[task.ap])*3)

def calculate_man(man, mean_task_size_ul, mean_task_size_dl, man_info):

    avgTaskSize = man_info["avg_download"] * 8
    PoissonMean = man_info["poisson_dl"]
    bandwidth = man_info["man_bandwidth"]
    deviceCount = man_info["dev_count"]

    lamda = (1 / PoissonMean)
    mu = bandwidth / avgTaskSize
    result = 1 / (mu-(lamda*deviceCount))


    avgTaskSize = man_info["avg_upload"] * 8
    PoissonMean = man_info["poisson_ul"]

    lamda = (1 / PoissonMean)
    mu = bandwidth / avgTaskSize
    result = result + (1 / (mu-(lamda*deviceCount)))

    result = result + 2*man_prop_delay

    return result


def calculate_wan(task, wan):
    return (((task.upload_size*8) + (task.download_size*8)) / return_wan_band(wan[task.ap])) + wan_prop_delay


def calculate_com_time(combination, vm, tasks, wlan, wan, man, mean_task_size_ul, mean_task_size_dl, man_info, proc_t, delta_inicial, sim_time, wait_t):
    com_time = {}
    uncomplete_task_signal = False

    for item in combination:
        
        if tasks[item].ap == vm.ap and vm.typ == 'Edge':
            com_time[item] = calculate_wlan(tasks[item], wlan)

        elif tasks[item].ap != vm.ap and vm.typ == 'Edge':
            com_time[item] = calculate_wlan(tasks[item], wlan) + calculate_man(man, mean_task_size_ul, mean_task_size_dl, man_info)

        elif vm.typ == 'Cloud':
            com_time[item] = calculate_wan(tasks[item], wan)

        if delta_inicial[item] + proc_t[item] + com_time[item] + wait_t[item] >= sim_time:
            uncomplete_task_signal = True
    
    if uncomplete_task_signal:
        return None
    else:
        return com_time

    

def algorithm2(concorrent, v, delta_dif, delta_atual, delta_final, tasks, cpt, capacity):
    concorrent_copy = concorrent
    cpu, ram = calculate_cpu_ram(concorrent, tasks, v)
    t = 0
    while(t < len(concorrent_copy)):

        capacity = cpt/(max(cpt, tasks[concorrent_copy[t]].cores_demand))*v.millions_of_instructions
        
        
        if float(format(delta_dif * capacity, '.4f')) >= float(format(tasks[concorrent_copy[t]].millions_of_instructions, '.4f')):
            
            delta_final[concorrent_copy[t]] = delta_atual + float(format(tasks[concorrent_copy[t]].millions_of_instructions, '.4f')) / float(format(capacity, '.4f'))
            concorrent.remove(concorrent[t])
            t = t - 1
            
        else:
            tasks[concorrent[t]].millions_of_instructions = float(format(tasks[concorrent[t]].millions_of_instructions - (delta_dif * capacity), '.4f'))

        t = t + 1

    return delta_final, tasks, concorrent, delta_atual + delta_dif, cpu, ram




def algorithm1(v, combination, t):
    tasks = copy.deepcopy(t)
    delta_final = {}
    sorted_items = sorted(combination, key=lambda x: return_delta_inicial(x, tasks))
    

    delta_atual = return_delta_inicial(sorted_items[0], tasks)
    concorrent = []
    cpu_max = 0
    ram_max = 0

    k = 0
    i = sorted_items[k]
    if len(sorted_items) >= 2:
        j = sorted_items[k + 1]
    
    concorrent.append(i)

    while (k != len(sorted_items)-1):
        delta_dif = return_delta_inicial(j, tasks) - return_delta_inicial(i, tasks)

        lowest_workload = min(concorrent, key=lambda x: return_remaining_demand(x, tasks))
        cpt = v.cores / (len(concorrent) + v.legacy_tasks )
        capacity = cpt/(max(cpt, tasks[lowest_workload].cores_demand))*v.millions_of_instructions
        betta_dif = tasks[lowest_workload].millions_of_instructions / capacity

        while(len(concorrent) !=0 and (delta_atual + betta_dif) < return_delta_inicial(j, tasks)):
            delta_final, tasks, concorrent, delta_atual, cpu, ram = algorithm2(concorrent, v, betta_dif, delta_atual, delta_final, tasks, cpt, capacity)

            if cpu > cpu_max:
                cpu_max = cpu
                
            if ram > ram_max:
                ram_max = ram
            
            if (len(concorrent) !=0):
                lowest_workload = min(concorrent, key=lambda x: return_remaining_demand(x, tasks))
                cpt = v.cores / (len(concorrent) + v.legacy_tasks )
                capacity = cpt/(max(cpt, tasks[lowest_workload].cores_demand))*v.millions_of_instructions
                betta_dif = tasks[lowest_workload].millions_of_instructions / capacity
        
        if (len(concorrent) !=0):    
            delta_final, tasks, concorrent, delta_atual, cpu, ram = algorithm2(concorrent, v, return_delta_inicial(j, tasks) - delta_atual, delta_atual, delta_final, tasks, cpt, capacity)

            if cpu > cpu_max:
                cpu_max = cpu
                
            if ram > ram_max:
                ram_max = ram
        
        if delta_atual < return_delta_inicial(j, tasks):
            delta_atual = return_delta_inicial(j, tasks)


        concorrent.append(j)
        k = k + 1
        i = sorted_items[k]
        if k+1 != len(sorted_items):
            j = sorted_items[k + 1]
    
    while (len(concorrent)!=0):

        sorted_items = sorted(concorrent, key=lambda x: return_remaining_demand(x, tasks))

        cpt = v.cores / (len(concorrent) + v.legacy_tasks )
        capacity = cpt/(max(cpt, tasks[sorted_items[0]].cores_demand))*v.millions_of_instructions
        delta_dif = tasks[sorted_items[0]].millions_of_instructions / capacity

        delta_final, tasks, concorrent, delta_atual, cpu, ram = algorithm2(concorrent, v, delta_dif, delta_atual, delta_final, tasks, cpt, capacity)

        if cpu > cpu_max:
            cpu_max = cpu
            
        if ram > ram_max:
            ram_max = ram
    delta_inicial = {}

    for it in combination:
        delta_inicial[it] = tasks[it].delta_inicial

    if v.legacy_tasks == 0:
        ram_max = ram_max + 1300
    return delta_final, delta_inicial, cpu_max, ram_max
    

def record_time_of_solution(type_of_record, time_of_execution, instance):

    file1 = open("../../../../../../../scripts/lotos/solver_solutions/" + str(instance) + "_" + "time", "a")

    file1.writelines(type_of_record + str(time_of_execution) + "\n")
    file1.close()