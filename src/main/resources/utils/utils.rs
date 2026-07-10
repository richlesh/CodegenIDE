#![allow(dead_code)]
#![allow(unused_macros)]

use std::any::Any;
use std::error::Error;
use std::fmt::Display;
use std::fs::{File};
use std::collections::HashMap;
use std::io::{BufReader, Read, Seek, SeekFrom, Write, stdin, stdout};
use regex::Regex;

pub fn debug(b:bool, msg:String, file:&str, line:u32) {
    if b { println!("Debug: {}({}) {}", file, line, msg); }
}

// https://fettblog.eu/rust-enums-wrapping-errors/
#[derive(Debug)]
pub enum CustomError { 
    GenericError(String),
    RuntimeError(String),
    NumberFormatError(String),
    InvalidArgumentError(String),
    IOError(String)
}

impl Display for CustomError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            CustomError::GenericError(msg) => 
                write!(f, "GenericError {}", msg),
            CustomError::RuntimeError(msg) => 
                write!(f, "RuntimeError {}", msg),
            CustomError::NumberFormatError(msg) => 
                write!(f, "NumberFormatError {}", msg),
            CustomError::InvalidArgumentError(msg) => 
                write!(f, "InvalidArgumentError {}", msg),
            CustomError::IOError(msg) => 
                write!(f, "IOError {}", msg),
        }
    }
}

impl Error for CustomError {}

impl From<std::io::Error> for CustomError {
    fn from(err: std::io::Error) -> Self {
        CustomError::IOError(err.to_string())
    }
}

impl From<std::num::ParseIntError> for CustomError {
    fn from(err: std::num::ParseIntError) -> Self {
        CustomError::NumberFormatError(err.to_string())
    }
}

impl From<std::num::ParseFloatError> for CustomError {
    fn from(err: std::num::ParseFloatError) -> Self {
        CustomError::NumberFormatError(err.to_string())
    }
}

pub fn variant_eq<T>(a: &T, b: &T) -> bool {
    std::mem::discriminant(a) == std::mem::discriminant(b)
}

pub fn log_mod() {
    println!("utils is imported and ready to go");
}

pub fn program_name() -> Option<String> {
    std::env::current_exe()
        .ok()?
        .file_name()?
        .to_str()?
        .to_owned()
        .into()
}

macro_rules! tuple_to_string2 {
    ($tuple:expr) => {
        {
            let s = format!("{:?}", $tuple);
            let len = s.len() - 1;
            "<".to_string() + &s[1..len] + ">"
        }
    };
}

pub trait TupleLen { fn len(&self) -> usize; } 
impl<T1> TupleLen for (T1,) { fn len(&self) -> usize { 1 } } 
impl<T1,T2> TupleLen for (T1,T2) { fn len(&self) -> usize { 2 } } 
impl<T1,T2,T3> TupleLen for (T1,T2,T3) { fn len(&self) -> usize { 3 } } 

pub trait ElementFormatter { fn format(&self) -> String; }

impl ElementFormatter for dyn Any {
    fn format(&self) -> String {
        format!("{:?}", self)
    }
}

