import json

CONFIG_FILE = '../../../../../../../scripts/lotos/solver_configs/_conf.json'
EXCLUSION_FILE = '../../../../../../../scripts/lotos/solver_configs/_exclusion.json'
TASKS_FILE = '../../../../../../../scripts/lotos/solver_configs/_tasks.json'
VMS_FILE = '../../../../../../../scripts/lotos/solver_configs/_vms.json'
APS_FILE = '../../../../../../../scripts/lotos/solver_configs/_aps.json'
MAN_FILE = '../../../../../../../scripts/lotos/solver_configs/_man.json'

class Config:
    def __init__(self, conf_id, cost, vm, tasks, cpu, ram, wlan, wan, man, processing_time, comunication_time, cost_initialize, cost_per_time, waiting_time):
        self.id = conf_id
        self.cost = cost
        self.vm = vm
        self.tasks = tasks
        self.cpu = cpu
        self.ram = ram
        self.wlan = wlan
        self.wan = wan
        self.man = man
        self.processing_time = processing_time
        self.comunication_time = comunication_time
        self.cost_initialize = cost_initialize
        self.cost_per_time = cost_per_time
        self.waiting_time = waiting_time

class Exclusion:
    def __init__(self, excs_id, exclusion):
        self.id = excs_id
        self.exclusion = exclusion
        
class Task:
    def __init__(self, task_id, user_id, processing_demand_edge, processing_demand_cloud, ram_demand, upload_size, download_size, cores_demand, millions_of_instructions, ap, delay_limit, delta_inicial, waiting_time):
        self.id = task_id
        self.user_id = user_id
        self.processing_demand_edge = processing_demand_edge
        self.processing_demand_cloud = processing_demand_cloud
        self.ram_demand = ram_demand
        self.upload_size = upload_size
        self.download_size = download_size
        self.cores_demand = cores_demand
        self.millions_of_instructions = millions_of_instructions
        self.ap = ap
        self.delay_limit = delay_limit
        self.delta_inicial = delta_inicial
        self.waiting_time = waiting_time
        
class VM:
    def __init__(self, vm_id, cpu_capacity, ram_capacity, cores, millions_of_instructions, ap, typ, cost_initialize, cost_per_time, legacy_tasks):
        self.id = vm_id
        self.cpu_capacity = cpu_capacity
        self.ram_capacity = ram_capacity
        self.cores = cores
        self.millions_of_instructions = millions_of_instructions
        self.ap = ap
        self.typ = typ
        self.cost_initialize = cost_initialize
        self.cost_per_time = cost_per_time
        self.legacy_tasks = legacy_tasks
        
class AP:
    def __init__(self, ap_id, wlan_capacity, wan_capacity):
        self.id = ap_id
        self.wlan_capacity = wlan_capacity
        self.wan_capacity = wan_capacity



def read_vms():
    vms_list  = {}

    with open(VMS_FILE) as json_file:
        vms = json.load(json_file)
        vms = vms["vms"]

        for item in vms:
            vm_id = vms[item]['vm_id']
            cpu_capacity = vms[item]['cpu_capacity']
            ram_capacity = vms[item]['ram_capacity']
            cores = vms[item]['cores']
            millions_of_instructions = vms[item]['millions_of_instructions']
            ap = vms[item]['ap']
            typ = vms[item]['type']
            cost_initialize = vms[item]['cost_initialize']
            cost_per_time = vms[item]['cost_per_time']
            legacy_tasks = vms[item]['legacy_tasks']
            
            vm = VM(vm_id, cpu_capacity, ram_capacity, cores, millions_of_instructions, ap, typ, cost_initialize, cost_per_time, legacy_tasks)

            vms_list['v' + str(vm_id)] = vm

    return vms_list


def read_aps():
    aps_list  = {}

    with open(APS_FILE) as json_file:
        aps = json.load(json_file)

        for item in aps["aps"]:
            ap_id = aps["aps"][item]['ap_id']
            wlan_capacity = aps["aps"][item]['wlan_capacity']
            wan_capacity = aps["aps"][item]['wan_capacity']

            ap = AP(ap_id, wlan_capacity, wan_capacity)

            aps_list[str(ap_id)] = ap

        man_capacity = aps['man_capacity']
    return aps_list, man_capacity

def read_man():
    man_info  = {}

    with open(MAN_FILE) as json_file:
        man_json = json.load(json_file)
        man_info["dev_count"] = man_json["man"]["dev_count"]
        man_info["avg_download"] = man_json["man"]["avg_download"]
        man_info["poisson_dl"] = man_json["man"]["poisson_dl"]
        man_info["poisson_ul"] = man_json["man"]["poisson_ul"]
        man_info["avg_upload"] = man_json["man"]["avg_upload"]
        man_info["man_bandwidth"] = man_json["man"]["man_bandwidth"]

    return man_info



def read_config():
    config_list  = {}
    with open(CONFIG_FILE) as json_file:
        cfgs = json.load(json_file)
        cfgs = cfgs["conf"]

        for item in cfgs:
            conf_id = cfgs[item]['id']
            cost = cfgs[item]['cost']
            vm = cfgs[item]['vm']
            tasks = cfgs[item]['tasks']
            cpu = cfgs[item]['cpu']       
            ram = cfgs[item]['ram']
            wlan = cfgs[item]['wlan']
            wan = cfgs[item]['wan']
            man = cfgs[item]['man']
            processing_time = cfgs[item]['processing_time']
            comunication_time = cfgs[item]['comunication_time']
            waiting_time = cfgs[item]['waiting_time']
            cost_initialize = cfgs[item]['cost_initialize']
            cost_per_time = cfgs[item]['cost_per_time']

            cfg = Config(conf_id, cost, vm, tasks, cpu, ram, wlan, wan, man, processing_time, comunication_time, cost_initialize, cost_per_time, waiting_time)

            config_list['c' + str(conf_id)] = cfg

    return config_list



def read_exclusions():
    exclusion_list  = {}

    with open(EXCLUSION_FILE) as json_file:
        excs = json.load(json_file)

        excs = excs["exclusion"]

        for item in excs:
            exc_id = item
            exclusion = excs[item]

            exc = Exclusion(exc_id, exclusion)

            exclusion_list[item] = exc


    return exclusion_list



def read_tasks():
    tasks_list  = {}
    user_list = {}
    
    with open(TASKS_FILE) as json_file:
        tasks = json.load(json_file)

        sim_time = tasks["simulation_time"]
        tasks = tasks["tasks"]
        
        for item in tasks:
            task_id = tasks[item]['task_id']
            user_id = tasks[item]['user_id']
            user_list['u' + str(user_id)] = tasks[item]['ram_demand']
            processing_demand_edge = tasks[item]['processing_demand_edge']
            processing_demand_cloud = tasks[item]['processing_demand_cloud']
            ram_demand = tasks[item]['ram_demand']
            upload_size = tasks[item]['upload_size']
            download_size = tasks[item]['download_size']
            cores_demand = tasks[item]['cores_demand']
            millions_of_instructions = tasks[item]['millions_of_instructions']
            ap = tasks[item]['ap']
            delay_limit = tasks[item]['delay_limit']
            delta_inicial = tasks[item]['delta_inicial']
            waiting_time = tasks[item]['waiting_time']
            
            task = Task(task_id, user_id, processing_demand_edge, processing_demand_cloud, ram_demand, upload_size, download_size, cores_demand, millions_of_instructions, ap, delay_limit, delta_inicial, waiting_time)

            tasks_list['t' + str(task_id)] = task

    return tasks_list, user_list, sim_time