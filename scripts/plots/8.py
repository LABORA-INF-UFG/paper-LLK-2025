import matplotlib.pyplot as plt  # type: ignore
import csv 
import pandas as pd # type: ignore
import prefix
import numpy as np # type: ignore

plt.rcParams["font.family"] = "Ubuntu"
plt.rcParams["figure.figsize"] = [3, 3]

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
    totalFuzzy = find_lines_with_string(prefix.prefixFuzzy_SEC, 'total_tasks')
    completedFuzzy = find_lines_with_string(prefix.prefixFuzzy_SEC, 'total_completed_tasks')

    totalOPT = find_lines_with_string(prefix.prefixOPT_SEC, 'total_tasks')
    completedOPT = find_lines_with_string(prefix.prefixOPT_SEC, 'total_completed_tasks')

    totalUTIL = find_lines_with_string(prefix.prefixUTIL_SEC, 'total_tasks')
    completedUTIL = find_lines_with_string(prefix.prefixUTIL_SEC, 'total_completed_tasks')

    totalCOST = find_lines_with_string(prefix.prefixCOST_SEC, 'total_tasks')
    completedCOST = find_lines_with_string(prefix.prefixCOST_SEC, 'total_completed_tasks')

    completedFuzzy = [(x / y)*100 for x, y in zip(completedFuzzy, totalFuzzy)]
    completedOPT = [(x / y)*100 for x, y in zip(completedOPT, totalOPT)]
    completedUTIL = [(x / y)*100 for x, y in zip(completedUTIL, totalUTIL)]
    completedCOST = [(x / y)*100 for x, y in zip(completedCOST, totalCOST)]

    max_competitor_value = [max(x, y, z) for x, y, z in zip(completedFuzzy, completedUTIL, completedCOST)]
    difference = [(x - y) for x, y in zip(completedOPT, max_competitor_value)]
    #print(difference)

    X_axis = np.arange(len(users)) 

    plt.plot(X_axis, completedOPT, "-o", markersize=3, linewidth=1.0, color = '#006bb3', label='LOTOS')
    plt.plot(X_axis, completedFuzzy, "-o", markersize=3, linewidth=1.0, color = '#ff5050', label='Fuzzy [Sonmez et al.]')
    plt.plot(X_axis, completedUTIL, "-v", markersize=3, linewidth=1.0, color = '#009933', label='UTIL.')
    plt.plot(X_axis, completedCOST, "-^", markersize=3, linewidth=1.0, color = '#001C4F', label='MIN.COST')

    #for i, value in enumerate(completedOPT):
        #print(i)
    #    plt.text(i-0.1, value + 1, f"+{difference[i]:.2f}%", ha='center', color='#006bb3', fontsize=8)

    plt.ylim(50, 105)
    #plt.xticks(X_axis, users, rotation=30, fontsize=14)
    plt.xticks([1,3,5,7,9,11],["400", "800", "1200", "1600", "2000", "2400"], rotation=30, fontsize=14)

    #plt.yticks([0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100], fontsize=14)
    plt.yticks([50, 60, 70, 80, 90, 100], fontsize=14)

    plt.xlabel('Number of user devices',fontsize=16) 
    plt.ylabel('Tasks completed (%)',fontsize=16) 
    plt.grid(axis = 'y', linestyle='--', linewidth=0.6)
    plt.legend(fontsize=10, ncol=1, loc="lower left", bbox_to_anchor=(0.002, 0.002), handletextpad=0.2, columnspacing=0.2) 
    plt.savefig("graficos/8.pdf", bbox_inches='tight')
    plt.savefig("graficos/PNG/8.png", dpi=300, bbox_inches='tight')


if __name__ == '__main__':
    plot()