#!/usr/bin/env python3
'''
Class containing information about experimental configs
'''

import json
from copy import deepcopy

class FormatDict(dict):
    def __missing__(self, k):
        return f'{{{k}}}'

def override_dict_values(d: dict, overrides: dict):
    for key in overrides.keys():
        value = overrides[key]
        if type(value) is dict:
            d.setdefault(key, {})
            override_dict_values(d[key], value)
        else:
            if type(value) is str and key in d: 
                # Replace "{$}" in strings for previous value
                value = value.format_map(FormatDict({'$': str(d[key])}))
            d[key] = value

class ExperimentConfig:
    @staticmethod
    def from_dict(data: dict, experiment=None):
        experiment_data = deepcopy(data.get('@', {}))
        if experiment is not None:
            override_dict_values(experiment_data, deepcopy(data.get(experiment, {})))
        return experiment_data
    @staticmethod
    def from_str(data: str, experiment=None):
        return ExperimentConfig.from_dict(json.loads(data), experiment)
    @staticmethod
    def from_file(fp, experiment=None):
        return ExperimentConfig.from_dict(json.load(fp), experiment)
    @staticmethod
    def from_filename(path: str, experiment=None):
        with open(path, mode='r') as fp:
            return ExperimentConfig.from_file(fp, experiment)
