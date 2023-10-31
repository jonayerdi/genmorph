from os import listdir, makedirs
from os.path import join, realpath

from filetypes.sut_config import SUTConfig
from states.generate_test_executions import SYSTEM_ID, generate_method_test_inputs
from tools.java import java_class_to_filepath

SUT_CONFIG = SUTConfig.from_filename(join('configs', 'sut-config-commons-math3.json'))
WORKDIRS = [join('baseline_ICST', f'{i}') for i in range(12)]
TESTSUITE_DIR = 'testsuite'
TEST_INPUTS_DIR = 'inputs'
INSTRUMENTED_BUILD_DIR = join('baseline_ICST', 'instrumented_build')

def evosuite_main_test_classes(testsuite_dir, sut_class):
    d = testsuite_dir
    parts = sut_class.split('.')
    clazzname = parts[-1]
    for p in parts[:-1]:
        d = join(d, p)
    for c in listdir(d):
        if c.startswith(f'{clazzname}_ESTest') and c.endswith('.class'):
            yield '.'.join((*parts[:-1], c[:-len('.class')]))

def collect_evosuite_inputs():
    for workdir in WORKDIRS:
        testsuite_dir = join(workdir, TESTSUITE_DIR)
        test_inputs_dir = join(workdir, TEST_INPUTS_DIR)
        for sut_class in SUT_CONFIG.suts:
            sut_class_relpath = java_class_to_filepath(sut_class, ext='.class')
            sut_source_relpath = java_class_to_filepath(sut_class, ext='.java')
            source_file = realpath(join(SUT_CONFIG.sources, sut_source_relpath))
            sut_classpath = list(map(realpath, SUT_CONFIG.classpaths))
            test_classes = [
                (testsuite_dir, evosuite_main_test_class)
                for evosuite_main_test_class in evosuite_main_test_classes(testsuite_dir, sut_class)
            ]
            for method_name, method_index in SUT_CONFIG.iter_class_methods(sut_class):
                system_id = SYSTEM_ID(sut_class, method_name, method_index)
                makedirs(join(test_inputs_dir, system_id), exist_ok=True)
                generate_method_test_inputs(
                    source_file=source_file, sut_class=sut_class, 
                    sut_class_relpath=sut_class_relpath, sut_classpath=sut_classpath,
                    method_name=method_name, method_index=method_index,
                    instrumented_build_dir=INSTRUMENTED_BUILD_DIR,
                    test_inputs_dir=test_inputs_dir,
                    test_classes=test_classes
                )

def main():
    collect_evosuite_inputs()
