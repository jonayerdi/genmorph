# GenMorph
Automatic generation of Metamorphic Relations (MRs) for Java methods.

## Requirements
Python 3.8+ and Java 8 are required at runtime (`apt install python3 openjdk-8-jdk` on Ubuntu 20.04).

This version of the tool has been tested with OpenJDK version 1.8.0_382 on Linux (Ubuntu 20.04), but should work on other systems as well (has been previously tested on Windows). We use Gradle 7.0 to build the project.

The following dependencies need to be installed separately (different versions may or may not work):
* Apache Maven 3.6.3 (https://maven.apache.org/) (`apt install maven` on Ubuntu 20.04)
* Evosuite 1.1.0 (https://www.evosuite.org/) (https://github.com/EvoSuite/evosuite)
* Major 2.0.0 (https://mutation-testing.org/)
* PITest 1.7.4 (https://pitest.org/) (https://github.com/hcoles/pitest)
* Randoop 4.3.0 (https://randoop.github.io/randoop/) (https://github.com/randoop/randoop)

The location of Java and these dependencies is configured directly from `scripts/config.py`. The script will also check certain environment variables for tool paths (see the code from `scripts/config.py`).

Apache Maven is assumed to be in PATH (the scripts run "mvn" directly).

## Usage

### GenMorph generate MRs
`python3 scripts/run/genmorph.py gen <EXPERIMENT_CONFIG>`

### GenMorph evaluate MRs
`python3 scripts/run/genmorph.py eval <EVALUATION_CONFIG>`

### Examples generate+evaluate MRs
`python3 scripts/run/genmorph.py all <EXPERIMENT_CONFIG> <EVALUATION_CONFIG>`

### Examples

The example configurations will write intermediate and output files to the `./output_dir_guava`, `./output_dir_lang`, and `./output_dir_math` directories. Here, the `assertions_seed*` directories contain the generated MRs, while the `mrs` directory will contain the corresponding Java assertions (`*.jir.txt` being the input relations as Java code, and `*.jor.txt` being the output relations).

After the evaluation step, the `pitest_seed*` directories will contain files named `mrs_status.csv` and `mutants_killed.csv` with the results for each MR. Note that MRs with FPs (FP column value is not `0.00%` in `mrs_status.csv`) will have a MS of `nan%(0/0)`, and will not be present in `mutants_killed.csv`. This is because PITest does not compute mutation scores when the original SUT does not pass the testsuite, and our own evaluation also considers the MS of invalid MRs (those with FPs) to be 0.

#### Run experiments for Guava
`python3 scripts/run/genmorph.py all configs/config-all-experiment-guava.json configs/config-all-evaluation-guava.json`

#### Run experiments for Apache Commons Lang
`python3 scripts/run/genmorph.py all configs/config-all-experiment-lang.json configs/config-all-evaluation-lang.json`

#### Run experiments for Apache Commons Math
`python3 scripts/run/genmorph.py all configs/config-all-experiment-math.json configs/config-all-evaluation-math.json`
