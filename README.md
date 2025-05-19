# Efficient Task Orchestration Including Mixed Reality Applications in a Combined Cloud-Edge Infrastructure

This repository contains all the experiments presented in the paper **Efficient Task Orchestration Including Mixed Reality Applications in a Combined Cloud-Edge Infrastructure.**

## Description

In this work, we propose a task orchestration solution that considers the hybrid Cloud/Edge infrastructure and generates a locally optimal solution through mathematical programming.
<p align="center">
  <img src="/doc/images/Three-Tier_Architecture.png" width="100%">
</p>


## Experimentation
First of all, to run the experiments, we need to install the CPLEX optimization tool used in this implementation.


First download the [IBM ILOG CPLEX Optimization Studio](https://www.ibm.com/br-pt/products/ilog-cplex-optimization-studio) on the project website. 

After downloading the binary, run the following command in the repository where the file is located:

```
bash <CPLEX_BINARY_FILE.bin>
```

It will be asked the path to install CPLEX. In the experiment  we choose:

```
/home/<USER>/CPLEX_Studio221
```
Note.: Replace `<USER>` with the proper username.

Follow the instructions provided to install the tool.

At the end of the installation, it is prompted the following command to be executed in order to configure the python API:
```
sudo python3 /home/<USER>/CPLEX_Studio221/python/setup.py install
```

The path where CPLEX is installed needs to be inserted on `~/.bashrc` file. We can do this by adding the following line a the end of `~/.bashrc`:
```
export PATH=$PATH:/home/<USER>/CPLEX_Studio221/cplex/bin/x86-64_linux
```

After that, execute:
```
source .bashrc
```
To confirm the installation run `cplex` to enter the CPLEX environment. Press `Ctrl+C` to exit.

After the installation, run the following command to install docplex:

```
pip3 install --upgrade docplex
```

The DOcplex tool is a modeling library used to formulate the optimization model and execute it using CPLEX.

After installing the required software, navigate to the `scripts/` directory and run the `automate.py` script as follows:

```
python3 install automate.py
```

This will launch the simulation tool and test each solution presented in the paper.

The results are stored in the `scripts/<SOLUTION>/output/` folder.

After the simulation, plots can be generated within the repository’s `scripts/plots/` directory by running the following command:

```
./plot_results.sh
```

Prior to executing the plot generation script, verify that the results path is properly configured in `scripts/plots/prefix.py`.

## Final results
Results for the proposed solutions are available at [here](scripts/fuzzy/output) and [here](scripts/lotos/output). The plots can be accessed [here](scripts/plots/img/).

## Citation

```
@inproceedings{luciano-sbrc2025,
 author = {Luciano Fraga and Leizer Pinto and Kleber Cardoso},
 title = { Efficient Task Orchestration Including Mixed Reality Applications in a Combined Cloud-Edge Infrastructure},
 booktitle = {Anais do XLIII Simpósio Brasileiro de Redes de Computadores e Sistemas Distribuídos},
 location = {Natal/RN},
 year = {2025},
 keywords = {},
 issn = {2177-9384},
 pages = {294--307},
 publisher = {SBC},
 address = {Porto Alegre, RS, Brasil},
 doi = {10.5753/sbrc.2025.5905},
 url = {https://sol.sbc.org.br/index.php/sbrc/article/view/35139}}
```