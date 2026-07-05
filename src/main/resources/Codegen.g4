grammar Codegen;
@header {
    package org.pureprogrammer;
}
file: stat+;
stat: sharedCommentBlock                                                # CommentBlock
      | sharedCommentStat                                               # CommentStat
      | 'Pragma:' name=ID ('=' value=(INT|ID))? NL                      # Pragma
      | 'ScalarDecl:' scalarDecl NL                                     # ScalarDeclStat
      | 'ListDecl:' listDecl2 NL                                        # ListDecl2Stat
      | 'ListDecl:' listDecl (NL 'EndListDecl')? NL                     # ListDeclStat
      | 'ListDecl:' listDecl3 NL                                        # ListDecl3Stat
      | 'MapDecl:' mapDecl2 NL                                          # MapDecl2Stat
      | 'MapDecl:' mapDecl (NL 'EndMapDecl')? NL                        # MapDeclStat
      | 'TupleDecl:' tupleDecl2 NL                                      # TupleDecl2Stat
      | 'TupleDecl:' tupleDecl (NL 'EndTupleDecl')? NL                  # TupleDeclStat
      | 'UnpackTuple:' tupleExpr=expr '->' expr (',' expr)+ NL          # UnpackTupleStat
      | 'UserTypeDecl:' userTypeDecl NL                                 # UserTypeDeclStat
      | sharedFunctionDef                                               # FunctionDef
      | sharedClassDef                                                  # ClassDef
      | sharedEnumDef                                                   # EnumDef
      | 'BeginProgram:' ID NL stat* 'EndProgram' NL?                    # Program
      | 'BeginMain:' NL stat* 'EndMain' NL                              # MainBlock
      | 'BeginBlock:' NL (stat)* 'EndBlock' NL                          # Block
      | ('Statement:'|'Expression:') expr NL                            # ExpressionStat
      | 'Assign:' assignExpr NL                                         # AssignStat
      | 'Print:' expr (',' expr)* NL                                    # PrintStat
      | 'PrintLine:' expr? (',' expr)* NL                               # PrintLineStat
      | ifClause elseIfClause* elseClause? 'EndIf' NL                   # If
      | ifEvalClause elseIfEvalClause* elseEvalClause? 'EndEval' NL     # Eval
      | 'BeginWhile:' expr NL (stat)+ 'EndWhile' NL                     # While
      | 'BeginDoWhile:' NL (stat)+ 'EndDoWhile:' expr NL                # DoWhile
      | 'BeginFor:' foreachType? newID 'in' range NL (stat)+ 'EndFor' NL   # DoForRange
      | 'BeginFor:' foreachType? forexpr NL (stat)+ 'EndFor' NL         # DoFor
      | 'BeginFor:' forexpr2 NL (stat)+ 'EndFor' NL                     # DoFor2
      | 'BeginForEach:' foreachType? newID 'in' expr NL (stat)+ 'EndForEach' NL      # DoForEach
      | 'BeginSwitch:' expr NL caseClause+ defaultClause? 'EndSwitch' NL          # Switch
      | 'BeginTry:' NL finallyClause? stat+ catchClause+ 'EndTry' NL              # TryCatch
      | 'Return:' expr? NL                                              # Return
      | 'Break' ':'? NL                                                 # Break
      | 'Continue' ':'? NL                                              # Continue
      | 'CanThrow:' (expr | assignExpr) NL                              # CanThrow
      | 'Throw:' (scalarType|userType) (':' expr)? NL                   # Throw
      | 'Assertion:' expr (':' expr)? NL                                # Assertion
      | 'Invariant:' expr (':' expr)? NL                                # Invariant
      | 'Precondition:' expr (':' expr)? NL                             # Precondition
      | 'Postcondition:' expr (':' expr)? NL                            # Postcondition
      | 'Debug:' (condExpr=expr ':')? expr (',' expr)* NL               # Debug
      | 'Delete:' expr NL                                               # DeleteObject
      | PASSTHROUGH                                                     # StatementPassthrough
      | NL                                                              # BlankLine
      ;

sharedCommentBlock: BLOCKCOMMENT ;
sharedCommentStat: COMMENT ;

