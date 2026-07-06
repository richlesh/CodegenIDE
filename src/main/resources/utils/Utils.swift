import Foundation

public class BaseError: LocalizedError
{
    let message: String

    public init(_ message: String)
    {
        self.message = message
    }

    public var errorDescription: String?
    {
        return message
    }
}

public class RuntimeError: BaseError
{
    override public var errorDescription: String?
    {
        return "RuntimeError " + message
    }
}

public class NumberFormatError: BaseError
{
    override public var errorDescription: String?
    {
        return "NumberFormatError " + message
    }
}

public class InvalidArgumentError: BaseError
{
    override public var errorDescription: String?
    {
        return "IllegalArgumentError " + message
    }
}

public class Utils : NSObject {
    public static func debug(_ b:Bool, _ msg:String, filePath: String = #file, line: Int = #line) {
        if (b) { print("debug: " + filePath + "(" + String(line) + ") " + msg); }
    }

    public static func ipow(_ base:Int32, _ exponent:Int32) -> Int32 {
        assert(exponent >= 0, "ipow() exponent must be >=0 but was " + toString(exponent));
        var exp:Int32 = exponent;
        var result:Int32 = 1;
        var pow:Int32 = base;
        while (exp != 0) {
            if ((exp & 1) == 1) {
                result *= pow;
            }
            exp >>= 1;
            if (exp != 0) {
                pow *= pow;
            }
        }
        return result;
    }

    public static func lpow(_ base:Int64, _ exponent:Int32) -> Int64 {
        assert(exponent >= 0, "lpow() exponent must be >=0 but was " + toString(exponent));
        var exp:Int32 = exponent;
        var result:Int64 = 1;
        var pow:Int64 = base;
        while (exp != 0) {
            if ((exp & 1) == 1) {
                result *= pow;
            }
            exp >>= 1;
            if (exp != 0) {
                pow *= pow;
            }
        }
        return result;
    }

    private static func elementFormatter(_ value:Any?) -> String {
        if let nonNil = value {
            var result = String(describing: nonNil) // "Optional(12)"
            let mirror = Mirror(reflecting: nonNil)
            if let _ = nonNil as? String {
                result = "\"" + result + "\"";
            } else if let list = nonNil as? [Any] {
                result = listToString(list);
            } else if let map = nonNil as? [AnyHashable:Any] {
                result = mapToString(map);
            } else if mirror.displayStyle == .tuple {
                result = tupleToString(nonNil);
            }
            return result;
        }
        return "nil"
    }

    public static func listToString(_ list:[Any]) -> String {
        let stringArray = list.map { elementFormatter($0) }
        return "[" + stringArray.joined(separator: ", ") + "]"
    }

    public static func mapToString(_ map:[AnyHashable:Any]) -> String {
        var result = ""
        for (key, value) in map {
            if (result.count != 0) {
                result += ", "
            }
            result += elementFormatter(key) + " => " + elementFormatter(value)
        }
        return "{" + result + "}";
    }
    
    public static func tupleToString(_ tuple: Any) -> String {
        let stringArray = Mirror(reflecting: tuple).children.map({ elementFormatter($0.value) })
        return "<" + stringArray.joined(separator: ", ") + ">"
    }

    public static func defined(_ o:Any?) -> Bool {
        return o != nil
    }

    public static func isEmpty(_ s:String?) -> Bool {
        return !defined(s) || s!.count == 0;
    }
    
    public static func ltrim(_ s:String) -> String {
        var view = s[...]
        while view.first?.isWhitespace == true {
            view = view.dropFirst()
        }
        return String(view)
    }

    public static func rtrim(_ s:String) -> String {
        var view = s[...]
        while view.last?.isWhitespace == true {
            view = view.dropLast()
        }
        return String(view)
    }

    public static func trim(_ s:String) -> String {
        return ltrim(rtrim(s));
    }
    
    public static func isBlank(_ s:String?) -> Bool {
        return !defined(s) || trim(s!).count == 0;
    }
    
    public static func chr(_ cp:Int) -> String {
        return String(Character(UnicodeScalar(cp)!))
    }

    // Doesn't work for characters that are composed of two or more codepoints
    public static func ord(_ s:String) -> Int {
        return Int(s.unicodeScalars.first!.value)
    }

