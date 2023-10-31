#!/usr/bin/env python3
'''
Get the results for all the MRs with PITest
'''

from os import makedirs
from os.path import realpath, join, split, isfile
from shutil import copyfile, rmtree

from config import *

import tools.pitest as pitest
from evaluation.generate_evaluation_states import generate_mrip_followups_all, generate_mrip_followups_mrinfos_recursive
from evaluation.pitest_generator import pitest_generator
from evaluation.split_mr import enumerate_dir_mrs, split_mrs_experiment
from filetypes.sut_config import SUTConfig
from filetypes.experiment_config import ExperimentConfig
from states.generate_test_executions import generate_tests, generate_method_test_inputs, generate_method_states_sut, generate_classifications, SYSTEM_ID
from states.instrument_method import find_method_lines, format_java_source
from states.list_methods import list_methods_java_file
from tools.java import java_class_to_filepath
from util.fsutil import isnonemptydir, moveall
from util.log import log_stdout
from util.normalized_number import NormalizedNumber

def generate_evaluation_original_states_sut(sut_config: SUTConfig, sut_class: str, config: dict, log=log_stdout):
    log(f'* Generating evaluation states for {sut_class}')
    paths = config['paths']
    output_dir = paths['output_dir']
    build_dir = realpath(join(output_dir, paths['build_dir']))
    instrumented_build_dir = realpath(join(output_dir, paths['instrumented_build_dir']))
    test_inputs_dir = realpath(join(output_dir, paths['test_inputs_dir']))
    states_dir = realpath(join(output_dir, paths['states_dir']))
    sut_classpath = list(map(realpath, sut_config.classpaths))
    mutants_dir = realpath(join(output_dir, paths['mutants_dir']))
    classifications_regular_dir = realpath(join(output_dir, paths['classifications_regular_dir']))
    original_system_name = paths['original_system_name']
    original_root = join(mutants_dir, original_system_name)
    sut_class_relpath = java_class_to_filepath(sut_class, ext='.class')
    sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
    sut_dir_relpath = dirname(sut_class_relpath)
    source_file = realpath(join(sut_config.sources, sut_source_relpath))
    source_filename = split(source_file)[1]
    methods=list(sut_config.iter_class_methods(sut_class))
    for method_name, method_index in methods:
        system_id = SYSTEM_ID(sut_class, method_name, method_index)
        test_inputs_dir_system = join(test_inputs_dir, system_id)
        states_dir_system = join(states_dir, system_id)
        # Generate + run tests
        generator = config['test_inputs_generator']
        generator_args = {}
        literals_file = realpath(join(output_dir, f'{sut_class}.literals.txt'))
        if generator == 'randoop' and isfile(literals_file):
            generator_args['literals'] = literals_file
            log(f'* Using literals file for Randoop')
        test_classes = generate_tests(test_inputs_generator=generator, 
            sut_class=sut_class, sut_class_relpath=sut_class_relpath, output_dir=output_dir,
            build_dir=build_dir, sut_classpath=sut_classpath, config=config, log=log, **generator_args)
        # Generate mutants (not really, only the "original" mutant is needed)
        original_dir= join(original_root, sut_dir_relpath)
        original_source = join(original_dir, source_filename)
        makedirs(original_dir, exist_ok=True)
        copyfile(source_file, original_source)
        # Generate test inputs
        generate_method_test_inputs(source_file=source_file, sut_class=sut_class, 
                sut_class_relpath=sut_class_relpath, sut_classpath=sut_classpath,
                method_name=method_name, method_index=method_index,
                instrumented_build_dir=instrumented_build_dir,
                test_inputs_dir=test_inputs_dir, test_classes=test_classes,
                max_tests=config.get('max_tests'), sample_seed=config.get('random_seed', 0), log=log)
        # Execute tests on original system
        generate_method_states_sut(source_file=source_file, sut_class=sut_class, sut_classpath=sut_classpath,
                sut_class_relpath=sut_class_relpath, method_name=method_name, method_index=method_index,
                mutants_dir=mutants_dir, relevant_mutants=[], states_dir=states_dir_system,
                test_inputs_dir=test_inputs_dir_system, original_system_name=original_system_name, log=log)

def generate_evaluation_original_states(sut_config: SUTConfig, config: dict, log=log_stdout):
    for sut_class in sut_config.suts:
        generate_evaluation_original_states_sut(sut_config=sut_config, sut_class=sut_class, config=config, log=log)

