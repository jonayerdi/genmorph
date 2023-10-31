#!/usr/bin/env python3
'''
Class containing information about multiple experimental configs
'''

import json
from copy import deepcopy

from filetypes.experiment_config import ExperimentConfig, FormatDict, override_dict_values
from filetypes.sut_config import SUTConfig

def set_variables(e, variables: dict):
    if type(e) is str:
        return e.format_map(FormatDict(variables))
    elif type(e) is list:
        return [set_variables(x, variables) for x in e]
    elif type(e) is dict:
        return {k: set_variables(e[k], variables) for k in e}
    return e

class ExperimentMultipleConfig:
    def __init__(self, sut_config: SUTConfig, experiment_config: dict, experiment_config_override: dict, runs: list=None):
        self.sut_config = sut_config
        self.experiment_config = experiment_config
        self.experiment_config_override = experiment_config_override
        self.runs = runs
        if self.runs is None:
            self.runs = [{}]
    def iter_configs(self, experiment=None):
        for variables in self.runs:
            experiment_config = ExperimentConfig.from_dict(self.experiment_config, experiment=experiment)
            def apply_overrides(experiment_config_override):
                override = set_variables(experiment_config_override, variables)
                override_dict_values(experiment_config, override)
            apply_overrides(self.experiment_config_override.get('@', {}))
            if experiment is not None:
                apply_overrides(self.experiment_config_override.get(experiment, {}))
            yield experiment_config
    @staticmethod
    def from_dict(data: dict):
        with open(data['experiment-config'], mode='r') as fd:
            return ExperimentMultipleConfig(
                sut_config=SUTConfig.from_filename(data['sut-config']),
                experiment_config=json.load(fd),
                experiment_config_override=deepcopy(data['experiment-config-override']),
                runs=deepcopy(data.get('runs', [{}])),
            )
    @staticmethod
    def from_str(data: str):
        return ExperimentMultipleConfig.from_dict(json.loads(data))
    @staticmethod
    def from_file(fp):
        return ExperimentMultipleConfig.from_dict(json.load(fp))
    @staticmethod
    def from_filename(path: str):
        with open(path, mode='r') as fp:
            return ExperimentMultipleConfig.from_file(fp)