sharedFunctionDef: 'BeginFunction:' isAsync=ASYNC? isStatic=STATIC? returnType newID '(' argDeclList ')' isFinal=FINAL? ('throws' exceptionTypes=scalarList)? NL stat* 'EndFunction' NL ;
argDeclClause: isInOut=INOUT? anyType newID ;
argDeclList:   NL? argDeclClause? (',' NL? argDeclClause)* (',' vararg='...')? ;
returnType:    (anyType|VOID) ;

sharedClassDef: 'BeginClass:' newID extendsClause? implementsClause? NL (classMember)+ 'EndClass' NL ;
extendsClause: 'extends' existingID ;
implementsClause: 'implements' existingID (',' existingID)* ;
classMember: sharedCommentBlock                                         # ClassCommentBlock
      | sharedCommentStat                                               # ClassCommentStat
      | 'Public:' NL                                                    # Public
      | 'Private:' NL                                                   # Private
      | 'Protected:' NL                                                 # Protected
      | 'ScalarDecl:' scalarDecl NL                                     # ClassScalarDeclStat
      | 'ListDecl:' listDecl2 NL                                        # ClassListDecl2Stat
      | 'ListDecl:' listDecl (NL 'EndListDecl')? NL                     # ClassListDeclStat
      | 'ListDecl:' listDecl3 NL                                        # ClassListDecl3Stat
      | 'MapDecl:' mapDecl2 NL                                          # ClassMapDecl2Stat
      | 'MapDecl:' mapDecl (NL 'EndMapDecl')? NL                        # ClassMapDeclStat
      | 'TupleDecl:' tupleDecl (NL 'EndTupleDecl')? NL                  # ClassTupleDeclStat
      | 'UserTypeDecl:' userTypeDecl NL                                 # ClassUserTypeDeclStat
      | sharedFunctionDef                                               # ClassFunctionDef
      | sharedEnumDef                                                   # ClassEnumDef
      | ifEvalClause elseIfEvalClause* elseEvalClause? 'EndEval' NL     # ClassEval
      | PASSTHROUGH                                                     # ClassPassthrough
      | NL                                                              # ClassBlankLine
      ;

sharedEnumDef: 'BeginEnum:' isStatic=STATIC? newID ':=' (','? scalarInit)+ 'EndEnum'? NL ;

ifClause:      'BeginIf:' expr NL stat+ ;
elseIfClause:  'ElseIf:' expr NL stat+ ;
elseClause:    'Else' ':'? NL stat+ ;

ifEvalClause:      'BeginEval:' evalExpr NL stat* ;
elseIfEvalClause:  'ElseIfEval:' evalExpr NL stat* ;
elseEvalClause:    'ElseEval' ':'? NL stat* ;

caseClause:    'Case:' expr (',' expr)* NL stat+ ;
defaultClause: 'Default' ':'? NL stat+ ;
catchClause:   'Catch:' (((scalarType|userType) newID)|catchAll='...' newID?) NL stat+ ;
finallyClause: ('BeginFinally'|'BeginDefer') ':'? NL stat+ ('EndFinally'|'EndDefer') NL ;

range:         ('FRANGE'|'IRANGE'|'XRANGE') '(' expr ',' expr (',' expr)? ')' ;
forexpr:       initExpr=assignExpr? ';' condExpr=expr ';' incrExpr=assignExpr? ;
forexpr2:      initExpr=expr? ';' condExpr=expr ';' incrExpr=expr? ;
foreachType:   (scalarType | userType | AUTO) ;

assignExpr:    <assoc=right> lvalue
               ('='|'*='|'/='|'%='|'+='|'-='|'<<='|'>>='|'>>>='|'&='|'^='|'|='|'&&&=')
               expr
               ;

lvalue:        variable                                 # VariableLValue
               | objectRef                              # ObjectRefLValue
               | staticMemberAccess                     # StaticMemberAccessLValue
               | lvalue '[' expr ']' forceUnwrap='!'?   # ListSubscriptLValue
               | lvalue '{' expr '}' forceUnwrap='!'?   # MapLookupLValue
               ;