def generate_mr_followups(sut_config: SUTConfig, config: dict, log=log_stdout):
    paths = config['paths']
    output_dir = paths['output_dir']
    assertions_dir = realpath(join(output_dir, paths['assertions_dir']))
    build_dir = realpath(join(output_dir, paths['build_dir']))
    test_inputs_dir = realpath(join(output_dir, paths['test_inputs_dir']))
    states_dir = realpath(join(output_dir, paths['states_dir']))
    mrs_dir = realpath(join(output_dir, paths['mrs_dir']))
    test_inputs_followup_dir = realpath(join(output_dir, paths['test_inputs_followup_dir']))
    system_ids = set((SYSTEM_ID(clazz, method, index) for (clazz, method, index) in sut_config.iter_methods()))
    classpath = [*map(realpath, sut_config.classpaths), build_dir]
    # Generate MRs dir if missing
    if not isnonemptydir(mrs_dir):
        log(f'* Collecting MRs')
        split_mrs_experiment(root_dir=assertions_dir, mrs_dir=mrs_dir, states_dir=states_dir)
    # Generate followup test inputs
    log(f'* Generating followups')
    generate_mrip_followups_all(
        mrs_dir=mrs_dir, suts=system_ids, test_inputs_dir=test_inputs_dir, states_dir=states_dir,
        test_inputs_followup_dir=test_inputs_followup_dir, seed=config.get('random_seed', 0), classpath=classpath, log=log,
    )
    generate_mrip_followups_mrinfos_recursive(followups_dir=test_inputs_followup_dir)

