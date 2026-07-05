#include <array>
#include <cctype>
#include <cmath>
#include <cstdio>
#include <cwchar>
#include <filesystem>
#include <ostream>
#include <string>
#include <fstream>
#include <iostream>
#include <sstream>
#include <unordered_map>
#include <vector>
#include <regex>
#include <algorithm>
#include <iterator>

namespace Utils {

#ifdef NDEBUG
#define assertion(b, msg)
#define invariant(b, msg)
#define precondition(b, msg)
#define postcondition(b, msg)
#define debug(b, msg)
#else
#define assertion(b, msg) Utils::assertWMsg(b, "Assertion", msg, __FILE__, __LINE__)
#define invariant(b, msg) Utils::assertWMsg(b, "Invariant", msg, __FILE__, __LINE__)
#define precondition(b, msg) Utils::assertWMsg(b, "Precondition", msg, __FILE__, __LINE__)
#define postcondition(b, msg) Utils::assertWMsg(b, "Postcondition", msg, __FILE__, __LINE__)
#define debug(b, msg) Utils::debugWMsg(b, msg, __FILE__, __LINE__)
#endif

//const std::basic_string<T> WHITESPACE = " \n\r    \f\v";

template<class Char>
struct string_traits;

template<>
struct string_traits<char>{
  template<class T>
  static std::basic_string<char> convert_to_string(T&& t){
    return std::to_string(std::forward<T>(t));
  }
};

template<>
struct string_traits<wchar_t>{
  template<class T>
  static std::basic_string<wchar_t> convert_to_string(T&& t){
    return std::to_wstring(std::forward<T>(t));
  }
};

template<class T>
struct cout_trait {};

template<>
struct cout_trait<char> {
    using type = decltype(std::cout);
    static constexpr type& cout = std::cout;
    static constexpr type& cerr = std::cerr;
    static constexpr type& clog = std::clog;
    static constexpr decltype(std::cin)& cin = std::cin;
};

template<>
struct cout_trait<wchar_t> {
    using type = decltype(std::wcout);
    static constexpr type& cout = std::wcout;
    static constexpr type& cerr = std::wcerr;
    static constexpr type& clog = std::wclog;
    static constexpr decltype(std::wcin)& cin = std::wcin;
};

template <class T>
bool defined(const T* o) {
    return o != NULL;
}

template <class T>
bool defined(T o) {
    return true;
}

template <typename Ch>
std::basic_string<Ch> any_string(const char *literal) {
    std::vector<Ch> r = {};

    size_t len = strlen(literal);
    for (size_t i = 0; i < len; i++)
        r.push_back(literal[i]);

    return std::basic_string<Ch>(r.cbegin(), r.cend());
}

template <typename Ch>
std::basic_string<Ch> any_string(const wchar_t * literal) {
    std::vector<Ch> r = {};

    size_t len = wcslen(literal);
    for (size_t i = 0; i < len; i++)
        r.push_back(literal[i]);

    return std::basic_string<Ch>(r.cbegin(), r.cend());
}

template<class Ch>
void assertWMsg(bool b, const char * type, std::basic_string<Ch> msg, const char * file, int line) {
    if (!b) {
        cout_trait<Ch>::cerr << any_string<Ch>(type) << any_string<Ch>(" failed: ") << msg << any_string<Ch>(", File: ") 
           << any_string<Ch>(file) << any_string<Ch>(" Line: ") << line << any_string<Ch>(".") << std::endl;
        exit(1);
    }
}

template<class Ch>
void assertWMsg(bool b, const char * type, const Ch * msg, const char * file, int line) {
    if (!b) {
        cout_trait<Ch>::cerr << any_string<Ch>(type) << any_string<Ch>(" failed: ") << msg << any_string<Ch>(", File: ") 
           << any_string<Ch>(file) << any_string<Ch>(" Line: ") << line << any_string<Ch>(".") << std::endl;
       exit(1);
    }
}

template<class Ch>
void debugWMsg(bool b, std::basic_string<Ch> msg, const char * file, int line) {
    if (b) {
        cout_trait<Ch>::cout << any_string<Ch>("debug: ") << any_string<Ch>(file)
        	<< any_string<Ch>("(") << line << any_string<Ch>(") ") << msg << std::endl;
    }
}

template<class Ch>
void debugWMsg(bool b, const Ch * msg, const char * file, int line) {
    if (b) {
        cout_trait<Ch>::cout << any_string<Ch>("debug: ") << any_string<Ch>(file)
        	<< any_string<Ch>("(") << line << any_string<Ch>(") ") << msg << std::endl;
    }
}

template<class Ch>
std::basic_string<Ch> exceptionMessage(const std::exception &ex) {
    return any_string<Ch>(typeid(ex).name()) + any_string<Ch>(": ") + any_string<Ch>(ex.what());
}

long ipow(int base, int exp) {
	precondition(exp >= 0, "ipow() exponent must be >=0 but was " + std::to_string(exp));
	long result = 1;
	long pow = base;
	while (exp != 0) {
		if (exp & 1) {
			result *= pow;
		}
		exp >>= 1;
		pow *= pow;
	}
	return result;
}

long lpow(long base, int exp) {
	precondition(exp >= 0, "lpow() exponent must be >=0 but was " + std::to_string(exp));
	long result = 1;
	long pow = base;
	while (exp != 0) {
		if (exp & 1) {
			result *= pow;
		}
		exp >>= 1;
		pow *= pow;
	}
	return result;
}

template<class T>
T elementFormatter(const T& t) {
    return t;
}

template<class T>
T welementFormatter(const T& t) {
    return t;
}

template<class T>
const T* elementFormatter(const T* t) {
    return t;
}

template<class T>
const T* welementFormatter(const T* t) {
    return t;
}

std::string elementFormatter(bool b) {
    return b ? "true" : "false";
}

std::wstring welementFormatter(bool b) {
    return b ? L"true" : L"false";
}

std::string elementFormatter(const std::string& t) {
    std::stringstream ss;
    ss << "\"" << t << "\"";
    return ss.str();
}

std::wstring UTF8_to_wstring(const std::string &in);

std::wstring welementFormatter(const std::string& t) {
    std::wstringstream ss;
    ss << L"\"" << UTF8_to_wstring(t) << L"\"";
    return ss.str();
}

std::string wchar_to_UTF8(const std::wstring &in);

std::string elementFormatter(const std::wstring& t) {
    std::stringstream ss;
    ss << "\"" << wchar_to_UTF8(t) << "\"";
    return ss.str();
}

std::wstring welementFormatter(const std::wstring& t) {
    std::wstringstream ss;
    ss << L"\"" << t << L"\"";
    return ss.str();
}

std::string elementFormatter(const char * t) {
    return elementFormatter(std::string(t));
}

std::wstring welementFormatter(const char * t) {
    return welementFormatter(UTF8_to_wstring(t));
}

std::string elementFormatter(const wchar_t * t) {
    return elementFormatter(wchar_to_UTF8(t));
}

std::wstring welementFormatter(const wchar_t * t) {
    return welementFormatter(std::wstring(t));
}

template<class T>
std::string to_string(const std::vector<T> &v);

template<class T>
std::wstring to_wstring(const std::vector<T> &v);

template<class T>
std::string elementFormatter(const std::vector<T> &v) {
    return to_string(v);
}

template<class T>
std::wstring welementFormatter(const std::vector<T> &v) {
    return to_wstring(v);
}

template<typename K, typename V>
std::string to_string(const std::unordered_map<K, V> &m);

template<typename K, typename V>
std::wstring to_wstring(const std::unordered_map<K, V> &m);

template<typename K, typename V>
std::string elementFormatter(const std::unordered_map<K, V> &m) {
    return to_string(m);
}

template<typename K, typename V>
std::wstring welementFormatter(const std::unordered_map<K, V> &m) {
    return to_wstring(m);
}

template<class... Args>
std::string to_string(const std::tuple<Args...>& t) ;

template<class... Args>
std::wstring to_wstring(const std::tuple<Args...>& t) ;

template<class... Args>
std::string elementFormatter(const std::tuple<Args...>& t) {
	return to_string(t);
}

template<class... Args>
std::wstring welementFormatter(const std::tuple<Args...>& t) {
	return to_wstring(t);
}

template<typename T>
std::string to_string(const std::vector<T> &v)
{
    std::stringstream ss;
    bool first = true;
    ss << "[";
    for (auto iter = v.begin(); iter != v.end(); ++iter) {
        if (!first)
            ss << ", ";
        ss << elementFormatter(*iter);
        first = false;
    }
    ss << "]";
    return ss.str();
}

template<typename T>
std::ostream& operator<<(std::ostream &os, const std::vector<T> &v) {
	return os << to_string(v);
}

template<typename T>
std::wstring to_wstring(const std::vector<T> &v)
{
    std::wstringstream ss;
    bool first = true;
    ss << L"[";
    for (auto iter = v.begin(); iter != v.end(); ++iter) {
        if (!first)
            ss << L", ";
        ss << welementFormatter(*iter);
        first = false;
    }
    ss << L"]";
    return ss.str();
}

template<typename T>
std::wostream& operator<<(std::wostream &os, const std::vector<T> &v) {
	return os << to_wstring(v);
}

template<typename T>
std::string join(std::string separator, const std::vector<T> &v)
{
    std::stringstream ss;
    bool first = true;
    for (auto iter = v.begin(); iter != v.end(); ++iter) {
        if (!first)
            ss << separator;
        ss << *iter;
        first = false;
    }
    return ss.str();
}

template<typename T>
std::wstring join(std::wstring separator, const std::vector<T> &v)
{
    std::wstringstream ss;
    bool first = true;
    for (auto iter = v.begin(); iter != v.end(); ++iter) {
        if (!first)
            ss << separator;
        ss << *iter;
        first = false;
    }
    return ss.str();
}

template<typename K, typename V>
std::string to_string(const std::unordered_map<K,V> & m)
{
    std::stringstream ss;
    bool first = true;
    ss << "{";
    for (const auto& kvp : m) {
        if (!first)
            ss << ", ";
        ss << elementFormatter(kvp.first) << " => " << elementFormatter(kvp.second);
        first = false;
    }
    ss << "}";
    return ss.str();
}

template<typename K, typename V>
std::ostream& operator<<(std::ostream &os, const std::unordered_map<K,V> & m) {
	return os << to_string(m);
}

template<typename K, typename V>
std::wstring to_wstring(const std::unordered_map<K,V> & m)
{
    std::wstringstream ss;
    bool first = true;
    ss << L"{";
    for (const auto& kvp : m) {
        if (!first)
            ss << L", ";
        ss << welementFormatter(kvp.first) << L" => " << welementFormatter(kvp.second);
        first = false;
    }
    ss << L"}";
    return ss.str();
}

template<typename K, typename V>
std::wostream& operator<<(std::wostream &os, const std::unordered_map<K,V> & m) {
	return os << to_wstring(m);
}

template<typename K, typename V>
std::vector<K> keys(const std::unordered_map<K,V> & m)
{
    std::vector<K> r;
    r.reserve(m.size());
    for (const auto& kvp : m) {
        r.push_back(kvp.first);
    }
    std::sort (r.begin(), r.end());
    return r;
}

template<typename K, typename V>
std::vector<V> values(const std::unordered_map<K,V> & m)
{
    std::vector<V> r;
    r.reserve(m.size());
    for (const auto& kvp : m) {
        r.push_back(kvp.second);
    }
    return r;
}

// helper function to print a tuple of any size
template<class Tuple, std::size_t N>
struct TuplePrinter {
    static std::string to_string(const Tuple& t) {
        std::stringstream ss;
        ss << TuplePrinter<Tuple, N-1>::to_string(t);
        ss << ", " << elementFormatter(std::get<N-1>(t));
        return ss.str();
    }

