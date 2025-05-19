from docplex.mp.model import Model # type: ignore
import json
from classes import *
import os
import time
from tools import *
import sys

os.chdir(os.path.dirname(os.path.abspath(__file__)))

def Mutual_Constraints(mdl, c, configs, vms, aps, man_capacity, exclusion, tasks, users):
    #------------------Restrição de processamento-------------------
    for v in vms:
        mdl.add_constraint(mdl.sum(mdl.c[it] * configs[it].cpu for it in c if configs[it].vm == v) 
                                                                               <= vms[v].cpu_capacity, 'processing_capacity')
    #----------------------------------------------------------------



    #-----------------------Restrição de RAM-------------------------
    for v in vms:
        mdl.add_constraint(mdl.sum(mdl.c[it] * configs[it].ram for it in c if configs[it].vm == v) 
                                                                               <= vms[v].ram_capacity, 'ram_capacity')
    #----------------------------------------------------------------



    #--------------------Restrição de banda wlan---------------------
    for ap in aps:
        mdl.add_constraint(mdl.sum(mdl.c[it]*configs[it].wlan[ap] for it in c if ap in configs[it].wlan) 
                                                                               <= aps[ap].wlan_capacity, 'wlan_capacity')
    #----------------------------------------------------------------



    
    #--------------------Restrição de banda wan---------------------
    for ap in aps:
        mdl.add_constraint(mdl.sum(mdl.c[it]*configs[it].wan[ap] for it in c if ap in configs[it].wan) 
                                                                               <= aps[ap].wan_capacity, 'wan_capacity')
    #----------------------------------------------------------------


    

    #--------------------Restrição de banda man---------------------
    mdl.add_constraint(mdl.sum(mdl.c[it]*configs[it].man for it in c) <= man_capacity, 'man_capacity')
    #----------------------------------------------------------------


    #----------------------Restrição de delay------------------------
    for t in tasks:
        mdl.add_constraint(mdl.sum(mdl.c[it]*(configs[it].processing_time[t] + configs[it].comunication_time[t] + configs[it].waiting_time[t]) for it in c if t in configs[it].tasks) 
                                                                               <= tasks[t].delay_limit, 'delay_limit')
    #----------------------------------------------------------------
    

    #---------------------Restrição de exclusão----------------------
    for excs in exclusion:
        mdl.add_constraint(mdl.sum(mdl.c[it] for it in c if excs in configs[it].tasks) <= 1, 'exclusion')
    #----------------------------------------------------------------


    #--------------------Restrição de consistência-------------------
    for v in vms:
        for it in c:
            mdl.add_constraint(mdl.sum(mdl.c[it] for it in c if configs[it].vm == v) <= 1, 'consistency')
    #----------------------------------------------------------------


def record_solution(Stage, S1_vars, configs):
    dictionary = {}
    dictionary['solution'] = {}
    
    for item in S1_vars:
        for t in configs[item].tasks:
            dictionary['solution'][t[1]] = configs[item].vm[1:]

    json_object = json.dumps(dictionary, indent=4)


    with open("../../../../../../../scripts/lotos/solver_solutions/" + Stage + "_sol.json", "w") as outfile:
        outfile.write(json_object)



            

def S1(configs, vms, aps, man_capacity, exclusion, tasks, users):

    mdl = Model(name='Task orchestration', log_output=False)
    mdl.parameters.mip.tolerances.absmipgap = 1
    
    
    #--------------Criação de variável de decisão c -----------------
    c = ['c' + str(configs[c].id) for c in configs]
    mdl.c = mdl.binary_var_dict(keys=c, name='C')
    #----------------------------------------------------------------


    #------------------Função objetivo------------------------------
    fo = mdl.sum(mdl.c[it]*configs[it].cost for it in c)
    mdl.maximize(fo)
    #----------------------------------------------------------------
    
    Mutual_Constraints(mdl, c, configs, vms, aps, man_capacity, exclusion, tasks, users)

    #-----------------------------Solve------------------------------
    mdl.solve()
    #print(mdl.solution)
    #print(mdl.solve_details)
    #print(mdl.statistics)
    mdl.export_as_lp("../../../../../../../scripts/lotos/solver_solutions/model_s1.lp")
    #----------------------------------------------------------------


    
    #--------------------------Warm Start----------------------------
    S1_vars = []
    for it in c:
        if mdl.c[it].solution_value > 0:
            S1_vars.append(it)
    #----------------------------------------------------------------


    record_solution("S1", S1_vars, configs)
    return mdl.solution.get_objective_value(), S1_vars

def S2(S1_Solution, S1_vars, configs, vms, aps, man_capacity, exclusion, tasks, users):

    mdl = Model(name='Minimize cost', log_output=False)
    mdl.parameters.mip.tolerances.absmipgap = 1
    
    
    #--------------Criação de variável de decisão c -----------------
    c = ['c' + str(configs[c].id) for c in configs]
    mdl.c = mdl.binary_var_dict(keys=c, name='C')
    #----------------------------------------------------------------
    

    #------------------Função objetivo------------------------------
    fo = mdl.sum(mdl.c[it]*configs[it].cost_initialize  for it in c) + mdl.sum(mdl.c[it]*configs[it].cost_per_time * (configs[it].processing_time[task] + configs[it].comunication_time[task]) for it in c for task in configs[it].tasks)
    mdl.minimize(fo)
    #----------------------------------------------------------------
    
    Mutual_Constraints(mdl, c, configs, vms, aps, man_capacity, exclusion, tasks, users)  

    #------------------Manter resultado do Stage 1-------------------
    mdl.add_constraint(mdl.sum(mdl.c[it]*configs[it].cost for it in c) == S1_Solution, 'Stage 1 solution')
    #----------------------------------------------------------------



    #--------------------------Warm Start----------------------------
    warm_start = mdl.new_solution()
    has_warm = False
    for it in S1_vars:
        has_warm = True
        warm_start.add_var_value(mdl.c[it], 1)
    if has_warm:
        mdl.add_mip_start(warm_start)
    #----------------------------------------------------------------

    

    #-----------------------------Solve------------------------------
    mdl.solve()
    #print(mdl.solution)
    #print(mdl.solve_details)
    #print(mdl.statistics)
    mdl.export_as_lp("../../../../../../../scripts/lotos/solver_solutions/model_s2.lp")
    #----------------------------------------------------------------



    #--------------------------Warm Start----------------------------
    S2_vars = []
    for it in c:
        if mdl.c[it].solution_value > 0:
            S2_vars.append(it)
    #----------------------------------------------------------------

    record_solution("S2", S2_vars, configs)

def model():

    configs = read_config()

    vms = read_vms()

    aps, man_capacity = read_aps()

    exclusion = read_exclusions()

    tasks, users, _ = read_tasks()

    start_f1 = time.time()
    
    S1_Solution, S1_vars = S1(configs, vms, aps, man_capacity, exclusion, tasks, users)

    start_f2 = time.time()
    
    S2(S1_Solution, S1_vars, configs, vms, aps, man_capacity, exclusion, tasks, users)

    final = time.time()

    record_time_of_solution("F1=", start_f2 - start_f1, sys.argv[1])
    record_time_of_solution("F2=", final - start_f2, sys.argv[1])

if __name__ == '__main__':
    
    model()

