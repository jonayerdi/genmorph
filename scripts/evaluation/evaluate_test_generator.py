from os import makedirs, listdir
from os.path import isfile, join, split, splitext, realpath
from shutil import rmtree

from evaluation.refactor_evosuites import refactor_evosuites
from evaluation.refactor_randoop import refactor_randoop
from filetypes.experiment_multiple_config import ExperimentMultipleConfig
from states.generate_test_executions import generate_tests
from states.instrument_method import find_method_lines, format_java_source
from states.list_methods import list_methods_java_file
from strategy.genmorph import SYSTEM_ID
from tools.java import java_class_to_filepath
from util.fsutil import isnonemptydir, moveall
from util.log import log_stdout
from util.normalized_number import NormalizedNumber

import tools.pitest as pitest

def relocate_test_classes(test_classes, generator, pitest_suite_dir):
    # for test_dir, test_class in test_classes:
    for test_dir in  list(set(map(lambda t: t[0], test_classes))):
        {
            'evosuite': lambda: refactor_evosuites(
                in_dir=test_dir,
                out_dir=pitest_suite_dir,
            ),
            'randoop': lambda: refactor_randoop(
                in_dir=test_dir,
                out_dir=pitest_suite_dir,
            ),
        }[generator]()

def get_test_dependencies(generator):
    return {
        'evosuite': pitest.EVOSUITE_DEPENDENCY,
        'randoop': '',
    }[generator]

