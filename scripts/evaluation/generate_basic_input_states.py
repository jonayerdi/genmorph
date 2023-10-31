import json

from os import listdir, makedirs
from os.path import join
from xml.etree import ElementTree

WORKDIRS = [join('baseline_ICST', f'{i}') for i in range(12)]
STATES_DIR = 'states'
TEST_INPUTS_DIR = 'inputs'

def parse_value(value):
    if value in ['NaN', 'Infinity', '-Infinity']:
        return value
    return float(value)

def get_methodinputs_inputs(methodinputs):
    inputs = {}
    methodinputs_tree = ElementTree.parse(methodinputs)
    root = methodinputs_tree.getroot()
    methodParameters = root.find('methodParameters')
    for methodParameter in methodParameters:
        name = methodParameter.find('name').text
        if name != 'this':
            value = parse_value(methodParameter.find('value').text)
            inputs[f'i_{name}'] = value
    return inputs

def get_methodinputs_state(system_id, methodinputs):
    return {
        "systemId": system_id,
        "testId": "test0",
        "variables": {
            "inputs": get_methodinputs_inputs(methodinputs),
            "outputs": {},
        },
    }

def generate_basic_input_states():
    for workdir in WORKDIRS:
        states_dir = join(workdir, STATES_DIR)
        test_inputs_dir = join(workdir, TEST_INPUTS_DIR)
        for sut in listdir(test_inputs_dir):
            makedirs(join(states_dir, sut), exist_ok=True)
            for methodinput in filter(lambda f: f.endswith('.methodinputs'), listdir(join(test_inputs_dir, sut))):
                state = get_methodinputs_state(system_id=f"{sut}@original", methodinputs=join(test_inputs_dir, sut, methodinput))
                state_file = join(states_dir, sut, methodinput[:-len('.methodinputs')] + '.state.json')
                with open(state_file, mode='w', encoding='utf-8', newline='\n') as fp:
                    json.dump(state, fp)
def main():
    generate_basic_input_states()
