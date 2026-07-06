module.exports = {
    sprintf: require('sprintf-js').sprintf,
    fs: require('fs'),
    readline: require('readline'),
    prompts: require('prompts'),
    child_process: require("child_process"),
    util: require('util'),

    RuntimeError: class extends Error {
      constructor(message) {
        super(message);
        this.name = "RuntimeError";
      }
    },

    NumberFormatError: class extends Error {
      constructor(message) {
        super(message);
        this.name = "NumberFormatError";
      }
    },

    InvalidArgumentError: class extends Error {
      constructor(message) {
        super(message);
        this.name = "InvalidArgumentError";
      }
    },

    IOError: class extends Error {
      constructor(message) {
        super(message);
        this.name = "IOError";
      }
    },

    assert: function(condition, message) {
        if (!condition)
            throw Error('Assert failed: ' + (message || ''));
    },

    debug: function(b, msg) {
        if (b) {
            const err = new Error(msg)
            const lines = err.stack.split("\n");
            const match = lines[2].match(/at.*?([\w\s\\.]+):(\d+):(\d+)/)
            if (match)
                console.log("debug: " + match[1] + "(" + match[2] + ") " + msg);
            else
                console.log("debug: " + msg);
        }
    },

    elementFormatter: function(obj) {
        if (typeof obj === "string") {
            return "\"" + obj + "\"";
        } else if (Array.isArray(obj)) {
            var result = "[";
            for (let i = 0; i < obj.length; i++) {
                if (result.length > 1)
                    result += ", ";
                result += module.exports.elementFormatter(obj[i]);
            }
            result += "]";
            return result;
        } else if (obj instanceof Map) {
            var result = "{";
            for (const [key, value] of obj) {
                if (result.length > 1)
                    result += ", ";
                result += module.exports.elementFormatter(key) + " => " + module.exports.elementFormatter(value);
            }
            result += "}";
            return result;
        }
        return String(obj);
    },

    listOrTupleToString: function(list, isTuple) {
        var result = "";
        if (list == null) return "null"
        for (let x of list) {
            if (result.length != 0)
                result += ", ";
            result += module.exports.elementFormatter(x);
        }
        return (isTuple?"<":"[") + result + (isTuple?">":"]");
    },

    listToString: function(list) {
        return module.exports.listOrTupleToString(list, false);
    },

    tupleToString: function(tuple) {
        return module.exports.listOrTupleToString(tuple, true);
    },

    mapToString: function(map) {
        var result = "";
        for (let [key, value] of map) {
            if (result.length != 0)
                result += ", ";
            result += module.exports.elementFormatter(key) + ' => ' + module.exports.elementFormatter(value);
        }
        return "{" + result + "}";
    },

    intToEnum: function(enumObj, searchValue) {
        for (let key in enumObj) {
            if (enumObj[key] === searchValue)
                return key;
        }
        return null;
    },

    deepCopy: function(obj) {
        if (obj === null) {
           return null;
        } else if (obj instanceof Array) {
            return obj.map(item => module.exports.deepCopy(item));
        } else if (obj instanceof Map) {
            const newMap = new Map();
            for (const [key, value] of obj.entries()) {
                newMap.set(key, module.exports.deepCopy(value));
            }
            return newMap;
        } else if (obj instanceof Set) {
            const newSet = new Set();
            for (const value of set) {
                newSet.add(module.exports.deepCopy(value));
            }
            return newSet;
        } else if (obj instanceof Object) {
            const newObj = {};
            for (const key in obj) {
                newObj[key] = module.exports.deepCopy(obj[key]);
            }
            return newObj;
        } else {
            return obj;
        }
    },

    isEmpty: function(s) {
        return !module.exports.defined(s) || s.length == 0;
//        return Object.keys(s).length === 0;
    },

    defined: function(x) {
        return x !== undefined && x !== null;
    },

    isEmpty: function(s) {
        return !module.exports.defined(s) || s.length == 0;
    },
    
    isBlank: function(s) {
        return !module.exports.defined(s) || module.exports.trim(s).length == 0;
    },
    
    trim: function(x) {
      return x.replace(/^\s+|\s+$/gm, '');
    },

    ltrim: function(x) {
      return x.replace(/^\s+/gm, '');
    },

    rtrim: function(x) {
      return x.replace(/\s+$/gm, '');
    },

    getFD: function(stream) {
        if (stream.fd != null) return Promise.resolve(stream.fd);
        return new Promise((resolve, reject) => {
            stream.on("open", () => resolve(stream.fd));
            stream.on("error", error => reject(error));
        });
    },

    getbyteSync: function(fd) {
        var byte_buffer = new Uint8Array(1);
        var bytes_read = module.exports.fs.readSync(fd, byte_buffer);
        return bytes_read == 0 ? -1 : byte_buffer[0];
    },

    putbyteSync: function(byte, fd) {
        var byte_buffer = new Uint8Array(1);
        byte_buffer[0] = byte;
        var bytes_written = module.exports.fs.writeSync(fd, byte_buffer);
        return bytes_written;
    },

    getbyte: async function(istream) {
        var fd = await module.exports.getFD(istream);
        var bytesRead = 0;
        var buf = Buffer.alloc(1);
        try {
            bytesRead = module.exports.fs.readSync(istream.fd, buf, 0, 1);
        } catch (e) {
            if (e.code === 'EAGAIN') { // 'resource temporarily unavailable'
                // Happens on OS X 10.8.3 (not Windows 7!), if there's no
                // stdin input - typically when invoking a script without any
                // input (for interactive stdin input).
                // If you were to just continue, you'd create a tight loop.
                throw new module.exports.RuntimeError('Interactive stdin input not supported.');
            } else if (e.code === 'EOF') {
                // Happens on Windows 7, but not OS X 10.8.3:
                // simply signals the end of *piped* stdin input.
                return -1;
            }
            throw e; // unexpected exception
        }
        if (bytesRead === 0) {
            // No more stdin input available.
            // OS X 10.8.3: regardless of input method, this is how the end 
            //   of input is signaled.
            // Windows 7: this is how the end of input is signaled for
            //   *interactive* stdin input.
            return -1;
        }
        // Process the chunk read.
        var c = buf.readInt8(0);
        if (c < 0) {
            c += 256;
        }
        return Promise.resolve(c);
    },
    
    getchar: async function(stream) {
        if (module.exports.getcharGen === undefined) {
            module.exports.getcharGen = new Map();
        }
        if (module.exports.getcharGen.get(stream) === undefined) {
            module.exports.getcharGen.set(stream, module.exports.getcharGenerator(stream));
        }
        let c = (await module.exports.getcharGen.get(stream).next()).value;
        return c == undefined ? undefined : c.codePointAt(0);
    },

    getcharGenerator: async function* (stream) {
        let line;
        while ((line = await module.exports.getline(stream)) !== undefined) {
            for (const c of line) {
                yield c;
            }
            yield "\n";
        }
    },

    getline: async function (stream) {
        if (module.exports.getlineGen === undefined) {
            module.exports.getlineGen = new Map();
        }
        if (module.exports.getlineGen.get(stream) === undefined) {
            module.exports.getlineGen.set(stream, module.exports.getlineGenerator(stream));
        }
        return (await module.exports.getlineGen.get(stream).next()).value
    },

    getlineGenerator: async function* (stream) {
        const rl = module.exports.readline.createInterface(stream);
        for await (const line of rl) {
            yield line;
        }
    },

    filesize: function(filepath) {
        try {
            return module.exports.fs.statSync(filepath).size;
        } catch (err) {
        console.log(err);
            return -1;
        }
    },

    cpLength: function(s) {
        var i = 0;
        for (var cp of s) {
            ++i;
        }
        return i;
    },

    cpAt: function(s, pos) {
        var i = 0;
        for (var cp of s) {
            if (i == pos) return cp.codePointAt(0);
            ++i;
        }
        return -1;
    },

    uniAt: function(s, pos) {
        return String.fromCodePoint(module.exports.cpAt(s, pos));
    },

    cpSubstr2End: function(s, pos) {
        var i = 0;
        var sb = "";
        for (var cp of s) {
            if (i >= pos) {
                sb += cp;
            }
            ++i;
        }
        return sb;
    },

    cpSubstr: function(s, pos, length) {
        if (length === undefined) {
            return module.exports.cpSubstr2End(s, pos);
        }
        var i = 0;
        var sb = "";
        for (var cp of s) {
            if (i >= pos) {
                if (length <= 0) break;
                sb += cp;
                --length;
            }
            ++i;
        }
        return sb;
    },

    cpIndexOf: function(s, search, fromIndex) {
        if (fromIndex === undefined) {
            fromIndex = 0;
        }
        var from = module.exports.cvtPosCP2Char(s, fromIndex);
        if (typeof(search) === "number") {
            search = String.fromCodePoint(search);
        }
        var result = s.indexOf(search, from);
        if (result != -1) {
            result = module.exports.cvtPosChar2CP(s, result);
        }
        return result;
    },
    
    cpLastIndexOf: function(s, search, fromIndex) {
        var from;
        if (fromIndex === undefined)
            from = s.length;
        else
            from = module.exports.cvtPosCP2Char(s, fromIndex);
        if (typeof(search) === "number") {
            search = String.fromCodePoint(search);
        }
        var result = s.lastIndexOf(search, from);
        if (result != -1) {
            result = module.exports.cvtPosChar2CP(s, result);
        }
        return result;
    },

    cvtPosCP2Char: function(s, cpPos) {
        var charIndex = 0;
        var cpIndex = 0;
        if (cpPos <= 0) return 0;
        for (var i = 0; i < s.length; ++i) {
            var c = s.charCodeAt(i);
            ++charIndex;
            if (c >= 0xD800 && c <= 0xDBFF) {
                //High-surrogate
            } else if (c >= 0xDC00 && c <= 0xDFFF) {
                ++cpIndex;
            } else {
                ++cpIndex;
            }
            if (cpIndex == cpPos) break;
        }
        return charIndex;
    },

    cvtPosChar2CP: function(s, charPos) {
        var charIndex = 0;
        var cpIndex = 0;
        if (charPos <= 0) return 0;
        for (var i = 0; i < s.length; ++i) {
            var c = s.charCodeAt(i);
            ++charIndex;
            if (c >= 0xD800 && c <= 0xDBFF) {
                // High-surrogate
            } else if (c >= 0xDC00 && c <= 0xDFFF) {
                ++cpIndex;
            } else {
                ++cpIndex;
            }
            if (charIndex == charPos) break;
        }
        return cpIndex;
    },

    prompt: async function(question) {
//        process.stdout.write(question);
//        module.exports.readline.clearLine(process.stdout, true);  // Helps with redirecting input
//        return await module.exports.getline();
        var properties = {message: question, name: 'answer', type: 'text'};
        const response = await module.exports.prompts(properties);
        return response.answer;
    },

    stoi: function(s) {
        var i = parseInt(s);
        if (isNaN(i)) {
            throw new module.exports.NumberFormatError("Invalid Integer Number Format: " + s);
        } else {
            return i;
        }
    },

    stod: function(s) {
        var d = parseFloat(s);
        if (isNaN(d)) {
            throw new module.exports.NumberFormatError("Invalid Floating Point Number Format: " + s);
        } else {
            return d;
        }
    },

    stoiWithDefault: function(s, d) {
        var i = parseInt(s);
        if (isNaN(i)) {
            return d;
        } else {
            return i;
        }
    },

    stodWithDefault: function(s, d) {
        var i = parseFloat(s);
        if (isNaN(i)) {
            return d;
        } else {
            return i;
        }
    },

    toHex: function (x) {
        var digits = ['0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'];
        if (typeof x === 'string') {
            x = x.charCodeAt(0);
        }
        x = Math.floor(x);
        if (!isFinite(x)) return "XXXXXXXX";
    
        var result = "";
        for (var i = 0; i < 14; ++i) {    // Javascript ints can be 52 bits
            var d = x & 0xf;
            result = digits[d] + result;
            x = Math.floor(x / 16);        // >> 4 bitwise only work on 32 bit ints
        }
        return result;
    },

    digits: ['0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'],

    toBase: function (x, base = 10) {
        if (!isFinite(x)) return "XXXXXXXX";
        if (base < 2 || base > 36) return "BaseError";
        x = parseInt(x);
        if (x == 0) return "0";
        var result = "";
        var neg = false;
        if (x < 0) { x = -x; neg = true; }
        while (x > 0) {
            var d = x % base;
            result = module.exports.digits[d] + result;
            x = Math.floor(x / base);
        }
        return neg ? "-" + result : result;
    },

    digitsStr: "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",

    fromBase: function(str, base = 10) {
        str = str.toUpperCase();
        var result = 0;
        var neg = false;
        for (var c of str.split('')) {
            if (c == '-') { neg = true; continue; }
            var pos = module.exports.digitsStr.indexOf(c);
            if (pos != -1) {
                result *= base;
                result += pos;
            }
        }
        if (neg) result = -result;
        return result;
    },

    PadAlignType: {
        LEFT:   0,
        CENTER: 1,
        RIGHT:  2
    },

    padAlign: function (s, width, which = 0, c = " ", prefix, prefix_front) {
        var result = s;
        if (typeof prefix !== 'undefined') result = prefix + result;
        var len = width - result.length;
        if (typeof prefix_front !== 'undefined') len -= prefix_front.toString().length;
        if (len > 0) {
            switch (which) {
                case module.exports.PadAlignType.RIGHT:
                    result = c.repeat(len) + result;
                    break;
                case module.exports.PadAlignType.CENTER:
                    result = c.repeat(Math.floor(len/2.)) + result + c.repeat(Math.ceil(len/2.));
                    break;
                default:
                    result = result + c.repeat(len);
                    break;
            }
        }
        if (typeof prefix_front !== 'undefined') result = prefix_front + result;
        return result;
    },

    buildHeader: function (header, c = "*", width = 79) {
        var separator = c.repeat(width);
        return `\n${separator}\n${header}\n${separator}\n`;
    },

    simpleMessageFormat: function() {
        var args = arguments;
        return args[0].replace(/\{(\d+)\}/g, function(match, p1) {
            return args[parseInt(p1) + 1];
        });
    },

    format: function() {
        var args = arguments;
        return args[0].replace(/\{(\d+)(?::(?:(.)?([<>^]))?([- +])?(#)?(0)?(\d+)?(?:\.(\d+))?([bBcdoxXeEfFgGs])?)?\}/g, 
            function(match, pos, fill, align, sign, alt, zero, width, precision, type) {
                var result = "";
                var fmt;
                if (typeof pos !== 'undefined') pos = parseInt(pos);
                if (typeof precision !== 'undefined') precision = parseInt(precision);
                if (typeof type === 'undefined') type = "s";
                else type = type.toString();
                var value = args[pos + 1];
            
                switch (type.toLowerCase().charAt(0)) {
                    case 'b': result = module.exports.toBase(value, 2); break;
                    case 'c': 
                        if (typeof value === 'number')
                            result = String.fromCodePoint(value);
                        else
                            for (var c of value) {
                                result = c;
                                break;
                            }
                        break;
                    case 'd': result = module.exports.toBase(value, 10); break;
                    case 'o': result = module.exports.toBase(value, 8); break;
                    case 'x': result = module.exports.toBase(value, 16); break;
                    case 'e':
                    case 'f':
                    case 'g':
                        if (typeof precision !== 'undefined')
                            fmt = "%." + precision + type.toLowerCase();
                        else fmt = "%" + type.toLowerCase();
                        result = module.exports.sprintf(fmt, value);
                        break;
                    default:
                        result = value.toString();
                        if (typeof precision !== 'undefined')
                            result = result.substring(0, precision);
                        break;
                }

                var prefix = '';
                var prefix_front;

                if ("bdoxefgBXEFG".indexOf(type) >= 0 && result.charAt(0) == '-') {
                    prefix = '-';
                    result = result.substr(1,result.length);
                }
                if (typeof sign !== 'undefined' && "bdoxefgBXEFG".indexOf(type) >= 0) {
                    if (" +".indexOf(sign) >= 0 && prefix != '-')
                        prefix = sign;
                }
                if (typeof alt !== 'undefined' && "boxBX".indexOf(type) >= 0) {
                    switch (type.toLowerCase()) {
                        case 'b': prefix += '0b'; break;
                        case 'o': if (result != "0") prefix += '0'; break;
                        case 'x': prefix += '0x'; break;
                    }
                }

                var padAlignchar = ' ', padAligntype = module.exports.PadAlignType.RIGHT;
                if (typeof width !== 'undefined') {
                    if (type == 'c' || type == 's') {
                        padAligntype = 0;
                    }
                    if (typeof fill !== 'undefined') {
                        padAlignchar = fill;
                    }
                    if (typeof align !== 'undefined') {
                        switch (align) {
                            default:
                                padAligntype = module.exports.PadAlignType.LEFT;
                                break;
                            case '^':
                                padAligntype = module.exports.PadAlignType.CENTER;
                                break;
                            case '>':
                                padAligntype = module.exports.PadAlignType.RIGHT;
                                break;
                        }
                    }
                    if (typeof zero !== 'undefined' && "bdoxBX".indexOf(type) >= 0) {
                        padAligntype = module.exports.PadAlignType.RIGHT;
                        padAlignchar = '0';
                        prefix_front = prefix;
                        prefix = undefined;
                    }
                    result = module.exports.padAlign(result, width, padAligntype, padAlignchar, prefix, prefix_front);
                } else {
                    if (typeof prefix !== 'undefined')
                        result = prefix + result;
                    if (typeof prefix_front !== 'undefined')
                        result = prefix_front + result;
                }
            
                if ("BXEFG".indexOf(type) >= 0) result = result.toUpperCase();
                else if ("bxefg".indexOf(type) >= 0) result = result.toLowerCase();

                return result;
            }
        );
    },
    
    exec: function(cmd) {
        var args = cmd.split(/ /);
        var cmd = args.shift();
        var child = module.exports.child_process.spawnSync(cmd, args, { encoding : 'utf8' });
        return child.stdout;
    },
    
    match: function(regex, str) {
        let result = str.match(regex);
        if (result == null) return [];
        else return result;
    },
    
    filename: function(path) {
        let groups = module.exports.match(/([- \._\w]+)$/, path);
        if (groups.length == 0) return path;
        else return groups[1];
    }
};

