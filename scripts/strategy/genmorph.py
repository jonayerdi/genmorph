#!/usr/bin/env python3
'''
Script to execute multiple GenMorph experiments.

Usage: ./genmorph.py gen|eval|all <CONFIG1> [<CONFIG2>]
'''

from os import makedirs
from os.path import join, realpath, isfile
from shutil import rmtree
from random import Random

from evaluation.pitest_generator import pitest_generator
from filetypes.sut_config import SUTConfig
from filetypes.experiment_config import ExperimentConfig
from filetypes.experiment_multiple_config import ExperimentMultipleConfig
from filetypes.mr_files import generate_mrs_dir, enumerate_dir_mrs
from generation.gassert import gassert
from states.generate_test_executions import find_relevant_mutants, generate_classifications, generate_tests, generate_method_test_inputs, generate_method_states_sut, generate_test_executions_sut_class, SYSTEM_ID, SYSTEM_ID_MUTANT
from states.instrument_method import find_method_lines, format_java_source
from states.list_methods import list_methods_java_file
from states.method_test_transformer_config import read_values_list_file
from states.states_updater import StatesUpdater
from strategy.transform import generate_inputs_transformer_config, generate_transformed_method_inputs, generate_mrips_and_mrinfos_files, generate_sampled_transformations
from tools.java import java_class_to_filepath, JavaVersion
from tools.major import MUTANTS_LOGFILE_CLASS
import tools.pitest as pitest
from util.fsutil import isnonemptydir, combine_directories, find_file, read_lines, moveall
from util.log import log_stdout
from util.normalized_number import NormalizedNumber

from config import JAVA8, SEPARATORS

def generation_states(sut_config: SUTConfig, sut: tuple, experiment_config: ExperimentConfig, log=log_stdout):
    sut_class, sut_method, sut_index = sut
    generate_test_executions_sut_class(sut_config=sut_config, sut_class=sut_class,
                                    methods=[(sut_method, sut_index)],
                                    config=experiment_config, log=log)
        
