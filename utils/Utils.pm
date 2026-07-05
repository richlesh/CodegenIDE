package Utils;

use strict;
use Switch;
use POSIX qw(floor ceil);
use Exporter;
use vars qw($VERSION @ISA @EXPORT @EXPORT_OK %EXPORT_TAGS);
use TryCatch;
use Scalar::Util qw(looks_like_number);

$VERSION     = 1.00;
@ISA         = qw(Exporter);
@EXPORT      = qw(buildHeader isFinite ltrim messageFormat padAlign prompt rtrim
                simpleMessageFormat toBase toHex trim LEFT CENTER RIGHT);
@EXPORT_OK   = @EXPORT;
%EXPORT_TAGS = ( DEFAULT => \@EXPORT,
                 ALL    => \@EXPORT);

sub debug {
    my ($b, $msg) = @_;
    my @call_details = caller(0);
    if ($b) {
        print "debug: " . $call_details[1] . "(" . $call_details[2] . ") " . $msg . "\n";
    }
}

sub elementFormatter {
    my ($obj) = @_;
    if (ref($obj) eq "ARRAY") {
        return listToString($obj);
    } elsif (ref($obj) eq "HASH") {
        return mapToString($obj);
    } elsif (!ref($obj) && !looks_like_number($obj)) {
        return '"' . $obj . '"';
    } else {
        return $obj;
    }
}

sub listOrTupleToString {
    my ($list, $isTuple) = @_;
    my @formatted = map { elementFormatter($_) } @$list;
    return ($isTuple?"<":"[") . join(", ", @formatted) . ($isTuple?">":"]");
}

sub listToString {
    my ($list) = @_;
    return listOrTupleToString($list, 0);
}

sub tupleToString {
    my ($tuple) = @_;
    return listOrTupleToString($tuple, 1);
}

sub mapToString {
    my ($result) = "";
    while (my ($key, $value) = each (%{$_[0]})) {
        $result .= ", " if (length $result != 0);
        $result .= elementFormatter($key) . ' => ' . elementFormatter($value);
    }
    return "{" . $result . "}";
}

sub enumToString {
    my ($obj) = @_;
    return undef if (!defined($obj));
    return $obj->name;
}

sub isFinite {
    my ($x) = @_;
    my $s = lc($x);
    return 0 if ($s eq "nan" || $s eq "inf" || $s eq "-inf");
    return 1;
}

sub isEmpty {
    my ($x) = @_;
    return !defined($x) || length($x) == 0;
}

sub isBlank {
    my ($x) = @_;
    return !defined($x) || length(trim($x)) == 0;
}

sub trim  {
    my ($s) = @_;
    $s =~ s/^\s*|\s*$//mg;
    return $s;
}

sub ltrim  {
    my ($s) = @_;
    $s =~ s/^\s*//mg;
    return $s;
}

sub rtrim  {
    my ($s) = @_;
    $s =~ s/\s*$//mg;
    return $s;
}

# Bitwise complement doesn't work in Perl so use twos-complement minus 1
# Could also use a "use integer;" block
sub complement32 {
    my ($x) = @_;
    my $result;
    $result = -$x - 1;
    return arshift32($result, 0);
}

sub lshift32 {
    my ($x, $y) = @_;
    my $result;
    $result = $x << $y;
    return arshift32($result, 0);
}

sub rshift32 {
    my ($x, $y) = @_;
    my $result;
    $result = (($x & 0xFFFFFFFF) >> $y);
    return $result;
}

# Pads 32 bit integers with FF if high bit (31) set so that it is a negative perl value.
sub convertToSigned32 {
    my ($x) = @_;
    return arshift32($x, 0);
}

# Also used to normalize value to a negative number if high bit is set when shift amount is 0.
sub arshift32 {
    my ($x, $y) = @_;
    my $result;
    $x = $x & 0xFFFFFFFF;
    # if sign bit is set make into a perl negative value
    if ($x >= 0x80000000) {
        $x -= 4294967296;
    }
    $result = $x;
    if ($y != 0) {use integer; $result = $x >> $y;}
    return $result;
}