    public static func ord(_ c:Character) -> Int {
        return ord(String(c))
    }

    public static func codepoint_at(_ s:String, _ i:Int) -> Int {
        return Int(Utils.substr(s, i, i + 1).unicodeScalars.first!.value)
    }

    // Used to normalize hex value to a negative 32-bit number if high bit is set.
    public static func convertToSigned32(_ x:Int64) -> Int32 {
        var result:Int32
        var x0:Int64 = x & 0xFFFFFFFF
        // if sign bit is set make into a swift negative value
        if (x0 > 0x80000000) {
            x0 = -((~x0 + 1) & 0xFFFFFFFF)
        }
        result = Int32(x0)
        return result
    }

    public static func getbyte(_ fh:FileHandle) -> UInt8? {
        var result:UInt8? = nil
        do {
            var d:Data? = nil
            try d = fh.read(upToCount: 1)
            if (d != nil) {
                result = d!.first!
            }
        }
        catch {
            // nop
        }
        return result;
    }

    private static var getchar_line:String.UnicodeScalarView?
    public static func getchar() -> Int? {
        if getchar_line == nil {
            let line = readLine(strippingNewline: false)
            if (line != nil) {
                getchar_line = line!.unicodeScalars
            }
        }
        if (getchar_line != nil) {
            let c:UnicodeScalar = getchar_line!.removeFirst()
            if getchar_line!.isEmpty {
                getchar_line = nil
            }
            return Int(c.value)
        } else {
            return nil
        }
    }

    public static func Foundation_getchar() -> Character? {
        let i:Int32 = Foundation.getchar();
        if (i != EOF) {
            if let us = Unicode.Scalar(UInt32(i)) {
                let c:Character = Character(us)
                return c;
            }
        }
        return nil;
    }

    public static func fread(_ list: inout [UInt8], _ count: Int, _ fh: FileHandle) -> Int {
        let newCount = min(list.count, count)
        do {
            guard let data = try fh.read(upToCount: newCount) else { return 0 }
            data.copyBytes(to: &list, count: data.count)
            return data.count
        } catch {
            print("Error reading from file handle: \(error)")
            return 0
        }
    }

    public static func filesize(_ fh:FileHandle) -> Int64 {
        var fileSize: UInt64 = 0
        do {
            let curPos = try fh.offset()
            fileSize = try fh.seekToEnd()
            try fh.seek(toOffset: curPos)
        } catch {
            return -1
        }
        return Int64(fileSize)
    }

    public static func filesize(_ filepath: String) -> Int64 {
        var filesize: UInt64 = 0
        do {
            let attributes = try FileManager.default.attributesOfItem(atPath: filepath)
            filesize = (attributes[.size] as? NSNumber)!.uint64Value
        } catch {
            return -1
        }
        return Int64(filesize)
    }

    public static func substr(_ s:String, _ startArg:Int, _ stopArg:Int = -1) -> String {
        let start = s.index(s.startIndex, offsetBy: startArg)
        var stop = s.index(s.endIndex, offsetBy: 0)
        if (stopArg != -1) {
            stop = s.index(s.startIndex, offsetBy: stopArg)
        }
            
        return String(s[start..<stop])
    }

    public static func compareTo(_ a:String, _ b:String) -> Int {
        return (((a > b) ? 1 : 0) - ((a < b) ? 1 : 0))
    }

    public static func prompt(_ question:String) -> String {
        print(question, terminator: "")
        fflush(stdout)
        if let answer = readLine(strippingNewline: true) {
            return answer
        } else {
            return ""
        }
    }

    public static func stoi(_ s:String) throws -> Int32 {
        var i:Int32?
        i = Int32(s)
        if (i == nil) {
            throw NumberFormatError("Invalid Intger Number Format: " + s)
        } else {
            return i!
        }
    }

    public static func stol(_ s:String) throws -> Int64 {
        var i:Int64?
        i = Int64(s)
        if (i == nil) {
            throw NumberFormatError("Invalid Floating Point Number Format: " + s)
        } else {
            return i!
        }
    }

    public static func stod(_ s:String) throws -> Double {
        var i:Double?
        i = Double(s)
        if (i == nil) {
            throw NumberFormatError("Invalid Number Format: " + s)
        } else {
            return i!
        }
    }