def generation_states_followup(sut_config: SUTConfig, sut: tuple, experiment_config: ExperimentConfig, log=log_stdout):
    sut_class, sut_method, sut_index = sut
    paths = experiment_config['paths']
    gassert_config = experiment_config['gassert']
    output_dir = paths['output_dir']
    build_dir = join(output_dir, paths['build_dir'])
    mutants_dir = realpath(join(output_dir, paths['mutants_dir']))
    states_dir = join(output_dir, paths['states_dir'])
    test_inputs_dir = realpath(join(output_dir, paths['test_inputs_dir']))
    instrumented_variables_build_dir = realpath(join(output_dir, experiment_config['paths']['instrumented_variables_build_dir']))
    constants_dir = realpath(join(output_dir, experiment_config['method_test_transformer']['constants_dir']))
    transformations_file_format = realpath(join(output_dir, experiment_config['method_test_transformer']['transformations_file']))
    transform_states_dir = join(output_dir, experiment_config['method_test_transformer']['states_dir'])
    transform_test_inputs_dir = realpath(join(output_dir, experiment_config['method_test_transformer']['test_inputs_dir']))
    transform_classifications_regular_dir = join(output_dir, experiment_config['method_test_transformer']['classifications_regular_dir'])
    original_system_name = paths['original_system_name']
    sut_class_relpath = java_class_to_filepath(sut_class, ext='.class')
    sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
    source_file = realpath(join(sut_config.sources, sut_source_relpath))
    sut_classpath = list(map(realpath, sut_config.classpaths))
    # Generate transformed input executions + extract MRIPs + generate output relations for each SUT
    system_id = SYSTEM_ID(sut_class, sut_method, sut_index)
    constants_dir_system = join(constants_dir, system_id)
    constants_file = join(constants_dir_system, f'{system_id}.constants.txt')
    transformations_file = transformations_file_format.format(system_id=system_id)
    transformations_sampled_file = transformations_file[:-len('.transformations.txt')] + f'_seed{gassert_config["random_seed"]}' + '.transformations.txt'
    test_inputs_dir_system = join(test_inputs_dir, system_id)
    transform_test_inputs_dir_system = join(transform_test_inputs_dir, system_id)
    original_system_id = SYSTEM_ID_MUTANT(system_id, original_system_name)
    states_dir_system = join(states_dir, system_id)
    transform_states_dir_system = join(transform_states_dir, system_id)
    transform_classifications_regular_dir_system = join(transform_classifications_regular_dir, system_id)
    # Find out method constants + generate test inputs transformer config
    generate_inputs_transformer_config(source_file=source_file, sut_class_relpath=sut_class_relpath,
        instrumented_build_dir=instrumented_variables_build_dir,
        test_inputs_dir=test_inputs_dir_system, sut_classpaths=sut_classpath,
        system_id=system_id, method_name=sut_method, method_index=sut_index,
        constants_dir=constants_dir_system, transformations_file=transformations_file, log=log)
    # Sample transformations
    constant_counts = read_values_list_file(constants_file)
    if isfile(transformations_sampled_file):
        log(f'* Sampled transformations file found: {transformations_sampled_file}')
    else:
        log(f'* Generating sampled transformations file: {transformations_sampled_file}')
        generate_sampled_transformations(
            transformations_file=transformations_file,
            sampled_file=transformations_sampled_file,
            rng=Random(x=gassert_config['random_seed']),
            number=gassert_config['mrips_count'],
            constant_counts=constant_counts,
        )
    # Transform source test inputs to generate followups
    generate_transformed_method_inputs(test_inputs_dir=test_inputs_dir_system,
        state_file=find_file(dir=states_dir_system, condition=lambda f, n: n.endswith('.state.json')),
        transform_test_inputs_dir=transform_test_inputs_dir_system, classpath=[build_dir],
        transformations_file=transformations_sampled_file, log=log)
    # Find out which mutants are relevant for the SUT
    relevant_mutants = find_relevant_mutants(mutants_logfile=join(output_dir, MUTANTS_LOGFILE_CLASS.format(sut_class=sut_class)),
        source_file=source_file, method_name=sut_method, method_index=sut_index)
    # Execute followup tests on each mutant
    generate_method_states_sut(source_file=source_file, sut_class=sut_class, sut_classpath=sut_classpath, sut_class_relpath=sut_class_relpath,
        method_name=sut_method, method_index=sut_index, mutants_dir=mutants_dir, relevant_mutants=relevant_mutants,
        states_dir=transform_states_dir_system, test_inputs_dir=transform_test_inputs_dir_system, original_system_name=original_system_name, log=log)
    # Combine states
    combine_directories(target_dir=transform_states_dir_system, other_dirs=[states_dir_system], overwrite=True)
    # Generate classifications for followups
    generate_classifications(states_dir=transform_states_dir_system,
        classifications_regular_dir=transform_classifications_regular_dir_system,
        original_system=original_system_id, log=log)