sub bitwiseAnd32 {
    my ($x, $y) = @_;
    my $result;
    {use integer; $result = $x & $y;}
    return arshift32($result, 0);
}

sub bitwiseOr32 {
    my ($x, $y) = @_;
    my $result;
    {use integer; $result = $x | $y;}
    return arshift32($result, 0);
}

sub bitwiseXOr32 {
    my ($x, $y) = @_;
    my $result;
    {use integer; $result = $x ^ $y;}
    return arshift32($result, 0);
}

# Arg is a FILEHANDLE
sub getbyte {
    my ($s) = @_;
    my $c;
    my $count = read($s, $c, 1);
    if ($count == 1) {
        return ord($c);
    } else {
        return undef;
    }
}

# Arg is a FILEHANDLE
sub getchar {
    my ($s) = @_;
    my $c = getc($s);
    if (defined($c)) {
        return ord($c);
    } else {
        return undef;
    }
}

sub prompt {
    my ($question) = @_;
    print $question;
    STDOUT->flush();
    my $answer = <STDIN>;
    chomp($answer);
    return $answer;
}

sub stoi {
    my ($s) = @_;
    my $i = int($s);
    if ($s =~ m/^[-+]?\d+$/) {
        return $i;
    } else {
        throw Error::NumberFormatException("Invalid Integer Number Format: " . $s);
    }
}

sub stod {
    my ($s) = @_;
    my $i = $s + 0.0;
    if ($s =~ m/^[-+]?\d*?(?:\d+\.|\.\d+)\d*(e[-+]\d+)?$/i) {
        return $i;
    } elsif ($s =~ m/^[-+]?\d+$/) {
        return $i;
    } else {
        throw Error::NumberFormatException("Invalid Floating Point Number Format: " . $s);
    }
}

sub stoiWithDefault {
    my ($s, $d) = @_;
    try {
        my $i = stoi($s);
        return $i;
    } otherwise {
        return $d;
    }
}

sub stodWithDefault {
    my ($s, $d) = @_;
    try {
        my $i = stod($s);
        return $i;
    } otherwise {
        return $d;
    }
}

sub toHex {
    my ($x) = @_;
    my @digits = ('0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F');
    $x = int($x);
    return "XXXXXXXX" if (!isFinite($x));
    
    my $result = "";
    for (my $i = 0; $i < 16; ++$i) {    # Perl ints can be 64 bits
        my $d = $x & 0xf;
        $result = $digits[$d] . $result;
        $x >>= 4;
    }
    return $result;
}

my @digits = ('0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z');

sub toBase {
    my ($x, $base) = @_;
    $x = -(~$x + 1) if ($x & 0x8000000000000000);
    $base = 10 if (! defined $base);
    return "XXXXXXXX" if (!isFinite($x));
    return "BaseError" if ($base < 2 || $base > 36);
    $x = int($x);
    return "0" if ($x == 0);
    my $result = "";
    my $neg = 0;
    if ($x < 0) { $x = -$x; $neg = 1; }
    while ($x > 0) {
        my $d = $x % $base;
        $result = $digits[$d] . $result;
        $x = floor($x / $base);
    }
    return $neg ? "-" . $result : $result;
}

my $digitsStr = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

sub fromBase {
    my ($str, $base) = @_;
    $str = uc($str);
    my $result = 0;
    my $neg = 0;
    foreach my $c (split //, $str) {
        if ($c eq '-') { $neg = 1; next; }
        my $pos = index($digitsStr, $c);
        if ($pos != -1) {
            $result *= $base;
            $result += $pos;
        }
    }
    if ($neg) { $result = -$result; }
    return $result;
}

our %PadAlignType = (
    LEFT => 0,
    CENTER => 1,
    RIGHT => 2
);

sub padAlign {
    my ($s, $width, $which, $c, $prefix, $prefix_front) = @_;
    $which = 0 if (! defined $which);
    $c = " " if (! defined $c);
    my $result = $s;
    $result = $prefix . $result if (defined $prefix);
    my $len = $width - length($result);
    $len -= length($prefix_front) if (defined $prefix_front);
    if ($len > 0) {
        switch ($which) {
            case 2    { $result = ($c x $len) . $result; }
            case 1  { $result = ($c x floor($len/2.)) . $result . ($c x ceil($len/2.)); }
            else    { $result = $result . ($c x $len); }
        }
    }
    $result = $prefix_front . $result if (defined($prefix_front));
    return $result;
}

