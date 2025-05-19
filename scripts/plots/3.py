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
                    service_time.append(float(line.strip().split("=")[1]))

    return service_time

def plot():
    completedFuzzy = find_lines_with_string(prefix.prefixFuzzy, 'total_completed_tasks')
    uncompletedFuzzy = find_lines_with_string(prefix.prefixFuzzy, 'total_uncompleted_tasks')
    delayFailedFuzzy = find_lines_with_string(prefix.prefixFuzzy, 'error_tasks_due_to_delay_limit')

    completedOPT = find_lines_with_string(prefix.prefixOPT, 'total_completed_tasks')
    uncompletedOPT = find_lines_with_string(prefix.prefixOPT, 'total_uncompleted_tasks')
    delayFailedOPT = find_lines_with_string(prefix.prefixOPT, 'error_tasks_due_to_delay_limit')

    completedUTIL = find_lines_with_string(prefix.prefixUTIL, 'total_completed_tasks')
    uncompletedUTIL = find_lines_with_string(prefix.prefixUTIL, 'total_uncompleted_tasks')
    delayFailedUTIL = find_lines_with_string(prefix.prefixUTIL, 'error_tasks_due_to_delay_limit')

    completedCOST = find_lines_with_string(prefix.prefixCOST, 'total_completed_tasks')
    uncompletedCOST = find_lines_with_string(prefix.prefixCOST, 'total_uncompleted_tasks')
    delayFailedCOST = find_lines_with_string(prefix.prefixCOST, 'error_tasks_due_to_delay_limit')


    efFuzzy = [(x / (x+y+z)*100) for x, y, z in zip(completedFuzzy, uncompletedFuzzy, delayFailedFuzzy)]
    efOPT = [(x / (x+y+z)*100) for x, y, z in zip(completedOPT, uncompletedOPT, delayFailedOPT)]

    efUTIL = [(x / (x+y+z)*100) for x, y, z in zip(completedUTIL, uncompletedUTIL, delayFailedUTIL)]
    efCOST = [(x / (x+y+z)*100) for x, y, z in zip(completedCOST, uncompletedCOST, delayFailedCOST)]


    X_axis = np.arange(len(users)) 

    plt.plot(X_axis, efOPT, "-o", color = '#006bb3', label='LOTOS')
    plt.plot(X_axis, efFuzzy, "-o", color = '#ff5050', label='Fuzzy [Sonmez et al.]')
    plt.plot(X_axis, efUTIL, "-v", color = '#009933', label='UTIL.')
    plt.plot(X_axis, efCOST, "-^", color = '#001C4F', label='MIN. COST')


    #plt.yscale('log')
    plt.xticks(X_axis, users, rotation=30, fontsize=18)
    plt.yticks(fontsize=18)
    plt.xticks([1,3,5,7,9,11],["400", "800", "1200", "1600", "2000", "2400"], fontsize=18)
    plt.xlabel('Number of user devices',fontsize=20) 
    plt.ylabel('Tasks completed / tasks sent to VM (%)',fontsize=17) 
    plt.grid(axis = 'both', linestyle='--', linewidth=0.6)
    plt.legend(loc='center right', fontsize=18, ncol=2, bbox_to_anchor=(1.02, 1.2), labelspacing=0.2, handletextpad=0.2, columnspacing=0.2) 
    plt.savefig("graficos/3.pdf", bbox_inches='tight')
    plt.savefig("graficos/PNG/3.png", dpi=300, bbox_inches='tight')

if __name__ == '__main__':
    plot()