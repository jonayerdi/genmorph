import re
from os import listdir
from os.path import join, split, splitext

from automr.mr_server import load_results_dir
from automr import settings

AUTOMR_PORT = 1337

def automr_pitest_generator_helpers(indent='', automr_port=AUTOMR_PORT):
    out = "public static final byte[] MESSAGE_END = new byte[] { (byte) '\\n' };\n\n"

    '''
    public static String nextMessage(Socket in) throws Exception {
        // Read message bytes
        byte[] messageBytes = new byte[0];
        byte[] buffer = new byte[512];
        int read = 0;
        while (true) {
            read = in.read(buffer);
            if (read < 0) {
                return "";
            }
            messageBytes = concat(messageBytes, messageBytes.length, buffer, read);
            if (endsWith(messageBytes, MESSAGE_END)) {
                break;
            }
        }
        // Decode UTF-8 and return
        return new String(messageBytes, "UTF-8").strip(); 
    }
    '''

    '''
    public static void sendMessage(Socket out, String message) throws Exception {
        byte[] messageBytes = message.getBytes("UTF-8");
        out.write(messageBytes);
        out.write(MESSAGE_END);
    }
    '''

    out += f'{indent}public static boolean automr(String params) {{'
    out += f'{indent*2}try {{\n'
    out += f'{indent*3}Socket socket = new Socket("127.0.0.1", {automr_port});\n'
    out += f'{indent*3}sendMessage(socket, params)\n'
    out += f'{indent*3}boolean verdict = false\n'
    out += f'{indent*3}while (true) {{\n'
    out += f'{indent*4}\n'
    out += f'{indent*4}\n'
    out += f'{indent*4}\n'
    out += f'{indent*4}\n'
    out += f'{indent*4}\n'
    out += f'{indent*4}\n'
    out += f'{indent*3}}}\n'
    out += f'{indent*3}socket.close();\n'
    out += f'{indent*3}return verdict;\n'
    out += f'{indent*2}catch (Exception ignore) {{\n'
    out += f'{indent*3}return true;\n'
    out += f'{indent*2}}}\n'
    out += f'{indent}}}\n'
    return out

def automr_pitest_generator_testcase(testcase, automr_params, indent=''):
    out = f'{indent}@Test\n'
    out += f'{indent*2}public void {testcase["name"]}() {{\n'
    out += f'{indent*2}verdict = automr("{";".join((*automr_params, *testcase["input"]))}")\n'
    out += f'{indent*2}org.junit.Assert.assertTrue(verdict);\n'
    out += f'{indent}}}\n'
    return out

def automr_pitest_generator_testsuite(testsuite_class, testcases, automr_params):
    out = 'import org.junit.Test;\n'
    out += 'import java.net.Socket;\n\n'
    out += automr_pitest_generator_helpers(indent='    ')
    out += f'public class {testsuite_class} {{\n\n'
    for testcase in testcases:
        out += automr_pitest_generator_testcase(testcase=testcase, automr_params=automr_params, indent='    ')
        out += '\n\n'
    out += '}'
    return out

REGEX_METHODINPUTS_VALUE = re.compile(r'^<value class="\w+">(.*)</value>$')
def get_testcase_inputs(filename):
    inputs = []
    with open(filename, mode='r') as fp:
        for line in fp:
            line = line.strip()
            match = REGEX_METHODINPUTS_VALUE.match(line)
            if match:
                inputs.append(match.group(1))
    return inputs

def get_testcases(source_test_inputs, system_id):
    for f in listdir(join(source_test_inputs, system_id)):
        if f.endswith('.methodinputs'):
            name = f[len(system_id):-len('.methodinputs')]
            inputs = get_testcase_inputs(join(source_test_inputs, system_id, f))
            yield { 'name': name, 'inputs': inputs }

def automr_pitest_generator(classpath, system_id, mrs_dir, source_test_inputs, output_test_prefix):
    func_index = settings.get_func_index(system_id)
    MRs = load_results_dir(result_dir=join(mrs_dir, 'phase2'), condition=lambda f: split(f)[1].startswith(f'{func_index}_'))
    testcases = list(get_testcases(source_test_inputs))
    for (func_index, parameters), v in MRs.items():
        A_all=v[0]
        for mr_index in range(A_all.shape[0]):
            testsuite_file = f'{output_test_prefix}_{func_index}_{parameters}_{mr_index}.java'
            testsuite_class = splitext(split(testsuite_file)[1])[0]
            with open(testsuite_file, mode='w', encoding='utf-8', newline='\n') as out:
                testsuite = automr_pitest_generator_testsuite(testsuite_class=testsuite_class, testcases=testcases, automr_params=automr_params)
                out.write(testsuite)

def automr_enumerate_dir_mrs(mrs_dir, system_id):
    func_index = settings.get_func_index(system_id)
    MRs = load_results_dir(result_dir=join(mrs_dir, 'phase2'), condition=lambda f: split(f)[1].startswith(f'{func_index}_'))
    for (func_index, parameters), v in MRs.items():
        A_all=v[0]
        for mr_index in range(A_all.shape[0]):
            yield f'{func_index}_{parameters}_{mr_index}'