sub buildHeader {
    my ($header, $c, $width) = @_;
    $c = "*" if (! defined $c);
    $width = 79 if (! defined $width);
    my $separator = $c x $width;
    return "\n${separator}\n${header}\n${separator}\n\n";
}

sub simpleMessageFormat {
    my ($fmt) = shift @_;
    $fmt =~ s/\{(\d+)\}/$_[$1]/gx;
    return $fmt;
}

sub messageFormat {
    my $format = shift @_;
    my @args = @_;

    $format =~ s/\{(\d+)(?::(?:(.)?([<>^]))?([- +])?(\#)?(0)?(\d+)?(?:\.(\d+))?([bBcdoxXeEfFgGs])?)?\}/formatReplacment($args[$1], $1, $2, $3, $4, $5, $6, $7, $8, $9)/ge;
    return $format;
}    
        
sub formatReplacment {
    my ($value, $pos, $fill, $align, $sign, $alt, $zero, $width, $precision, $type) = @_;
    my $result = "";
    my $fmt;
    $pos = int($pos) if (defined $pos);
    $precision = int($precision) if (defined $precision);
    if (! defined $type) { $type = "s"; }
    
    switch (lc($type)) {
        case 'b' { $result = toBase($value, 2); }
        case 'c' { 
            if (int($value) eq $value) {
                $result = chr($value)
            } else {
                $result = substr($value, 0, 1); 
            }
        }
        case 'd' { $result = toBase($value, 10); }
        case 'o' { $result = toBase($value, 8); }
        case 'x' { $result = toBase($value, 16); }
        case /^[efg]$/ {
            if (defined $precision) {
                $fmt = "%." . $precision . lc($type);
            } else {
                $fmt = "%" . lc($type);
            }
            $result = sprintf($fmt, $value);
        }
        else {
            $result = $value;
            $result = substr($result, 0, $precision) if (defined $precision);
        }
    }

    my $prefix = '';
    my $prefix_front;

    if (index("bdoxefgBXEFG", $type) >= 0 && substr($result, 0, 1) eq '-') {
        $prefix = '-';
        $result = substr($result, 1, length($result) - 1);
    }
    if (defined $sign && index("bdoxefgBXEFG", $type) >= 0) {
        $prefix = $sign if (index(" +", $sign) >= 0 && $prefix ne '-');
    }
    if (defined $alt && index("boxBX", $type) >= 0) {
        switch (lc($type)) {
            case 'b' { $prefix .= '0b'; }
            case 'o' { $prefix .= '0' if $result ne "0"; }
            case 'x' { $prefix .= '0x'; }
        }
    }

    my $padAlignchar = ' ';
    my $padAligntype = 2;
    if (defined $width) {
        if ($type eq 'c' || $type eq 's') {
            $padAligntype = 0;
        }
        if (defined $fill) {
            $padAlignchar = $fill;
        }
        if (defined $align) {
            switch ($align) {
                case '^' {$padAligntype = 1;}
                case '>' {$padAligntype = 2;}
                else {$padAligntype = 0;}
            }
        }
        if (defined $zero && index("bdoxBX", $type) >= 0) {
            $padAligntype = 2;
            $padAlignchar = '0';
            $prefix_front = $prefix;
            undef $prefix;
        }
        $result = padAlign($result, $width, $padAligntype, $padAlignchar, $prefix, $prefix_front);
    } else {
        $result = $prefix . $result if (defined $prefix);
        $result = $prefix_front . $result if (defined $prefix_front);
    }
    
    $result = uc($result) if (index("BXEFG", $type) >= 0);
    $result = lc($result) if (index("bxefg", $type) >= 0);
    return $result;
}

package Error::NumberFormatException;
use base "Error::Simple";

package Error::InvalidArgumentException;
use base "Error::Simple";

package Error::IOException;
use base "Error::Simple";

package Error::RuntimeException;
use base "Error::Simple";

1;