def generation(sut_config: SUTConfig, sut: tuple, experiment_config: ExperimentConfig, log=log_stdout):
    sut_class, sut_method, sut_index = sut
    paths = experiment_config['paths']
    output_dir = paths['output_dir']
    gassert_config = experiment_config['gassert']
    states_updater = StatesUpdater(gassert_config['states_updater'])
    transform_states_dir = join(output_dir, experiment_config['method_test_transformer']['states_dir'])
    transform_test_inputs_dir = realpath(join(output_dir, experiment_config['method_test_transformer']['test_inputs_dir']))
    transform_classifications_regular_dir = join(output_dir, experiment_config['method_test_transformer']['classifications_regular_dir'])
    assertions_dir = join(output_dir, gassert_config['assertions_dir'])
    original_system_name = paths['original_system_name']
    # Generate transformed input executions + extract MRIPs + generate output relations for each SUT
    system_id = SYSTEM_ID(sut_class, sut_method, sut_index)
    transform_test_inputs_dir_system = join(transform_test_inputs_dir, system_id)
    original_system_id = SYSTEM_ID_MUTANT(system_id, original_system_name)
    transform_states_dir_system = join(transform_states_dir, system_id)
    transform_classifications_regular_dir_system = join(transform_classifications_regular_dir, system_id)
    transformations_file_format = realpath(join(output_dir, experiment_config['method_test_transformer']['transformations_file']))
    transformations_file = transformations_file_format.format(system_id=system_id)
    transformations_sampled_file = transformations_file[:-len('.transformations.txt')] + f'_seed{gassert_config["random_seed"]}' + '.transformations.txt'
    # Generate MRIPs and MRInfos files
    mrips_path = join(assertions_dir, gassert_config['mrips_file'].format(system_id=system_id))
    mrinfos_path = join(assertions_dir, gassert_config['mrinfos_file'].format(system_id=system_id))
    generate_mrips_and_mrinfos_files(transform_test_inputs_dir=transform_test_inputs_dir_system,
        mrips_path=mrips_path, mrinfos_path=mrinfos_path, 
        states_dir=transform_states_dir_system, original_system_id=original_system_id, log=log)
    # For each MRIP
    mrips = list(map(lambda mr: f'{system_id}{SEPARATORS[0]}{mr}', read_lines(transformations_sampled_file)))
    log(f'* Generating output relations for {len(mrips)} MRIPs')
    for mrip_index, mrip in enumerate(mrips, start=1):
        log(f'MRIP {mrip_index}/{len(mrips)}')
        # Run GAssert
        assertion_file = join(
            assertions_dir,
            gassert_config['assertion_file'].format(mrip=mrip)
        )
        if isfile(assertion_file):
            log(f'* GAssertMRs assertion found: {assertion_file}')
        else:
            with JavaVersion(JAVA8): # Java 8 needed for OASIs: Evosuite does not work on newer versions
                log(f'* Running GAssertMRs for: {assertion_file}')
                states_updater.before_gassert(sut_config.root)
                if gassert(args=[
                    "ch.usi.gassert.data.manager.method.MethodMetamorphicORDataManager",
                    f"{gassert_config['tool']};{transform_states_dir_system};{transform_classifications_regular_dir_system};{mrinfos_path};{mrips_path};{mrip}",
                    states_updater.clazz,
                    states_updater.args(sut_config.root, sut_class, sut_method),
                    gassert_config['initial_assertion'],
                    gassert_config['random_seed'],
                    gassert_config['time_budget_minutes'],
                    assertion_file,
                ]) == 0:
                    log(f'* Finished GAssertMRs for: {assertion_file}')
                else:
                    log(f'* Error in GAssertMRs for: {assertion_file}')

def evaluation_states(sut_config: SUTConfig, sut: tuple, experiment_config: ExperimentConfig, log=log_stdout):
    sut_class, sut_method, sut_index = sut
    paths = experiment_config['paths']
    output_dir = paths['output_dir']
    build_dir = realpath(join(output_dir, paths['build_dir']))
    instrumented_build_dir = realpath(join(output_dir, paths['instrumented_build_dir']))
    test_inputs_dir = realpath(join(output_dir, paths['test_inputs_dir']))
    sut_classpath = list(map(realpath, sut_config.classpaths))
    sut_class_relpath = java_class_to_filepath(sut_class, ext='.class')
    sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
    source_file = realpath(join(sut_config.sources, sut_source_relpath))
    # Generate + run tests
    generator = experiment_config['test_inputs_generator']
    generator_args = {}
    literals_file = realpath(join(output_dir, f'{sut_class}.literals.txt'))
    if generator == 'randoop' and isfile(literals_file):
        generator_args['literals'] = literals_file
        log(f'* Using literals file for Randoop')
    test_classes = generate_tests(test_inputs_generator=generator, 
        sut_class=sut_class, sut_class_relpath=sut_class_relpath, output_dir=output_dir,
        build_dir=build_dir, sut_classpath=sut_classpath, config=experiment_config, log=log, **generator_args)
    # Generate test inputs
    generate_method_test_inputs(source_file=source_file, sut_class=sut_class, 
            sut_class_relpath=sut_class_relpath, sut_classpath=sut_classpath,
            method_name=sut_method, method_index=sut_index,
            instrumented_build_dir=instrumented_build_dir,
            test_inputs_dir=test_inputs_dir, test_classes=test_classes,
            max_tests=experiment_config.get('max_tests'), sample_seed=experiment_config.get('random_seed', 0), log=log)