    static std::wstring to_wstring(const Tuple& t) {
        std::wstringstream ss;
        ss << TuplePrinter<Tuple, N-1>::to_wstring(t);
        ss << L", " << welementFormatter(std::get<N-1>(t));
        return ss.str();
    }
};

template<class Tuple>
struct TuplePrinter<Tuple, 1> {
    static std::string to_string(const Tuple& t) {
        std::stringstream ss;
        ss << elementFormatter(std::get<0>(t));
        return ss.str();
    }

    static std::wstring to_wstring(const Tuple& t) {
        std::wstringstream ss;
        ss << welementFormatter(std::get<0>(t));
        return ss.str();
    }
};

template<class... Args>
std::string to_string(const std::tuple<Args...>& t) 
{
    std::stringstream ss;
    ss << "<";
    ss << TuplePrinter<decltype(t), sizeof...(Args)>::to_string(t);
    ss << ">";
    return ss.str();
}

template<class... Args>
std::ostream& operator<<(std::ostream &os, const std::tuple<Args...>& t) {
	return os << to_string(t);
}

template<class... Args>
std::wstring to_wstring(const std::tuple<Args...>& t) 
{
    std::wstringstream ss;
    ss << "<";
    ss << TuplePrinter<decltype(t), sizeof...(Args)>::to_wstring(t);
    ss << ">";
    return ss.str();
}

template<class... Args>
std::wostream& operator<<(std::wostream &os, const std::tuple<Args...>& t) {
	return os << to_wstring(t);
}

// Get enum underlying type value
template <typename E>
constexpr auto to_underlying(E e) noexcept -> typename std::underlying_type<E>::type
{
    return static_cast<typename std::underlying_type<E>::type>(e);
}

// end helper function

template <typename Ch>
std::basic_string<Ch> ltrim(const std::basic_string<Ch>& s)
{
    typename std::basic_string<Ch>::size_type i;
    for (i = 0; i < s.size(); ++i)
        if (!isspace(s[i])) break;
    return s.substr(i);
//    size_t start = s.find_first_not_of(WHITESPACE);
//    return (start == std::basic_string<T>::npos) ? "" : s.substr(start);
}

template <typename Ch>
std::basic_string<Ch> rtrim(const std::basic_string<Ch>& s)
{
    typename std::basic_string<Ch>::size_type i;
    for (i = s.size() - 1; i >= 0; --i)
        if (!isspace(s[i])) break;
    return s.substr(0, i + 1);
//    size_t end = s.find_last_not_of(WHITESPACE);
//    return (end == std::basic_string<T>::npos) ? "" : s.substr(0, end + 1);
}

template <typename Ch>
std::basic_string<Ch> trim(const std::basic_string<Ch>& s)
{
    return rtrim(ltrim(s));
}

template <typename Ch>
bool isBlank(const std::basic_string<Ch>& s)
{
    return trim(s).length() == 0;
}

template <typename Ch>
std::basic_string<Ch> tolower(const std::basic_string<Ch>& s)
{
    std::basic_string<Ch> sl = s;
    transform(sl.begin(), sl.end(), sl.begin(), ::tolower);
    return sl;
}
template <typename Ch>
std::basic_string<Ch> toupper(const std::basic_string<Ch>& s)
{
    std::basic_string<Ch> sl = s;
    transform(sl.begin(), sl.end(), sl.begin(), ::toupper);
    return sl;
}

template <typename Item>
bool getbytes(std::istream& is, Item& i)
{
    is.read((char*)(&i), sizeof(i));
    return is.good();
}

template <typename Ch>
std::basic_string<Ch> prompt(const Ch* question) {
    cout_trait<Ch>::cout << question << std::flush;
    std::basic_string<Ch> answer;
    std::getline(cout_trait<Ch>::cin, answer);
    return answer;
}

template <typename Ch>
std::basic_string<Ch> prompt(const std::basic_string<Ch>& question) {
    return prompt(question.c_str());
}

template <typename Ch>
int stoiWithDefault(const std::basic_string<Ch> &s, int d) {
    try {
        return stoi(s);
    } catch (...) {
        return d;
    }
}

template <typename Ch>
int stoiWithDefault(const Ch *s, int d) {
    return stoiWithDefault(std::basic_string<Ch>(s), d);
}

template <typename T>
long stolWithDefault(const std::basic_string<T> &s, long d) {
    try {
        return stol(s);
    } catch (...) {
        return d;
    }
}

template <typename Ch>
long stolWithDefault(const Ch *s, int d) {
    return stolWithDefault(std::basic_string<Ch>(s), d);
}

template <typename Ch>
double stodWithDefault(const std::basic_string<Ch> &s, double d) {
    try {
        return stod(s);
    } catch (...) {
        return d;
    }
}

template <typename Ch>
double stodWithDefault(const Ch *s, int d) {
    return stodWithDefault(std::basic_string<Ch>(s), d);
}

template <typename Ch>
std::basic_string<Ch> repeat(const std::basic_string<Ch>& input, unsigned count)
{
    if (count < 1) return any_string<Ch>("");
    if (count == 1) return input;
    std::basic_string<Ch> result;
    result.reserve(input.size() * count);
    while (count--)
        result += input;
    return result;
}

template <typename Ch, typename T>
std::basic_string<Ch> toHex(T x) {
    Ch digits[] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    std::basic_string<Ch> result;
    for (int i = 0; i < sizeof(T) * 2; ++i) {
        int d = x & 0xf;
        result = digits[d] + result;
        x >>= 4;
    }
    return result;
}

template <typename Ch, typename T>
std::basic_string<Ch> toBase(T x, int base = 10) {
    static Ch digits[] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};
    if (base < 2 || base > 36) return any_string<Ch>("BaseError");
    if (x == 0) return any_string<Ch>("0");
    std::basic_string<Ch> result;
    bool neg = false;
    if (x < 0) { x = -x; neg = true; }
    while (x > 0) {
        int d = x % base;
        result = digits[d] + result;
        x = x / base;
    }
    return neg ? any_string<Ch>("-") + result : result;
}