scalarInit:    newID ('=' expr)? ;
scalarDecl:    isStatic=STATIC? isFinal=FINAL? isFinalOpt=FINAL_OPT? scalarType (','? scalarInit)+ ;
listDecl:      isStatic=STATIC? isFinal=FINAL? listType newID (':=' exprList)? ;
listDecl2:     isStatic=STATIC? isFinal=FINAL? listType newID '=' expr ;
listDecl3:     isStatic=STATIC? isFinal=FINAL? listType newID '[' expr ']' ;
mapDecl:       isStatic=STATIC? isFinal=FINAL? mapType newID (':=' NL? mapExprList)? ;
mapDecl2:      isStatic=STATIC? isFinal=FINAL? mapType newID '=' expr ;
tupleDecl:     isStatic=STATIC? isFinal=FINAL? tupleType newID (':=' exprList)? ;
tupleDecl2:    isStatic=STATIC? isFinal=FINAL? tupleType newID '=' expr ;
userTypeDecl:  isStatic=STATIC? isFinal=FINAL? userType (','? scalarInit)+ ;

objectRef:     isAwait=AWAIT? isTry=TRY? object '.' functionCall   # objectMethodAccess
               | object '.' existingID forceUnwrap='!'?            # objectMemberAccess
               | objectRef '.' functionCall                        # methodAccess
               | objectRef '.' existingID forceUnwrap='!'?		   # memberAccess
               ;
object: variable|listSubscript|mapLookup|tupleSubscript ;

staticMethodAccess: isAwait=AWAIT? isTry=TRY? existingID ('::' existingID)* '::' functionCall ;
staticMemberAccess: existingID ('::' existingID)+ ;
functionCall:  isAwait=AWAIT? isTry=TRY? existingID '(' expr0List ')' forceUnwrap='!'? ;

listSubscript: variable '[' expr ']' forceUnwrap='!'? ;
mapLookup:     variable '{' expr '}' forceUnwrap='!'? ;
tupleSubscript: variable '«' expr '»' forceUnwrap='!'? ;

exprList:      NL? expr (',' INLINECOMMENT? NL? expr)* INLINECOMMENT? ; 
expr0List:     NL? expr? (',' INLINECOMMENT? NL? expr)* INLINECOMMENT? ; 
mapExprList:   NL? mapItem (',' INLINECOMMENT? NL? mapItem)* INLINECOMMENT? ; 
mapItem:       expr '=>' expr ;
scalarList:    NL? scalarType? (',' NL? scalarType)* ; 

expr:          'format(' expr (',' expr)* ')'           # Format
               | 'string(' expr ')'                     # StringCast
               | 'char(' expr ')'                       # CharCast
               | 'byte(' expr ')'                       # ByteCast
               | 'short(' expr ')'                      # ShortCast
               | 'int(' expr ')'                        # IntegerCast
               | 'long(' expr ')'                       # LongCast
               | 'int16(' expr ')'                      # Int16Cast
               | 'int32(' expr ')'                      # Int32Cast
               | 'int64(' expr ')'                      # Int64Cast
               | 'double(' expr ')'                     # DoubleCast
               | 'isNull(' expr ')'                     # IsNull
               | 'listsize(' variable ')'               # ListSize
               | 'mapsize(' variable ')'                # MapSize
               | 'mapkeys(' variable ')'                # MapKeys
               | 'mapkeysAsList(' variable ')'          # MapKeysAsList
               | isTry=TRY? 'new' existingID '(' expr0List ')'  # NewObject
               | isTry=TRY? 'new' 'List' listType '(' expr0List ')'  # NewList
               | isTry=TRY? 'new' 'Map' mapType '(' mapExprList? ')'  # NewMap
               | objectRef                              # ObjectRefRValue
               | staticMethodAccess                     # StaticMethodAccessRValue
               | staticMemberAccess                     # StaticMemberAccessRValue
               | functionCall                           # FunctionCallRValue
               | expr '[' expr ']' forceUnwrap='!'?     # ListSubscriptRValue
               | expr '{' expr '}' forceUnwrap='!'?     # MapLookupRValue
               | expr '«' expr '»' forceUnwrap='!'?     # TupleSubscriptRValue
               | <assoc=right> expr '**' expr           # Exponentiation
               | <assoc=right> ('-'|'!'|'~'|'&') expr   # UnaryOps
               | expr ('*'|'//'|'/'|'%') expr           # MultOps
               | expr ('+'|'-') expr                    # AddOps
               | expr ('&&&' expr)+                     # StringCatOps
               | expr ('<<'|'>>>'|'>>') expr            # BitShiftOps
               | expr ('<'|'>'|'<='|'>=') expr          # ComparisonOps
               | expr ('lt'|'gt'|'le'|'ge') expr        # StringCompOps
               | expr ('=='|'!=') expr                  # EquivalenceOps
               | expr ('eq'|'ne') expr                  # StringEquivOps
               | expr ('=~'|'!~') expr                  # RegexCompareOps
               | expr ('&') expr                        # BitwiseAnd
               | expr ('|') expr                        # BitwiseOr
               | expr ('^') expr                        # BitwiseXor
               | expr ('&&') expr                       # LogicalAnd
               | expr ('||') expr                       # LogicalOr
               | <assoc=right> expr '?' expr ':' expr   # Ternary
               | '(' expr ')'                           # Parenthesis
               | variable                               # VariableRValue
               | BOOLEAN                                # Boolean
               | FLOAT                                  # Float
               | HEXINT                                 # HexInteger
               | INT                                    # Integer
               | CHAR                                   # Char
               | STRING                                 # String
               | BACKTICK_STRING                        # Backtick_String
               | REGEX                                  # Regex
               | PASSTHROUGH                            # Passthrough
               ;

