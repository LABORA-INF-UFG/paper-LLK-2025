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

    #eFuzzy = find_lines_with_string(prefix.prefixFuzzy, 'vm_load_on_edge')
    cFuzzy = find_lines_with_string(prefix.prefixFuzzy, 'vm_load_on_cloud')

    #eOPT = find_lines_with_string(prefix.prefixOPT, 'vm_load_on_edge')
    cOPT = find_lines_with_string(prefix.prefixOPT, 'vm_load_on_cloud')

    #eUTIL = find_lines_with_string(prefix.prefixUTIL, 'vm_load_on_edge')
    cUTIL = find_lines_with_string(prefix.prefixUTIL, 'vm_load_on_cloud')

    #eCOST = find_lines_with_string(prefix.prefixCOST, 'vm_load_on_edge')
    cCOST = find_lines_with_string(prefix.prefixCOST, 'vm_load_on_cloud')

    X_axis = np.arange(len(users)) 

    plt.title("AVERAGE VM UTILIZATION ON CLOUD")

    plt.plot(X_axis, cOPT, "-o", color = '#006bb3', label='LOTOS')
    plt.plot(X_axis, cFuzzy, "-o", color = '#ff5050', label='Fuzzy [Sonmez et al.]')
    plt.plot(X_axis, cUTIL, "-v", color = '#009933', label='UTIL.')
    plt.plot(X_axis, cCOST, "-^", color = '#001C4F', label='MIN. COST')

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
    plt.ylabel('Average utilization (%)',fontsize=20) 
    plt.grid(axis = 'both', linestyle='--', linewidth=0.6)
    #plt.yscale('log')
    plt.legend(loc='center right', fontsize=18, ncol=2, bbox_to_anchor=(1.02, 1.26), labelspacing=0.2, handletextpad=0.2, columnspacing=0.2) 
    plt.savefig("graficos/PNG/7.png", dpi=300, bbox_inches='tight')
    plt.savefig("graficos/7.pdf", dpi=100, bbox_inches='tight')


    #plt.show()

if __name__ == '__main__':
    plot()