MACHINE LibraryMath
/* This Library Machine provides a convenient access to the ProB
   external mathematical functions */
ABSTRACT_CONSTANTS
 sin, cos, tan,  /* trigonometric functions with standard precision multiplier of 100 */
 sinx, cosx, tanx, /* more flexible version; user can specify precision multiplier */
 /* SINx(Multiplier,x) = round(Multiplier*sin(x/Multiplier)) */
 logx,
 /* LOGx(Multiplier,base,x) = round(Multiplier*log(base/Multiplier,x/Multiplier)) */
 gcd, /* greatest common divisor */
 msb, /* most significant bit */
 abs, /* absolute value */
 random /* generate a random number in a range */
DEFINITIONS
 "LibraryMath.def"
PROPERTIES
  cos: NATURAL --> INTEGER &
  cos = %x.(x:NATURAL|COS(x)) /* This generates a "real" B function, where we can compute domain, compute image, ... */ &
  sin: INTEGER --> INTEGER &
  sin = %x.(x:INTEGER|SIN(x)) /* EXTERNAL */ &
  tan: INTEGER --> INTEGER &
  tan = %x.(x:INTEGER|TAN(x)) /* EXTERNAL */ &
  sinx: NATURAL1*INTEGER --> INTEGER &
  sinx = %(m,x).(x:INTEGER & m:NATURAL1|SINx(m,x)) /* EXTERNAL */  &
  cosx: NATURAL1*INTEGER --> INTEGER &
  cosx = %(m,x).(x:INTEGER & m:NATURAL1|COSx(m,x)) /* EXTERNAL */  &
  tanx: NATURAL1*INTEGER --> INTEGER &
  tanx = %(m,x).(x:INTEGER & m:NATURAL1|TANx(m,x)) /* EXTERNAL */  &
  logx: NATURAL1*NATURAL1*NATURAL1 --> INTEGER &
  logx = %(m,b,x).(x:NATURAL1 & b:NATURAL1 & m:NATURAL1|LOGx(m,b,x)) /* EXTERNAL */ & 
  gcd: INTEGER*INTEGER --> INTEGER &
  gcd = %(x,y).(x:INTEGER & y:INTEGER|GCD(x,y)) /* EXTERNAL */  &
  msb: NATURAL1 --> INTEGER &
  msb = %x.(x:NATURAL1|MSB(x)) /* EXTERNAL */  &
  abs: INTEGER --> INTEGER &
  abs = %x.(x:INTEGER|ABS(x)) /* EXTERNAL */  &
  
  /* warning: this is *not* a mathematical function */
  /* it may not work as expected; e.g.,  {x,y|x:1..20 & y=random(1,100)} will generate
     the same number over and over; you have to use this: {x,y|x:1..20 & y=random(1,100+x-x)} */
  random: INTEGER*INTEGER --> INTEGER &
  random = %(x,y).(x:INTEGER & y:INTEGER|RANDOM(x,y)) /* EXTERNAL */

END
