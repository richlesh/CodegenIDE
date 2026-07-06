# Utils.py

import inspect
import sys
import math
import os
import re
from enum import Enum

def debug(b, msg) :
    if (b) :
        print("debug: " + os.path.basename(inspect.stack()[1][1]) + "(" + str(inspect.stack()[1][2]) + ") " + msg);

def ipow(base, exp) :
	assert exp >= 0, "ipow() exponent must be >=0 but was " + exp;
	result = 1
	pow = base
	while (exp != 0) :
		if (exp & 1) :
			result *= pow
		exp >>= 1
		pow *= pow
	return result

def elementFormatter(obj) :
    if (isinstance(obj, str)) :
        return "\"" + obj + "\""
    else :
        return str(obj)

def listOrTupleToString(list, isTuple):
    return ("<" if isTuple else "[") + ", ".join(map(elementFormatter, list)) + (">" if isTuple else "]");

def listToString(list):
    return listOrTupleToString(list, False)

def tupleToString(tuple):
    return listOrTupleToString(tuple, True)

def mapToString(map):
    result = ""
    for k, v in map.items():
        if (len(result) != 0):
            result += ", "
        result += elementFormatter(k) + " => " + elementFormatter(v)
    return "{" + result + "}"   

def mapRemove(map, key):
    try:
        del map[key]
    except KeyError:
        pass

def defined(var):
    return var != None

def isEmpty(var):
    return not defined(var) or len(var) == 0

def isBlank(var):
    return not defined(var) or len(var.strip()) == 0

def complement32(x):
    result = -x - 1
    return arshift32(result, 0)

def lshift32(x, y):
    result = x << y
    return arshift32(result, 0)

def rshift32(x, y):
    return ((x & 0xFFFFFFFF) >> y)

# Pads 32 bit integers with FF if high bit (31) set so that it is a negative perl value.
def convertToSigned32(x):
    return arshift32(x, 0)

# Also used to normalize value to a negative number if high bit is set when shift amount is 0.
def arshift32(x, y):
    x = x & 0xFFFFFFFF
    # if sign bit is set make into a python negative value
    if (x >= 0x80000000):
        x -= 4294967296;
    result = x
    if (y != 0): result = x >> y
    return result

def bitwiseAnd32(x, y):
    result = x & y
    return arshift32(result, 0)

def bitwiseOr32(x, y):
    result = x | y
    return arshift32(result, 0)

def bitwiseXOr32(x, y):
    result = x ^ y
    return arshift32(result, 0)

def getbyte(file):
    if hasattr(file, "buffer"):
        c = file.buffer.read(1)
    else:
        c = file.read(1)
    if c:
        return ord(c)
    else:
        return None

def getchar(file):
    c = file.read(1)
    if c != "":
        return ord(c)
    else:
        return None

def getline(file):
    line = file.readline()
    if (line == ""):
        return None
    line = line.replace('\n', '')
    return line

def printf(str, *args):
    sys.stdout.write(str % args)

def sprintf(str, *args):
    return str % args

""" Readable switch construction

    Example:
    
    c = 'z'
    for case in switch(c):
        if case('a'): pass # only necessary if the rest of the suite is empty
        if case('b'): pass
        # ...
        if case('y'): pass
        if case('z'):
            print "c is lowercase!"
            break
        if case('A'): pass
        # ...
        if case('Z'):
            print "c is uppercase!"
            break
        if case(): # default
            print "I dunno what c was!"
    
	c = 'A'
	for case in switch(c):
		if case(*string.lowercase): # note the * for unpacking as arguments
			print "c is lowercase!"
			break
		if case(*string.uppercase):
			print "c is uppercase!"
			break
		if case('!', '?', '.'): # normal argument passing style also applies
			print "c is a sentence terminator!"
			break
		if case(): # default
			print "I dunno what c was!"

    source: Brian Beck, PSF License, ActiveState Code
    http://code.activestate.com/recipes/410692/
"""

class switch(object):
    def __init__(self, value):
        self.value = value
        self.fall = False

    def __iter__(self):
        """Return the match method once, then stop"""
        yield self.match
        raise StopIteration

    def match(self, *args):
        """Indicate whether or not to enter a case suite"""
        if self.fall or not args:
            return True
        elif self.value in args: # changed for v1.5, see below
            self.fall = True
            return True
        else:
            return False

def compareTo(a, b):
    return ((a > b) - (a < b))

def prompt(question):
    print(question, end="", flush=True)
    sys.stdin.flush()
    return getline(sys.stdin)
    
def stoiWithDefault(s, d):
    try:
        return int(s)
    except:
        return d

def stodWithDefault(s, d):
    try:
        return float(s)
    except:
        return d

def toHex(x, bits = 64):
    digits = ['0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F']
    if type(x) == str:
        x = ord(x)
    x = int(x)
    if not math.isfinite(x): 
        return "XXXXXXXX"

    result = ""
    nibbles = math.ceil(bits / 4)
    for i in range(0, nibbles):    # Python ints can be infinitely large but print 64 bits
        d = x & 0xf
        result = digits[d] + result
        x >>= 4
    return result

digits = ['0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z']

def toBase(x, base = 10):
    if not math.isfinite(x): return "XXXXXXXX"
    if base < 2 or base > 36: return "BaseError"
    x = int(x)
    if x == 0: return "0"
    result = ""
    neg = False
    if x < 0: x = -x; neg = True
    while x > 0:
        d = x % base
        result = digits[d] + result
        x = math.floor(x / base)
    return "-" + result if neg else result

digitsStr = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

def fromBase(str, base = 10):
    str = str.upper()
    result = 0
    neg = False
    for c in str:
        if c == '-': neg = True; pass
        pos = digitsStr.find(c)
        if pos != -1:
            result *= base
            result += pos
    if neg: result = -result
    return result

class PadAlignType(Enum):
    LEFT = 0
    CENTER = 1
    RIGHT = 2

def padAlign(s, width, which = 0, c = " ", prefix = None, prefix_front = None):
    result = s
    if prefix != None: result = prefix + result
    length = width - len(result)
    if prefix_front != None: length -= len(prefix_front)
    if length > 0:
        for case in switch (which):
            if case (PadAlignType.RIGHT):
                result = c * length + result
                break
            if case (PadAlignType.CENTER):
                result = c * (math.floor(length/2)) + result + c * (math.ceil(length/2))
                break;
            if case():
                result = result + c * length
                break
    if prefix_front != None: result = prefix_front + result
    return result

def buildHeader(header, c = "*", width = 79):
    separator = c * width
    return f"\n{separator}\n{header}\n{separator}\n"

def simpleMessageFormat(fmt, *args):
    replacer = lambda m : str(args[int(m.group(1))])
    return re.sub(r"\{(\d+)\}", replacer, fmt)

def findFirst(regex, search):
    m = regex.search(search)
    if m :
        results = []
        for i in range(0, m.lastindex + 1):
            results.append(m.group(i))
        return results
    else :
        return []
