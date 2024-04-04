from os import makedirs
from os.path import isfile, join, realpath
from shutil import rmtree

from evaluation.automr_pitest_generator import automr_pitest_generator, automr_enumerate_dir_mrs
from filetypes.sut_config import SUTConfig
from filetypes.experiment_config import ExperimentConfig
from filetypes.experiment_multiple_config import ExperimentMultipleConfig
from states.instrument_method import find_method_lines, format_java_source
from states.list_methods import list_methods_java_file
from strategy.genmorph import SYSTEM_ID, evaluation_states
from tools.java import java_class_to_filepath
from util.fsutil import isnonemptydir, moveall
from util.log import log_stdout
from util.normalized_number import NormalizedNumber

import tools.pitest as pitest

def evaluation(sut_config: SUTConfig, sut: tuple, experiment_config: ExperimentConfig, log=log_stdout):
    sut_class, sut_method, sut_method_index = sut
    system_id = SYSTEM_ID(sut_class, sut_method, sut_method_index)
    generation_seeds = experiment_config['generation_seeds']
    paths = experiment_config['paths']
    output_dir = paths['output_dir']
    build_dir = realpath(join(output_dir, paths['build_dir']))
    mrs_dir = realpath(join(output_dir, paths['mrs_dir']))
    test_inputs_dir = realpath(join(output_dir, paths['test_inputs_dir']))
    classpath = [*map(realpath, sut_config.classpaths), build_dir]
    sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
    source_file = realpath(join(sut_config.sources, sut_source_relpath))
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
        makedirs(pitest_suite_dir, exist_ok=True)
        assert automr_pitest_generator(
            sut=sut,
            mrs_dir=mrs_dir,
            source_test_inputs=test_inputs_dir,
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
    mrs = list(automr_enumerate_dir_mrs(mrs_dir=mrs_dir, system_id=system_id))
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

def evaluate_automr(config: ExperimentMultipleConfig, log=log_stdout):
    sut_config = config.sut_config
    for sut_class, sut_method, sut_method_index in sut_config.iter_methods():
        system_id = SYSTEM_ID(sut_class, sut_method, sut_method_index)
        sut = (sut_class, sut_method, sut_method_index)
        configs = list(config.iter_configs())
        for index, experiment_config in enumerate(configs, start=1):
            experiment_config['paths']['output_dir'] = experiment_config['paths']['output_dir_automr']
            experiment_config['paths']['assertions_dir'] = experiment_config['paths']['assertions_dir_automr']
            experiment_config['paths']['mrs_dir'] = experiment_config['paths']['assertions_dir']
            try:
                #log(f'[{system_id}]({index}/{len(configs)}) EVALUATION STATES')
                #evaluation_states(sut_config=sut_config, sut=sut, experiment_config=experiment_config, log=log)
                log(f'[{system_id}]({index}/{len(configs)}) EVALUATION')
                evaluation(sut_config=sut_config, sut=sut, experiment_config=experiment_config, log=log)
            except Exception as ex:
                import traceback
                import sys
                print(f'[{system_id}] EXCEPTION', file=sys.stderr)
                traceback.print_exception(type(ex), ex, ex.__traceback__)

def main():
    import sys
    if len(sys.argv) != 2:
        print('./automr_evaluate_mrs.py <CONFIG>')
        exit(1)
    evaluate_automr(
        config=ExperimentMultipleConfig.from_filename(sys.argv[1]),
    )
