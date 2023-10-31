#!/usr/bin/env python3
'''
Script to generate mutants, states and transformations configs.

Usage: ./generate_mutants_states_transform_config.py <SUT_CONFIG> <EXPERIMENT_CONFIG>
'''

from config import *

from filetypes.sut_config import SUTConfig
from filetypes.experiment_config import ExperimentConfig
from states.generate_test_executions import generate_test_executions, generate_test_executions_sut_class, SYSTEM_ID, SYSTEM_ID_MUTANT
from strategy.transform import generate_inputs_transformer_config
from tools.java import java_class_to_filepath
from util.log import log_stdout

def generate_mutants_states_transform_config_sut_class(sut_config: SUTConfig, sut_class: str, config: dict, log=log_stdout, generate_executions=True):
    if generate_executions:
        # Generate test executions + mutants + MRIPs
        generate_test_executions_sut_class(sut_config=sut_config, sut_class=sut_class, config=config, log=log)
    # Generate output relations
    paths = config['paths']
    output_dir = paths['output_dir']
    test_inputs_dir = realpath(join(output_dir, paths['test_inputs_dir']))
    instrumented_variables_build_dir = realpath(join(output_dir, config['paths']['instrumented_variables_build_dir']))
    constants_dir = realpath(join(output_dir, config['method_test_transformer']['constants_dir']))
    transformations_file_format = realpath(join(output_dir, config['method_test_transformer']['transformations_file']))
    sut_class_relpath = java_class_to_filepath(sut_class, ext='.class')
    sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
    source_file = realpath(join(sut_config.sources, sut_source_relpath))
    methods = list(sut_config.iter_class_methods(sut_class))
    sut_classpath = list(map(realpath, sut_config.classpaths))
    # Generate transformed input executions + extract MRIPs + generate output relations for each SUT
    for method_name, method_index in methods:
        system_id = SYSTEM_ID(sut_class, method_name, method_index)
        constants_dir_system = join(constants_dir, system_id)
        transformations_file = transformations_file_format.format(**config, system_id=system_id)
        test_inputs_dir_system = join(test_inputs_dir, system_id)
        # Find out method constants + generate test inputs transformer config
        generate_inputs_transformer_config(source_file=source_file, sut_class_relpath=sut_class_relpath,
            instrumented_build_dir=instrumented_variables_build_dir,
            test_inputs_dir=test_inputs_dir_system, sut_classpaths=sut_classpath,
            system_id=system_id, method_name=method_name, method_index=method_index,
            constants_dir=constants_dir_system, transformations_file=transformations_file, log=log)

def generate_mutants_states_transform_config(sut_config: SUTConfig, experiment_config: dict, log=log_stdout):
    generate_test_executions(sut_config=sut_config, experiment_config=experiment_config, log=log)
    for sut_class in sut_config.suts:
        generate_mutants_states_transform_config_sut_class(sut_config=sut_config, sut_class=sut_class,
            config=experiment_config, log=log, generate_executions=False)

def main():
    import sys
    if len(sys.argv) != 3:
        print('./generate_mutants_states_transform_config.py <SUT_CONFIG> <EXPERIMENT_CONFIG>')
        exit(1)
    generate_mutants_states_transform_config(
        sut_config=SUTConfig.from_filename(sys.argv[1]),
        experiment_config=ExperimentConfig.from_filename(sys.argv[2], experiment='transform'),
    )
