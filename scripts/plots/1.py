import matplotlib.pyplot as plt  # type: ignore
import csv 
import pandas as pd # type: ignore
import prefix
import numpy as np # type: ignore

plt.rcParams["font.family"] = "Ubuntu"
plt.rcParams["figure.figsize"] = [10, 3]

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
    totalFuzzy = find_lines_with_string(prefix.prefixFuzzy, 'total_tasks')
    completedFuzzy = find_lines_with_string(prefix.prefixFuzzy, 'total_completed_tasks')

    totalOPT = find_lines_with_string(prefix.prefixOPT, 'total_tasks')
    completedOPT = find_lines_with_string(prefix.prefixOPT, 'total_completed_tasks')

    totalUTIL = find_lines_with_string(prefix.prefixUTIL, 'total_tasks')
    completedUTIL = find_lines_with_string(prefix.prefixUTIL, 'total_completed_tasks')

    totalCOST = find_lines_with_string(prefix.prefixCOST, 'total_tasks')
    completedCOST = find_lines_with_string(prefix.prefixCOST, 'total_completed_tasks')

    completedFuzzy = [(x / y)*100 for x, y in zip(completedFuzzy, totalFuzzy)]
    completedOPT = [(x / y)*100 for x, y in zip(completedOPT, totalOPT)]
    completedUTIL = [(x / y)*100 for x, y in zip(completedUTIL, totalUTIL)]
    completedCOST = [(x / y)*100 for x, y in zip(completedCOST, totalCOST)]

    max_competitor_value = [max(x, y, z) for x, y, z in zip(completedFuzzy, completedUTIL, completedCOST)]
    difference = [(x - y) for x, y in zip(completedOPT, max_competitor_value)]
    #print(difference)

    X_axis = np.arange(len(users)) 

    plt.bar(X_axis-0.18, completedOPT, 0.12, color = '#006bb3', label='LOTOS')
    plt.bar(X_axis-0.06, completedFuzzy, 0.12, color = '#ff5050', label='Fuzzy [Sonmez et al.]')
    plt.bar(X_axis+0.06, completedUTIL, 0.12, color = '#009933', label='UTIL.')
    plt.bar(X_axis+0.18, completedCOST, 0.12, color = '#001C4F', label='MIN.COST')

    for i, value in enumerate(completedOPT):
        #print(i)
        plt.text(i-0.1, value + 1, f"+{difference[i]:.2f}%", ha='center', color='#006bb3', fontsize=8)

    plt.ylim(0, 110)
    plt.xticks(X_axis, users, rotation=30, fontsize=14)
    plt.yticks([0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100], fontsize=14)
    plt.yticks([0, 20, 40, 60, 80, 100], fontsize=14)

    plt.tick_params(labelleft = False, axis='y', labelsize=14, labelright=True)
    #plt.tick_params(labelleft = False) 

    plt.xlabel('Number of user devices',fontsize=16) 
    #plt.ylabel('Tasks completed (%)',fontsize=16) 
    plt.grid(axis = 'y', linestyle='--', linewidth=0.6)
    plt.legend(fontsize=14, ncol=2, loc="lower left") 
    plt.savefig("graficos/1.pdf", bbox_inches='tight')
    plt.savefig("graficos/PNG/1.png", dpi=300, bbox_inches='tight')


if __name__ == '__main__':
    plot()