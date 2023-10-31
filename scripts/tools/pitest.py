#!/usr/bin/env python3
'''
Utility script to run PITest with Maven
'''

import re
from os.path import isfile, join, split, splitext, relpath
from shutil import copyfile
from subprocess import Popen
from tempfile import TemporaryDirectory

DEFAULT_PITEST_HOME = '.'# join('C:/', 'tools', 'evosuite')
DEFAULT_PITEST_JAR = 'pitest-wrapper-1.7.4.jar'

POM_FILENAME = 'pom.xml'
TEST_POM_TEMPLATE = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>ch.usi.gassert</groupId>
    <artifactId>PITest</artifactId>
    <version>1.0</version>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
    </properties>

    <build>
        <sourceDirectory>{sources_dir}</sourceDirectory>
        <testSourceDirectory>{tests_dir}</testSourceDirectory>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.0.0-M5</version>
            </plugin>
        </plugins>  
    </build>

    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
        {dependencies}
    </dependencies>
</project>
'''
PITEST_POM_TEMPLATE = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>ch.usi.gassert</groupId>
    <artifactId>PITest</artifactId>
    <version>1.0</version>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
    </properties>

    <build>
        <sourceDirectory>{sources_dir}</sourceDirectory>
        <testSourceDirectory>{tests_dir}</testSourceDirectory>
        <plugins>
            <plugin>
                <groupId>org.pitest</groupId>
                <artifactId>pitest-maven</artifactId>
                <version>1.7.4</version>
                <configuration>
                    <targetClasses>
                        {target_classes}
                    </targetClasses>
                    <targetTests>
                        {target_tests}
                    </targetTests>
                    <excludedMethods>
                        {excluded_methods}
                    </excludedMethods>
                    <outputFormats>
                        <param>HTML</param>
                        <param>CSV</param>
                    </outputFormats>
                    <avoidCallsTo>
                        <avoidCallsTo>java.util.logging</avoidCallsTo>
                        <avoidCallsTo>org.apache.log4j</avoidCallsTo>
                        <avoidCallsTo>org.slf4j</avoidCallsTo>
                        <avoidCallsTo>org.apache.commons.logging</avoidCallsTo>
                        <avoidCallsTo>ch.usi.gassert.serialization</avoidCallsTo>
                    </avoidCallsTo>
                    <threads>16</threads>
                    <timestampedReports>false</timestampedReports>
                </configuration>
            </plugin>
        </plugins>  
    </build>

    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
        {dependencies}
    </dependencies>
</project>
'''
REGEX_SUREFIRE_FAILURE = re.compile(r'^(.+)\.(test.+)  Time elapsed: .+ s  <<< ([A-Z]+)!$')
REGEX_TEST_METHOD = re.compile(r'^\s*public\s+void\s*(.+)\s*\(\)(\s+throws\s+[\w_\.]+)?\s*\{\s*$')

EVOSUITE_DEPENDENCY = '''<dependency>
            <groupId>org.evosuite</groupId>
            <artifactId>evosuite</artifactId>
            <version>1.1.0</version>
            <scope>test</scope>
        </dependency>
'''

def get_test_failures(workdir, target_test):
    report_file = join(workdir, 'target', 'surefire-reports', f'{target_test}.txt')
    if isfile(report_file):
        with open(report_file, mode='r') as fp:
            for line in map(lambda l: l.strip(), fp):
                match = REGEX_SUREFIRE_FAILURE.match(line)
                if match is not None:
                    yield match.group(2), match.group(3) # testId, type

def write_mr_stats(mr_stats, output_file):
    lines = list(map(lambda mr: f'{mr[0]},{mr[1]},{mr_stats[mr][0]},{mr_stats[mr][1]}\n', mr_stats))
    lines.sort()
    with open(output_file, mode='wb') as fp:
        fp.write('EXPERIMENT,MR,FP,MS\n'.encode(encoding='utf-8'))
        for line in lines:
            fp.write(line.encode(encoding='utf-8'))

def write_mutants_killed(mutants_killed, output_file):
    if mutants_killed:
        mutants_count = len(next(iter(mutants_killed.values())))
        mutants_header = ','.join((f'M{i+1}' for i in range(mutants_count)))
        with open(output_file, mode='wb') as fp:
            fp.write(f'EXPERIMENT,MR,{mutants_header},COUNT\n'.encode(encoding='utf-8'))
            experiment_totals = {}
            totals = [0 for _ in range(mutants_count)]
            for mr in mutants_killed:
                experiment_totals[mr[0]] = [
                    int(bool(acc + cur)) 
                    for acc, cur in zip(
                        experiment_totals.get(mr[0], [0 for _ in range(mutants_count)]),
                        mutants_killed[mr],
                    )
                ]
                totals = [
                    int(bool(acc + cur)) 
                    for acc, cur in zip(
                        totals,
                        mutants_killed[mr],
                    )
                ]
                killed = f','.join((str(k) for k in mutants_killed[mr]))
                fp.write(f'{mr[0]},{mr[1]},{killed},{sum(mutants_killed[mr])}\n'.encode(encoding='utf-8'))
            experiments = list(experiment_totals.keys())
            experiments.sort()
            for experiment in experiments:
                killed = f','.join((str(k) for k in experiment_totals[experiment]))
                fp.write(f'{experiment},*,{killed},{sum(experiment_totals[experiment])}\n'.encode(encoding='utf-8'))
            killed = f','.join((str(k) for k in totals))
            fp.write(f'*,*,{killed},{sum(totals)}\n'.encode(encoding='utf-8'))

