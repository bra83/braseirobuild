import math


def is_finite_number(value):
    """Return False for booleans, non-numbers, NaN/Inf and oversized integers."""
    if not isinstance(value,(int,float)) or isinstance(value,bool):
        return False
    try:
        return math.isfinite(value)
    except OverflowError:
        return False
