import subprocess
import os

config_file = 'config/default_config.properties'

min_number_of_mobile_devices = 200
max_number_of_mobile_devices = 2400
mobile_device_counter_size = 200

orchestrator_policies = ["FUZZY_BASED", "UTILIZATION_BASED", "MIN_COST"]
#orchestrator_policies = []
def change_string(before, after):
    os.system(f"sed -i 's/{before}/{after}/' {config_file}")
    
def solve_models():
    os.chdir('lotos/')
    os.system("rm -rf solver_solutions/ && mkdir solver_solutions")
    os.system("rm -rf solver_configs/ && mkdir solver_configs")

    os.chdir('../fuzzy/')
    os.system('./compile.sh')
    for o_p in orchestrator_policies:
        if os.path.exists("output/" + o_p):
            os.system("rm -rf output/" + o_p)

        ant_polic = subprocess.getoutput(f"grep 'orchestrator_policies=' {config_file}")
        new_polic = "orchestrator_policies=" + o_p
        change_string(ant_polic, new_polic)

        for item in range(min_number_of_mobile_devices, max_number_of_mobile_devices+1, mobile_device_counter_size):
            print(o_p, item)
            ant_size_min = subprocess.getoutput(f"grep 'min_number_of_mobile_devices=' {config_file}")
            ant_size_max = subprocess.getoutput(f"grep 'max_number_of_mobile_devices=' {config_file}")
            new_size_min = "min_number_of_mobile_devices=" + str(item)
            new_size_max = "max_number_of_mobile_devices=" + str(item)

            change_string(ant_size_min, new_size_min)
            change_string(ant_size_max, new_size_max)

            os.system("./run_scenarios.sh 1 1")

    
    os.chdir('../lotos/')
    os.system('./compile.sh')

    o_p = "LOTOS"
    if os.path.exists("output/" + o_p):
         os.system("rm -rf output/" + o_p)

    ant_polic = subprocess.getoutput(f"grep 'orchestrator_policies=' {config_file}")
    new_polic = "orchestrator_policies=" + o_p
    change_string(ant_polic, new_polic)

    for item in range(min_number_of_mobile_devices, max_number_of_mobile_devices+1, mobile_device_counter_size):
        print(o_p, item)
        ant_size_min = subprocess.getoutput(f"grep 'min_number_of_mobile_devices=' {config_file}")
        ant_size_max = subprocess.getoutput(f"grep 'max_number_of_mobile_devices=' {config_file}")
        new_size_min = "min_number_of_mobile_devices=" + str(item)
        new_size_max = "max_number_of_mobile_devices=" + str(item)

        change_string(ant_size_min, new_size_min)
        change_string(ant_size_max, new_size_max)

        os.system("./run_scenarios.sh 1 1")
    
def plot_results():
    os.chdir('../plots/')
    if not os.path.exists("graficos"):
        os.system("mkdir graficos")
    os.system('./plot_results.sh')

if __name__ == '__main__':
    solve_models()
    #plot_results()