def get_verdicts(mutations, sut_lines=None, separator=','):
    with open(mutations, mode='r') as fp:
        verdicts = []
        for row in  map(lambda l: l.split(separator), filter(lambda l: l, map(lambda l: l.strip(), fp))):
            mutant_source_file, mutant_class, mutation_operator, mutant_method, mutant_line, verdict, failing_test = row[0], row[1], row[2], row[3], int(row[4]), row[5], row[6]
            if sut_lines is None or mutant_line in sut_lines:
                if verdict not in ['NO_COVERAGE', 'SURVIVED', 'MEMORY_ERROR', 'TIMED_OUT', 'KILLED']:
                    raise Exception(f'Unhandled mutant verdict in "{mutations}": "{verdict}"')
                verdicts.append(verdict)
        return verdicts

def count_mr_tests(test_suite):
    with open(test_suite, mode='r') as fp:
        return sum(1 for _ in filter(lambda l: l == '@Test', map(lambda l: l.strip(), fp)))

def remove_testsuite_methods(test_suite, to_remove, method_end, test_suite_out=None):
    tmpdir = None
    if (test_suite_out is None) or (test_suite_out == test_suite):
        tmpdir = TemporaryDirectory()
        copied_test_suite = join(tmpdir.__enter__(), split(test_suite)[1])
        copyfile(src=test_suite, dst=copied_test_suite)
        test_suite_out = test_suite
        test_suite = copied_test_suite
    test_class_old = splitext(split(test_suite)[1])[0]
    test_class_new = splitext(split(test_suite_out)[1])[0]
    with open(test_suite, mode='r') as foriginal, open(test_suite_out, mode='w') as fpass:
        lines = iter(foriginal)
        while True:
            line = next(lines, None)
            if line is None:
                break
            stripped = line.strip()
            if stripped == f'public class {test_class_old} {{':
                fpass.write(f'public class {test_class_new} {{\n')
            elif stripped.startswith('@Test'):
                test_method = next(lines)
                test_method_stripped = test_method.strip()
                match = REGEX_TEST_METHOD.match(test_method_stripped)
                if match.group(1) not in to_remove:
                    fpass.write(line)
                    fpass.write(test_method) 
                else:
                    while not method_end(next(lines)):
                        pass # Skip until the end of the method
                    #assert len(next(lines).strip()) == 0
            else:
                fpass.write(line)
    if tmpdir is not None:
        tmpdir.cleanup()

def get_pom_params_list(params):
    return '\n                        '.join((f'<param>{param}</param>' for param in params))

def get_test_pom(sources_dir, tests_dir, dependencies=''):
    return TEST_POM_TEMPLATE.format(
        sources_dir=sources_dir,
        tests_dir=tests_dir,
        dependencies=dependencies,
    )

def get_pitest_pom(sources_dir, tests_dir, target_classes, target_tests, excluded_methods, dependencies=''):
    return PITEST_POM_TEMPLATE.format(
        sources_dir=sources_dir,
        tests_dir=tests_dir,
        target_classes=get_pom_params_list(target_classes),
        target_tests=get_pom_params_list(target_tests),
        excluded_methods=get_pom_params_list(excluded_methods),
        dependencies=dependencies,
    )

def write_test_pom(dir, sources_dir, tests_dir, dependencies=''):
    sources_dir = join('${project.basedir}', relpath(sources_dir, start=dir))
    tests_dir = join('${project.basedir}', relpath(tests_dir, start=dir))
    with open(join(dir, POM_FILENAME), mode='w') as fp:
        fp.write(get_test_pom(sources_dir, tests_dir, dependencies))

def write_pitest_pom(dir, sources_dir, tests_dir, target_classes, target_tests, excluded_methods, dependencies=''):
    sources_dir = join('${project.basedir}', relpath(sources_dir, start=dir))
    tests_dir = join('${project.basedir}', relpath(tests_dir, start=dir))
    with open(join(dir, POM_FILENAME), mode='w') as fp:
        fp.write(get_pitest_pom(sources_dir, tests_dir, target_classes, target_tests, excluded_methods, dependencies))

import sys
MVN_SHELL = sys.platform == 'win32' # On Windows, we cannot start the subprocess with 'mvn' without the shell

def test(workdir):
    return Popen(shell=MVN_SHELL, cwd=workdir, args=['mvn', '-q', '-B', '-Dmaven.test.failure.ignore=true', 'test']).wait()

def pitest(workdir):
    #return Popen(shell=SHELL, cwd=workdir, args=['mvn', '-q', '-B', 'test-compile', 'org.pitest:pitest-maven:mutationCoverage']).wait()
    return Popen(shell=MVN_SHELL, cwd=workdir, args=['mvn', '-q', '-B', 'org.pitest:pitest-maven:mutationCoverage']).wait()

REGEX_PITEST_FAILING_LINE = re.compile(r'^.+name=(.+)\((.+)\)] did not pass without mutation\.\s*$')
def stderr_failing_tests(lines):
    for line in lines:
        match = REGEX_PITEST_FAILING_LINE.match(line)
        if match is not None:
            yield match.group(2), match.group(1)

def test_class_name(prefix, experiment, mr):
    test_class = f'{prefix}_{experiment}_{mr}'
    test_class = ''.join([c if c.isalnum() else '_' for c in test_class])
    return test_class

def main():
    import sys
    workdir = None
    if len(sys.argv) > 1:
        workdir = sys.argv[1]
    pitest(workdir=workdir)