def run_pitest_sut_method(sut_config: SUTConfig, sut: tuple, config: dict, log=log_stdout):
    sut_class, sut_method, sut_method_index = sut
    system_id = SYSTEM_ID(sut_class, sut_method, sut_method_index)
    paths = config['paths']
    output_dir = paths['output_dir']
    build_dir = realpath(join(output_dir, paths['build_dir']))
    mrs_dir = realpath(join(output_dir, paths['mrs_dir']))
    test_inputs_dir = realpath(join(output_dir, paths['test_inputs_dir']))
    test_inputs_followup_dir = realpath(join(output_dir, paths['test_inputs_followup_dir']))
    classpath = [*map(realpath, sut_config.classpaths), build_dir]
    sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
    source_file = realpath(join(sut_config.sources, sut_source_relpath))
    # Find method lines in source file
    format_java_source(source_file=source_file)
    method_lines = set(find_method_lines(source_file=source_file, method_name=sut_method, method_index=sut_method_index))
    # Generate PITest testsuite
    pitest_suite_dir = join(output_dir, config['pitest']['tests_dir'], system_id)
    tests_class_prefix = config['pitest']['tests_class_prefix']
    if isnonemptydir(pitest_suite_dir):
        log(f'* Found PITest suites in {pitest_suite_dir}')
    else:
        log(f'* Running PITestGenerator for {system_id}')
        assert pitest_generator(
            classpath=classpath,
            sut=system_id,
            mrs=mrs_dir,
            source_test_inputs=test_inputs_dir,
            followup_test_inputs=test_inputs_followup_dir,
            output_test_prefix=join(pitest_suite_dir, tests_class_prefix)
        ) == 0, f'Error executing PITestGenerator for {system_id}'
    # Generate list of methods in the SUT class
    sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
    source_file = realpath(join(sut_config.sources, sut_source_relpath))
    all_methods = list_methods_java_file(source_file=source_file, container=set)
    excluded_methods = list(filter(lambda m: m != sut_method, all_methods))
    pitest_workdir = join(output_dir, config['pitest']['workdir'], system_id)
    if isnonemptydir(join(pitest_workdir, 'target', 'surefire-reports')):
        log(f'* Found test reports for {system_id}')
    else:
        # Generate PITest directory and pom.xml
        log(f'* Generating PITest pom.xml for {system_id}')
        makedirs(pitest_workdir, exist_ok=True)
        pitest.write_test_pom(
            dir=pitest_workdir,
            sources_dir=sut_config.sources,
            tests_dir=pitest_suite_dir,
        )
        # Run tests on original SUT
        log(f'* Running test suite for {system_id}')
        assert pitest.test(workdir=pitest_workdir) == 0, f'Error executing tests for {system_id}'
    # Identify FPs
    recompile_test_clases = False
    mrs = list(enumerate_dir_mrs(mrs_dir=mrs_dir, system_id=system_id))
    mr_stats_fp = { mr: None for mr in mrs }
    for key in mrs:
        experiment, mr = key
        tests_class = f'{tests_class_prefix}_{experiment}_{mr}'
        tests_source  = join(pitest_suite_dir, f'{tests_class}.java')
        if isfile(tests_source):
            fps = 0
            error_tests = set()
            for test_id, type in pitest.get_test_failures(workdir=pitest_workdir, target_test=tests_class):
                if type == 'FAILURE':
                    fps += 1
                elif type == 'ERROR':
                    error_tests.add(test_id)
                else:
                    raise Exception(f'Unknown test error type: {type}')
            if error_tests:
                pitest.remove_testsuite_methods(test_suite=tests_source, to_remove=error_tests, method_end=lambda l: l.strip() == '}')
                recompile_test_clases = True
            mr_test_counts = pitest.count_mr_tests(test_suite=tests_source)
            mr_stats_fp[key] = NormalizedNumber(
                value=fps, 
                divisor=mr_test_counts,
            )
        else:
            mr_stats_fp[key] = NormalizedNumber(0, 0)
    # Remove compiled tests, since their code may have been changed by pitest.remove_testsuite_methods
    if recompile_test_clases:
        rmtree(join(pitest_workdir, 'target', 'test-classes'), ignore_errors=True)
    # Run PITest (for MRs with 0 FPs)
    mr_stats_ms = { mr: NormalizedNumber(0, 0) for mr in mrs }
    mutants_killed = {}
    for key in mrs:
        experiment, mr = key
        tests_class = f'{tests_class_prefix}_{experiment}_{mr}'
        if mr_stats_fp[key].value == 0:
            try:
                pit_reports_sut_dir = join(pitest_workdir, 'pit-reports-sut', experiment, mr)
                if not isnonemptydir(pit_reports_sut_dir):
                    log(f'* Running PITest: {tests_class}')
                    # Write pom.xml for PITest
                    pitest.write_pitest_pom(
                        dir=pitest_workdir,
                        sources_dir=sut_config.sources,
                        tests_dir=pitest_suite_dir,
                        target_classes=[sut_class],
                        target_tests=[tests_class],
                        excluded_methods=excluded_methods,
                    )
                    # Run PITest
                    log(f'* Running PITest for {system_id}:{experiment}_{mr}')
                    assert pitest.pitest(workdir=pitest_workdir) == 0, f'Error executing PITest for {system_id}'
                    # Move out PITest reports
                    pit_reports_dir = join(pitest_workdir, 'target', 'pit-reports')
                    moveall(src=pit_reports_dir, dst=pit_reports_sut_dir)
                verdicts = pitest.get_verdicts(mutations=join(pit_reports_sut_dir, 'mutations.csv'), sut_lines=method_lines)
                killed = list((int(verdict in ['TIMED_OUT', 'KILLED']) for verdict in verdicts))
                mr_stats_ms[key] = NormalizedNumber(sum(killed), len(verdicts))
                mutants_killed[key] = killed
            except Exception as e:
                log(f'* Error in PITest {tests_class}: {e}')
    # Write final MR stats
    mr_stats = { mr: (mr_stats_fp[mr], mr_stats_ms[mr]) for mr in mrs }
    pitest.write_mr_stats(mr_stats=mr_stats, output_file=join(pitest_workdir, config['pitest']['mrs_status']))
    pitest.write_mutants_killed(mutants_killed=mutants_killed, output_file=join(pitest_workdir, config['pitest']['mutants_killed']))

def run_pitest(sut_config: SUTConfig, config: dict, log=log_stdout):
    for sut_class, sut_method, sut_method_index in sut_config.iter_methods():
        system_id = SYSTEM_ID(sut_class, sut_method, sut_method_index)
        try:
            run_pitest_sut_method(sut_config=sut_config, sut=(sut_class, sut_method, sut_method_index), config=config, log=log)
            log(f'* [OK] PITest ({system_id})')
        except Exception as e:
            raise e
            log(f'* [ERROR] PITest ({system_id}):\n{e}')

def evaluate_pitest(sut_config: SUTConfig, config: dict, log=log_stdout):
    generate_evaluation_original_states(sut_config=sut_config, config=config, log=log)
    generate_mr_followups(sut_config=sut_config, config=config, log=log)
    run_pitest(sut_config=sut_config, config=config, log=log)

def main():
    import sys
    if len(sys.argv) != 3:
        print('./evaluate_pitest.py <SUT_CONFIG> <EVALUATION_CONFIG>')
        exit(1)
    evaluate_pitest(
        sut_config=SUTConfig.from_filename(sys.argv[1]),
        config=ExperimentConfig.from_filename(sys.argv[2]),
    )