    public static func isAlpha(_ c:Character) -> Bool {
        if let us = UnicodeScalar(String(c)) {
            return CharacterSet.letters.contains(us)
        }
        return false
    }

    static let digits = ["0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"]

    public static func toHex<T: BinaryInteger>(_ x: T) -> String {
        var result = ""
        var y = x
        for _ in 0 ..< MemoryLayout.size(ofValue: x) * 2 {
            let d : Int = Int(y & 0xf)
            result = digits[d] + result
            y = y >> 4
        }
        return result
    }

    public static func toHex(_ c:Character) -> String {
        if let us = c.unicodeScalars.first {
            return toHex(us.value);
        } else {
            return "XXXXXXXX";
        }
    }

// BinaryInteger is the protocol for all Integer types, signed or unsigned
    public static func toBase<T: SignedInteger>(_ x:T, base:Int = 10) -> String {
        if base < 2 || base > 36 { return "BaseError" }
        if (x == 0) { return "0" }
        let b = T(base)
        var result = ""
        var neg = false
        var y = x
        if y < 0 { y = -y; neg = true }
        while y > 0 {
            let d : Int = Int(y % b)
            result = digits[d] + result
            y = y / b;
        }
        return neg ? "-" + result : result
    }

    public static func toBase<T: UnsignedInteger>(_ x:T, base:Int = 10) -> String {
        if base < 2 || base > 36 { return "BaseError" }
        if (x == 0) { return "0" }
        let b = T(base)
        var result = ""
        var y = x
        while y > 0 {
            let d : Int = Int(y % b)
            result = digits[d] + result;
            y = y / b;
        }
        return result;
    }

    static let digitsStr = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    public static func fromBase(_ str0:String, base:Int = 10) -> Int64 {
        let str = str0.uppercased()
        var result:Int64 = 0
        var neg:Bool = false
        for c in str {
            if (c == "-") { neg = true; continue }
            let pos = digitsStr.indexOf(String(c))
            if pos != -1 {
                result *= Int64(base)
                result += Int64(pos)
            }
        }
        if neg { result = -result }
        return result
    }

    public enum PadAlignType: Int8 {
        case LEFT = 0
        case CENTER
        case RIGHT
    }

    public static func padAlign(_ s: String, _ width: Int, 
        align: PadAlignType = PadAlignType.LEFT, padWith: Character = " ", 
        prefix: String? = nil, prefix_front: String? = nil) -> String{
        var result = s;
        let c: String = String(padWith);
        if let prefix: String = prefix { result = prefix + result }
        var len = width - result.count;
        if let prefix_front: String = prefix_front { len -= prefix_front.count }
        if len > 0 {
            switch (align) {
                case PadAlignType.RIGHT:
                    result = String(repeating: c, count: len) + result
                    break
                case PadAlignType.CENTER:
                    result = String(repeating: c, count: len/2) + result + String(repeating: c, count: Int(ceil(Double(len) / 2.0)))
                    break
                default:
                    result = result + String(repeating: c, count: len)
                    break
            }
        }
        if let prefix_front: String = prefix_front { result = prefix_front + result }
        return result
    }

    public static func buildHeader(_ header:String, _ c:Character = "*", _ width:Int = 79) -> String {
        let separator = String(repeating: c, count: width)
        return "\n\(separator)\n\(header)\n\(separator)\n"
    }
    
    public static func toString(_ value:Any?) -> String {
        if let nonNil = value {
            return String(describing: nonNil) // "Optional(12)"
        }
        return "nil"
    }

    public static func getTypeName(_ x:Any) -> String {
        return String(describing: type(of: x))
    }

    // Doesn't work!  Can't forward varargs
    public static func printf(_ fmt:String, _ args:CVarArg ...) {
        let s = String(format: fmt, arguments: args)
        print(s)
    }

    public static func simpleMessageFormat(_ fmt:String, _ args:Any ...) -> String {
        var result: String = ""
        let format = fmt as String
        var lastPos = format.startIndex;
        let fmtRange = NSRange(location: 0, length: format.utf16.count)
        let regex = NSRegularExpression("\\{(\\d+)\\}")
        let matches = regex.matches(in: format, range: fmtRange)
        for match in matches {
            let m = match.range
            let sm = Range(m, in: format)!
            let r = match.range(at: 1)
            let sr = Range(r, in: format)!
            let pos = Int(format[sr]) ?? 0
            result = result + format[lastPos..<sm.lowerBound]
            result = result + toString(args[pos])
            lastPos = sm.upperBound
        }
        result = result + format[lastPos..<format.endIndex]
        return result;
    }