template <typename T, typename Ch>
T fromBase(std::basic_string<Ch> str, int base = 10) {
    static std::basic_string<Ch> digits = any_string<Ch>("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    str = Utils::toupper(str);
    T result = 0;
    bool neg = false;
    for (auto c : str) {
        if (c == '-') { neg = true; continue; }
        int pos = digits.find(c);
        if (pos != std::basic_string<Ch>::npos) {
            result *= base;
            result += pos;
        }
    }
    if (neg) result = -result;
    return result;
}

enum class PadAlignType {LEFT = 0, CENTER, RIGHT};

template<typename Ch>
std::basic_string<Ch> padAlign(const Ch* s, int width, 
    PadAlignType which = PadAlignType::LEFT, const Ch *c = NULL,
    const Ch *prefix = NULL, const Ch *prefix_front = NULL) {
    return padAlign(std::basic_string<Ch>(s), width, which, 
        c == NULL ? any_string<Ch>(" ") : std::basic_string<Ch>(c), 
        prefix == NULL ? any_string<Ch>("") : std::basic_string<Ch>(prefix),
        prefix_front == NULL ? any_string<Ch>("") : std::basic_string<Ch>(prefix_front));
}

template<typename Ch>
std::basic_string<Ch> padAlign(const std::basic_string<Ch> &s, int width, 
    PadAlignType which, const Ch *c, const Ch *prefix = NULL, 
    const Ch *prefix_front = NULL) {
    return padAlign(s, width, which, 
        c == NULL ? any_string<Ch>(" ") : std::basic_string<Ch>(c), 
        prefix == NULL ? any_string<Ch>("") : std::basic_string<Ch>(prefix),
        prefix_front == NULL ? any_string<Ch>("") : std::basic_string<Ch>(prefix_front));
}

template<typename Ch>
std::basic_string<Ch> padAlign(const std::basic_string<Ch> &s, int width, 
    PadAlignType which = PadAlignType::LEFT, 
    const std::basic_string<Ch> &padCh = any_string<Ch>(" "), 
    const std::basic_string<Ch> &prefix = any_string<Ch>(""),
    const std::basic_string<Ch> &prefix_front = any_string<Ch>("")) {
    Ch c = ' ';
    if (!isBlank(padCh)) c = padCh.front();
    std::basic_string<Ch> result = s;
    result = prefix + result;
    int len = width - result.size();
    len -= prefix_front.size();
    if (len > 0) {
        switch (which) {
            case PadAlignType::LEFT:
                result = result + std::basic_string<Ch>(len, c);
                break;
            default:
                result = std::basic_string<Ch>(len, c) + result;
                break;
            case PadAlignType::CENTER:
                result = std::basic_string<Ch>(floor(len/2.), c) + result + 
                    std::basic_string<Ch>(ceil(len/2.), c);
                break;
        }
    }
    result = prefix_front + result;
    return result;
}

template<typename Ch>
std::basic_string<Ch> buildHeader(std::basic_string<Ch> header, Ch c = '*', 
    int width = 79) {
    std::basic_string<Ch> separator = std::basic_string<Ch>(width, c);
    return any_string<Ch>("\n") + separator + any_string<Ch>("\n") + header + 
        any_string<Ch>("\n") + separator + any_string<Ch>("\n");
}

template<typename Ch>
std::basic_string<Ch> buildHeader(const Ch* header, Ch c = '*', 
    int width = 79) {
    return buildHeader(std::basic_string<Ch>(header), c, width);
}

template <typename... Types> 
std::string sprintf(const std::string fmt, Types... args) 
{
    char buffer[1024];
    std::snprintf(buffer, 1024, fmt.c_str(), args...);
    return std::string(buffer);
}

template <typename... Types> 
std::string sprintf(const char* fmt, Types... args) 
{
    char buffer[1024];
    std::snprintf(buffer, 1024, fmt, args...);
    return std::string(buffer);
}

template <typename... Types> 
std::wstring sprintf(const std::wstring fmt, Types... args) 
{
    wchar_t buffer[1024];
    std::swprintf(buffer, 1024, fmt.c_str(), args...);
    return std::wstring(buffer);
}

template <typename... Types> 
std::wstring sprintf(const wchar_t* fmt, Types... args) 
{
    wchar_t buffer[1024];
    std::swprintf(buffer, 1024, fmt, args...);
    return std::wstring(buffer);
}

template<typename Ch>
void findAndReplaceAll(std::basic_string<Ch> & data, 
    std::basic_string<Ch> toSearch, std::basic_string<Ch> replaceStr) {
    // Get the first occurrence
    size_t pos = data.find(toSearch);
    // Repeat till end is reached
    while( pos != std::basic_string<Ch>::npos)
    {
        // Replace this occurrence of Sub String
        data.replace(pos, toSearch.size(), replaceStr);
        // Get the next occurrence from the current position
        pos = data.find(toSearch, pos + replaceStr.size());
    }
}

template<typename Ch>
void simpleMessageFormat0(std::basic_string<Ch> & fmt, int pos) {
  return;
}

template <typename Ch, typename T, typename... Types> 
void simpleMessageFormat0(std::basic_string<Ch> & fmt, int pos, T arg, 
    Types... args) 
{ 
    findAndReplaceAll(fmt, any_string<Ch>("{") + 
        string_traits<Ch>::convert_to_string(pos) + 
        any_string<Ch>("}"), string_traits<Ch>::convert_to_string(arg));
  
    simpleMessageFormat0(fmt, pos + 1, args...) ; 
} 

template <typename Ch, typename... Types> 
void simpleMessageFormat0(std::basic_string<Ch> & fmt, int pos, const Ch* arg, 
    Types... args) 
{ 
    findAndReplaceAll(fmt, any_string<Ch>("{") + 
        string_traits<Ch>::convert_to_string(pos) + 
        any_string<Ch>("}"), std::basic_string<Ch>(arg));
  
    simpleMessageFormat0(fmt, pos + 1, args...) ; 
} 

template <typename Ch, typename... Types> 
void simpleMessageFormat0(std::basic_string<Ch> & fmt, int pos, 
    const std::basic_string<Ch> & arg, Types... args) 
{ 
    findAndReplaceAll(fmt, any_string<Ch>("{") + 
        string_traits<Ch>::convert_to_string(pos) + 
        any_string<Ch>("}"), arg);
  
    simpleMessageFormat0(fmt, pos + 1, args...) ; 
} 

template <typename Ch, typename... Types> 
std::basic_string<Ch> simpleMessageFormat(std::basic_string<Ch> fmt, Types... args) {
    simpleMessageFormat0(fmt, 0, args...);
    return fmt;
}

template <typename Ch, typename... Types> 
std::basic_string<Ch> simpleMessageFormat(const Ch *fmt, Types... args) {
    std::basic_string<Ch> fmt0 = fmt;
    simpleMessageFormat0(fmt0, 0, args...);
    return fmt0;
}

// Thanks to Mark Ransom https://stackoverflow.com/questions/148403/utf8-to-from-wide-char-conversion-in-stl/14809553
std::string wchar_to_UTF8(const wchar_t * in)
{
    std::string out;
    unsigned int codepoint = 0;
    for (;  *in != 0;  ++in)
    {
        if (*in >= 0xd800 && *in <= 0xdbff)
            codepoint = ((*in - 0xd800) << 10) + 0x10000;
        else
        {
            if (*in >= 0xdc00 && *in <= 0xdfff)
                codepoint |= *in - 0xdc00;
            else
                codepoint = *in;

            if (codepoint <= 0x7f)
                out.append(1, static_cast<char>(codepoint));
            else if (codepoint <= 0x7ff)
            {
                out.append(1, static_cast<char>(0xc0 | ((codepoint >> 6) & 0x1f)));
                out.append(1, static_cast<char>(0x80 | (codepoint & 0x3f)));
            }
            else if (codepoint <= 0xffff)
            {
                out.append(1, static_cast<char>(0xe0 | ((codepoint >> 12) & 0x0f)));
                out.append(1, static_cast<char>(0x80 | ((codepoint >> 6) & 0x3f)));
                out.append(1, static_cast<char>(0x80 | (codepoint & 0x3f)));
            }
            else
            {
                out.append(1, static_cast<char>(0xf0 | ((codepoint >> 18) & 0x07)));
                out.append(1, static_cast<char>(0x80 | ((codepoint >> 12) & 0x3f)));
                out.append(1, static_cast<char>(0x80 | ((codepoint >> 6) & 0x3f)));
                out.append(1, static_cast<char>(0x80 | (codepoint & 0x3f)));
            }
            codepoint = 0;
        }
    }
    return out;
}

std::string wchar_to_UTF8(const std::wstring &in) {
    return wchar_to_UTF8(in.c_str());
}

// Thanks to Mark Ransom https://stackoverflow.com/questions/148403/utf8-to-from-wide-char-conversion-in-stl/14809553
std::wstring UTF8_to_wstring(const char * in)
{
    std::wstring out;
    unsigned int codepoint;
    while (*in != 0)
    {
        unsigned char ch = static_cast<unsigned char>(*in);
        if (ch <= 0x7f)
            codepoint = ch;
        else if (ch <= 0xbf)
            codepoint = (codepoint << 6) | (ch & 0x3f);
        else if (ch <= 0xdf)
            codepoint = ch & 0x1f;
        else if (ch <= 0xef)
            codepoint = ch & 0x0f;
        else
            codepoint = ch & 0x07;
        ++in;
        if (((*in & 0xc0) != 0x80) && (codepoint <= 0x10ffff))
        {
            if (sizeof(wchar_t) > 2)
                out.append(1, static_cast<wchar_t>(codepoint));
            else if (codepoint > 0xffff)
            {
                out.append(1, static_cast<wchar_t>(0xd800 + (codepoint >> 10)));
                out.append(1, static_cast<wchar_t>(0xdc00 + (codepoint & 0x03ff)));
            }
            else if (codepoint < 0xd800 || codepoint >= 0xe000)
                out.append(1, static_cast<wchar_t>(codepoint));
        }
    }
    return out;
}

std::wstring UTF8_to_wstring(const std::string &in) {
    return UTF8_to_wstring(in.c_str());
}

std::string exec(std::string cmd) {
    std::array<char, 128> buffer;
    std::string result;

    auto pipe = popen(cmd.c_str(), "r");

    if (!pipe) return "Can't run '" + cmd + "'";

    while (!feof(pipe)) {
        if (fgets(buffer.data(), 128, pipe) != nullptr)
            result += buffer.data();
    }

    auto rc = pclose(pipe);

    if (rc == EXIT_SUCCESS) { // == 0

    } else {

    }
    return result;
}

std::string exec(const char * cmd) {
    return Utils::exec(std::string(cmd));
}

template<class T>
void push(std::vector<T> &v, T val) {
    v.push_back(val);
}

template<class T>
T pop(std::vector<T> &v) {
    T temp = v.back();
    v.pop_back();
    return temp;
}

template<class T>
T peek(std::vector<T> &v) {
    T temp = v.back();
    return temp;
}

template<class T>
void unshift(std::vector<T> &v, T val) {
    v.insert(v.begin(), val);
}

template<class T>
T shift(std::vector<T> &v) {
    T temp = v.front();
    v.erase(v.begin());
    return temp;
}

template<typename T>
std::vector<T> sort(const std::vector<T> &v)
{
    std::vector<T> newV;
    newV.assign(v.begin(), v.end());
    std::stable_sort(newV.begin(), newV.end());
    return newV;
}

std::vector<std::string> split(const std::string regex_str, const std::string str)
{
    std::regex re = std::regex(regex_str);
    return { std::sregex_token_iterator(str.begin(), str.end(), re, -1), std::sregex_token_iterator() };
}

std::vector<std::wstring> split(const std::wstring regex_str, const std::wstring str)
{
    std::wregex re = std::wregex(regex_str);
    return { std::wsregex_token_iterator(str.begin(), str.end(), re, -1), std::wsregex_token_iterator() };
}

std::vector<std::string> split(const std::regex regex, const std::string str)
{
    return { std::sregex_token_iterator(str.begin(), str.end(), regex, -1), std::sregex_token_iterator() };
}

std::vector<std::wstring> split(const std::wregex regex, const std::wstring str)
{
    return { std::wsregex_token_iterator(str.begin(), str.end(), regex, -1), std::wsregex_token_iterator() };
}

std::vector<std::string> regex_findfirst(const std::regex re, const std::string s) {
    std::smatch m;
    std::vector<std::string> results;

    if (std::regex_search(s, m, re)) {
        for (auto x:m) results.push_back(x);
    }
    return results;
}

std::vector<std::string> regex_findall(const std::regex re, const std::string s) {
    std::string remaining = s;
    std::smatch m;
    std::vector<std::string> results;

    while (std::regex_search(remaining, m, re)) {
        results.push_back(m[0]);
        remaining = m.suffix().str();
    }
    return results;
}

std::vector<std::wstring> regex_findfirst(const std::wregex re, const std::wstring s) {
    std::wsmatch m;
    std::vector<std::wstring> results;

    if (std::regex_search(s, m, re)) {
        for (auto x:m) results.push_back(x);
    }
    return results;
}

std::vector<std::wstring> regex_findall(const std::wregex re, const std::wstring s) {
    std::wstring remaining = s;
    std::wsmatch m;
    std::vector<std::wstring> results;

    while (std::regex_search(remaining, m, re)) {
        results.push_back(m[0]);
        remaining = m.suffix().str();
    }
    return results;
}

long filesize(std::istream &fs) {
	long cur_pos = fs.tellg();
    fs.seekg(0L, std::ios_base::seekdir::end);
    long file_size = fs.tellg();
    fs.seekg(cur_pos, std::ios_base::seekdir::beg);
	return file_size;
}

long filesize(std::string filepath){
  std::error_code ec;  // To store potential errors
  // Get file size with error handling
  auto filesize = std::filesystem::file_size(filepath, ec);
  if (!ec) {
    return filesize;
  } else {
    return -1;
  }
}

long filesize(std::wstring filepath){
  std::error_code ec;  // To store potential errors
  // Get file size with error handling
  auto filesize = std::filesystem::file_size(filepath, ec);
  if (!ec) {
    return filesize;
  } else {
    return -1;
  }
}
}
