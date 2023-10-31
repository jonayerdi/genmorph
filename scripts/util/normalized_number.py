import re

class NormalizedNumber:
    NUMBER_REGEX = re.compile(r'^(.+)%\((\d+)/(\d+)\)$')
    def __init__(self, value, divisor):
        self.value = value
        self.divisor = divisor
    def __repr__(self):
        return f'{self.percentage():.2%}({self.value}/{self.divisor})'
    def __str__(self):
        return f'{self.percentage():.2%}({self.value}/{self.divisor})'
    def percentage(self):
        if self.divisor == 0:
            if self.value > 0:
                return float('inf')
            if self.value < 0:
                return float('-inf')
            return float('nan')
        return self.value/self.divisor
    def __add__(self, other):
        return NormalizedNumber(self.value + other.value, self.divisor + other.divisor)
    @staticmethod
    def parse(s):
        match = NormalizedNumber.NUMBER_REGEX.match(s)
        if match:
            return NormalizedNumber(value=int(match.group(2)), divisor=int(match.group(3)))
        return None
    @staticmethod
    def sum(nums):
        acc = NormalizedNumber(0, 0)
        for num in nums:
            acc += num
        return acc