def evaluation_states_followup(sut_config: SUTConfig, sut: tuple, experiment_config: ExperimentConfig, log=log_stdout):
    sut_class, sut_method, sut_index = sut
    system_id = SYSTEM_ID(sut_class, sut_method, sut_index)
    generation_seeds = experiment_config['generation_seeds']
    paths = experiment_config['paths']
    output_dir = paths['output_dir']
    build_dir = join(output_dir, paths['build_dir'])
    test_inputs_dir = realpath(join(output_dir, paths['test_inputs_dir']))
    transformations_file_format = realpath(join(output_dir, paths['transformations_file']))
    transform_test_inputs_dir = realpath(join(output_dir, paths['test_inputs_followup_dir']))
    # Generate transformed input executions + extract MRIPs + generate output relations for each SUT
    transformations_file = transformations_file_format.format(system_id=system_id)
    test_inputs_dir_system = join(test_inputs_dir, system_id)
    for generation_seed in generation_seeds:
        states_dir = join(output_dir, paths['states_dir'].format(seed=generation_seed), system_id)
        assertions_dir = paths['assertions_dir'].format(seed=generation_seed)
        transform_test_inputs_dir_system = join(transform_test_inputs_dir, assertions_dir, system_id)
        transformations_sampled_file = transformations_file[:-len('.transformations.txt')] + f'_seed{generation_seed}' + '.transformations.txt'
        # Transform source test inputs to generate followups
        generate_transformed_method_inputs(test_inputs_dir=test_inputs_dir_system,
            state_file=find_file(dir=states_dir, condition=lambda f, n: n.endswith('.state.json')),
            transform_test_inputs_dir=transform_test_inputs_dir_system, classpath=[build_dir],
            transformations_file=transformations_sampled_file, log=log)

def evaluation(sut_config: SUTConfig, sut: tuple, experiment_config: ExperimentConfig, log=log_stdout):
    sut_class, sut_method, sut_method_index = sut
    system_id = SYSTEM_ID(sut_class, sut_method, sut_method_index)
    generation_seeds = experiment_config['generation_seeds']
    paths = experiment_config['paths']
    output_dir = paths['output_dir']
    build_dir = realpath(join(output_dir, paths['build_dir']))
    mrs_dir = realpath(join(output_dir, paths['mrs_dir']))
    test_inputs_dir = realpath(join(output_dir, paths['test_inputs_dir']))
    test_inputs_followup_dir = realpath(join(output_dir, paths['test_inputs_followup_dir']))
    classpath = [*map(realpath, sut_config.classpaths), build_dir]
    sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
    source_file = realpath(join(sut_config.sources, sut_source_relpath))
    # Generate MRs dir if missing
    for generation_seed in generation_seeds:
        states_dir = join(output_dir, paths['states_dir'].format(seed=generation_seed))
        experiment = paths['assertions_dir'].format(seed=generation_seed)
        assertions_dir = realpath(join(output_dir, experiment))
        mrs_dir_experiment = join(mrs_dir, experiment, system_id)
        if not isnonemptydir(mrs_dir_experiment):
            log(f'* Collecting MRs')
            generate_mrs_dir(assertions_dir=assertions_dir, states_dir=states_dir, system_id=system_id, out_dir=mrs_dir_experiment)
    # Find method lines in source file
    format_java_source(source_file=source_file)
    method_lines = set(find_method_lines(source_file=source_file, method_name=sut_method, method_index=sut_method_index))
    # Generate PITest testsuite
    pitest_suite_dir = join(output_dir, experiment_config['pitest']['tests_dir'], system_id)
    tests_class_prefix = experiment_config['pitest']['tests_class_prefix']
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
    pitest_workdir = join(output_dir, experiment_config['pitest']['workdir'], system_id)
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
        tests_class = pitest.test_class_name(tests_class_prefix, experiment, mr)
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
        rmtree(join(pitest_workdir), ignore_errors=True)
        makedirs(pitest_workdir, exist_ok=True)
        pitest.write_test_pom(
            dir=pitest_workdir,
            sources_dir=sut_config.sources,
            tests_dir=pitest_suite_dir,
        )
        log(f'* Re-running test suite for {system_id}')
        assert pitest.test(workdir=pitest_workdir) == 0, f'Error executing tests for {system_id}'
    # Run PITest (for MRs with 0 FPs)
    mr_stats_ms = { mr: NormalizedNumber(0, 0) for mr in mrs }
    mutants_killed = {}
    for key in mrs:
        experiment, mr = key
        tests_class = pitest.test_class_name(tests_class_prefix, experiment, mr)
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
                killed = list((int(verdict in ['TIMED_OUT', 'MEMORY_ERROR', 'KILLED']) for verdict in verdicts))
                mr_stats_ms[key] = NormalizedNumber(sum(killed), len(verdicts))
                mutants_killed[key] = killed
            except Exception as e:
                log(f'* Error in PITest {tests_class}: {e}')
    # Write final MR stats
    mr_stats = { mr: (mr_stats_fp[mr], mr_stats_ms[mr]) for mr in mrs }
    pitest.write_mr_stats(mr_stats=mr_stats, output_file=join(pitest_workdir, experiment_config['pitest']['mrs_status']))
    pitest.write_mutants_killed(mutants_killed=mutants_killed, output_file=join(pitest_workdir, experiment_config['pitest']['mutants_killed']))