def evaluate_test_generator(config: ExperimentMultipleConfig, log=log_stdout):
    sut_config = config.sut_config
    for generator in ['randoop', 'evosuite']:
        for sut_class, sut_method, sut_method_index in sut_config.iter_methods():
            system_id = SYSTEM_ID(sut_class, sut_method, sut_method_index)
            configs = list(config.iter_configs())
            for index, experiment_config in enumerate(configs, start=1):
                try:
                    # Override config
                    output_dir = experiment_config['paths']['output_dir']
                    experiment_config['test_inputs_generator'] = generator
                    old_workdir = experiment_config[generator]["workdir"]
                    experiment_config[generator]["workdir"] = f'baseline_{old_workdir}'
                    pitest_suite_dir = join(output_dir, 'baseline_pitest_suite_' + old_workdir.format(index=''), system_id)
                    pitest_workdir = join(output_dir, 'baseline_pitest_' + old_workdir.format(index=''), system_id)
                    # Generate tests
                    log(f'[{system_id}]({index}/{len(configs)}) EVALUATE {generator.upper()}')
                    build_dir = realpath(join(output_dir, experiment_config['paths']['build_dir']))
                    sut_classpath = list(map(realpath, sut_config.classpaths))
                    sut_class_relpath = java_class_to_filepath(sut_class, ext='.class')
                    sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
                    source_file = realpath(join(sut_config.sources, sut_source_relpath))
                    test_classes = generate_tests(test_inputs_generator=generator, 
                        sut_class=sut_class, sut_class_relpath=sut_class_relpath, output_dir=output_dir,
                        build_dir=build_dir, sut_classpath=sut_classpath, config=experiment_config,
                        log=log, assertions=True,
                    )
                    if type(test_classes) is str:
                        test_classes = [test_classes]
                    else:
                        test_classes = list(test_classes)
                    relocate_test_classes(test_classes, generator, pitest_suite_dir)
                    test_dependencies = get_test_dependencies(generator)
                    test_classes = list(map(lambda f: splitext(split(f)[1])[0], filter(lambda f: f.endswith('.java'), listdir(pitest_suite_dir))))
                    # Find method lines in source file
                    format_java_source(source_file=source_file)
                    method_lines = set(find_method_lines(source_file=source_file, method_name=sut_method, method_index=sut_method_index))
                    # Generate list of methods in the SUT class
                    source_file = realpath(join(sut_config.sources, sut_source_relpath))
                    all_methods = list_methods_java_file(source_file=source_file, container=set)
                    excluded_methods = list(filter(lambda m: m != sut_method, all_methods))
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
                            dependencies=test_dependencies,
                        )
                        # Run tests on original SUT
                        log(f'* Running test suite for {system_id}')
                        assert pitest.test(workdir=pitest_workdir) == 0, f'Error executing tests for {system_id}'
                    # Identify FPs
                    recompile_test_clases = False
                    stats_fp = { test_class: None for test_class in test_classes }
                    for tests_class in test_classes:
                        tests_source  = join(pitest_suite_dir, f'{tests_class}.java')
                        if isfile(tests_source):
                            fps = 0
                            error_tests = set()
                            for test_id, _type in pitest.get_test_failures(workdir=pitest_workdir, target_test=tests_class):
                                error_tests.add(test_id)
                                fps += 1
                            if error_tests:
                                pitest.remove_testsuite_methods(test_suite=tests_source, to_remove=error_tests, method_end=lambda l: l.strip() == '}')
                                recompile_test_clases = True
                            test_counts = pitest.count_mr_tests(test_suite=tests_source)
                            stats_fp[tests_class] = NormalizedNumber(
                                value=fps, 
                                divisor=test_counts,
                            )
                        else:
                            stats_fp[tests_class] = NormalizedNumber(0, 0)
                    # Remove compiled tests, since their code may have been changed by pitest.remove_testsuite_methods
                    if recompile_test_clases:
                        rmtree(join(pitest_workdir), ignore_errors=True)
                        makedirs(pitest_workdir, exist_ok=True)
                        pitest.write_test_pom(
                            dir=pitest_workdir,
                            sources_dir=sut_config.sources,
                            tests_dir=pitest_suite_dir,
                            dependencies=test_dependencies,
                        )
                        log(f'* Re-running test suite for {system_id}')
                        assert pitest.test(workdir=pitest_workdir) == 0, f'Error executing tests for {system_id}'
                    # Run PITest
                    stats_ms = { test_class: NormalizedNumber(0, 0) for test_class in test_classes }
                    mutants_killed = {}
                    for tests_class in test_classes:
                        try:
                            pit_reports_sut_dir = join(pitest_workdir, 'pit-reports-sut', tests_class)
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
                                    dependencies=test_dependencies,
                                )
                                # Run PITest
                                log(f'* Running PITest for {system_id}')
                                assert pitest.pitest(workdir=pitest_workdir) == 0, f'Error executing PITest for {system_id}'
                                # Move out PITest reports
                                pit_reports_dir = join(pitest_workdir, 'target', 'pit-reports')
                                moveall(src=pit_reports_dir, dst=pit_reports_sut_dir)
                            verdicts = pitest.get_verdicts(mutations=join(pit_reports_sut_dir, 'mutations.csv'), sut_lines=method_lines)
                            killed = list((int(verdict in ['TIMED_OUT', 'MEMORY_ERROR', 'KILLED']) for verdict in verdicts))
                            stats_ms[tests_class] = NormalizedNumber(sum(killed), len(verdicts))
                            mutants_killed[(generator, tests_class)] = killed
                        except Exception as e:
                            log(f'* Error in PITest {tests_class}: {e}')
                    # Write final MR stats
                    mr_stats = { (generator, tests_class): (stats_fp[tests_class], stats_ms[tests_class]) for tests_class in test_classes }
                    pitest.write_mr_stats(mr_stats=mr_stats, output_file=join(pitest_workdir, experiment_config['pitest']['mrs_status']))
                    pitest.write_mutants_killed(mutants_killed=mutants_killed, output_file=join(pitest_workdir, experiment_config['pitest']['mutants_killed']))
                except Exception as ex:
                    import traceback
                    import sys
                    print(f'[{generator}:{system_id}] EXCEPTION', file=sys.stderr)
                    traceback.print_exception(type(ex), ex, ex.__traceback__)

def main():
    import sys
    if len(sys.argv) != 2:
        print('./evaluate_test_generator.py <CONFIG>')
        exit(1)
    evaluate_test_generator(
        config=ExperimentMultipleConfig.from_filename(sys.argv[1]),
    )