    public static func format(_ fmt:String, _ args:Any ...) -> String {
        var result: String = ""
        let format = fmt as String
        var lastPos = format.startIndex;
        let fmtRange = NSRange(location: 0, length: format.utf16.count)
        let regex = NSRegularExpression("\\{(\\d+)(?::(?:(.)?([<>^]))?([- +])?(#)?(0)?(\\d+)?(?:\\.(\\d+))?([bBcdoxXeEfFgGs])?)?\\}")
        let matches = regex.matches(in: format, range: fmtRange)
        for match in matches {
            let m = match.range
            let sm = Range(m, in: format)!
            let r = match.range(at: 1)
            let sr = Range(r, in: format)!
            let pos = Int(format[sr]) ?? 0
            var groups: [String] = []
            groups.append(String(format[sm]))
            for i in 1 ..< match.numberOfRanges {
                let gr = Range(match.range(at: i), in: format) 
                if gr == nil { 
                    groups.append("")
                } else {
                    groups.append(String(format[Range(match.range(at: i), in: format)!]))
                }
            }
            result = result + format[lastPos..<sm.lowerBound]
            result = result + formatReplacement(String(format[sm]), groups[1],
                groups[2], groups[3], groups[4], groups[5], groups[6], groups[7],
                groups[8], groups[9], toString(args[pos]))
            lastPos = sm.upperBound
        }
        result = result + format[lastPos..<format.endIndex]
        return result;
    }
    
    private static func formatReplacement(_ match:String, _ posArg:String, _ fill:String, 
        _ align:String, _ signArg:String, _ alt:String, _ zero:String, _ widthArg:String, 
        _ precisionArg:String, _ typeArg:String, _ value:String) -> String {
        var result  = ""

        var precision: Int, width: Int
        var sign: Character, type: Character
        precision = Int(precisionArg) ?? -1
        width = Int(widthArg) ?? -1
        if signArg == "" { sign = Character("-") }
        else { sign = Character(String(signArg.prefix(1))) }
        if typeArg == "" { type = "s" }
        else { type = Character(String(typeArg.prefix(1))) }

        switch Character(type.lowercased()) {
            case "b": result = toBase(Int64(value) ?? 0, base: 2)
            case "c": 
                let num = Int(value) ?? -1
                if num >= 0 {
                    result = String(Character(UnicodeScalar(num)!))
                } else {
                    result = String(value.prefix(1))
                }
            case "d": result = toBase(Int64(value) ?? 0, base: 10)
            case "o": result = toBase(Int64(value) ?? 0, base: 8)
            case "x": result = toBase(Int64(value) ?? 0, base: 16)
            case "e", "f", "g":
                var fmt: String;
                if precision != -1 {
                    fmt = "%." + String(precision) + type.lowercased()
                } else {
                    fmt = "%" + type.lowercased()
                }
                result = String(format: fmt, Double(value) ?? 0.0)
            default:
                result = value;
                if precision != -1 {
                    result = String(result.prefix(precision))
                }
        }

        var prefix = ""
        var prefix_front = ""

        if "bdoxefgBXEFG".firstIndex(of: type) != nil && result.prefix(1) == "-" {
            prefix = "-"
            result = String(result.suffix(result.count - 1))
        }
        if "bdoxefgBXEFG".firstIndex(of: type) != nil {
            if " +".firstIndex(of: sign) != nil && prefix != "-" {
                prefix = String(sign)
            }
        }
        if alt != "" && "boxBX".firstIndex(of: type) != nil {
            switch type.lowercased() {
                case "b": prefix += "0b"
                case "o": if result != "0" { prefix += "0" }
                case "x": prefix += "0x"
                default: 
                    break;
            }
        }

        var padAlignchar:Character = " ";
        var padAligntype = PadAlignType.RIGHT
        if width != -1 {
            if type == "c" || type == "s" {
                padAligntype = .LEFT
            }
            if fill != "" {
                padAlignchar = Character(fill)
            }
            if align != "" {
                switch Character(String(align.prefix(1))) {
                    case "^":
                        padAligntype = .CENTER
                        break
                    case ">":
                        padAligntype = .RIGHT
                        break
                    default:
                        padAligntype = .LEFT
                        break
                }
            }
            if zero != "" && "bdoxBX".firstIndex(of: type) != nil {
                padAligntype = PadAlignType.RIGHT
                padAlignchar = "0"
                prefix_front = prefix
                prefix = ""
            }
            result = padAlign(result, width, align: padAligntype, padWith: padAlignchar, prefix: prefix, prefix_front: prefix_front)
        } else {
            if prefix != "" {
                result = prefix + result
            }
            if prefix_front != "" {
                result = prefix_front + result
            }
        }
        if "BXEFG".firstIndex(of: type) != nil {
            result = result.uppercased()
        } else if "bxefg".firstIndex(of: type) != nil {
            result = result.lowercased()
        }

        return result;
    }