def genmorph(mode: str, config1: ExperimentMultipleConfig, config2: ExperimentMultipleConfig=None, log=log_stdout):
    assert mode in ['all', 'gen', 'eval'], f'Invalid mode: {mode}'
    assert (mode == 'all') ^ (config2 is None)
    sut_config = config1.sut_config
    for sut in sut_config.iter_methods():
        system_id = SYSTEM_ID(*sut)
        try:
            if mode in ['all', 'gen']:
                configs = list(config1.iter_configs())
                for index, experiment_config in enumerate(configs, start=1):
                    log(f'[{system_id}]({index}/{len(configs)}) GENERATION STATES')
                    generation_states(sut_config=sut_config, sut=sut, experiment_config=experiment_config, log=log)
                    log(f'[{system_id}]({index}/{len(configs)}) GENERATION STATES FOLLOWUP')
                    generation_states_followup(sut_config=sut_config, sut=sut, experiment_config=experiment_config, log=log)
                    log(f'[{system_id}]({index}/{len(configs)}) GENERATION')
                    generation(sut_config=sut_config, sut=sut, experiment_config=experiment_config, log=log)
            if mode in ['all', 'eval']:
                config = config1 if mode == 'eval' else config2
                configs = list(config.iter_configs())
                for index, experiment_config in enumerate(configs, start=1):
                    log(f'[{system_id}]({index}/{len(configs)}) EVALUATION STATES')
                    evaluation_states(sut_config=sut_config, sut=sut, experiment_config=experiment_config, log=log)
                    log(f'[{system_id}]({index}/{len(configs)}) EVALUATION STATES FOLLOWUP')
                    evaluation_states_followup(sut_config=sut_config, sut=sut, experiment_config=experiment_config, log=log)
                    log(f'[{system_id}]({index}/{len(configs)}) EVALUATION')
                    evaluation(sut_config=sut_config, sut=sut, experiment_config=experiment_config, log=log)
        except Exception as ex:
            import traceback
            import sys
            print(f'[{system_id}] EXCEPTION', file=sys.stderr)
            traceback.print_exception(type(ex), ex, ex.__traceback__)

def main():
    import sys
    if len(sys.argv) not in [3, 4]:
        print('./genmorph.py gen|eval|all <CONFIG1> [<CONFIG2>]')
        exit(1)
    genmorph(
        mode=sys.argv[1],
        config1=ExperimentMultipleConfig.from_filename(sys.argv[2]),
        config2=ExperimentMultipleConfig.from_filename(sys.argv[3]) if len(sys.argv) > 3 else None,
    )
