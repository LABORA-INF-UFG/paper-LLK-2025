import matplotlib.pyplot as plt  # type: ignore
import csv 
import pandas as pd # type: ignore
import prefix
import numpy as np # type: ignore

plt.rcParams["font.family"] = "Ubuntu"
plt.rcParams["figure.figsize"] = [6, 3]

users = prefix.users

def find_lines_with_string(solution_type, search_string):
    service_time = []

    for item in users:
        filename = solution_type + str(item) + "DEVICES_ALL_APPS_GENERIC.log"
        with open(filename, 'r') as file:
            for line in file:
                if search_string in line:
                    service_time.append(round(float(line.strip().split("=")[1]), 5))

    return service_time

def plot():

    costFuzzy = find_lines_with_string(prefix.prefixFuzzy, 'cost_of_vm_utilization')
    completedFuzzy = find_lines_with_string(prefix.prefixFuzzy, 'total_completed_tasks')

    costOPT = find_lines_with_string(prefix.prefixOPT, 'cost_of_vm_utilization')
    completedOPT = find_lines_with_string(prefix.prefixOPT, 'total_completed_tasks')

    costUTIL = find_lines_with_string(prefix.prefixUTIL, 'cost_of_vm_utilization')
    completedUTIL = find_lines_with_string(prefix.prefixUTIL, 'total_completed_tasks')

    costCOST = find_lines_with_string(prefix.prefixCOST, 'cost_of_vm_utilization')
    completedCOST = find_lines_with_string(prefix.prefixCOST, 'total_completed_tasks')

    costFuzzy = [(x / y) if y != 0 else 0 for x, y in zip(costFuzzy, completedFuzzy)]
    costOPT = [(x / y) if y != 0 else 0 for x, y in zip(costOPT, completedOPT)]

    costUTIL = [(x / y) if y != 0 else 0 for x, y in zip(costUTIL, completedUTIL)]
    costCOST = [(x / y) if y != 0 else 0 for x, y in zip(costCOST, completedCOST)]


    comp = [(x / y) if y != 0 else 0 for x, y in zip(costFuzzy, costOPT)]

    X_axis = np.arange(len(users)) 

    plt.plot(X_axis, costOPT, "-o", color = '#006bb3', label='LOTOS')
    plt.plot(X_axis, costFuzzy, "-o", color = '#ff5050', label='Fuzzy [Sonmez et al.]')
    plt.plot(X_axis, costUTIL, "-v", color = '#009933', label='UTIL.')
    plt.plot(X_axis, costCOST, "-^", color = '#001C4F', label='MIN. COST')


    #plt.plot(X_axis, comp, "-o", color = 'black', label='diference')

    #for i, value in enumerate(costFuzzy):
    #    if i == 0 or i == 1:
    #        plt.text(i+0.4, value-0.0030, f"{comp[i]:.2f}x", ha='center', color='black', fontsize=16)
    #    elif i == 2:
    #        plt.text(i+0.4, value-0.0060, f"{comp[i]:.2f}x", ha='center', color='black', fontsize=16)
    #    elif i == 3:
    #        plt.text(i+0.45, value-0.0010, f"{comp[i]:.2f}x", ha='center', color='black', fontsize=16)
    #    else:
    #        plt.text(i-0.4, value-0.0060, f"{comp[i]:.2f}x", ha='center', color='black', fontsize=16) 


    plt.xticks(X_axis, users, rotation=30, fontsize=18)
    plt.yticks(fontsize=18)
    plt.xticks([1,3,5,7,9,11],["400", "800", "1200", "1600", "2000", "2400"], fontsize=18)
    plt.xlabel('Number of user devices',fontsize=20) 
    plt.ylabel('Total cost / tasks completed ($)',fontsize=20) 
    plt.grid(axis = 'both', linestyle='--', linewidth=0.6)
    plt.yscale('log')
    plt.legend(loc='center right', fontsize=18, ncol=2, bbox_to_anchor=(1.02, 1.2), labelspacing=0.2, handletextpad=0.2, columnspacing=0.2) 
    plt.savefig("graficos/PNG/2.png", dpi=300, bbox_inches='tight')
    #plt.savefig("graficos/2.pdf", dpi=100, bbox_inches='tight')
    plt.savefig("graficos/2.eps", dpi=300, bbox_inches='tight')

    #plt.show()

if __name__ == '__main__':
    plot()