    public static func exec(_ command: String) -> String {
        let task = Process()
        let pipe = Pipe()
    
        task.standardOutput = pipe
        task.arguments = ["-c", command]
        task.launchPath = "/bin/bash"
        task.launch()
    
        let data = pipe.fileHandleForReading.readDataToEndOfFile()
        let output = String(data: data, encoding: .utf8)!
    
        return output
    }
}

// Lets us construct a regex with a String
extension NSRegularExpression {
    public convenience init(_ pattern:String, options:NSRegularExpression.Options = []) {
        do {
            try self.init(pattern: pattern, options: options)
        } catch {
            preconditionFailure("Illegal regular expression: \(pattern).")
        }
    }
}

extension String {
    public func toDouble() -> Double? {
        return NumberFormatter().number(from: self)?.doubleValue
//        return (self as NSString).doubleValue
    }

// From https://medium.com/@weijentu/find-and-return-the-ranges-of-all-the-occurrences-of-a-given-string-in-swift-2a2015907a0e
    public func indices(of occurrence: String) -> [Int] {
        var indices = [Int]()
        var position = startIndex
        while let range = range(of: occurrence, range: position..<endIndex) {
            let i = distance(from: startIndex,
                             to: range.lowerBound)
            indices.append(i)
            let offset = occurrence.distance(from: occurrence.startIndex,
                                             to: occurrence.endIndex) - 1
            guard let after = index(range.lowerBound,
                                    offsetBy: offset,
                                    limitedBy: endIndex) else {
                                        break
            }
            position = index(after: after)
        }
        return indices
    }

    public func indexOf(_ search:String) -> Int {
        let indexArray = indices(of: search);
        if (indexArray.count == 0) {
            return -1;
        } else {
            return indexArray[0];
        }
    }
    
    public func lastIndexOf(_ search:String) -> Int {
        let indexArray = indices(of: search);
        if (indexArray.count == 0) {
            return -1;
        } else {
            return indexArray[indexArray.count - 1];
        }
    }

    public func match(_ regex:NSRegularExpression) -> Bool {
        let range = NSRange(0..<self.utf16.count)
        return regex.firstMatch(in: self, options: [], range: range) != nil
    }

    public func findFirst(_ regex:NSRegularExpression) -> [String] {
        var result:[String] = []
        if let match = regex.firstMatch(in: self, options: [], 
            range: NSRange(0..<self.utf16.count)) {
            for x in 0 ..< match.numberOfRanges {
                if let mr:Range<Substring.Index> = Range(match.range(at: x), in: self) {
                    result.append(String(self[mr]))
                }
            }
        }
        return result;
    }

    public func findAll(_ regex:NSRegularExpression) -> [String] {
        let str = self as NSString
        var values:[String] = []
        values = regex.matches(in: self, options: [], 
            range: NSRange(0..<self.utf16.count)).map {
            return String(str.substring(with: $0.range))
        }
        return values
    }

    public func splitRegex(_ regex:NSRegularExpression) -> [String] {
        let str = self as NSString
        var startPos:Int = 0
        var values:[String] = []
        values = regex.matches(in: self, options: [], range: NSRange(0..<str.length)).map {
            let prevRange = NSRange(location: startPos, length: $0.range.location - startPos)
            startPos = NSMaxRange($0.range)
            return str.substring(with: prevRange)
        }
        if (startPos != str.length) {
            let finalRange = NSRange(location: startPos, length: str.length - startPos)
            values.append(String(str.substring(with: finalRange)))
        }
        return values
    }