impl ElementFormatter for bool {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for char {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for i8 {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for i16 {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for isize {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for i32 {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for i64 {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for u8 {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for u16 {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for usize {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for u32 {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for u64 {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for f64 {
    fn format(&self) -> String {
        format!("{}", self)
    }
}

impl ElementFormatter for &str {
    fn format(&self) -> String {
        format!("\"{}\"", self)
    }
}

impl ElementFormatter for String {
    fn format(&self) -> String {
        format!("\"{}\"", self)
    }
}

impl<T: ElementFormatter> ElementFormatter for Vec<T> {
    fn format(&self) -> String {
        let mut result = String::new();
        for value in self {
            let s = value.format();
            result.push_str(s.as_str());
            result.push_str(", ");
        }
        if !result.is_empty() {
            result.pop(); // remove trailing comma and space
            result.pop();
        }
        format!("[{0}]", result)
    }
}

impl<T: ElementFormatter, U: ElementFormatter> ElementFormatter for HashMap<T, U> {
    fn format(&self) -> String {
        let mut result = String::new();
        for (key, value) in self.iter() {
            result.push_str(&format!("{} => {}, ", 
                key.format(), 
                value.format()));
        }
        if !result.is_empty() {
            result.pop(); // remove trailing comma and space
            result.pop();
        }
        format!("{{{0}}}", result)
    }
}

impl<T1: ElementFormatter> ElementFormatter for (T1,) { 
    fn format(&self) -> String {
        let v0:String = self.0.format();
        "<".to_string() + &v0 + ">"
    } 
}

impl<T1: ElementFormatter, T2: ElementFormatter> ElementFormatter for (T1,T2) {
    fn format(&self) -> String { 
        let v0:String = self.0.format();
        let v1:String = self.1.format();
        "<".to_string() + &v0 + ", " + &v1 + ">"
    }
} 

impl<T1: ElementFormatter, T2: ElementFormatter, T3: ElementFormatter> ElementFormatter for (T1,T2,T3) {
    fn format(&self) -> String { 
        let v0:String = self.0.format();
        let v1:String = self.1.format();
        let v2:String = self.2.format();
        "<".to_string() + &v0 + ", " + &v1 + ", " + &v2 + ">"
    }
} 

impl<T1: ElementFormatter, T2: ElementFormatter, T3: ElementFormatter, T4: ElementFormatter> ElementFormatter for (T1,T2,T3,T4) {
    fn format(&self) -> String { 
        let v0:String = self.0.format();
        let v1:String = self.1.format();
        let v2:String = self.2.format();
        let v3:String = self.3.format();
        "<".to_string() + &v0 + ", " + &v1 + ", " + &v2 + ", " + &v3 + ">"
    }
} 

impl<T1: ElementFormatter, T2: ElementFormatter, T3: ElementFormatter, T4: ElementFormatter, T5: ElementFormatter> ElementFormatter for (T1,T2,T3,T4,T5) {
    fn format(&self) -> String { 
        let v0:String = self.0.format();
        let v1:String = self.1.format();
        let v2:String = self.2.format();
        let v3:String = self.3.format();
        let v4:String = self.4.format();
        "<".to_string() + &v0 + ", " + &v1 + ", " + &v2 + ", " + &v3 + ", " + &v4 + ">"
    }
} 

impl<T1: ElementFormatter, T2: ElementFormatter, T3: ElementFormatter, T4: ElementFormatter, T5: ElementFormatter, T6: ElementFormatter> ElementFormatter for (T1,T2,T3,T4,T5,T6) {
    fn format(&self) -> String { 
        let v0:String = self.0.format();
        let v1:String = self.1.format();
        let v2:String = self.2.format();
        let v3:String = self.3.format();
        let v4:String = self.4.format();
        let v5:String = self.5.format();
        "<".to_string() + &v0 + ", " + &v1 + ", " + &v2 + ", " + &v3 + ", " + &v4 + ", " + &v5 + ">"
    }
} 

macro_rules! list_to_string {
    ($list:expr) => {
        {
            use $crate::utils::ElementFormatter;
            $list.format()
        }
    };
}

macro_rules! map_to_string {
    ($map:expr) => {
        {
            use $crate::utils::ElementFormatter;
            $map.format()
        }
    };
}

macro_rules! tuple_to_string {
    ($tuple:expr) => {
        {
            use $crate::utils::ElementFormatter;
            $tuple.format()
        }
    };
}

// Allows enums to have a string context
macro_rules! enum_str {
    (enum $name:ident {
        $($variant:ident = $val:expr),*,
    }) => {
        #[derive(Debug, Eq, PartialEq, IntoPrimitive, TryFromPrimitive, Copy, Clone)]
        #[repr(isize)]
        enum $name {
            $($variant = $val),*
        }

        impl $name {
            fn name(&self) -> &'static str {
                match self {
                    $($name::$variant => stringify!($variant)),*
                }
            }
        }
        impl utils::ElementFormatter for $name {
            fn format(&self) -> String {
                format!("{}", self.name())
            }
        }
    };
}

pub fn regex_search(pattern: Regex, text: &str) -> bool {
    if let Some(_matched) = pattern.find(text) {
        return true;
    } else {
        return false;
    }
}

pub fn regex_find_first(pattern: Regex, text: &str) -> Vec<String> {
    let mut result = Vec::new();
    if let Some(captures) = pattern.captures(text) {
        for i in 0..captures.len() {
            if let Some(capture) = captures.get(i) {
                result.push(capture.as_str().to_string());
            }
        }
    }
    return result;
}

pub fn regex_find_all(pattern: Regex, text: &str) -> Vec<String> {
    let mut result = Vec::new();
    for captures in pattern.captures_iter(text) {
        for i in 0..captures.len() {
            if let Some(capture) = captures.get(i) {
                result.push(capture.as_str().to_string());
            }
        }
    }
    return result;
}

pub fn regex_replace_first(pattern: Regex, text: &str, replace_with: &str) -> String {
    pattern.replace(text, replace_with).to_string()
}

pub fn regex_replace_all(pattern: Regex, text: &str, replace_with: &str) -> String {
    pattern.replace_all(text, replace_with).to_string()
}

pub fn regex_split(pattern: Regex, text: &str) -> Vec<String> {
    pattern.split(text).map(|s| s.to_string()).collect()
}

pub fn file_size(fh: &mut File) -> i64 {
    let cur_pos = fh.stream_position();
    let file_size = fh.seek(SeekFrom::End(0)).expect("Can't seek to end of file");
    fh.seek(SeekFrom::Start(cur_pos.unwrap())).expect("Can't seek to last position of file");
    file_size as i64
}

pub fn get_byte<R: Read>(fh: &mut BufReader<R>) -> Option<u8> {
    let mut buffer = [0u8; 1];     // Allocate a buffer for a single byte

    match fh.read(&mut buffer) {
        Ok(0) => None,             // End of file
        Ok(_) => Some(buffer[0]),
        Err(_) => None,            // Handle errors by returning None
    }
}

pub fn get_codepoint<R: Read>(fh: &mut R) -> Option<u32> {
    let mut buffer = [0u8; 1];    // Buffer for reading one byte at a time

    while let Ok(bytes_read) = fh.read(&mut buffer) {
        if bytes_read == 0 {
            return None
        }

        let first_byte = buffer[0];

        // Determine the number of bytes in this UTF-8 sequence
        let mut codepoint:u32;
        let remaining_bytes = if first_byte & 0b1000_0000 == 0 {
            // 1-byte sequence (ASCII)
            codepoint = first_byte as u32;
            0
        } else if first_byte & 0b1110_0000 == 0b1100_0000 {
            // 2-byte sequence
            codepoint = (first_byte & 0b0001_1111) as u32;
            1
        } else if first_byte & 0b1111_0000 == 0b1110_0000 {
            // 3-byte sequence
            codepoint = (first_byte & 0b0000_1111) as u32;
            2
        } else if first_byte & 0b1111_1000 == 0b1111_0000 {
            // 4-byte sequence
            codepoint = (first_byte & 0b0000_0111) as u32;
            3
        } else {
            // Invalid UTF-8 start byte
            eprintln!("Error: Invalid UTF-8 start byte: 0x{:02X}", first_byte);
            continue;
        };

        // Read the continuation bytes
        for _ in 0..remaining_bytes {
            let bytes_read = fh.read(&mut buffer).unwrap_or(0);
            if bytes_read == 0 {
                eprintln!("Error: Unexpected EOF in the middle of a UTF-8 sequence");
                return None;
            }

            let continuation_byte = buffer[0];
            if continuation_byte & 0b1100_0000 != 0b1000_0000 {
                // Invalid continuation byte
                eprintln!("Error: Invalid UTF-8 continuation byte: 0x{:02X}", continuation_byte);
                return None;
            }

            codepoint = (codepoint << 6) | (continuation_byte & 0b0011_1111) as u32;
        }

        // Validate the codepoint
        if codepoint > 0x10FFFF || (codepoint >= 0xD800 && codepoint <= 0xDFFF) {
            eprintln!("Error: Invalid Unicode codepoint: U+{:X}", codepoint);
            return None;
        }

        return Some(codepoint);
    }

    return None;
}

pub fn put_codepoint<W: Write>(writer: &mut W, codepoint: u32) {
    // Convert the codepoint to a char, ensuring it's valid
    if let Some(character) = char::from_u32(codepoint) {
        // Convert the char to UTF-8 bytes and write it
        let _ = writer.write_all(character.to_string().as_bytes());
    } else {
        eprintln!("Invalid Unicode codepoint: U+{:X}", codepoint);
    }
}

pub fn prompt(message: &str) -> String {
    print!("{}", message);         // Print the prompt message
    stdout().flush().unwrap(); // Flush to ensure the message appears immediately
    
    let mut input = String::new();
    stdin()
        .read_line(&mut input)
        .expect("Failed to read line");
    
    input.trim().to_string()       // Trim whitespace and return the input
}