evalExpr:      id                                       # EvalPragmaTrue
               | '!' id                                 # EvalPragmaFalse
               | BOOLEAN                                # EvalPragmaBoolean
               | id ('=='|'!=') INT                     # EvalPragmaIntEq
               | id ('eq'|'ne') STRING                  # EvalPragmaStrEq
               | id ('=~'|'!~') REGEX                   # EvalPragmaRegex
               ;

variable    : existingID forceUnwrap='!'? ;

scalarType  : '<' id isReference='*'? '>' isOptional='?'? isImplicit='!'? ;
listType    : '[' (anyType) '?'? ']' isOptional='?'? isImplicit='!'? ;
mapType     : '{' scalarType '|' (anyType) '}' isOptional='?'? isImplicit='!'? ;
tupleType   : '«' anyType ('|' anyType)+ '»' isOptional='?'? isImplicit='!'? ;
userType    : '≤' existingID '≥' isOptional='?'? isImplicit='!'? ;
anyType     : scalarType|listType|mapType|tupleType|userType ;
id          : ID ;
newID	    : ID ;
existingID  : ID ;

MULTILINECOMMENT: '/#' (MULTILINECOMMENT | ('#' ~'/') | ~'#')* '#/' -> skip ;
BLOCKCOMMENT : 'BeginComment:' .*? NL [ \t]* 'EndComment' ':'? NL ;
COMMENT      : 'Comment:' .*? NL ;
INLINECOMMENT : '/*' (.*?) '*/' ;
AUTO         : 'auto' ;
VOID         : 'void' ;
ASYNC        : 'async' ;
AWAIT        : 'await' ;
STATIC       : 'static' ;
FINAL        : 'const' | 'final';
FINAL_OPT    : 'Const' | 'Final';
INOUT        : 'inout' ;
TRY          : 'try' ;
BOOLEAN      : 'true'|'false' ;
ID           : [_\p{Alpha}][_\p{Alnum}]* ;
FLOAT        : ('-'? POSINT '.' FRAC? EXP? | INT EXP) ;
fragment FRAC: [0-9]* ;
fragment EXP : [eE]INT ;
fragment POSINT: ([0-9] | [1-9][0-9,]*[0-9]) ;
HEXINT       : '0' [xX] [0-9A-Fa-f]+;
INT          : ('-'? [1-9] | '-'? [1-9][0-9,]*[0-9] | '0') 'L'? ;
REGEX        : ('qr/' | 'm/') (ESC | '\\/' | ~[/\r\n])* '/' 'g'? 'i'?;
PASSTHROUGH  : '@@@' (.*?) '@@@' ;
BACKTICK_STRING : '`' (ESC | ~[`\r\n])* '`' ;
STRING       : ('f' | 'o')? ["] (ESC | ~["\r\n])* ["] ;
CHAR         : '\'' (ESC | ~['\r\n]) '\'' ;
ESC          : '\\\\' | '\\"' | '\\\'' | '\\' [0bNnrt] | UNICODE ;
UNICODE      : '\\u{' [0-9A-Fa-f]+ '}' ;
FORCED_NL    : 'NL' ;
NL           : '\r'? '\n' ;
SINGLELINECOMMENT : {getCharPositionInLine() == 0}? [ \t]* '#' ~[\r\n]* NL -> skip ;
EOLCOMMENT  : '#' ~[\r\n]* -> skip ;
WS           : [ \t]+ -> skip ;
ErrorCharacter : . ;
