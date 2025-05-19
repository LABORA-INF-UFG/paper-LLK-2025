import matplotlib.pyplot as plt  # type: ignore
import csv 
import pandas as pd # type: ignore
import prefix
import numpy as np # type: ignore
import matplotlib.patches as mpatches # type: ignore

plt.rcParams["font.family"] = "Ubuntu"

fig, (ax1,ax2, ax3, ax4) = plt.subplots(1,4,figsize=(15,15))

solutions = [prefix.prefixOPT, prefix.prefixFuzzy, prefix.prefixUTIL, prefix.prefixCOST]
sol_to_ax = {prefix.prefixOPT:ax1, prefix.prefixFuzzy:ax2, prefix.prefixUTIL:ax3, prefix.prefixCOST:ax4}
sol_to_name = {prefix.prefixOPT:"LOTOS", prefix.prefixFuzzy:"Fuzzy [Sonmez et al.]", prefix.prefixUTIL:"UTIL.", prefix.prefixCOST:"MIN. COST"}
sol_to_name_pos_x = {prefix.prefixOPT:-0.4, prefix.prefixFuzzy:-0.9, prefix.prefixUTIL:-0.2, prefix.prefixCOST:-0.5}
sol_to_name_pos_y = {prefix.prefixOPT:-1.1, prefix.prefixFuzzy:-1.1, prefix.prefixUTIL:-1.1, prefix.prefixCOST:-1.1}
users = prefix.users

def format(x):
    if x > 4:
        return '{:.1f}%'.format(x)
    else:
        return ''

def find_lines_with_string(solution_type, search_string):
    service_time = 0
    for item in users:
        filename = solution_type + str(item) + "DEVICES_ALL_APPS_GENERIC.log"
        with open(filename, 'r') as file:
            for line in file:
                if search_string in line:
                    service_time = service_time + float(line.strip().split("=")[1])

    return service_time

def plot(solution):
    
    comp = {}
    uncompleted = {}
    delay = {}
    rejected_solver = {}
    vm_cap = {}
    mob = {}
    wlan_cov = {}
    ram_cap = {}
    bw = {}

    labels = {}
    explode = {}
    sizes = {}
    colors = {}

    all_labels = []
    all_colors = []



    for solution in solutions:
        comp[solution] = find_lines_with_string(solution, 'total_completed_tasks')
        uncompleted[solution] = find_lines_with_string(solution, 'total_uncompleted_tasks')
        delay[solution] = find_lines_with_string(solution, 'error_tasks_due_to_delay_limit')
        rejected_solver[solution] = find_lines_with_string(solution, 'rejected_tasks_by_solver')
        vm_cap[solution] = find_lines_with_string(solution, 'total_failed_tasks_due_to_vm_capacity')
        mob[solution] = find_lines_with_string(solution, 'failed_tasks_due_to_mobility')
        wlan_cov[solution] = find_lines_with_string(solution, 'rejected_tasks_due_to_wlan_range')
        ram_cap[solution] = find_lines_with_string(solution, 'error_tasks_due_to_ram_capacity')
        bw[solution] = find_lines_with_string(solution, 'failed_tasks_due_to_bw')


        labels[solution] = []
        explode[solution] = ()
        sizes[solution] = []
        colors[solution] = []


        if comp[solution] != 0:
            explode[solution] = explode[solution] + (0.1,)
            sizes[solution].append(comp[solution])
            colors[solution].append('#3399ff')
            if 'Completed' not in all_labels:
                all_labels.append('Completed')
                all_colors.append('#3399ff')

        if uncompleted[solution] != 0:
            explode[solution] = explode[solution] + (0,)
            sizes[solution].append(uncompleted[solution])
            colors[solution].append('#ff9900')
            if 'Uncompleted' not in all_labels:
                all_labels.append('Uncompleted')
                all_colors.append('#ff9900')

        if delay[solution] != 0:
            explode[solution] = explode[solution] + (0,)
            sizes[solution].append(delay[solution])
            colors[solution].append('#669900')
            if 'Delay limit' not in all_labels:
                all_labels.append('Delay limit')
                all_colors.append('#669900')

        if rejected_solver[solution] != 0:
            explode[solution] = explode[solution] + (0,)
            sizes[solution].append(rejected_solver[solution])
            colors[solution].append('#ff5050')
            if 'Rejected by Solver' not in all_labels:
                all_labels.append('Rejected by Solver')
                all_colors.append('#ff5050')

        if vm_cap[solution] != 0:
            explode[solution] = explode[solution] + (0,)
            sizes[solution].append(vm_cap[solution])
            colors[solution].append('#ff00ff')
            #if 'CPU error' not in all_labels:
            #    all_labels.append('CPU error')
            #    all_colors.append('#ff00ff')

        if mob[solution] != 0:
            explode[solution] = explode[solution] + (0,)
            sizes[solution].append(mob[solution])
            colors[solution].append('#737373')
            #if 'Mobility Error' not in all_labels:
            #    all_labels.append('Mobility Error')
            #    all_colors.append('#737373')

        if wlan_cov[solution] != 0:
            explode[solution] = explode[solution] + (0,)
            sizes[solution].append(wlan_cov[solution])
            colors[solution].append('#009999')
            #if 'WLAN coverage' not in all_labels:
            #    all_labels.append('WLAN coverage')
            #    all_colors.append('#009999')

        if ram_cap[solution] != 0:
            explode[solution] = explode[solution] + (0,)
            sizes[solution].append(ram_cap[solution])
            colors[solution].append('#9966ff')
            if 'RAM capacity' not in all_labels:
                all_labels.append('RAM capacity')
                all_colors.append('#9966ff')

        if bw[solution] != 0:
            explode[solution] = explode[solution] + (0,)
            sizes[solution].append(bw[solution])
            colors[solution].append('#e6b800')
            if 'Network error' not in all_labels:
                all_labels.append('Network error')
                all_colors.append('#e6b800')


        sol_to_ax[solution].pie(sizes[solution], explode=explode[solution], autopct=format, textprops={'fontsize': 16}, colors=colors[solution])
        sol_to_ax[solution].text(sol_to_name_pos_x[solution], sol_to_name_pos_y[solution], sol_to_name[solution], va='top', fontsize=16)

        sol_to_ax[solution].grid(axis = 'both', linestyle='--', linewidth=0.6)


    bbox = (0.8, 0.6)
    #print(all_labels)
    #print(all_colors)
    blue_patch = []
    for x, y in zip(all_labels, all_colors):
        blue_patch.append(mpatches.Patch(color=y, label=x))

    fig.legend(loc='center right', handles=blue_patch, fontsize=14, bbox_to_anchor=bbox, ncol=6, handletextpad=0.4, columnspacing=1.95) 

    plt.subplots_adjust(wspace=0, hspace=0)
    plt.savefig("graficos/4.pdf", bbox_inches='tight')
    plt.savefig("graficos/PNG/4.png", dpi=300, bbox_inches='tight')

if __name__ == '__main__':
    plot(prefix.prefixOPT)