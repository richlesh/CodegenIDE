grammar Macrodef;
@header {
    package org.pureprogrammer;
}
file: defs+ ;
defs: macroDef      # Definition
      | NL          # BlankLine
      ;

macroDef: ID '(' argType? (',' argType)* ')' '=>' returnType NL (MACROENTRY)* ;

argType:     (SCALAR|LIST|MAP|TUPLE|USERTYPE|ANY|ANYLIST|ANYMAP|ANYTUPLE|ANYUSERTYPE|VARARG) ;
returnType:  (SCALAR|LIST|MAP|TUPLE|USERTYPE|ANY|ANYLIST|ANYMAP|ANYTUPLE|ANYUSERTYPE|VOID) ;

SKIPCOMMENT  : '#' (~[\r\n])* NL -> skip ;
MACROENTRY:  ID ':' [ \t]* .*? [ \t]* NL ;
VOID         : 'void' ;
SCALAR       : '<' ID '*'? '>' '?'? ;
LIST         : '[' ID ']' '*'? '?'? ;
MAP          : '{' ID '|' ID '}' '*'? '?'? ;
TUPLE        : '«' ID ('|' ID)+ '»' '*'? '?'? ;
USERTYPE     : '≤' ID '»' '*'? '?'? ;
ANYLIST      : '[any]' '*'? '?'? ;
ANYMAP       : '{any}' '*'? '?'? ;
ANYTUPLE     : '«any»' '*'? '?'? ;
ANYUSERTYPE  : '≤any≥' '*'? '?'? ;
ANY	         : 'any' '*'? '?'? ;
VARARG       : '...' ;
ID           : [_a-zA-Z][_@0-9a-zA-Z<>{}[\]«»|*]* ;
NL           : '\r'? '\n' ;
WS           : [ \t]+ -> skip ;
