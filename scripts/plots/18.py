import matplotlib.pyplot as plt  # type: ignore
import csv 
import pandas as pd # type: ignore
import prefix
import numpy as np # type: ignore

plt.rcParams["font.family"] = "Ubuntu"
plt.rcParams["figure.figsize"] = [6, 3]

users = prefix.users

def read_data(solution_type):
    service_time = []

    for item in users:
        filename = solution_type + str(item) + "DEVICES_TASKS.csv"
        history = pd.read_csv(filename, delimiter=';')
        history = history[(history['status'] == 'COMPLETED') | (history['status'] == 'delay_above_the_limit')]


        service_time.append(((history['taskEndTime'] - history['taskStartTime']).sum())/len(history))


    return service_time

def plot():

    cFuzzy = read_data(prefix.prefixFuzzy)
    print(cFuzzy)

    cOPT = read_data(prefix.prefixOPT)
    print(cOPT)

    cUTIL = read_data(prefix.prefixUTIL)
    print(cUTIL)

    cCOST = read_data(prefix.prefixCOST)
    print(cCOST)

    X_axis = np.arange(len(users)) 

    plt.plot(X_axis, cOPT, "-o", color = '#006bb3', label='LOTOS')
    plt.plot(X_axis, cFuzzy, "-o", color = '#ff5050', label='Fuzzy [Sonmez et al.]')
    plt.plot(X_axis, cUTIL, "-v", color = '#009933', label='UTIL.')
    plt.plot(X_axis, cCOST, "-^", color = '#001C4F', label='MIN. COST')

    plt.xticks(X_axis, users, rotation=30, fontsize=18)
    plt.yticks(fontsize=18)
    plt.xticks([1,3,5,7,9,11],["400", "800", "1200", "1600", "2000", "2400"], fontsize=18)
    plt.xlabel('Number of user devices',fontsize=20) 
    plt.ylabel('Mean service time (ms)',fontsize=20) 
    plt.grid(axis = 'both', linestyle='--', linewidth=0.6)
    #plt.yscale('log')
    plt.legend(loc='center right', fontsize=17, ncol=2, bbox_to_anchor=(1.02, 1.25), handletextpad=0.2, columnspacing=0.2) 
    plt.savefig("graficos/PNG/18.png", dpi=300, bbox_inches='tight')
    plt.savefig("graficos/18.pdf", dpi=100, bbox_inches='tight')


    #plt.show()

if __name__ == '__main__':
    plot()