    public func replaceFirst(_ regex:NSRegularExpression, _ replacementStr:String) -> String {
        if let match = regex.firstMatch(in: self, options: [], 
            range: NSRange(0..<self.utf16.count)) {
            let modString = regex.stringByReplacingMatches(in: self, options: [], 
                range: match.range(at: 0), withTemplate: replacementStr)
            return modString
        }
        return self;
    }

    public func replaceAll(_ regex:NSRegularExpression, _ replacementStr:String) -> String {
        let modString = regex.stringByReplacingMatches(in: self, options: [], 
            range: NSRange(0..<self.utf16.count), withTemplate: replacementStr)
        return modString
    }
}

// Source: stackoverflow.com/a/24648951
// Usage:
/*
 if let aStreamReader = StreamReader(path: "/path/to/file") {
    defer {
        aStreamReader.close()
    }
    while let line = aStreamReader.nextLine() {
        print(line)
    }
 }
 */

public class StreamReader  {
    
    let encoding : String.Encoding
    let chunkSize : Int
    var fileHandle : FileHandle!
    let delimData : Data
    var buffer : Data
    var atEof : Bool
    
    public init?(_ path: String, delimiter: String = "\n", encoding: String.Encoding = .utf8,
          chunkSize: Int = 4096) {
        
        guard let fileHandle = FileHandle(forReadingAtPath: path),
            let delimData = delimiter.data(using: encoding) else {
                return nil
        }
        self.encoding = encoding
        self.chunkSize = chunkSize
        self.fileHandle = fileHandle
        self.delimData = delimData
        self.buffer = Data(capacity: chunkSize)
        self.atEof = false
    }
    
    deinit {
        self.close()
    }
    
    var lastline:String? = nil
    var additionalCodepoints:[UnicodeScalar]? = nil
    public func getCodepoint() -> Int? {
        if (additionalCodepoints != nil && !additionalCodepoints!.isEmpty) {
            let result = additionalCodepoints!.removeFirst()
            if (additionalCodepoints!.isEmpty) {
                additionalCodepoints = nil
            }
            return Int(result.value)
        }
        if lastline == nil || lastline!.isEmpty {
            lastline = getLine(includeLineEnd: true)
        }
        if lastline == nil || lastline!.isEmpty {
            return nil
        } else {
            let c : Character = lastline!.removeFirst()
            additionalCodepoints = String(c).unicodeScalars.map{$0}
            let uc:UnicodeScalar = additionalCodepoints!.removeFirst()
            return Int(uc.value)
        }
    }
    
    /// Return next line, or nil on EOF.
    public func getLine(includeLineEnd includeNL : Bool = false) -> String? {
        precondition(fileHandle != nil, "Attempt to read from closed file")
        
        // Read data chunks from file until a line delimiter is found:
        while !atEof {
            if let range = buffer.range(of: delimData) {
                var line:String? = nil
                // Convert complete line (including/excluding the delimiter) to a string:
                if (includeNL) {
                    line = String(data: buffer.subdata(in: 0..<range.upperBound), encoding: encoding)
                } else {
                    line = String(data: buffer.subdata(in: 0..<range.lowerBound), encoding: encoding)
                }
                // Remove line (and the delimiter) from the buffer:
                buffer.removeSubrange(0..<range.upperBound)
                return line
            }
            let tmpData = fileHandle.readData(ofLength: chunkSize)
            if tmpData.count > 0 {
                buffer.append(tmpData)
            } else {
                // EOF or read error.
                atEof = true
                if buffer.count > 0 {
                    // Buffer contains last line in file (not terminated by delimiter).
                    let line = String(data: buffer as Data, encoding: encoding)
                    buffer.count = 0
                    return line
                }
            }
        }
        return nil
    }
    
    /// Start reading from the beginning of file.
    public func rewind() -> Void {
        fileHandle.seek(toFileOffset: 0)
        buffer.count = 0
        atEof = false
    }
    
    /// Close the underlying file. No reading must be done after calling this method.
    public func close() -> Void {
        fileHandle?.closeFile()
        fileHandle = nil
    }
}

extension StreamReader : Sequence {
    public func makeIterator() -> AnyIterator<String> {
        return AnyIterator {
            return self.getLine()
        }
    }
}
